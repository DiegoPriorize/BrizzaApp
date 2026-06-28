package com.priorizedev.brizza.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.priorizedev.brizza.data.model.Cliente
import com.priorizedev.brizza.data.model.Ambiente
import com.priorizedev.brizza.data.model.Equipamento
import com.priorizedev.brizza.data.model.Tecnico
import com.priorizedev.brizza.data.model.OrdemServico
import com.priorizedev.brizza.data.model.PmocReport
import com.priorizedev.brizza.data.model.PmocLogbookEntry
import com.priorizedev.brizza.data.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ClimaGestViewModel(private val repository: AppRepository, private val context: android.content.Context) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("climagest_prefs", android.content.Context.MODE_PRIVATE)

    private fun getDeletedIdsFromPrefs(): Set<String> {
        return sharedPrefs.getStringSet("tracked_deleted_ids", emptySet()) ?: emptySet()
    }

    private fun addDeletedIdToPrefs(table: String, id: String) {
        val currentSet = getDeletedIdsFromPrefs().toMutableSet()
        currentSet.add("$table:$id")
        sharedPrefs.edit().putStringSet("tracked_deleted_ids", currentSet).apply()
    }

    private fun removeDeletedIdFromPrefs(entry: String) {
        val currentSet = getDeletedIdsFromPrefs().toMutableSet()
        if (currentSet.remove(entry)) {
            sharedPrefs.edit().putStringSet("tracked_deleted_ids", currentSet).apply()
        }
    }

    private suspend fun syncDeletedRecordsToSupabase() {
        val deletedEntries = getDeletedIdsFromPrefs()
        if (deletedEntries.isEmpty()) return
        val entriesToProcess = deletedEntries.toList()
        entriesToProcess.forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val table = parts[0]
                val id = parts[1]
                val success = try {
                    when (table) {
                        "clientes" -> com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteCliente(id)
                        "ambientes" -> com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteAmbiente(id)
                        "equipamentos" -> com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteEquipamento(id)
                        "tecnicos" -> com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteTecnico(id)
                        "ordens_servico" -> com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteOrdemServico(id)
                        "pmoc_reports" -> com.priorizedev.brizza.data.api.SupabaseSyncClient.deletePmocReport(id)
                        "pecas" -> com.priorizedev.brizza.data.api.SupabaseSyncClient.deletePeca(id)
                        "programas_preventivos" -> com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteProgramaPreventivo(id)
                        "marcas_equipamentos" -> com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteMarcaEquipamento(id)
                        else -> false
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ClimaGestViewModel", "Erro ao deletar remoto de $table: $id: ${e.message}")
                    false
                }
                if (success) {
                    removeDeletedIdFromPrefs(entry)
                }
            }
        }
    }

    // Theme state (null = system default, true = dark, false = light)
    private val _themeState = MutableStateFlow<Boolean?>(null)
    val themeState: StateFlow<Boolean?> = _themeState.asStateFlow()

    fun updateTheme(isDark: Boolean?) {
        _themeState.value = isDark
        if (isDark != null) {
            sharedPrefs.edit().putBoolean("is_dark_theme", isDark).apply()
            
            // Também salva no escopo local do usuário logado local/remoto se aplicável
            if (_isLoggedIn.value && _userEmail.value.isNotBlank()) {
                viewModelScope.launch {
                    try {
                        val currentEmail = _userEmail.value
                        val user = repository.getUsuarioByEmail(currentEmail)
                        if (user != null) {
                            val updatedUser = user.copy(isDarkMode = isDark)
                            repository.insertUsuario(updatedUser)
                            triggerAutoSync()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ClimaGestViewModel", "Erro ao salvar tema no usuário", e)
                    }
                }
            }
        } else {
            sharedPrefs.edit().remove("is_dark_theme").apply()
        }
    }

    // User Profile state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _premiumAtivo = MutableStateFlow(false)
    val premiumAtivo: StateFlow<Boolean> = _premiumAtivo.asStateFlow()

    private val _userPlan = MutableStateFlow("FREE")
    val userPlan: StateFlow<String> = _userPlan.asStateFlow()

    private val _syncState = MutableStateFlow("Pendente")
    val syncState: StateFlow<String> = _syncState.asStateFlow()

    private val _shouldOpenNewOrderFlow = MutableStateFlow(false)
    val shouldOpenNewOrderFlow: StateFlow<Boolean> = _shouldOpenNewOrderFlow.asStateFlow()

    private val _preselectedEquipamentoForNewOrder = MutableStateFlow<com.priorizedev.brizza.data.model.Equipamento?>(null)
    val preselectedEquipamentoForNewOrder: StateFlow<com.priorizedev.brizza.data.model.Equipamento?> = _preselectedEquipamentoForNewOrder.asStateFlow()

    fun requestNewOrderFlow(equipamento: com.priorizedev.brizza.data.model.Equipamento?) {
        _preselectedEquipamentoForNewOrder.value = equipamento
        _shouldOpenNewOrderFlow.value = true
    }

    fun clearNewOrderFlow() {
        _shouldOpenNewOrderFlow.value = false
        _preselectedEquipamentoForNewOrder.value = null
    }

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _dataRegistro = MutableStateFlow("")
    val dataRegistro: StateFlow<String> = _dataRegistro.asStateFlow()

    private val _ultimoAcesso = MutableStateFlow<String?>(null)
    val ultimoAcesso: StateFlow<String?> = _ultimoAcesso.asStateFlow()

    // Company Settings
    private val _companyName = MutableStateFlow(sharedPrefs.getString("company_name", "Ar-Control Climatização") ?: "Ar-Control Climatização")
    val companyName: StateFlow<String> = _companyName.asStateFlow()

    private val _companyCnpj = MutableStateFlow(sharedPrefs.getString("company_cnpj", "12.345.678/0001-90") ?: "12.345.678/0001-90")
    val companyCnpj: StateFlow<String> = _companyCnpj.asStateFlow()

    private val _companyPhone = MutableStateFlow(sharedPrefs.getString("company_phone", "(11) 98765-4321") ?: "(11) 98765-4321")
    val companyPhone: StateFlow<String> = _companyPhone.asStateFlow()

    private val _companyEmail = MutableStateFlow(sharedPrefs.getString("company_email", "contato@arcontrol.com.br") ?: "contato@arcontrol.com.br")
    val companyEmail: StateFlow<String> = _companyEmail.asStateFlow()

    private val _companyLogoPreset = MutableStateFlow(sharedPrefs.getString("company_logo_preset", "❄️ Clima Neve") ?: "❄️ Clima Neve")
    val companyLogoPreset: StateFlow<String> = _companyLogoPreset.asStateFlow()

    fun updateCompanySettings(name: String, cnpj: String, phone: String, email: String, logoPreset: String) {
        _companyName.value = name
        _companyCnpj.value = cnpj
        _companyPhone.value = phone
        _companyEmail.value = email
        _companyLogoPreset.value = logoPreset

        sharedPrefs.edit()
            .putString("company_name", name)
            .putString("company_cnpj", cnpj)
            .putString("company_phone", phone)
            .putString("company_email", email)
            .putString("company_logo_preset", logoPreset)
            .apply()
    }

    fun registrarAcesso() {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        val currentAccessStr = sdf.format(java.util.Date())
        _ultimoAcesso.value = currentAccessStr
        sharedPrefs.edit().putString("last_access_time", currentAccessStr).apply()
        
        // Se estiver logado, atualiza o campo 'ultimoAcesso' no banco local e remoto
        val email = _userEmail.value
        if (email.isNotBlank()) {
            viewModelScope.launch {
                try {
                    val user = repository.getUsuarioByEmail(email)
                    if (user != null) {
                        val updatedUser = user.copy(ultimoAcesso = currentAccessStr)
                        repository.insertUsuario(updatedUser)
                        com.priorizedev.brizza.data.api.SupabaseSyncClient.syncUsuario(updatedUser)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ClimaGestViewModel", "Erro ao registrar acesso no banco de dados", e)
                }
            }
        }
    }

    init {
        // Carrega e atualiza o histórico do último acesso ao app (mesmo sem estar logado)
        val lastAccess = sharedPrefs.getString("last_access_time", null)
        _ultimoAcesso.value = lastAccess

        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        val currentAccessStr = sdf.format(java.util.Date())
        sharedPrefs.edit().putString("last_access_time", currentAccessStr).apply()

        // Inicializa logs de atualização locais
        viewModelScope.launch {
            seedDefaultLogsIfNeeded()
        }

        // 1. Carrega imediatamente o tema local salvo (evita atraso visual na reinicialização)
        val savedTheme = if (sharedPrefs.contains("is_dark_theme")) {
            sharedPrefs.getBoolean("is_dark_theme", false)
        } else {
            null
        }
        _themeState.value = savedTheme

        // 2. Login automático se o usuário não fez logout da última vez
        val savedEmail = sharedPrefs.getString("logged_in_email", null)
        if (!savedEmail.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    val user = repository.getUsuarioByEmail(savedEmail)
                    if (user != null) {
                        updateUserSessionState(user)
                        _isLoggedIn.value = true
                        
                        // Sincroniza o modo escuro salvo no perfil do usuário
                        _themeState.value = user.isDarkMode
                        sharedPrefs.edit().putBoolean("is_dark_theme", user.isDarkMode).apply()
                        
                        // Salva e sincroniza o timestamp atual de acesso no perfil do usuário
                        val updatedUser = user.copy(ultimoAcesso = currentAccessStr)
                        repository.insertUsuario(updatedUser)
                        com.priorizedev.brizza.data.api.SupabaseSyncClient.syncUsuario(updatedUser)

                        // Sincronização inicial em background
                        triggerAutoSync()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ClimaGestViewModel", "Erro ao recuperar sessão persistente", e)
                }
            }
        }

        // Run a periodic sync every 30 minutes if logged in to keep everything auto synchronized
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1800000)
                if (_isLoggedIn.value) {
                    try {
                        syncDeletedRecordsToSupabase()
                        val deletedRaw = getDeletedIdsFromPrefs()
                        val deletedIdsSet = deletedRaw.map { it.substringAfter(":") }.toSet()
                        com.priorizedev.brizza.data.api.SupabaseSyncClient.syncAllData(repository.database)
                        com.priorizedev.brizza.data.api.SupabaseSyncClient.downloadAllData(repository.database, _userEmail.value, deletedIdsSet)
                        
                        // Reload user state post daily/periodic sync
                        val user = repository.getUsuarioByEmail(_userEmail.value)
                        if (user != null) {
                            updateUserSessionState(user)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ClimaGestViewModel", "AutoSync background failed: ${e.message}")
                    }
                }
            }
        }
    }

    private fun updateUserSessionState(user: com.priorizedev.brizza.data.model.Usuario) {
        _userName.value = user.nome
        _userEmail.value = user.email
        _dataRegistro.value = user.dataRegistro
        
        val manual = user.planoManual.lowercase(java.util.Locale.getDefault()).trim()
        val isPremium = when (manual) {
            "premium_manual", "teste_premium" -> true
            else -> user.premiumAtivo
        }
        _premiumAtivo.value = isPremium
        
        _userPlan.value = when (manual) {
            "premium_manual" -> "PREMIUM_MANUAL"
            "teste_premium" -> "TESTE_PREMIUM"
            else -> if (user.premiumAtivo) "PREMIUM" else "FREE"
        }
    }

    private fun generateRandomId(length: Int = 15): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    fun triggerAutoSync() {
        viewModelScope.launch {
            if (_isLoggedIn.value && _userEmail.value.isNotBlank()) {
                val email = _userEmail.value
                try {
                    _syncState.value = "Sincronizando..."
                    syncDeletedRecordsToSupabase()
                    val deletedRaw = getDeletedIdsFromPrefs()
                    val deletedIdsSet = deletedRaw.map { it.substringAfter(":") }.toSet()
                    
                    // 1. Send local offline changes first
                    com.priorizedev.brizza.data.api.SupabaseSyncClient.syncAllData(repository.database)
                    // 2. Fetch the latest online data from Supabase
                    com.priorizedev.brizza.data.api.SupabaseSyncClient.downloadAllData(repository.database, email, deletedIdsSet)
                    
                    // Reload user state post auto sync to update plano_manual / premium
                    val user = repository.getUsuarioByEmail(email)
                    if (user != null) {
                        updateUserSessionState(user)
                    }

                    // 3. Seed default brands if they are empty
                    seedDefaultBrandsIfNeeded(email)
                    seedDefaultLogsIfNeeded()
                    _syncState.value = "Sincronizado"
                } catch (e: Exception) {
                    _syncState.value = "Erro"
                    android.util.Log.e("ClimaGestViewModel", "triggerAutoSync failed: ${e.message}")
                }
            }
        }
    }

    private suspend fun seedDefaultBrandsIfNeeded(email: String) {
        try {
            val existing = repository.allMarcasEquipamentosRaw.first()
            if (existing.isEmpty()) {
                val defaultBrandsList = listOf(
                    "Agratto", "Carrier", "Comfee", "Consul", "Daikin", 
                    "Elgin", "Electrolux", "Fujitsu", "Gree", "Hisense", 
                    "Hitachi", "Komeco", "Midea", "Philco", 
                    "Samsung", "Springer", "TCL", "York"
                )
                val entities = defaultBrandsList.map { brandName ->
                    com.priorizedev.brizza.data.model.MarcaEquipamento(
                        nome = brandName,
                        usuarioEmail = ""
                    )
                }
                repository.insertMarcasEquipamentos(entities)
                // Upload newly seeded brands
                try {
                    com.priorizedev.brizza.data.api.SupabaseSyncClient.syncAllData(repository.database)
                } catch (ex: Exception) {
                    android.util.Log.e("ClimaGestViewModel", "Error uploading seeded brands: ${ex.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ClimaGestViewModel", "Error in seedDefaultBrandsIfNeeded: ${e.message}")
        }
    }

    private suspend fun seedDefaultLogsIfNeeded() {
        try {
            val existing = repository.allLogsVersao.first()
            if (existing.isEmpty()) {
                val defaults = listOf(
                    com.priorizedev.brizza.data.model.LogVersao(
                        id = 2,
                        versao = "v2.5.0-pro",
                        data = "07/06/2026",
                        alteracoes = "- Cadastro de Equipamentos: Menu de marcas aprimorado com design moderno e inclusão de botão 'Cadastrar Nova Marca' diretamente no formulário.\n" +
                                "- Modos de Visualização: Telas de seleção de ambiente, capacidade térmica e tipos de aparelhos redesenhadas com componentes modernos do Material Design 3.\n" +
                                "- Ordens de Serviço: Visual de submenus suspensos aprimorado e modernizado para simplificar seleção de clientes, técnicos e máquinas correspondentes.\n" +
                                "- PMOC: Painel 'Gerenciar Programa' amplamente reformulado, exibindo status mais elegante e controle otimizado das notificações.\n" +
                                "- Assinaturas: Badge moderno exibindo status do plano ativo estruturado profissionalmente com cores de fácil escaneabilidade.\n" +
                                "- Logs Técnicos: Central centralizada com histórico detalhado das melhorias e novidades implementadas no sistema."
                    ),
                    com.priorizedev.brizza.data.model.LogVersao(
                        id = 1,
                        versao = "v2.4.1-pro",
                        data = "29/05/2026",
                        alteracoes = "- Mecanismo de sincronização offline-first aprimorado com relatórios de status em tempo real.\n" +
                                "- Integração robusta com faturamento inteligente via Stripe Checkout.\n" +
                                "- Correções de usabilidade, otimização de imagens de ordens técnicas e melhoria de carregamento offline."
                    )
                )
                repository.insertLogsVersao(defaults)
            }
        } catch (e: Exception) {
            android.util.Log.e("ClimaGestViewModel", "Error in seedDefaultLogsIfNeeded: ${e.message}")
        }
    }

    fun loginUser(emailInput: String, passwordInput: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                // 1. Authenticate with Supabase Auth online
                val authRes = com.priorizedev.brizza.data.api.SupabaseSyncClient.signInAuth(emailInput, passwordInput)
                var user: com.priorizedev.brizza.data.model.Usuario? = null

                when (authRes) {
                    is com.priorizedev.brizza.data.api.AuthResult.Success -> {
                        // Online auth succeeded! Try to get profile from local DB or Supabase.
                        user = repository.getUsuarioByEmail(emailInput)
                        if (user == null) {
                            val fetchedUser = com.priorizedev.brizza.data.api.SupabaseSyncClient.fetchUsuarioByEmail(emailInput)
                            if (fetchedUser != null) {
                                repository.insertUsuario(fetchedUser)
                                user = fetchedUser
                            } else {
                                // AUTORECUPERAÇÃO: Caso autenticado com sucesso mas sem perfil nas tabelas, cria um perfil básico autogerado
                                val defaultName = emailInput.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                                val generatedId = generateRandomId(15)
                                val df = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                val currentDate = df.format(java.util.Date())
                                
                                val recoveredUser = com.priorizedev.brizza.data.model.Usuario(
                                    email = emailInput,
                                    nome = defaultName,
                                    usuarioId = generatedId,
                                    premiumAtivo = false,
                                    planoAtivo = "FREE",
                                    isDarkMode = _themeState.value ?: false,
                                    dataRegistro = currentDate
                                )
                                repository.insertUsuario(recoveredUser)
                                com.priorizedev.brizza.data.api.SupabaseSyncClient.syncUsuario(recoveredUser)
                                user = recoveredUser
                            }
                        }
                    }
                    is com.priorizedev.brizza.data.api.AuthResult.NotConfigured -> {
                        // Não configurada - tentar login offline
                        user = repository.getUsuarioByEmail(emailInput)
                        if (user == null) {
                            onResult(false, "As credenciais do Supabase não foram configuradas. Por favor, adicione SUPABASE_URL e SUPABASE_ANON_KEY no painel de Secrets do AI Studio.")
                            return@launch
                        }
                    }
                    is com.priorizedev.brizza.data.api.AuthResult.NetworkError -> {
                        // Rede indisponível - tentar login offline
                        user = repository.getUsuarioByEmail(emailInput)
                        if (user == null) {
                            onResult(false, "Erro de rede e usuário não encontrado localmente. Verifique sua conexão com a internet ou as credenciais.")
                            return@launch
                        }
                    }
                    is com.priorizedev.brizza.data.api.AuthResult.Error -> {
                        onResult(false, authRes.message)
                        return@launch
                    }
                }

                if (user == null) {
                    onResult(false, "Usuário autenticado, mas registro de perfil não encontrado.")
                } else {
                    updateUserSessionState(user)
                    
                    // Salva e atualiza preferências no dispositivo local
                    _themeState.value = user.isDarkMode
                    sharedPrefs.edit()
                        .putString("logged_in_email", user.email)
                        .putBoolean("is_dark_theme", user.isDarkMode)
                        .apply()
                        
                    _isLoggedIn.value = true
                    
                    // Trigger autoSync in background
                    triggerAutoSync()
                    
                    onResult(true, "Bem-vindo de volta, ${user.nome}!")
                }
            } catch (e: Exception) {
                android.util.Log.e("ClimaGestViewModel", "Erro ao fazer login", e)
                onResult(false, "Erro ao acessar banco de dados: ${e.localizedMessage}")
            }
        }
    }

    fun registerUser(nomeInput: String, emailInput: String, passwordInput: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                var existing = repository.getUsuarioByEmail(emailInput)
                if (existing == null) {
                    existing = com.priorizedev.brizza.data.api.SupabaseSyncClient.fetchUsuarioByEmail(emailInput)
                }
                
                if (existing != null) {
                    onResult(false, "Este e-mail já está cadastrado.")
                } else {
                    // Try to register user in Supabase Auth first
                    val signUpRes = com.priorizedev.brizza.data.api.SupabaseSyncClient.signUpAuth(emailInput, passwordInput)
                    
                    when (signUpRes) {
                        is com.priorizedev.brizza.data.api.AuthResult.Success -> {
                            val generatedId = generateRandomId(15)
                            val df = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                            val currentDate = df.format(java.util.Date())

                            val newUser = com.priorizedev.brizza.data.model.Usuario(
                                email = emailInput,
                                nome = nomeInput,
                                usuarioId = generatedId,
                                premiumAtivo = false,
                                planoAtivo = "FREE",
                                isDarkMode = _themeState.value ?: false,
                                dataRegistro = currentDate
                            )
                            // Salva localmente primeiro
                            repository.insertUsuario(newUser)
                            
                            // Cria imediatamente a linha do perfil de usuário na tabela 'usuarios' do Supabase de forma direta
                            com.priorizedev.brizza.data.api.SupabaseSyncClient.syncUsuario(newUser)

                            updateUserSessionState(newUser)
                            
                            // Salva preferências para mantê-lo logado automaticamente
                            sharedPrefs.edit()
                                .putString("logged_in_email", emailInput)
                                .putBoolean("is_dark_theme", _themeState.value ?: false)
                                .apply()
                                
                            _isLoggedIn.value = true
                            
                            // Sincroniza em segundo plano os outros dados
                            viewModelScope.launch {
                                try {
                                    com.priorizedev.brizza.data.api.SupabaseSyncClient.syncAllData(repository.database)
                                } catch (syncEx: Exception) {
                                    android.util.Log.e("ClimaGestViewModel", "Erro ao sincronizar pós-cadastro: ${syncEx.message}", syncEx)
                                }
                            }
                            
                            onResult(true, "Conta criada com sucesso!")
                        }
                        is com.priorizedev.brizza.data.api.AuthResult.NetworkError, is com.priorizedev.brizza.data.api.AuthResult.NotConfigured -> {
                            val generatedId = generateRandomId(15)
                            val df = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                            val currentDate = df.format(java.util.Date())

                            val newUser = com.priorizedev.brizza.data.model.Usuario(
                                email = emailInput,
                                nome = nomeInput,
                                usuarioId = generatedId,
                                premiumAtivo = false,
                                planoAtivo = "FREE",
                                isDarkMode = _themeState.value ?: false,
                                dataRegistro = currentDate
                            )
                            // Salva localmente
                            repository.insertUsuario(newUser)

                            updateUserSessionState(newUser)
                            
                            sharedPrefs.edit()
                                .putString("logged_in_email", emailInput)
                                .putBoolean("is_dark_theme", _themeState.value ?: false)
                                .apply()
                                
                            _isLoggedIn.value = true

                            val msg = if (signUpRes is com.priorizedev.brizza.data.api.AuthResult.NetworkError) {
                                "Cadastro realizado localmente (Sem conexão à Internet). Os dados serão sincronizados futuramente!"
                            } else {
                                "Cadastro realizado localmente com sucesso! (Supabase não configurado)."
                            }
                            onResult(true, msg)
                        }
                        is com.priorizedev.brizza.data.api.AuthResult.Error -> {
                            onResult(false, signUpRes.message)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ClimaGestViewModel", "Erro ao cadastrar usuário", e)
                onResult(false, "Erro ao cadastrar: ${e.localizedMessage ?: "falha no banco de dados"}")
            }
        }
    }

    fun logout() {
        // Limpa a credencial de login do SharedPreferences para o login automático parar até que entre novamente
        sharedPrefs.edit().remove("logged_in_email").apply()
        
        _isLoggedIn.value = false
        _userName.value = ""
        _userEmail.value = ""
        _premiumAtivo.value = false
        _userPlan.value = "FREE"
    }

    fun syncWithSupabase(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _syncState.value = "Sincronizando"
            val email = _userEmail.value
            syncDeletedRecordsToSupabase()
            val deletedRaw = getDeletedIdsFromPrefs()
            val deletedIdsSet = deletedRaw.map { it.substringAfter(":") }.toSet()
            
            // 1. Upload local data to Supabase
            val resUpload = com.priorizedev.brizza.data.api.SupabaseSyncClient.syncAllData(repository.database)
            
            if (resUpload is com.priorizedev.brizza.data.api.SyncResult.Success) {
                // 2. If logged in, download updated data from Supabase
                if (email.isNotBlank()) {
                    val resDownload = com.priorizedev.brizza.data.api.SupabaseSyncClient.downloadAllData(repository.database, email, deletedIdsSet)
                    if (resDownload is com.priorizedev.brizza.data.api.SyncResult.Success) {
                        // Seed default brands if they are empty
                        seedDefaultBrandsIfNeeded(email)
                        seedDefaultLogsIfNeeded()
                        _syncState.value = "Sucesso"
                        onResult(true, "Sincronização bidirecional concluída com sucesso!")
                    } else if (resDownload is com.priorizedev.brizza.data.api.SyncResult.Error) {
                        _syncState.value = "Erro"
                        onResult(false, "Envio concluído, mas download falhou: ${resDownload.message}")
                    } else {
                        _syncState.value = "Sucesso"
                        onResult(true, "Envio concluído com sucesso!")
                    }
                } else {
                    _syncState.value = "Sucesso"
                    onResult(true, "Sincronização realizada com sucesso!")
                }
            } else if (resUpload is com.priorizedev.brizza.data.api.SyncResult.NotConfigured) {
                _syncState.value = "Não Configurado"
                onResult(false, "Supabase não está configurado. Ajuste os dados!")
            } else if (resUpload is com.priorizedev.brizza.data.api.SyncResult.Error) {
                _syncState.value = "Erro"
                onResult(false, resUpload.message)
            }
        }
    }

    fun updateProfile(name: String, email: String) {
        _userName.value = name
        _userEmail.value = email
    }

    // Clientes list
    val clientesState: StateFlow<List<Cliente>> = combine(repository.allClientes, _userEmail) { list, email ->
        list.filter { it.usuarioEmail == email || it.usuarioEmail.isBlank() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Equipamentos list
    val equipamentosState: StateFlow<List<Equipamento>> = combine(repository.allEquipamentos, _userEmail) { list, email ->
        list.filter { it.usuarioEmail == email || it.usuarioEmail.isBlank() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Ambientes list
    val ambientesState: StateFlow<List<Ambiente>> = combine(repository.allAmbientes, _userEmail) { list, email ->
        list.filter { it.usuarioEmail == email || it.usuarioEmail.isBlank() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Tecnicos list
    val tecnicosState: StateFlow<List<Tecnico>> = combine(repository.allTecnicos, _userEmail) { list, email ->
        list.filter { it.usuarioEmail == email || it.usuarioEmail.isBlank() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Ordens de Serviço list
    val ordensServicoState: StateFlow<List<OrdemServico>> = combine(repository.allOrdensServico, _userEmail) { list, email ->
        list.filter { it.usuarioEmail == email || it.usuarioEmail.isBlank() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // PMOC Reports list
    val pmocReportsState: StateFlow<List<PmocReport>> = combine(repository.allPmocReports, _userEmail) { list, email ->
        list.filter { it.usuarioEmail == email || it.usuarioEmail.isBlank() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Peças list
    val pecasState: StateFlow<List<com.priorizedev.brizza.data.model.Peca>> = combine(repository.allPecas, _userEmail) { list, email ->
        list.filter { it.usuarioEmail == email || it.usuarioEmail.isBlank() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Programas Preventivos list
    val programasPreventivosState: StateFlow<List<com.priorizedev.brizza.data.model.ProgramaPreventivo>> = combine(repository.allProgramasPreventivos, _userEmail) { list, email ->
        list.filter { it.usuarioEmail == email || it.usuarioEmail.isBlank() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Marcas de Equipamentos list
    val marcasEquipamentosState: StateFlow<List<com.priorizedev.brizza.data.model.MarcaEquipamento>> = combine(repository.allMarcasEquipamentosRaw, _userEmail) { list, email ->
        list.filter { it.usuarioEmail == email || it.usuarioEmail.isNullOrBlank() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Logs de Versão list
    val logsVersaoState: StateFlow<List<com.priorizedev.brizza.data.model.LogVersao>> = repository.allLogsVersao.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _limitationMessage = MutableStateFlow<String?>(null)
    val limitationMessage: StateFlow<String?> = _limitationMessage.asStateFlow()

    fun clearLimitationMessage() {
        _limitationMessage.value = null
    }

    fun triggerLimitation(message: String) {
        _limitationMessage.value = message
    }

    fun togglePremiumMock() {
        viewModelScope.launch {
            val email = _userEmail.value
            if (email.isNotBlank()) {
                val user = repository.getUsuarioByEmail(email)
                if (user != null) {
                    val updatedUser = user.copy(
                        premiumAtivo = !user.premiumAtivo,
                        planoAtivo = if (!user.premiumAtivo) "PREMIUM" else "FREE"
                    )
                    repository.insertUsuario(updatedUser)
                    updateUserSessionState(updatedUser)
                    triggerAutoSync()
                }
            }
        }
    }

    private val _stripeChecking = MutableStateFlow(false)
    val stripeChecking: StateFlow<Boolean> = _stripeChecking.asStateFlow()

    fun verificarAssinaturaStripe(onResult: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val email = _userEmail.value
            if (email.isBlank() || !_isLoggedIn.value) {
                onResult?.invoke("Usuário não está devidamente autenticado.")
                return@launch
            }
            _stripeChecking.value = true
            try {
                when (val result = com.priorizedev.brizza.data.api.StripeSyncClient.verifySubscriptionDirectly(email)) {
                    is com.priorizedev.brizza.data.api.SubscriptionCheckResult.Active -> {
                        val user = repository.getUsuarioByEmail(email)
                        if (user != null) {
                            val updatedUser = user.copy(
                                premiumAtivo = true,
                                planoAtivo = result.planType
                            )
                            repository.insertUsuario(updatedUser)
                            updateUserSessionState(updatedUser)
                            triggerAutoSync()
                        }
                        onResult?.invoke("Assinatura ativa detectada no Stripe! Plano: ${result.planType}.")
                    }
                    is com.priorizedev.brizza.data.api.SubscriptionCheckResult.Inactive -> {
                        val user = repository.getUsuarioByEmail(email)
                        if (user != null) {
                            val updatedUser = user.copy(
                                premiumAtivo = false,
                                planoAtivo = "FREE"
                            )
                            repository.insertUsuario(updatedUser)
                            updateUserSessionState(updatedUser)
                            triggerAutoSync()
                        }
                        onResult?.invoke("Cliente encontrado no Stripe, mas sem nenhuma assinatura ativa.")
                    }
                    is com.priorizedev.brizza.data.api.SubscriptionCheckResult.NoCustomer -> {
                        val user = repository.getUsuarioByEmail(email)
                        if (user != null) {
                            val updatedUser = user.copy(
                                premiumAtivo = false,
                                planoAtivo = "FREE"
                            )
                            repository.insertUsuario(updatedUser)
                            updateUserSessionState(updatedUser)
                            triggerAutoSync()
                        }
                        onResult?.invoke("Nenhum cadastro de cliente encontrado no Stripe para o e-mail $email.")
                    }
                    is com.priorizedev.brizza.data.api.SubscriptionCheckResult.NotConfigured -> {
                        onResult?.invoke("A API do Stripe não está configurada nos Secrets do AI Studio. Utilizando plano de demonstração local.")
                    }
                    is com.priorizedev.brizza.data.api.SubscriptionCheckResult.Error -> {
                        onResult?.invoke("Erro Stripe: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                onResult?.invoke("Erro ao verificar assinatura: ${e.localizedMessage}")
            } finally {
                _stripeChecking.value = false
            }
        }
    }

    fun iniciarCheckoutStripe(isAnnualPlan: Boolean, onResult: (com.priorizedev.brizza.data.api.CheckoutSessionResult) -> Unit) {
        viewModelScope.launch {
            val email = _userEmail.value
            if (email.isBlank()) {
                onResult(com.priorizedev.brizza.data.api.CheckoutSessionResult.Error("E-mail do usuário está indisponível."))
                return@launch
            }
            _stripeChecking.value = true
            try {
                val result = com.priorizedev.brizza.data.api.StripeSyncClient.createStripeCheckoutSession(email, isAnnualPlan)
                onResult(result)
            } finally {
                _stripeChecking.value = false
            }
        }
    }

    fun obterPortalFaturamentoStripe(onResult: (com.priorizedev.brizza.data.api.PortalSessionResult) -> Unit) {
        viewModelScope.launch {
            val email = _userEmail.value
            if (email.isBlank()) {
                onResult(com.priorizedev.brizza.data.api.PortalSessionResult.Error("E-mail do usuário está indisponível."))
                return@launch
            }
            _stripeChecking.value = true
            try {
                val result = com.priorizedev.brizza.data.api.StripeSyncClient.createStripePortalSession(email)
                onResult(result)
            } finally {
                _stripeChecking.value = false
            }
        }
    }


    fun salvarMarcaEquipamento(nome: String) {
        viewModelScope.launch {
            if (!_premiumAtivo.value) {
                _limitationMessage.value = "Limite do Plano FREE! Não é possível adicionar novas marcas de equipamentos no plano gratuito. Adquira o plano PREMIUM."
                return@launch
            }
            if (_userEmail.value.isNotBlank()) {
                val email = _userEmail.value
                val formattedName = nome.trim()
                if (formattedName.isNotBlank()) {
                    val entity = com.priorizedev.brizza.data.model.MarcaEquipamento(
                        nome = formattedName,
                        usuarioEmail = email
                    )
                    repository.insertMarcaEquipamento(entity)
                    triggerAutoSync()
                }
            }
        }
    }

    fun excluirMarcaEquipamento(marca: com.priorizedev.brizza.data.model.MarcaEquipamento) {
        addDeletedIdToPrefs("marcas_equipamentos", marca.id)
        viewModelScope.launch {
            try {
                com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteMarcaEquipamento(marca.id)
            } catch (e: Exception) {
                android.util.Log.e("ClimaGestViewModel", "Falha ao excluir marca no Supabase: ${e.message}")
            }
            repository.deleteMarcaEquipamento(marca)
            triggerAutoSync()
        }
    }

    // Clientes operations
    fun salvarCliente(cliente: Cliente, onCompleted: (String) -> Unit = {}) {
        viewModelScope.launch {
            val isNew = clientesState.value.none { it.id == cliente.id }
            if (isNew && clientesState.value.size >= 2 && !_premiumAtivo.value) {
                _limitationMessage.value = "Limite do Plano FREE! Você cadastrou o máximo de 2 clientes permitidos. Faça upgrade para o plano PREMIUM para cadastrar clientes ilimitados."
                return@launch
            }
            val withUser = if (cliente.usuarioEmail.isBlank()) cliente.copy(usuarioEmail = _userEmail.value) else cliente
            repository.insertCliente(withUser)
            onCompleted(withUser.id)
            triggerAutoSync()
        }
    }

    fun atualizarCliente(cliente: Cliente, onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            val withUser = if (cliente.usuarioEmail.isBlank()) cliente.copy(usuarioEmail = _userEmail.value) else cliente
            repository.updateCliente(withUser)
            onCompleted()
            triggerAutoSync()
        }
    }

    fun excluirCliente(cliente: Cliente) {
        addDeletedIdToPrefs("clientes", cliente.id)
        viewModelScope.launch {
            try {
                com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteCliente(cliente.id)
            } catch (e: Exception) {
                android.util.Log.e("ClimaGestViewModel", "Falha ao excluir cliente no Supabase: ${e.message}")
            }
            repository.deleteCliente(cliente)
            triggerAutoSync()
        }
    }

    fun obeterClienteFlow(clienteId: String): Flow<Cliente?> {
        return repository.getClienteById(clienteId)
    }

    // Peças operations
    fun salvarPeca(peca: com.priorizedev.brizza.data.model.Peca) {
        viewModelScope.launch {
            if (!_premiumAtivo.value) {
                _limitationMessage.value = "Limite do Plano FREE! Adição de peças e componentes não está disponível no plano gratuito. Adquira o plano PREMIUM."
                return@launch
            }
            val withUser = if (peca.usuarioEmail.isBlank()) peca.copy(usuarioEmail = _userEmail.value) else peca
            repository.insertPeca(withUser)
            triggerAutoSync()
        }
    }

    fun excluirPeca(peca: com.priorizedev.brizza.data.model.Peca) {
        addDeletedIdToPrefs("pecas", peca.id)
        viewModelScope.launch {
            try {
                com.priorizedev.brizza.data.api.SupabaseSyncClient.deletePeca(peca.id)
            } catch (e: Exception) {
                android.util.Log.e("ClimaGestViewModel", "Falha ao excluir peca no Supabase: ${e.message}")
            }
            repository.deletePeca(peca)
            triggerAutoSync()
        }
    }

    // Ambientes operations
    fun getAmbientesPorClienteFlow(clienteId: String): Flow<List<Ambiente>> {
        return repository.getAmbientesByCliente(clienteId)
    }

    fun getAmbienteFlow(ambienteId: String): Flow<Ambiente?> {
        return repository.getAmbienteByIdFlow(ambienteId)
    }

    fun salvarAmbiente(ambiente: Ambiente) {
        viewModelScope.launch {
            val isNew = ambientesState.value.none { it.id == ambiente.id }
            val existingCount = ambientesState.value.count { it.clienteId == ambiente.clienteId }
            if (isNew && existingCount >= 2 && !_premiumAtivo.value) {
                _limitationMessage.value = "Limite do Plano FREE! Não é possível ter mais de 2 ambientes por cliente no plano gratuito. Adquira o plano PREMIUM."
                return@launch
            }
            val withUser = if (ambiente.usuarioEmail.isBlank()) ambiente.copy(usuarioEmail = _userEmail.value) else ambiente
            repository.insertAmbiente(withUser)
            triggerAutoSync()
        }
    }

    fun excluirAmbiente(ambiente: Ambiente) {
        addDeletedIdToPrefs("ambientes", ambiente.id)
        viewModelScope.launch {
            try {
                com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteAmbiente(ambiente.id)
            } catch (e: Exception) {
                android.util.Log.e("ClimaGestViewModel", "Falha ao excluir ambiente no Supabase: ${e.message}")
            }
            repository.deleteAmbiente(ambiente)
            triggerAutoSync()
        }
    }

    // Equipamentos operations
    fun getEquipamentosPorClienteFlow(clienteId: String): Flow<List<Equipamento>> {
        return repository.getEquipamentosByCliente(clienteId)
    }

    fun getEquipamentosPorAmbienteFlow(ambienteId: String): Flow<List<Equipamento>> {
        return repository.getEquipamentosByAmbiente(ambienteId)
    }

    fun getEquipamentoByIdFlow(id: String): Flow<Equipamento?> {
        return repository.getEquipamentoByIdFlow(id)
    }

    fun getOrdensPorEquipamentoFlow(equipamentoId: String): Flow<List<OrdemServico>> {
        return repository.getOrdensServicoByEquipamento(equipamentoId)
    }

    fun salvarEquipamento(equipamento: Equipamento) {
        viewModelScope.launch {
            val isNew = equipamentosState.value.none { it.id == equipamento.id }
            val existingCount = equipamentosState.value.count { it.ambienteId == equipamento.ambienteId }
            if (isNew && existingCount >= 1 && !_premiumAtivo.value) {
                _limitationMessage.value = "Limite do Plano FREE! Não é possível ter mais de 1 equipamento por ambiente no plano gratuito. Adquira o plano PREMIUM."
                return@launch
            }
            val withUser = if (equipamento.usuarioEmail.isBlank()) equipamento.copy(usuarioEmail = _userEmail.value) else equipamento
            repository.insertEquipamento(withUser)
            triggerAutoSync()
        }
    }

    fun excluirEquipamento(equipamento: Equipamento) {
        addDeletedIdToPrefs("equipamentos", equipamento.id)
        viewModelScope.launch {
            try {
                com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteEquipamento(equipamento.id)
            } catch (e: Exception) {
                android.util.Log.e("ClimaGestViewModel", "Falha ao excluir equipamento no Supabase: ${e.message}")
            }
            repository.deleteEquipamento(equipamento)
            triggerAutoSync()
        }
    }

    fun excluirEquipamentoComOrdens(equipamento: Equipamento, ordens: List<OrdemServico>) {
        ordens.forEach { ord ->
            addDeletedIdToPrefs("ordens_servico", ord.id)
        }
        addDeletedIdToPrefs("equipamentos", equipamento.id)
        viewModelScope.launch {
            ordens.forEach { ord ->
                try {
                    com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteOrdemServico(ord.id)
                } catch (e: Exception) {
                    android.util.Log.e("ClimaGestViewModel", "Falha ao excluir ordem do equipamento no Supabase: ${e.message}")
                }
                repository.deleteOrdemServico(ord)
            }
            try {
                com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteEquipamento(equipamento.id)
            } catch (e: Exception) {
                android.util.Log.e("ClimaGestViewModel", "Falha ao excluir equipamento no Supabase: ${e.message}")
            }
            repository.deleteEquipamento(equipamento)
            triggerAutoSync()
        }
    }

    // Tecnicos operations
    fun salvarTecnico(tecnico: Tecnico) {
        viewModelScope.launch {
            val isNew = tecnicosState.value.none { it.id == tecnico.id }
            if (isNew && tecnicosState.value.size >= 1 && !_premiumAtivo.value) {
                _limitationMessage.value = "Limite do Plano FREE! Não é possível cadastrar mais de 1 técnico no plano gratuito. Adquira o plano PREMIUM."
                return@launch
            }
            val withUser = if (tecnico.usuarioEmail.isBlank()) tecnico.copy(usuarioEmail = _userEmail.value) else tecnico
            repository.insertTecnico(withUser)
            triggerAutoSync()
        }
    }

    fun excluirTecnico(tecnico: Tecnico) {
        addDeletedIdToPrefs("tecnicos", tecnico.id)
        viewModelScope.launch {
            try {
                com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteTecnico(tecnico.id)
            } catch (e: Exception) {
                android.util.Log.e("ClimaGestViewModel", "Falha ao excluir tecnico no Supabase: ${e.message}")
            }
            repository.deleteTecnico(tecnico)
            triggerAutoSync()
        }
    }

    // Ordens de Serviço operations
    fun getOrdemServicoFlow(ordemId: String): Flow<OrdemServico?> {
        return repository.getOrdemServicoById(ordemId)
    }

    fun salvarOrdemServico(ordemServico: OrdemServico) {
        viewModelScope.launch {
            val isNew = ordensServicoState.value.none { it.id == ordemServico.id }
            val existingCount = ordensServicoState.value.count { it.equipamentoId == ordemServico.equipamentoId }
            if (isNew && existingCount >= 1 && !_premiumAtivo.value) {
                _limitationMessage.value = "Limite do Plano FREE! Não é possível abrir mais de 1 ordem de serviço por equipamento no plano gratuito. Adquira o plano PREMIUM."
                return@launch
            }
            var updatedOrdem = if (ordemServico.usuarioEmail.isBlank()) ordemServico.copy(usuarioEmail = _userEmail.value) else ordemServico

            // 1. Upload images in background to Supabase Storage if they are local content URIs
            try {
                if (updatedOrdem.foto1.isNotBlank()) {
                    val url1 = com.priorizedev.brizza.data.api.SupabaseSyncClient.uploadImage(context, updatedOrdem.foto1)
                    if (url1 != null) {
                        updatedOrdem = updatedOrdem.copy(foto1 = url1)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ClimaGestViewModel", "Erro ao fazer upload da foto1: ${e.message}", e)
            }

            try {
                if (updatedOrdem.foto2.isNotBlank()) {
                    val url2 = com.priorizedev.brizza.data.api.SupabaseSyncClient.uploadImage(context, updatedOrdem.foto2)
                    if (url2 != null) {
                        updatedOrdem = updatedOrdem.copy(foto2 = url2)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ClimaGestViewModel", "Erro ao fazer upload da foto2: ${e.message}", e)
            }

            try {
                if (updatedOrdem.foto3.isNotBlank()) {
                    val url3 = com.priorizedev.brizza.data.api.SupabaseSyncClient.uploadImage(context, updatedOrdem.foto3)
                    if (url3 != null) {
                        updatedOrdem = updatedOrdem.copy(foto3 = url3)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ClimaGestViewModel", "Erro ao fazer upload da foto3: ${e.message}", e)
            }

            // 2. Save the final OrdemServico with public URLs to local DB and trigger automatic sync to remote Supabase
            repository.insertOrdemServico(updatedOrdem)
            triggerAutoSync()
        }
    }

    fun excluirOrdemServico(ordemServico: OrdemServico) {
        addDeletedIdToPrefs("ordens_servico", ordemServico.id)
        viewModelScope.launch {
            try {
                com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteOrdemServico(ordemServico.id)
            } catch (e: Exception) {
                android.util.Log.e("ClimaGestViewModel", "Falha ao excluir ordem no Supabase: ${e.message}")
            }
            repository.deleteOrdemServico(ordemServico)
            triggerAutoSync()
        }
    }

    // PMOC operations
    fun getPmocReportFlow(pmocId: String): Flow<PmocReport?> {
        return repository.getPmocReportById(pmocId)
    }

    fun salvarPmocReport(report: PmocReport) {
        viewModelScope.launch {
            if (!_premiumAtivo.value) {
                _limitationMessage.value = "Limite do Plano FREE! A geração de relatórios de PMOC é exclusiva do plano PREMIUM."
                return@launch
            }
            val withUser = if (report.usuarioEmail.isBlank()) report.copy(usuarioEmail = _userEmail.value) else report
            repository.insertPmocReport(withUser)
            triggerAutoSync()
        }
    }

    fun excluirPmocReport(report: PmocReport) {
        addDeletedIdToPrefs("pmoc_reports", report.id)
        viewModelScope.launch {
            try {
                com.priorizedev.brizza.data.api.SupabaseSyncClient.deletePmocReport(report.id)
            } catch (e: Exception) {
                android.util.Log.e("ClimaGestViewModel", "Falha ao excluir PMOC no Supabase: ${e.message}")
            }
            repository.deletePmocReport(report)
            triggerAutoSync()
        }
    }

    // PMOC Logbook Entries operations
    fun getLogbookEntriesByPmocFlow(pmocId: String): Flow<List<PmocLogbookEntry>> {
        return repository.getLogbookEntriesByPmoc(pmocId)
    }

    fun salvarLogbookEntry(entry: PmocLogbookEntry) {
        viewModelScope.launch {
            repository.insertLogbookEntry(entry)
            triggerAutoSync()
        }
    }

    fun salvarLogbookEntries(entries: List<PmocLogbookEntry>) {
        viewModelScope.launch {
            repository.insertLogbookEntries(entries)
            triggerAutoSync()
        }
    }

    fun excluirLogbookEntry(pmocId: String, equipamentoId: String, atividade: String) {
        viewModelScope.launch {
            repository.deleteLogbookEntry(pmocId, equipamentoId, atividade)
            triggerAutoSync()
        }
    }

    // Programas Preventivos operations
    fun salvarProgramaPreventivo(programa: com.priorizedev.brizza.data.model.ProgramaPreventivo) {
        viewModelScope.launch {
            val withUser = if (programa.usuarioEmail.isBlank()) programa.copy(usuarioEmail = _userEmail.value) else programa
            repository.insertProgramaPreventivo(withUser)
            triggerAutoSync()
        }
    }

    fun excluirProgramaPreventivo(programa: com.priorizedev.brizza.data.model.ProgramaPreventivo) {
        addDeletedIdToPrefs("programas_preventivos", programa.id)
        viewModelScope.launch {
            try {
                com.priorizedev.brizza.data.api.SupabaseSyncClient.deleteProgramaPreventivo(programa.id)
            } catch (e: Exception) {
                android.util.Log.e("ClimaGestViewModel", "Falha ao excluir programa no Supabase: ${e.message}")
            }
            repository.deleteProgramaPreventivo(programa)
            triggerAutoSync()
        }
    }
}

class ClimaGestViewModelFactory(private val repository: AppRepository, private val context: android.content.Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClimaGestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClimaGestViewModel(repository, context.applicationContext) as T
        }
        throw IllegalArgumentException("Classe ViewModel desconhecida")
    }
}
