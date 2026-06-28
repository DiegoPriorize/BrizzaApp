package com.priorizedev.brizza.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.priorizedev.brizza.data.model.Tecnico
import com.priorizedev.brizza.ui.viewmodel.ClimaGestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TecnicosScreen(
    viewModel: ClimaGestViewModel,
    onBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit
) {
    val context = LocalContext.current
    val tecnicos by viewModel.tecnicosState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Técnicos Credenciados",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                windowInsets = WindowInsets(top = 0.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF3F51B5),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Cadastrar Técnico")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Context header info bar style
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Estes perfis correspondem aos profissionais autorizados a assinar relatórios PMOC e Ordens de Serviço.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (tecnicos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Engineering,
                        contentDescription = null,
                        modifier = Modifier.size(68.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Nenhum técnico cadastrado",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Adicione membros clicando no botão '+' abaixo.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(tecnicos) { tecnico ->
                        val isAtivo = tecnico.status == "Ativo"
                        val statusColor = if (isAtivo) Color(0xFF4CAF50) else Color(0xFFF44336)
                        val statusBg = if (isAtivo) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onNavigateToDetails(tecnico.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Circular initial avatar with gradient
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF3F51B5), Color(0xFF00BCD4))
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tecnico.nome.take(2).uppercase(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = tecnico.nome,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        
                                        // Status badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(statusBg)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = tecnico.status.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Reg: ${tecnico.creaCft.ifEmpty { "Não cadastrado" }}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(2.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = tecnico.telefone.ifEmpty { "Sem telefone" },
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "Ver Perfil",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // REDESIGNED ADD FORM DIALOG (Formulário moderno de 11 campos, idêntico ao de cadastro de equipamentos)
    if (showAddDialog) {
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        var nCompleto by remember { mutableStateOf("") }
        var nExibicao by remember { mutableStateOf("") }
        var cpfVal by remember { mutableStateOf("") }
        var dtNasc by remember { mutableStateOf("") }
        var emVal by remember { mutableStateOf("") }
        var telVal by remember { mutableStateOf("") }
        var regVal by remember { mutableStateOf("") }
        var switchCnh by remember { mutableStateOf(false) }
        var switchViag by remember { mutableStateOf(false) }
        var obsTxt by remember { mutableStateOf("") }
        var statVal by remember { mutableStateOf("Ativo") }

        var nExibicaoError by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { showAddDialog = false },
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
                        // Header with gradient directly mirroring the equipment/ambient registration style
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
                                        text = "Cadastrar Técnico",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Registre os dados cadastrais e profissionais do técnico",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                IconButton(
                                    onClick = { showAddDialog = false },
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
                                    placeholder = { Text("Ex: Carlos Silva (Obrigatório)") },
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

                            // Section header: Contato e Endereço
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

                            // Section header: Credencial profissional
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
                                    placeholder = { Text("Ex: REG-SP 123456") },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) }
                                )
                            }

                            // Section header: Atributos de logística & status
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

                                        // Divider block
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

                                        // Divider block
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
                                    placeholder = { Text("Observações, especialidades, restrições...") },
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
                                onClick = { showAddDialog = false },
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
                                            Tecnico(
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
                                        showAddDialog = false
                                        Toast.makeText(context, "Técnico cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
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
