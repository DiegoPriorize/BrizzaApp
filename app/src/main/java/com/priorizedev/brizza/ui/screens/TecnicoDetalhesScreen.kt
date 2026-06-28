package com.priorizedev.brizza.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.priorizedev.brizza.data.model.Tecnico
import com.priorizedev.brizza.ui.viewmodel.ClimaGestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TecnicoDetalhesScreen(
    tecnicoId: String,
    viewModel: ClimaGestViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tecnicos by viewModel.tecnicosState.collectAsStateWithLifecycle()
    val tecnico = tecnicos.find { it.id == tecnicoId }
    
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (tecnico == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Técnico Não Encontrado") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("O técnico selecionado não foi encontrado.", color = Color.Gray)
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Visualizar Técnico",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Mais opções",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar Informações") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showMenu = false
                                showEditDialog = true
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        DropdownMenuItem(
                            text = { Text("Excluir Técnico", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                windowInsets = WindowInsets(top = 0.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 40.dp)
        ) {
            // 1. BRANDED MAIN HEADER AVATAR CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Initials or Icon with premium multi-color gradient background
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF3F51B5), Color(0xFF00BCD4))
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tecnico.nome.take(2).uppercase(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Text(
                            text = if (tecnico.nomeCompleto.isNotEmpty()) tecnico.nomeCompleto else tecnico.nome,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        
                        if (tecnico.nomeCompleto.isNotEmpty()) {
                            Text(
                                text = "Nome de Exibição: ${tecnico.nome}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Status Pill Badge Custom Fitted
                        val isAtivo = tecnico.status == "Ativo"
                        val statusMainColor = if (isAtivo) Color(0xFF4CAF50) else Color(0xFFE53935)
                        val statusBgColor = if (isAtivo) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusBgColor)
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(statusMainColor)
                                )
                                Text(
                                    text = tecnico.status.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusMainColor
                                )
                            }
                        }
                    }
                }
            }

            // 2. DOCUMENTOS & REGISTROS
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        CardHeader(title = "Documentação & Identificação", icon = Icons.Default.Badge)
                        
                        DividerSpacer()
                        
                        DetailRow(label = "Nome Completo", value = tecnico.nomeCompleto.ifEmpty { "Não informado" })
                        DetailRow(label = "CPF", value = tecnico.cpf.ifEmpty { "Não informado" })
                        DetailRow(label = "Data de Nascimento", value = tecnico.dataNascimento.ifEmpty { "Não informada" })
                        DetailRow(label = "Registro Profissional (CFT / CREA)", value = tecnico.creaCft.ifEmpty { "Não informado" })
                    }
                }
            }

            // 3. CONTATOS DO PROFISSIONAL
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        CardHeader(title = "Canais de Comunicação", icon = Icons.Default.ContactPhone)
                        
                        DividerSpacer()
                        
                        DetailRow(label = "Celular / WhatsApp", value = tecnico.telefone.ifEmpty { "Não informado" })
                        DetailRow(label = "E-mail de Trabalho", value = tecnico.email.ifEmpty { "Não informado" })
                    }
                }
            }

            // 4. ATRIBUTOS PROFISSIONAIS & LOGÍSTICA
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        CardHeader(title = "Atributos de Campo", icon = Icons.Default.DirectionsCar)
                        
                        DividerSpacer()
                        
                        ToggleStatusRow(
                            label = "Carteira de Habilitação (CNH)", 
                            enabled = tecnico.temCnh,
                            onIcon = Icons.Default.CheckCircle,
                            offIcon = Icons.Default.Cancel
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ToggleStatusRow(
                            label = "Disponibilidade para Viagens", 
                            enabled = tecnico.disponivelViagens,
                            onIcon = Icons.Default.CheckCircle,
                            offIcon = Icons.Default.Cancel
                        )
                    }
                }
            }

            // 5. OBSERVAÇÕES
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        CardHeader(title = "Anotações Internas / Observações", icon = Icons.Default.Description)
                        
                        DividerSpacer()
                        
                        Text(
                            text = tecnico.obs.ifEmpty { "Nenhuma observação interna adicionada para este profissional." },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }

    // DELETE CONFIRMATION DIALOG
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Credencial", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja mesmo revogar a credencial de ${tecnico.nome}? Ele deixará de aparecer nas opções de Ordens de Serviço e PMOC secundárias.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.excluirTecnico(tecnico)
                        showDeleteConfirm = false
                        Toast.makeText(context, "Técnico excluído com sucesso!", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Voltar")
                }
            }
        )
    }

    // EDIT FORM DIALOG - STYLED TO MATCH THE EXACT REGISTRATION OF EQUIPAMENTO
    if (showEditDialog) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        var nCompleto by remember { mutableStateOf(tecnico.nomeCompleto) }
        var nExibicao by remember { mutableStateOf(tecnico.nome) }
        var cpfVal by remember { mutableStateOf(tecnico.cpf) }
        var dtNasc by remember { mutableStateOf(tecnico.dataNascimento) }
        var emVal by remember { mutableStateOf(tecnico.email) }
        var telVal by remember { mutableStateOf(tecnico.telefone) }
        var regVal by remember { mutableStateOf(tecnico.creaCft) }
        var switchCnh by remember { mutableStateOf(tecnico.temCnh) }
        var switchViag by remember { mutableStateOf(tecnico.disponivelViagens) }
        var obsTxt by remember { mutableStateOf(tecnico.obs) }
        var statVal by remember { mutableStateOf(tecnico.status) }

        var nExibicaoError by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { showEditDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .imePadding()
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header with gradient directly mirroring the registration styling
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF3F51B5), Color(0xFF00BCD4))
                                    ),
                                    shape = RoundedCornerShape(topStart = 23.dp, topEnd = 23.dp)
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.White.copy(alpha = 0.25f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Engineering,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Editar Cadastro",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Atualize os dados cadastrais do especialista",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                IconButton(
                                    onClick = { showEditDialog = false },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.15f), shape = CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fechar",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Form Scrollable body mirroring equipment fields
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 440.dp)
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Section header: Informações Pessoais
                            item {
                                HighlightedSectionHeader(
                                    title = "Informações Pessoais",
                                    icon = Icons.Default.Person,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = nCompleto,
                                    onValueChange = { nCompleto = it },
                                    label = { Text("Nome Completo", fontSize = 12.sp) },
                                    placeholder = { Text("Ex: Carlos Roberto Silva") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) }
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = nExibicao,
                                    onValueChange = {
                                        nExibicao = it
                                        nExibicaoError = it.isEmpty()
                                    },
                                    label = { Text("Nome para Exibição*", fontSize = 12.sp) },
                                    placeholder = { Text("Ex: Carlos Silva") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    isError = nExibicaoError,
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) }
                                )
                                if (nExibicaoError) {
                                    Text("Nome de exibição é obrigatório.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                                }
                            }

                            item {
                                OutlinedTextField(
                                    value = cpfVal,
                                    onValueChange = { cpfVal = it },
                                    label = { Text("CPF", fontSize = 12.sp) },
                                    placeholder = { Text("000.000.000-00") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) }
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = dtNasc,
                                    onValueChange = { dtNasc = it },
                                    label = { Text("Data de Nascimento", fontSize = 12.sp) },
                                    placeholder = { Text("DD/MM/AAAA") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) }
                                )
                            }

                            // Section header: Contato
                            item {
                                HighlightedSectionHeader(
                                    title = "Contato & Comunicação",
                                    icon = Icons.Default.ContactPhone,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = emVal,
                                    onValueChange = { emVal = it },
                                    label = { Text("E-mail", fontSize = 12.sp) },
                                    placeholder = { Text("carlos@empresa.com") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) }
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = telVal,
                                    onValueChange = { telVal = it },
                                    label = { Text("Contato / Telefone", fontSize = 12.sp) },
                                    placeholder = { Text("(00) 00000-0000") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) }
                                )
                            }

                            // Section header: Credencial
                            item {
                                HighlightedSectionHeader(
                                    title = "Registro Profissional",
                                    icon = Icons.Default.WorkspacePremium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = regVal,
                                    onValueChange = { regVal = it },
                                    label = { Text("Registro CFT / CREA", fontSize = 12.sp) },
                                    placeholder = { Text("Ex: CFT-SP 123456") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) }
                                )
                            }

                            // Section header: Atributos
                            item {
                                HighlightedSectionHeader(
                                    title = "Preferências & Status",
                                    icon = Icons.Default.Tune,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Possui CNH", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                            }
                                            Switch(checked = switchCnh, onCheckedChange = { switchCnh = it })
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Disponível para Viagens", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                            }
                                            Switch(checked = switchViag, onCheckedChange = { switchViag = it })
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Status Ativo", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                            }
                                            Switch(
                                                checked = statVal == "Ativo", 
                                                onCheckedChange = { isAtiv -> statVal = if (isAtiv) "Ativo" else "Inativo" }
                                            )
                                        }
                                    }
                                }
                            }

                            // Section header: Observações
                            item {
                                HighlightedSectionHeader(
                                    title = "Observações",
                                    icon = Icons.Default.Notes,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = obsTxt,
                                    onValueChange = { obsTxt = it },
                                    label = { Text("Obs", fontSize = 12.sp) },
                                    placeholder = { Text("Destaque qualificações, restrições ou especialidades...") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) }
                                )
                            }
                        }

                        // Bottom Actions Area
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showEditDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancelar", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (nExibicao.isEmpty()) {
                                        nExibicaoError = true
                                    } else {
                                        viewModel.salvarTecnico(
                                            tecnico.copy(
                                                nome = nExibicao,
                                                nomeCompleto = nCompleto,
                                                cpf = cpfVal,
                                                dataNascimento = dtNasc,
                                                email = emVal,
                                                telefone = telVal,
                                                creaCft = regVal,
                                                temCnh = switchCnh,
                                                disponivelViagens = switchViag,
                                                obs = obsTxt,
                                                status = statVal
                                            )
                                        )
                                        showEditDialog = false
                                        Toast.makeText(context, "Dados atualizados com sucesso!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salvar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightedSectionHeader(
    title: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp)
            .background(color.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(color.copy(alpha = 0.18f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = color
        )
    }
}

@Composable
private fun CardHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DividerSpacer() {
    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

@Composable
private fun ToggleStatusRow(label: String, enabled: Boolean, onIcon: ImageVector, offIcon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (enabled) onIcon else offIcon,
                contentDescription = null,
                tint = if (enabled) Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (enabled) "SIM" else "NÃO",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
    }
}
