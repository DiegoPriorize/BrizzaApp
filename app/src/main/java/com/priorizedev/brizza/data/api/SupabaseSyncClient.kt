package com.priorizedev.brizza.data.api

import android.util.Log
import com.priorizedev.brizza.BuildConfig
import com.priorizedev.brizza.data.db.AppDatabase
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

sealed class SyncResult {
    object Success : SyncResult()
    data class Error(val message: String) : SyncResult()
    object NotConfigured : SyncResult()
}

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    object NetworkError : AuthResult()
    object NotConfigured : AuthResult()
}

object SupabaseSyncClient {
    private const val TAG = "SupabaseSyncClient"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun getSupabaseUrl(): String? {
        // Use reflections or check BuildConfig.
        // We can safely read compiled build config field.
        return try {
            val url = BuildConfig.SUPABASE_URL
            if (url.isBlank() || url == "https://your-project.supabase.co" || url.startsWith("YOUR_") || url == "SUPABASE_URL_DEFAULT_VALUE") {
                null
            } else {
                if (url.endsWith("/")) url else "$url/"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro lendo SUPABASE_URL: ${e.message}")
            null
        }
    }

    private fun getSupabaseKey(): String? {
        return try {
            val key = BuildConfig.SUPABASE_ANON_KEY
            if (key.isBlank() || key == "your-anon-key" || key.startsWith("YOUR_") || key == "SUPABASE_ANON_KEY_DEFAULT_VALUE") {
                null
            } else {
                key
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro lendo SUPABASE_ANON_KEY: ${e.message}")
            null
        }
    }

    private fun getApi(): SupabaseApi? {
        val baseUrl = getSupabaseUrl() ?: return null
        return try {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(SupabaseApi::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao instanciar API do Supabase: ${e.message}")
            null
        }
    }

    suspend fun syncAllData(database: AppDatabase): SyncResult = withContext(Dispatchers.IO) {
        val api = getApi() ?: return@withContext SyncResult.NotConfigured
        val apiKey = getSupabaseKey() ?: return@withContext SyncResult.NotConfigured
        val authHeader = "Bearer $apiKey"

        try {
            val dao = database.appDao()

            // 1. Usuarios
            val usuarios = dao.getAllUsuarios().first()
            if (usuarios.isNotEmpty()) {
                val res = api.upsertUsuarios(apiKey, authHeader, usuarios)
                if (!res.isSuccessful) {
                    val errMsg = res.errorBody()?.string() ?: res.message()
                    return@withContext SyncResult.Error("Erro ao sincronizar Usuários: $errMsg")
                }
            }

            // 2. Clientes
            val clientes = dao.getAllClientes().first()
            if (clientes.isNotEmpty()) {
                val res = api.upsertClientes(apiKey, authHeader, clientes)
                if (!res.isSuccessful) {
                    val errMsg = res.errorBody()?.string() ?: res.message()
                    return@withContext SyncResult.Error("Erro ao sincronizar Clientes: $errMsg")
                }
            }

            // 3. Ambientes
            val ambientes = dao.getAllAmbientes().first()
            if (ambientes.isNotEmpty()) {
                val res = api.upsertAmbientes(apiKey, authHeader, ambientes)
                if (!res.isSuccessful) {
                    val errMsg = res.errorBody()?.string() ?: res.message()
                    return@withContext SyncResult.Error("Erro ao sincronizar Ambientes: $errMsg")
                }
            }

            // 4. Equipamentos
            val equipamentos = dao.getAllEquipamentos().first()
            if (equipamentos.isNotEmpty()) {
                val res = api.upsertEquipamentos(apiKey, authHeader, equipamentos)
                if (!res.isSuccessful) {
                    val errMsg = res.errorBody()?.string() ?: res.message()
                    return@withContext SyncResult.Error("Erro ao sincronizar Equipamentos: $errMsg")
                }
            }

            // 5. Tecnicos
            val tecnicos = dao.getAllTecnicos().first()
            if (tecnicos.isNotEmpty()) {
                val res = api.upsertTecnicos(apiKey, authHeader, tecnicos)
                if (!res.isSuccessful) {
                    val errMsg = res.errorBody()?.string() ?: res.message()
                    return@withContext SyncResult.Error("Erro ao sincronizar Tecnicos: $errMsg")
                }
            }

            // 6. Ordens de Serviço
            val ordens = dao.getAllOrdensServico().first()
            if (ordens.isNotEmpty()) {
                val res = api.upsertOrdensServico(apiKey, authHeader, ordens)
                if (!res.isSuccessful) {
                    val errMsg = res.errorBody()?.string() ?: res.message()
                    return@withContext SyncResult.Error("Erro ao sincronizar Ordens de Serviço: $errMsg")
                }
            }

            // 7. PMOC Reports
            val pmocs = dao.getAllPmocReports().first()
            if (pmocs.isNotEmpty()) {
                val res = api.upsertPmocReports(apiKey, authHeader, pmocs)
                if (!res.isSuccessful) {
                    val errMsg = res.errorBody()?.string() ?: res.message()
                    return@withContext SyncResult.Error("Erro ao sincronizar Relatórios PMOC: $errMsg")
                }
            }

            // 8. PMOC Logbook Entries
            val logbooks = dao.getAllLogbookEntries().first()
            if (logbooks.isNotEmpty()) {
                val res = api.upsertPmocLogbookEntries(apiKey, authHeader, logbooks)
                if (!res.isSuccessful) {
                    val errMsg = res.errorBody()?.string() ?: res.message()
                    return@withContext SyncResult.Error("Erro ao sincronizar Logbooks: $errMsg")
                }
            }

            // 9. Marcas de Equipamentos
            val marcas = dao.getAllMarcasEquipamentosRaw().first()
            val marcasToSync = marcas.filter { !it.usuarioEmail.isNullOrBlank() }
            if (marcasToSync.isNotEmpty()) {
                val res = api.upsertMarcasEquipamentos(apiKey, authHeader, marcasToSync)
                if (!res.isSuccessful) {
                    val errMsg = res.errorBody()?.string() ?: res.message()
                    return@withContext SyncResult.Error("Erro ao sincronizar Marcas: $errMsg")
                }
            }

            // 10. Peças
            val pecas = dao.getAllPecas().first()
            if (pecas.isNotEmpty()) {
                val res = api.upsertPecas(apiKey, authHeader, pecas)
                if (!res.isSuccessful) {
                    val errMsg = res.errorBody()?.string() ?: res.message()
                    return@withContext SyncResult.Error("Erro ao sincronizar Peças: $errMsg")
                }
            }

            // 11. Programas Preventivos
            try {
                val programas = dao.getAllProgramasPreventivos().first()
                if (programas.isNotEmpty()) {
                    val res = api.upsertProgramasPreventivos(apiKey, authHeader, programas)
                    if (!res.isSuccessful) {
                        val errMsg = res.errorBody()?.string() ?: res.message()
                        Log.e(TAG, "Erro ao sincronizar Programas Preventivos: $errMsg")
                        if (errMsg.contains("42P01") || errMsg.contains("relation") || errMsg.contains("not found")) {
                            return@withContext SyncResult.Error("Erro ao sincronizar Programas: A tabela 'programas_preventivos' não foi encontrada no seu Supabase. Por favor, copie e execute a query desta tabela presente em 'supabase_setup.sql' no SQL Editor do Supabase.")
                        }
                        return@withContext SyncResult.Error("Erro ao sincronizar Programas Preventivos: $errMsg")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception ao sincronizar Programas Preventivos", e)
                return@withContext SyncResult.Error("Erro ao sincronizar Programas Preventivos: ${e.localizedMessage}")
            }

            Log.i(TAG, "Sincronização offline-first concluída com sucesso com o Supabase!")
            SyncResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Exception durante a sincronização com o Supabase", e)
            SyncResult.Error("Conexão falhou: ${e.localizedMessage ?: "Verifique sua internet"}")
        }
    }

    suspend fun downloadAllData(database: AppDatabase, email: String, deletedIds: Set<String> = emptySet()): SyncResult = withContext(Dispatchers.IO) {
        val api = getApi() ?: return@withContext SyncResult.NotConfigured
        val apiKey = getSupabaseKey() ?: return@withContext SyncResult.NotConfigured
        val authHeader = "Bearer $apiKey"
        val filter = "eq.$email"

        try {
            val dao = database.appDao()
            Log.i(TAG, "Iniciando download completo de dados para: $email")

            // 1. Clientes
            val clientesResponse = api.getClientes(apiKey, authHeader, filter)
            if (clientesResponse.isSuccessful) {
                clientesResponse.body()?.forEach { cliente ->
                    if (!deletedIds.contains(cliente.id)) {
                        dao.insertCliente(cliente)
                    }
                }
            }

            // 2. Ambientes
            val ambientesResponse = api.getAmbientes(apiKey, authHeader, filter)
            if (ambientesResponse.isSuccessful) {
                ambientesResponse.body()?.forEach { amb ->
                    if (!deletedIds.contains(amb.id)) {
                        dao.insertAmbiente(amb)
                    }
                }
            }

            // 3. Equipamentos
            val equipamentosResponse = api.getEquipamentos(apiKey, authHeader, filter)
            if (equipamentosResponse.isSuccessful) {
                equipamentosResponse.body()?.forEach { equip ->
                    if (!deletedIds.contains(equip.id)) {
                        dao.insertEquipamento(equip)
                    }
                }
            }

            // 4. Tecnicos
            val tecnicosResponse = api.getTecnicos(apiKey, authHeader, filter)
            if (tecnicosResponse.isSuccessful) {
                tecnicosResponse.body()?.forEach { tec ->
                    if (!deletedIds.contains(tec.id)) {
                        dao.insertTecnico(tec)
                    }
                }
            }

            // 5. Ordens de Servico
            val ordensResponse = api.getOrdensServico(apiKey, authHeader, filter)
            if (ordensResponse.isSuccessful) {
                ordensResponse.body()?.forEach { os ->
                    if (!deletedIds.contains(os.id)) {
                        dao.insertOrdemServico(os)
                    }
                }
            }

            // 6. PMOC Reports
            val pmocResponse = api.getPmocReports(apiKey, authHeader, filter)
            if (pmocResponse.isSuccessful) {
                pmocResponse.body()?.forEach { report ->
                    if (!deletedIds.contains(report.id)) {
                        dao.insertPmocReport(report)
                    }
                }
            }

            // 7. PMOC Logbook Entries
            val logbookResponse = api.getPmocLogbookEntries(apiKey, authHeader)
            if (logbookResponse.isSuccessful) {
                val allLogbooks = logbookResponse.body() ?: emptyList()
                val filteredLogbooks = allLogbooks.filter { !deletedIds.contains(it.id) }
                if (filteredLogbooks.isNotEmpty()) {
                    dao.insertLogbookEntries(filteredLogbooks)
                }
            }

            // 8. Pecas
            val pecasResponse = api.getPecas(apiKey, authHeader, filter)
            if (pecasResponse.isSuccessful) {
                pecasResponse.body()?.forEach { peca ->
                    if (!deletedIds.contains(peca.id)) {
                        dao.insertPeca(peca)
                    }
                }
            }

            // 9. Programas Preventivos
            try {
                val progResponse = api.getProgramasPreventivos(apiKey, authHeader, filter)
                if (progResponse.isSuccessful) {
                    progResponse.body()?.forEach { prog ->
                        if (!deletedIds.contains(prog.id)) {
                            dao.insertProgramaPreventivo(prog)
                        }
                    }
                } else {
                    val errMsg = progResponse.errorBody()?.string() ?: progResponse.message()
                    Log.w(TAG, "Aviso ao baixar Programas Preventivos: $errMsg")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao baixar Programas Preventivos", e)
            }

            // 10. Marcas de Equipamentos
            val marcasOrFilter = "(usuario_email.eq.$email,usuario_email.eq.,usuario_email.is.null)"
            val marcasResponse = api.getMarcasEquipamentos(apiKey, authHeader, marcasOrFilter)
            if (marcasResponse.isSuccessful) {
                marcasResponse.body()?.forEach { marca ->
                    if (!deletedIds.contains(marca.id)) {
                        dao.insertMarcaEquipamento(marca)
                    }
                }
            }

            // 11. Logs de Versões (Resiliente, caso a tabela ainda não exista no Supabase remoto do usuário)
            try {
                val logsResponse = api.getLogsVersao(apiKey, authHeader)
                if (logsResponse.isSuccessful) {
                    logsResponse.body()?.forEach { log ->
                        dao.insertLogVersao(log)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao baixar log_versao do Supabase (tabela pode nao ter sido criada ainda): ${e.message}")
            }

            Log.i(TAG, "Download completo e atualização do Room concluída!")
            SyncResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Erro baixando dados do Supabase", e)
            SyncResult.Error("Download falhou: ${e.localizedMessage}")
        }
    }

    private fun parseSupabaseError(errorBody: String, defaultMessage: String): String {
        try {
            // Simple robust regex parsing to extract error_description or message/msg
            val descRegex = """"(error_description|message|msg)"\s*:\s*"([^"]+)"""".toRegex()
            val match = descRegex.find(errorBody)
            if (match != null) {
                val rawMsg = match.groupValues[2]
                // Translate the most common messages
                return when {
                    rawMsg.contains("Email not confirmed", ignoreCase = true) -> 
                        "O e-mail ainda não foi confirmado no Supabase. Por favor, desative a opção 'Confirm email' nas configurações de Auth do seu painel do Supabase ou verifique sua caixa de entrada para confirmá-lo."
                    rawMsg.contains("Invalid login credentials", ignoreCase = true) || rawMsg.contains("invalid_credentials", ignoreCase = true) ->
                        "E-mail ou senha incorretos."
                    rawMsg.contains("User already exists", ignoreCase = true) || rawMsg.contains("already registered", ignoreCase = true) ->
                        "Este e-mail já está cadastrado."
                    rawMsg.contains("Password should be at least", ignoreCase = true) ->
                        "A senha deve ter pelo menos 6 caracteres."
                    else -> "Erro: $rawMsg"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao parsear erro do Supabase: ${e.message}")
        }
        return defaultMessage
    }

    suspend fun signUpAuth(email: String, passwordId: String): AuthResult = withContext(Dispatchers.IO) {
        val api = getApi() ?: return@withContext AuthResult.NotConfigured
        val apiKey = getSupabaseKey() ?: return@withContext AuthResult.NotConfigured
        val authHeader = "Bearer $apiKey"
        try {
            val res = api.signUp(apiKey, authHeader, SignUpRequestBody(email, passwordId))
            if (res.isSuccessful) {
                AuthResult.Success
            } else {
                val errorBody = res.errorBody()?.string() ?: ""
                Log.e(TAG, "Erro signup auth code ${res.code()}: $errorBody")
                val cleanMsg = parseSupabaseError(errorBody, "Cadastro falhou na nuvem.")
                AuthResult.Error(cleanMsg)
            }
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Erro de rede no signup auth supabase: ${e.message}", e)
            AuthResult.NetworkError
        } catch (e: Exception) {
            Log.e(TAG, "Erro desconhecido no signup auth supabase: ${e.message}", e)
            AuthResult.Error("Erro: ${e.localizedMessage}")
        }
    }

    suspend fun signInAuth(email: String, passwordId: String): AuthResult = withContext(Dispatchers.IO) {
        val api = getApi() ?: return@withContext AuthResult.NotConfigured
        val apiKey = getSupabaseKey() ?: return@withContext AuthResult.NotConfigured
        val authHeader = "Bearer $apiKey"
        try {
            val res = api.signIn(apiKey, authHeader, "password", SignUpRequestBody(email, passwordId))
            if (res.isSuccessful) {
                AuthResult.Success
            } else {
                val errorBody = res.errorBody()?.string() ?: ""
                Log.e(TAG, "Erro signin auth code ${res.code()}: $errorBody")
                val cleanMsg = parseSupabaseError(errorBody, "Login falhou: E-mail ou senha incorretos.")
                AuthResult.Error(cleanMsg)
            }
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Erro de rede no signin auth supabase: ${e.message}", e)
            AuthResult.NetworkError
        } catch (e: Exception) {
            Log.e(TAG, "Erro desconhecido no signin auth supabase: ${e.message}", e)
            AuthResult.Error("Erro: ${e.localizedMessage}")
        }
    }

    suspend fun fetchUsuarioByEmail(email: String): com.priorizedev.brizza.data.model.Usuario? = withContext(Dispatchers.IO) {
        val api = getApi() ?: return@withContext null
        val apiKey = getSupabaseKey() ?: return@withContext null
        val authHeader = "Bearer $apiKey"
        try {
            val res = api.getUsuarioByEmail(apiKey, authHeader, "eq.$email")
            if (res.isSuccessful) {
                res.body()?.firstOrNull()
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Erro fetchUsuarioByEmail supabase: ${e.message}", e)
            null
        }
    }

    suspend fun syncUsuario(usuario: com.priorizedev.brizza.data.model.Usuario): Boolean = withContext(Dispatchers.IO) {
        val api = getApi() ?: return@withContext false
        val apiKey = getSupabaseKey() ?: return@withContext false
        val authHeader = "Bearer $apiKey"
        try {
            val list = listOf(usuario)
            val res = api.upsertUsuarios(apiKey, authHeader, list)
            if (res.isSuccessful) {
                Log.i(TAG, "Perfil de usuário ${usuario.email} sincronizado na tabela 'usuarios' com sucesso!")
                true
            } else {
                val errorBody = res.errorBody()?.string() ?: ""
                Log.e(TAG, "Erro ao sincronizar perfil do usuário: ${res.code()} - $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha na chamada de sincronização do perfil: ${e.message}", e)
            false
        }
    }

    fun copyUriToLocalCache(context: android.content.Context, uriString: String): String {
        if (uriString.isBlank() || uriString.startsWith("http://") || uriString.startsWith("https://") || uriString.startsWith("sim_")) {
            return uriString
        }
        return try {
            val uri = android.net.Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return uriString
            // Ensure safe file name
            val file = java.io.File(context.cacheDir, "cached_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}.jpg")
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error copying uri to local cache: ${e.message}", e)
            uriString
        }
    }

    suspend fun deleteCliente(id: String): Boolean = deleteRow("clientes", id)
    suspend fun deleteAmbiente(id: String): Boolean = deleteRow("ambientes", id)
    suspend fun deleteEquipamento(id: String): Boolean = deleteRow("equipamentos", id)
    suspend fun deleteTecnico(id: String): Boolean = deleteRow("tecnicos", id)
    suspend fun deleteOrdemServico(id: String): Boolean = deleteRow("ordens_servico", id)
    suspend fun deletePmocReport(id: String): Boolean = deleteRow("pmoc_reports", id)
    suspend fun deletePeca(id: String): Boolean = deleteRow("pecas", id)
    suspend fun deleteProgramaPreventivo(id: String): Boolean = deleteRow("programas_preventivos", id)
    suspend fun deleteMarcaEquipamento(id: String): Boolean = deleteRow("marcas_equipamentos", id)

    private suspend fun deleteRow(table: String, id: String): Boolean = withContext(Dispatchers.IO) {
        val api = getApi() ?: return@withContext false
        val apiKey = getSupabaseKey() ?: return@withContext false
        val authHeader = "Bearer $apiKey"
        val idFilter = "eq.$id"
        try {
            val response = when (table) {
                "clientes" -> api.deleteCliente(apiKey, authHeader, idFilter)
                "ambientes" -> api.deleteAmbiente(apiKey, authHeader, idFilter)
                "equipamentos" -> api.deleteEquipamento(apiKey, authHeader, idFilter)
                "tecnicos" -> api.deleteTecnico(apiKey, authHeader, idFilter)
                "ordens_servico" -> api.deleteOrdemServico(apiKey, authHeader, idFilter)
                "pmoc_reports" -> api.deletePmocReport(apiKey, authHeader, idFilter)
                "pecas" -> api.deletePeca(apiKey, authHeader, idFilter)
                "programas_preventivos" -> api.deleteProgramaPreventivo(apiKey, authHeader, idFilter)
                "marcas_equipamentos" -> api.deleteMarcaEquipamento(apiKey, authHeader, idFilter)
                else -> null
            }
            if (response != null && response.isSuccessful) {
                Log.i(TAG, "Deletado com sucesso de $table: $id")
                true
            } else {
                Log.e(TAG, "Falha ao deletar de $table: $id, code=${response?.code()} error=${response?.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro de excecao ao deletar de $table: $id, msg=${e.message}")
            false
        }
    }

    suspend fun uploadImage(context: android.content.Context, uriString: String): String? = withContext(Dispatchers.IO) {
        if (uriString.isBlank()) return@withContext null
        if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
            return@withContext uriString
        }
        if (uriString.startsWith("sim_")) {
            return@withContext uriString
        }

        val api = getApi() ?: return@withContext null
        val apiKey = getSupabaseKey() ?: return@withContext null
        val authHeader = "Bearer $apiKey"

        try {
            // Check if it's a content URI, file URI, or an absolute path
            val inputStream = if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                val uri = android.net.Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)
            } else {
                val file = java.io.File(uriString)
                if (file.exists()) java.io.FileInputStream(file) else null
            }
            
            if (inputStream == null) {
                Log.e(TAG, "Nao foi possivel abrir input stream para a URI: $uriString")
                return@withContext null
            }
            
            val bytes = inputStream.use { it.readBytes() }

            val mimeType = if (uriString.startsWith("content://")) {
                context.contentResolver.getType(android.net.Uri.parse(uriString)) ?: "image/jpeg"
            } else if (uriString.endsWith(".png", ignoreCase = true)) {
                "image/png"
            } else {
                "image/jpeg"
            }
            
            val extension = if (mimeType.contains("png")) "png" else "jpg"
            val fileName = "ft_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}.$extension"

            val mediaType = mimeType.toMediaTypeOrNull()
            val requestBody = bytes.toRequestBody(mediaType)

            val response = api.uploadFotoTecnica(apiKey, authHeader, mimeType, fileName, requestBody)
            if (response.isSuccessful) {
                var baseUrl = getSupabaseUrl() ?: return@withContext null
                if (!baseUrl.endsWith("/")) {
                    baseUrl = "$baseUrl/"
                }
                val publicUrl = "${baseUrl}storage/v1/object/public/fotos_tecnicas/$fileName"
                Log.i(TAG, "Upload de foto técnica para o storage do Supabase concluído! URL pública: $publicUrl")
                publicUrl
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Log.e(TAG, "Erro ao fazer upload da foto para Supabase Storage: code=${response.code()} msg=$errorMsg")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception no upload de imagem para Supabase: ${e.message}", e)
            null
        }
    }
}
