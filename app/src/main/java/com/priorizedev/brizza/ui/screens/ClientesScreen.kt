package com.priorizedev.brizza.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.priorizedev.brizza.data.model.Ambiente
import com.priorizedev.brizza.data.model.Cliente
import com.priorizedev.brizza.data.model.Equipamento
import com.priorizedev.brizza.ui.components.EquipamentoDetalhesDialog
import com.priorizedev.brizza.ui.viewmodel.ClimaGestViewModel
import com.priorizedev.brizza.util.ReportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(
    viewModel: ClimaGestViewModel,
    onNavigateToDetails: (String) -> Unit,
    onBack: () -> Unit
) {
    val clientes by viewModel.clientesState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredClientes = clientes.filter {
        it.nome.contains(searchQuery, ignoreCase = true) || 
        it.documento.contains(searchQuery)
    }

    if (showAddDialog) {
        var nomeCompleto by remember { mutableStateOf("") }
        var nomeExibicaoByUsr by remember { mutableStateOf("") }
        var tipoPessoa by remember { mutableStateOf("Física") } // "Física" or "Jurídica"
        var documentoRaw by remember { mutableStateOf("") }
        var telefoneRaw by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var endereco by remember { mutableStateOf("") }

        var showNameError by remember { mutableStateOf(false) }

        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Cadastrar Novo Cliente",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Insira os dados do cliente",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { showAddDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
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
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(scrollState)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fields group: Identificação
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "DADOS DE IDENTIFICAÇÃO",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }

                // Nome para Exibição (Obrigatório)
                OutlinedTextField(
                    value = nomeExibicaoByUsr,
                    onValueChange = { 
                        if (it.length <= 30) {
                            nomeExibicaoByUsr = it
                            if (it.isNotEmpty()) showNameError = false
                        }
                    },
                    label = { Text("Nome para Exibição *") },
                    isError = showNameError,
                    supportingText = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            if (showNameError) {
                                Text("O nome para exibição é obrigatório!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Máximo 30 caracteres", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${nomeExibicaoByUsr.length}/30", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Nome Completo / Razão Social (Opcional)
                OutlinedTextField(
                    value = nomeCompleto,
                    onValueChange = { nomeCompleto = it },
                    label = { Text("Nome Completo / Razão Social") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Tipo de Pessoa: Física ou Jurídica
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tipo de Cliente *",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { 
                            tipoPessoa = "Física"
                            documentoRaw = ""
                        }
                    ) {
                        RadioButton(
                            selected = tipoPessoa == "Física",
                            onClick = { 
                                tipoPessoa = "Física"
                                documentoRaw = ""
                            }
                        )
                        Text("Física", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { 
                            tipoPessoa = "Jurídica"
                            documentoRaw = ""
                        }
                    ) {
                        RadioButton(
                            selected = tipoPessoa == "Jurídica",
                            onClick = { 
                                tipoPessoa = "Jurídica"
                                documentoRaw = ""
                            }
                        )
                        Text("Jurídica", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // CPF or CNPJ formatado
                val labelText = if (tipoPessoa == "Física") "CPF" else "CNPJ"
                val placeholderText = if (tipoPessoa == "Física") "Ex: 000.000.000-00" else "Ex: 00.000.000/0000-00"

                OutlinedTextField(
                    value = documentoRaw,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        documentoRaw = if (tipoPessoa == "Física") {
                            formatCpf(digitsOnly)
                        } else {
                            formatCnpj(digitsOnly)
                        }
                    },
                    label = { Text(labelText) },
                    placeholder = { Text(placeholderText) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Contato & Localização Header
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HomeWork,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "CONTATO & LOCALIZAÇÃO",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }

                // Telefone formatado
                OutlinedTextField(
                    value = telefoneRaw,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        telefoneRaw = formatPhone(digitsOnly)
                    },
                    label = { Text("Número para Contato") },
                    placeholder = { Text("Ex: (99) 99999-9999") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // E-mail
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail para Contato") },
                    placeholder = { Text("Ex: duto@refrigeracao.com") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Cidade e Endereço
                OutlinedTextField(
                    value = endereco,
                    onValueChange = { endereco = it },
                    label = { Text("Cidade e Endereço") },
                    placeholder = { Text("Ex: Porto Alegre, Rua das Flores, 450") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Footer Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
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
                            if (nomeExibicaoByUsr.isBlank()) {
                                showNameError = true
                            } else {
                                viewModel.salvarCliente(
                                    Cliente(
                                        nome = nomeExibicaoByUsr.trim(),
                                        documento = documentoRaw.trim(),
                                        telefone = telefoneRaw.trim(),
                                        email = email.trim(),
                                        endereco = endereco.trim(),
                                        nomeCompleto = nomeCompleto.trim(),
                                        tipoPessoa = tipoPessoa
                                    )
                                )
                                showAddDialog = false
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Meus Clientes",
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Novo Cliente")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 0.dp
                )
                .background(MaterialTheme.colorScheme.background)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar cliente (Nome / CNPJ)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().background(Color.Transparent),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (filteredClientes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PeopleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(68.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) "Nenhum cliente cadastrado ainda." else "Nenhum cliente encontrado.",
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Toque no botão '+' abaixo para cadastrar.",
                        fontSize = 12.sp,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredClientes) { cliente ->
                        val initials = cliente.nome.split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .map { it.first().toString().uppercase() }
                            .joinToString("")
                            .take(2)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDetails(cliente.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Elegant Avatar with name initials
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials.ifEmpty { "?" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = cliente.nome,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f, fill = false),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        
                                        // Badge indicating type of person
                                        Surface(
                                            color = if (cliente.tipoPessoa == "Jurídica") 
                                                MaterialTheme.colorScheme.secondaryContainer 
                                            else 
                                                MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = cliente.tipoPessoa,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (cliente.tipoPessoa == "Jurídica") 
                                                    MaterialTheme.colorScheme.onSecondaryContainer 
                                                else 
                                                    MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContactPage,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "CPF/CNPJ: ${cliente.documento}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = cliente.endereco.ifEmpty { "Sem endereço cadastrado" },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Ver detalhes",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

    // New Cliente Dialog
    if (false) {
        var nomeCompleto by remember { mutableStateOf("") }
        var nomeExibicaoByUsr by remember { mutableStateOf("") }
        var tipoPessoa by remember { mutableStateOf("Física") } // "Física" or "Jurídica"
        var documentoRaw by remember { mutableStateOf("") }
        var telefoneRaw by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var endereco by remember { mutableStateOf("") }

        var showNameError by remember { mutableStateOf(false) }

        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        val listState = rememberLazyListState()
        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        }

        val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
        val maxDialogHeight = screenHeight - 64.dp

        Dialog(
            onDismissRequest = { showAddDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .heightIn(max = maxDialogHeight)
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header with vibrant premium gradient background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
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
                                        .background(Color.White.copy(alpha = 0.2f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cadastrar Novo Cliente",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Insira os dados do cliente",
                                        style = MaterialTheme.typography.labelSmall,
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

                        // Form fields list
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Fields group: Identificação
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "DADOS DE IDENTIFICAÇÃO",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Nome para Exibição (Obrigatório)
                            item {
                                OutlinedTextField(
                                    value = nomeExibicaoByUsr,
                                    onValueChange = { 
                                        if (it.length <= 30) {
                                            nomeExibicaoByUsr = it
                                            if (it.isNotEmpty()) showNameError = false
                                        }
                                    },
                                    label = { Text("Nome para Exibição *") },
                                    isError = showNameError,
                                    supportingText = {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            if (showNameError) {
                                                Text("O nome para exibição é obrigatório!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                            } else {
                                                Text("Máximo 30 caracteres", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Text("${nomeExibicaoByUsr.length}/30", style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            // Nome Completo / Razão Social (Opcional)
                            item {
                                OutlinedTextField(
                                    value = nomeCompleto,
                                    onValueChange = { nomeCompleto = it },
                                    label = { Text("Nome Completo / Razão Social") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            // Tipo de Pessoa: Física ou Jurídica
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tipo de Cliente *",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { 
                                            tipoPessoa = "Física"
                                            documentoRaw = ""
                                        }
                                    ) {
                                        RadioButton(
                                            selected = tipoPessoa == "Física",
                                            onClick = { 
                                                tipoPessoa = "Física"
                                                documentoRaw = ""
                                            }
                                        )
                                        Text("Física", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { 
                                            tipoPessoa = "Jurídica"
                                            documentoRaw = ""
                                        }
                                    ) {
                                        RadioButton(
                                            selected = tipoPessoa == "Jurídica",
                                            onClick = { 
                                                tipoPessoa = "Jurídica"
                                                documentoRaw = ""
                                            }
                                        )
                                        Text("Jurídica", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            // CPF or CNPJ formatado
                            item {
                                val labelText = if (tipoPessoa == "Física") "CPF" else "CNPJ"
                                val placeholderText = if (tipoPessoa == "Física") "Ex: 000.000.000-00" else "Ex: 00.000.000/0000-00"

                                OutlinedTextField(
                                    value = documentoRaw,
                                    onValueChange = { input ->
                                        val digitsOnly = input.filter { it.isDigit() }
                                        documentoRaw = if (tipoPessoa == "Física") {
                                            formatCpf(digitsOnly)
                                        } else {
                                            formatCnpj(digitsOnly)
                                        }
                                    },
                                    label = { Text(labelText) },
                                    placeholder = { Text(placeholderText) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            // Contato & Localização Header
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HomeWork,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "CONTATO & LOCALIZAÇÃO",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Telefone formatado
                            item {
                                OutlinedTextField(
                                    value = telefoneRaw,
                                    onValueChange = { input ->
                                        val digitsOnly = input.filter { it.isDigit() }
                                        telefoneRaw = formatPhone(digitsOnly)
                                    },
                                    label = { Text("Número para Contato") },
                                    placeholder = { Text("Ex: (99) 99999-9999") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            // E-mail
                            item {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("E-mail para Contato") },
                                    placeholder = { Text("Ex: duto@refrigeracao.com") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            // Cidade e Endereço
                            item {
                                OutlinedTextField(
                                    value = endereco,
                                    onValueChange = { endereco = it },
                                    label = { Text("Cidade e Endereço") },
                                    placeholder = { Text("Ex: Porto Alegre, Rua das Flores, 450") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Footer Action Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(16.dp),
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
                                    if (nomeExibicaoByUsr.isBlank()) {
                                        showNameError = true
                                    } else {
                                        viewModel.salvarCliente(
                                            Cliente(
                                                nome = nomeExibicaoByUsr.trim(),
                                                documento = documentoRaw.trim(),
                                                telefone = telefoneRaw.trim(),
                                                email = email.trim(),
                                                endereco = endereco.trim(),
                                                nomeCompleto = nomeCompleto.trim(),
                                                tipoPessoa = tipoPessoa
                                            )
                                        )
                                        showAddDialog = false
                                    }
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteDetalhesScreen(
    clienteId: String,
    viewModel: ClimaGestViewModel,
    onBack: () -> Unit,
    onNavigateToAmbienteDetails: (String) -> Unit,
    onNavigateToEquipamentoDetails: (String) -> Unit
) {
    val clienteState = viewModel.obeterClienteFlow(clienteId).collectAsStateWithLifecycle(initialValue = null)
    val ambientes by viewModel.getAmbientesPorClienteFlow(clienteId).collectAsStateWithLifecycle(initialValue = emptyList())
    val equipamentos by viewModel.getEquipamentosPorClienteFlow(clienteId).collectAsStateWithLifecycle(initialValue = emptyList())
    val ordens by viewModel.ordensServicoState.collectAsStateWithLifecycle(initialValue = emptyList())

    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    // Dialog triggers
    var showAddAmbienteDialog by remember { mutableStateOf(false) }
    var showAddEquipamentoDialog by remember { mutableStateOf(false) }
    var isShowingFullProfilePage by remember { mutableStateOf(false) }
    var showEditClienteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedEquipamentoForDetails by remember { mutableStateOf<Equipamento?>(null) }

    var elegantNotificationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(elegantNotificationMessage) {
        if (elegantNotificationMessage != null) {
            kotlinx.coroutines.delay(3500)
            elegantNotificationMessage = null
        }
    }

    val cliente = clienteState.value ?: return

    if (showEditClienteDialog) {
        var nomeCompleto by remember { mutableStateOf(cliente.nomeCompleto) }
        var nomeExibicaoByUsr by remember { mutableStateOf(cliente.nome) }
        var tipoPessoa by remember { mutableStateOf(cliente.tipoPessoa) } // "Física" or "Jurídica"
        var documentoRaw by remember { mutableStateOf(cliente.documento) }
        var telefoneRaw by remember { mutableStateOf(cliente.telefone) }
        var email by remember { mutableStateOf(cliente.email) }
        var endereco by remember { mutableStateOf(cliente.endereco) }

        var showNameError by remember { mutableStateOf(false) }

        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Editar Cliente",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Atualize os dados cadastrais",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { showEditClienteDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
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
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(scrollState)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fields group: Identificação
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "DADOS DE IDENTIFICAÇÃO",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }

                // Nome para Exibição (Obrigatório)
                OutlinedTextField(
                    value = nomeExibicaoByUsr,
                    onValueChange = { 
                        if (it.length <= 30) {
                            nomeExibicaoByUsr = it
                            if (it.isNotEmpty()) showNameError = false
                        }
                    },
                    label = { Text("Nome para Exibição *") },
                    isError = showNameError,
                    supportingText = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            if (showNameError) {
                                Text("O nome para exibição é obrigatório!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Máximo 30 caracteres", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${nomeExibicaoByUsr.length}/30", style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Nome Completo / Razão Social (Opcional)
                OutlinedTextField(
                    value = nomeCompleto,
                    onValueChange = { nomeCompleto = it },
                    label = { Text("Nome Completo / Razão Social") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Tipo de Pessoa: Física ou Jurídica
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tipo de Cliente *",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { 
                            tipoPessoa = "Física"
                            documentoRaw = ""
                        }
                    ) {
                        RadioButton(
                            selected = tipoPessoa == "Física",
                            onClick = { 
                                tipoPessoa = "Física"
                                documentoRaw = ""
                            }
                        )
                        Text("Física", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { 
                            tipoPessoa = "Jurídica"
                            documentoRaw = ""
                        }
                    ) {
                        RadioButton(
                            selected = tipoPessoa == "Jurídica",
                            onClick = { 
                                tipoPessoa = "Jurídica"
                                documentoRaw = ""
                            }
                        )
                        Text("Jurídica", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // CPF or CNPJ formatado
                val labelText = if (tipoPessoa == "Física") "CPF" else "CNPJ"
                val placeholderText = if (tipoPessoa == "Física") "Ex: 000.000.000-00" else "Ex: 00.000.000/0000-00"

                OutlinedTextField(
                    value = documentoRaw,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        documentoRaw = if (tipoPessoa == "Física") {
                            formatCpf(digitsOnly)
                        } else {
                            formatCnpj(digitsOnly)
                        }
                    },
                    label = { Text(labelText) },
                    placeholder = { Text(placeholderText) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Contato & Localização Header
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HomeWork,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "CONTATO & LOCALIZAÇÃO",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }

                // Telefone formatado
                OutlinedTextField(
                    value = telefoneRaw,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        telefoneRaw = formatPhone(digitsOnly)
                    },
                    label = { Text("Número para Contato") },
                    placeholder = { Text("Ex: (99) 99999-9999") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // E-mail
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail para Contato") },
                    placeholder = { Text("Ex: duto@refrigeracao.com") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Cidade e Endereço
                OutlinedTextField(
                    value = endereco,
                    onValueChange = { endereco = it },
                    label = { Text("Cidade e Endereço") },
                    placeholder = { Text("Ex: Porto Alegre, Rua das Flores, 450") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Footer Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showEditClienteDialog = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (nomeExibicaoByUsr.isBlank()) {
                                showNameError = true
                            } else {
                                viewModel.atualizarCliente(
                                    cliente.copy(
                                        nome = nomeExibicaoByUsr.trim(),
                                        documento = documentoRaw.trim(),
                                        telefone = telefoneRaw.trim(),
                                        email = email.trim(),
                                        endereco = endereco.trim(),
                                        nomeCompleto = nomeCompleto.trim(),
                                        tipoPessoa = tipoPessoa
                                    )
                                ) {
                                    showEditClienteDialog = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    } else if (showAddAmbienteDialog) {
        var nomeAmb by remember { mutableStateOf("") }
        var areaAmb by remember { mutableStateOf("") }
        var cargaAmb by remember { mutableStateOf("") }
        
        var fixos by remember { mutableIntStateOf(0) }
        var flutuantes by remember { mutableIntStateOf(0) }
        var janelas by remember { mutableIntStateOf(0) }
        var portas by remember { mutableIntStateOf(0) }
        var fontesCalor by remember { mutableIntStateOf(0) }
        var incidenciaSolar by remember { mutableStateOf("Baixa") }

        var diferenteEndereco by remember { mutableStateOf(false) }
        var endInput by remember { mutableStateOf("") }

        var showNameError by remember { mutableStateOf(false) }

        val listState = rememberLazyListState()
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        }

        val sugerirBtu = remember(areaAmb, fixos, flutuantes, janelas, portas, fontesCalor, incidenciaSolar) {
            val areaVal = areaAmb.toDoubleOrNull() ?: 0.0
            val baseFactor = when (incidenciaSolar) {
                "Alta" -> 800
                "Média" -> 700
                else -> 600
            }
            val pessoas = fixos + flutuantes
            val carga = (areaVal * baseFactor) + (pessoas * 600) + (janelas + portas + fontesCalor) * 600
            if (carga > 0) {
                val suggested = (carga / 1000.0).toInt() * 1000
                if (suggested < 7000) 7000 else suggested
            } else {
                0
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Novo Ambiente",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { showAddAmbienteDialog = false }) {
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
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(10.dp)) }

                        // SEÇÃO PRINCIPAL
                        item {
                            HighlightedSectionHeader(
                                title = "Identificação do Ambiente",
                                icon = Icons.Default.HomeWork,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = nomeAmb,
                                onValueChange = {
                                    nomeAmb = it
                                    if (it.isNotBlank()) showNameError = false
                                },
                                isError = showNameError,
                                label = { Text("Nome do Ambiente*") },
                                placeholder = { Text("Ex: Sala de Reunião, CPD, Living") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.MeetingRoom,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // ÁREA E BTUS
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = areaAmb,
                                    onValueChange = { areaAmb = it.replace(',', '.') },
                                    label = { Text("Área (m²)", fontSize = 11.sp, maxLines = 1) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    )
                                )

                                OutlinedTextField(
                                    value = cargaAmb,
                                    onValueChange = { cargaAmb = it },
                                    label = { Text("Carga (BTU)", fontSize = 11.sp, maxLines = 1) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }

                        if (sugerirBtu > 0) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Sugestão térmica: $sugerirBtu BTUs",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        TextButton(
                                            onClick = { cargaAmb = sugerirBtu.toString() },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Usar", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }

                        // SEÇÃO OCUPANTES
                        item {
                            HighlightedSectionHeader(
                                title = "Ocupantes do Ambiente",
                                icon = Icons.Default.Person,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        item {
                            CounterField(
                                label = "Fixos",
                                value = fixos,
                                onValueChange = { fixos = it }
                            )
                        }

                        item {
                            CounterField(
                                label = "Flutuantes",
                                value = flutuantes,
                                onValueChange = { flutuantes = it }
                            )
                        }

                        // CARACTERÍSTICAS
                        item {
                            HighlightedSectionHeader(
                                title = "Características Físicas",
                                icon = Icons.Default.Info,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        item {
                            CounterField(
                                label = "Janelas",
                                value = janelas,
                                onValueChange = { janelas = it }
                            )
                        }

                        item {
                            CounterField(
                                label = "Portas",
                                value = portas,
                                onValueChange = { portas = it }
                            )
                        }

                        item {
                            CounterField(
                                label = "Fontes de Calor",
                                value = fontesCalor,
                                onValueChange = { fontesCalor = it }
                            )
                        }

                        // INCIDÊNCIA SOLAR
                        item {
                            HighlightedSectionHeader(
                                title = "Incidência Solar",
                                icon = Icons.Default.WbSunny,
                                color = Color(0xFFE28743)
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Baixa", "Média", "Alta").forEach { level ->
                                    val selected = incidenciaSolar == level
                                    val baseColor = when (level) {
                                        "Baixa" -> Color(0xFF137333)
                                        "Média" -> Color(0xFFB06000)
                                        else -> Color(0xFFC5221F)
                                    }
                                    val containerColor = if (selected) baseColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                                    val contentColor = if (selected) baseColor else Color.Gray
                                    val borderColor = if (selected) baseColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = borderColor,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .background(containerColor)
                                            .clickable { incidenciaSolar = level }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = baseColor
                                                )
                                            }
                                            Text(
                                                text = level,
                                                fontWeight = FontWeight.Bold,
                                                color = contentColor,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // CAMPO ENDEREÇO PERSONALIZADO
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), shape = RoundedCornerShape(14.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), shape = RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Endereço Diferente",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Este ambiente fica em endereço diferente do cliente",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = diferenteEndereco,
                                        onCheckedChange = { 
                                            diferenteEndereco = it
                                            if (!it) endInput = "" 
                                        }
                                    )
                                }
                                
                                if (diferenteEndereco) {
                                    OutlinedTextField(
                                        value = endInput,
                                        onValueChange = { endInput = it },
                                        label = { Text("Endereço do Ambiente") },
                                        placeholder = { Text("Ex: Rua das Flores, 123, Sala 10") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Directions,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(4.dp)) }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Actions bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showAddAmbienteDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (nomeAmb.isBlank()) {
                                    showNameError = true
                                    Toast.makeText(context, "⚠️ O nome do ambiente não pode ficar em branco!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val areaVal = areaAmb.toDoubleOrNull() ?: 10.0
                                    val cargaVal = cargaAmb.toIntOrNull() ?: sugerirBtu
                                    viewModel.salvarAmbiente(
                                        Ambiente(
                                            clienteId = clienteId,
                                            nome = nomeAmb.trim(),
                                            areaM2 = areaVal,
                                            cargaTermicaBtu = if (cargaVal <= 0) 9000 else cargaVal,
                                            fixos = fixos,
                                            flutuantes = flutuantes,
                                            janelas = janelas,
                                            portas = portas,
                                            fontesCalor = fontesCalor,
                                            incidenciaSolar = incidenciaSolar,
                                            endereco = if (diferenteEndereco) endInput.trim() else ""
                                        )
                                    )
                                    elegantNotificationMessage = "Ambiente '${nomeAmb.trim()}' adicionado com sucesso!"
                                    showAddAmbienteDialog = false
                                }
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E88E5)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else if (showAddEquipamentoDialog) {
        val nextIndex = (equipamentos.size + 1)
        var tag by remember { mutableStateOf("AC-${nextIndex.toString().padStart(2, '0')}") }
        var tipo by remember { mutableStateOf("Split") }
        var marca by remember { mutableStateOf("") }
        var modelo by remember { mutableStateOf("") }
        var capacidade by remember { mutableStateOf("12000") }
        var dataInstalacao by remember { mutableStateOf("") }
        var serial by remember { mutableStateOf("") }
        var observacoes by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Ativo") }
        
        // Tecnicas
        var potenciaW by remember { mutableStateOf("") }
        var tensao by remember { mutableStateOf("220V") }
        var sistema by remember { mutableStateOf("Inverter") }
        var fluido by remember { mutableStateOf("R-410A") }
        var ciclo by remember { mutableStateOf("Frio") }

        var selectedAmbiente by remember { mutableStateOf<Ambiente?>(null) }

        // Dropdowns states
        var tipoDropdownExpanded by remember { mutableStateOf(false) }
        var marcaDropdownExpanded by remember { mutableStateOf(false) }
        var capacidadeDropdownExpanded by remember { mutableStateOf(false) }
        var statusDropdownExpanded by remember { mutableStateOf(false) }
        var selectAmbDropdownExpanded by remember { mutableStateOf(false) }

        var tagError by remember { mutableStateOf(false) }
        var ambError by remember { mutableStateOf(false) }

        // Brand dynamic lists variables
        val databaseMarcas by viewModel.marcasEquipamentosState.collectAsStateWithLifecycle()
        val defaultMarcas = remember {
            listOf(
                "Agratto", "Carrier", "Comfee", "Consul", "Daikin", 
                "Elgin", "Electrolux", "Fujitsu", "Gree", "Hisense", 
                "Hitachi", "Komeco", "Midea", "Philco", 
                "Samsung", "Springer", "TCL", "York"
            )
        }
        val marcasList = remember(databaseMarcas) {
            val dbNames = databaseMarcas.map { it.nome }
            (defaultMarcas + dbNames).distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
        }
        val filteredMarcasList = remember(marca, marcasList) {
            if (marca.isBlank()) {
                marcasList
            } else {
                marcasList.filter { it.contains(marca, ignoreCase = true) }
            }
        }
        var showNewBrandInput by remember { mutableStateOf(false) }
        var novaMarcaInput by remember { mutableStateOf("") }

        val borderColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            errorBorderColor = MaterialTheme.colorScheme.error,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = Color.Gray,
            disabledLabelColor = Color.Gray,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val fieldTextStyle = LocalTextStyle.current.copy(fontSize = 13.sp)

        val listState = rememberLazyListState()
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        }

        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dataInstalacao = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
            },
            java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
            java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
            java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
        )

        if (showNewBrandInput) {
            AlertDialog(
                onDismissRequest = { showNewBrandInput = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Adicionar Nova Marca",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Cadastre uma nova marca de equipamento. Ela ficará salva na sua conta de usuário.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = novaMarcaInput,
                            onValueChange = { novaMarcaInput = it },
                            label = { Text("Nome da Marca*") },
                            placeholder = { Text("Ex: Daitsu, Electrolux") },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (novaMarcaInput.isNotBlank()) {
                                val trimmed = novaMarcaInput.trim()
                                if (!viewModel.premiumAtivo.value) {
                                    viewModel.triggerLimitation("Limite do Plano FREE! Não é possível adicionar novas marcas de equipamentos no plano gratuito. Adquira o plano PREMIUM.")
                                    showNewBrandInput = false
                                    novaMarcaInput = ""
                                } else {
                                    viewModel.salvarMarcaEquipamento(trimmed)
                                    marca = trimmed
                                    showNewBrandInput = false
                                    novaMarcaInput = ""
                                }
                            } else {
                                Toast.makeText(context, "Nome da marca não pode ser vazio", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cadastrar Marca", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showNewBrandInput = false
                        novaMarcaInput = ""
                    }) {
                        Text("Cancelar")
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Novo Equipamento",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { showAddEquipamentoDialog = false }) {
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
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(10.dp)) }

                        // SEÇÃO 1: INFORMAÇÕES BÁSICAS
                        item {
                            HighlightedSectionHeader(
                                title = "Informações Básicas",
                                icon = Icons.Default.Info,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Identificador TAG
                        item {
                            OutlinedTextField(
                                value = tag,
                                onValueChange = {
                                    tag = it
                                    tagError = it.isBlank()
                                },
                                isError = tagError,
                                label = { Text("Identificação / TAG do Equipamento*", fontSize = 11.sp) },
                                placeholder = { Text("Ex: Split-02, AC-CPD") },
                                textStyle = fieldTextStyle,
                                colors = borderColors,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                        }

                        // tipo
                        item {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = tipo,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Tipo de Equipamento*", fontSize = 11.sp) },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { tipoDropdownExpanded = true },
                                    enabled = false,
                                    textStyle = fieldTextStyle,
                                    colors = borderColors,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                DropdownMenu(
                                    expanded = tipoDropdownExpanded,
                                    onDismissRequest = { tipoDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    listOf(
                                        "Split" to Icons.Default.AcUnit,
                                        "Janela" to Icons.Default.GridOn,
                                        "Central" to Icons.Default.Settings,
                                        "Fan Coil" to Icons.Default.Air,
                                        "Cassete" to Icons.Default.TripOrigin,
                                        "Piso Teto" to Icons.Default.Layers,
                                        "Chiller" to Icons.Default.Kitchen
                                    ).forEach { (t, icon) ->
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                            text = { Text(t, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                tipo = t
                                                tipoDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // marca dynamic dropdown and button below
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = marca,
                                        onValueChange = {
                                            marca = it
                                            marcaDropdownExpanded = true
                                        },
                                        label = { Text("Marca*", fontSize = 11.sp) },
                                        placeholder = { Text("Selecione ou digite para buscar") },
                                        trailingIcon = {
                                            IconButton(onClick = { marcaDropdownExpanded = !marcaDropdownExpanded }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = fieldTextStyle,
                                        colors = borderColors,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    DropdownMenu(
                                        expanded = marcaDropdownExpanded,
                                        onDismissRequest = { marcaDropdownExpanded = false },
                                        properties = androidx.compose.ui.window.PopupProperties(focusable = false),
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), RoundedCornerShape(16.dp))
                                            .heightIn(max = 280.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        if (filteredMarcasList.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Nenhuma marca encontrada") },
                                                onClick = {}
                                            )
                                        } else {
                                            filteredMarcasList.forEach { m ->
                                                DropdownMenuItem(
                                                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp)) },
                                                    text = { Text(m, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface) },
                                                    onClick = {
                                                        marca = m
                                                        marcaDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Button(
                                    onClick = { showNewBrandInput = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Cadastrar Nova Marca",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // modelo
                        item {
                            OutlinedTextField(
                                value = modelo,
                                onValueChange = { modelo = it },
                                label = { Text("Modelo", fontSize = 11.sp) },
                                placeholder = { Text("Ex: ASNW09GDSH0") },
                                textStyle = fieldTextStyle,
                                colors = borderColors,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                        }

                        // capacidade
                        item {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = "$capacidade BTU/h",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Capacidade Térmica (BTU/h)*", fontSize = 11.sp) },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { capacidadeDropdownExpanded = true },
                                    enabled = false,
                                    textStyle = fieldTextStyle,
                                    colors = borderColors,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                DropdownMenu(
                                    expanded = capacidadeDropdownExpanded,
                                    onDismissRequest = { capacidadeDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    listOf("9000", "12000", "18000", "24000", "30000", "36000", "48000", "60000").forEach { cap ->
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.Thermostat, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp)) },
                                            text = { Text("$cap BTU/h", fontWeight = FontWeight.Medium) },
                                            onClick = {
                                                capacidade = cap
                                                capacidadeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // SEÇÃO 2: LOCALIZAÇÃO
                        item {
                            HighlightedSectionHeader(
                                title = "Localização",
                                icon = Icons.Default.LocationOn,
                                color = Color(0xFF8E24AA)
                            )
                        }

                        // select ambiente
                        item {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedAmbiente?.nome ?: "Selecionar Ambiente*",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Ambiente Vinculado*", fontSize = 11.sp, color = Color(0xFF8E24AA)) },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF8E24AA)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectAmbDropdownExpanded = true },
                                    enabled = false,
                                    isError = ambError,
                                    textStyle = fieldTextStyle.copy(color = if (selectedAmbiente != null) MaterialTheme.colorScheme.onSurface else Color(0xFF8E24AA)),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = if (selectedAmbiente != null) MaterialTheme.colorScheme.onSurface else Color(0xFF8E24AA),
                                        disabledBorderColor = if (ambError) MaterialTheme.colorScheme.error else Color(0xFFCC99FF),
                                        disabledLabelColor = Color(0xFF8E24AA),
                                        disabledTrailingIconColor = Color(0xFF8E24AA)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                DropdownMenu(
                                    expanded = selectAmbDropdownExpanded,
                                    onDismissRequest = { selectAmbDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(BorderStroke(1.dp, Color(0xFFCC99FF).copy(alpha = 0.5f)), RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    if (ambientes.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Nenhum ambiente cadastrado. Cadastre primeiro.") },
                                            onClick = { selectAmbDropdownExpanded = false }
                                        )
                                    } else {
                                        ambientes.forEach { amb ->
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = Color(0xFF8E24AA), modifier = Modifier.size(18.dp)) },
                                                text = { Text(amb.nome, fontWeight = FontWeight.Medium) },
                                                onClick = {
                                                    selectedAmbiente = amb
                                                    ambError = false
                                                    selectAmbDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // SEÇÃO 3: DETALHES TÉCNICOS
                        item {
                            HighlightedSectionHeader(
                                title = "Detalhes Técnicos",
                                icon = Icons.Default.Build,
                                color = Color(0xFFE65100)
                            )
                        }

                        // serial number
                        item {
                            OutlinedTextField(
                                value = serial,
                                onValueChange = { serial = it },
                                label = { Text("Número de Série / Chassi", fontSize = 11.sp) },
                                placeholder = { Text("Ex: S/N 847293-A") },
                                textStyle = fieldTextStyle,
                                colors = borderColors,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                        }

                        // potencia and tensao
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = potenciaW,
                                    onValueChange = { potenciaW = it },
                                    label = { Text("Potência (Watts)", fontSize = 11.sp) },
                                    placeholder = { Text("Ex: 1080W") },
                                    textStyle = fieldTextStyle,
                                    colors = borderColors,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = tensao,
                                    onValueChange = { tensao = it },
                                    label = { Text("Tensão/Voltagem*", fontSize = 11.sp) },
                                    textStyle = fieldTextStyle,
                                    colors = borderColors,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Gás (Fluido)
                        item {
                            SelectableBadgeRow(
                                title = "Gás Refrigerante*",
                                options = listOf("R-410A", "R-32", "R-22"),
                                selectedValue = fluido,
                                onSelected = { fluido = it },
                                activeColor = MaterialTheme.colorScheme.secondary,
                                textColor = MaterialTheme.colorScheme.onSecondary
                            )
                        }

                        // Ciclo
                        item {
                            SelectableBadgeRow(
                                title = "Ciclo de Funcionamento*",
                                options = listOf("Frio", "Quente", "Quente/Frio"),
                                selectedValue = ciclo,
                                onSelected = { ciclo = it },
                                activeColor = MaterialTheme.colorScheme.primary,
                                textColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        // Tecnologia
                        item {
                            SelectableBadgeRow(
                                title = "Tecnologia (Compressor)*",
                                options = listOf("Convencional", "Inverter"),
                                selectedValue = sistema,
                                onSelected = { sistema = it },
                                activeColor = MaterialTheme.colorScheme.tertiary,
                                textColor = MaterialTheme.colorScheme.onTertiary
                            )
                        }

                        // data de instalacao
                        item {
                            OutlinedTextField(
                                value = dataInstalacao,
                                onValueChange = { dataInstalacao = it },
                                readOnly = true,
                                label = { Text("Data de Instalação", fontSize = 11.sp) },
                                trailingIcon = {
                                    IconButton(onClick = { datePickerDialog.show() }) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "Selecionar data")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { datePickerDialog.show() },
                                enabled = false,
                                textStyle = fieldTextStyle,
                                colors = borderColors,
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        // Observações
                        item {
                            OutlinedTextField(
                                value = observacoes,
                                onValueChange = { observacoes = it },
                                label = { Text("Observações Gerais", fontSize = 11.sp) },
                                placeholder = { Text("Insira o histórico rápido, acesso ou restrição") },
                                textStyle = fieldTextStyle,
                                colors = borderColors,
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                shape = RoundedCornerShape(14.dp),
                                maxLines = 4
                            )
                        }

                        // Status Geral
                        item {
                            SelectableBadgeRow(
                                title = "Status do Equipamento*",
                                options = listOf("Ativo", "Inativo", "Manutenção"),
                                selectedValue = status,
                                onSelected = { status = it },
                                activeColor = when (status) {
                                    "Ativo" -> Color(0xFF2E7D32)
                                    "Inativo" -> Color(0xFFC62828)
                                    else -> Color(0xFFEF6C00)
                                },
                                textColor = Color.White
                            )
                        }

                        item { Spacer(modifier = Modifier.height(4.dp)) }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Footer Actions Bar on dark container
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showAddEquipamentoDialog = false },
                            modifier = Modifier.weight(1.0f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                tagError = tag.isBlank()
                                ambError = selectedAmbiente == null

                                if (tag.isBlank()) {
                                    Toast.makeText(context, "⚠️ TAG do equipamento está em branco!", Toast.LENGTH_SHORT).show()
                                } else if (selectedAmbiente == null) {
                                    Toast.makeText(context, "⚠️ Escolha um ambiente para vincular esta máquina!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.salvarEquipamento(
                                        Equipamento(
                                            clienteId = clienteId,
                                            ambienteId = selectedAmbiente!!.id,
                                            tag = tag.trim().uppercase(),
                                            tipo = tipo,
                                            marca = marca.trim(),
                                            modelo = modelo.trim().uppercase(),
                                            capacidadeBtu = capacidade.toIntOrNull() ?: 12000,
                                            numeroSerie = serial.trim().uppercase(),
                                            status = status,
                                            dataInstalacao = dataInstalacao,
                                            observacoes = observacoes.trim(),
                                            potenciaW = potenciaW.trim(),
                                            tensao = tensao,
                                            sistema = sistema,
                                            ciclo = ciclo,
                                            fluidoRefrigerante = fluido.trim()
                                        )
                                    )
                                    Toast.makeText(context, "Módulos de ar condicionado '${tag.trim().uppercase()}' salvo com sucesso!", Toast.LENGTH_LONG).show()
                                    showAddEquipamentoDialog = false
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
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Detalhes do Cliente",
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
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
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
                                text = { Text("Editar Cliente") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showEditClienteDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Gerar PDF") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val reportFile = ReportUtils.generateClientFullReportPdf(
                                        context = context,
                                        cliente = cliente,
                                        ambientes = ambientes,
                                        equipamentos = equipamentos,
                                        ordens = ordens
                                    )
                                    if (reportFile != null) {
                                        ReportUtils.sharePdf(context, reportFile)
                                    } else {
                                        Toast.makeText(context, "Erro ao gerar relatório", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Excluir Cliente", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                windowInsets = WindowInsets(top = 0.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 0.dp,
                    start = innerPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = innerPadding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
            // Summary banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable { isShowingFullProfilePage = true }
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "FICHA DE CADASTRO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cliente.nome,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (cliente.documento.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Doc: ${cliente.documento}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Ver completo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Tabs
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Ambientes (${ambientes.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Máquinas (${equipamentos.size})") }
                )
            }

            // Tab contents
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                if (selectedTabIndex == 0) {
                    // Ambientes list
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ambientes Cadastrados", fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { showAddAmbienteDialog = true },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ambiente", fontSize = 12.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        if (ambientes.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Nenhum ambiente registrado.", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(ambientes) { ambiente ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToAmbienteDetails(ambiente.id) }
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.HomeWork,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(end = 12.dp).size(24.dp)
                                                )
                                                Column {
                                                    Text(ambiente.nome, fontWeight = FontWeight.Bold)
                                                    Text("Área: ${ambiente.areaM2} m² | Carga: ${ambiente.cargaTermicaBtu} BTU/h", fontSize = 12.sp, color = Color.Gray)
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Ver Detalhes",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Equipamentos list
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Máquinas do Cliente", fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { showAddEquipamentoDialog = true },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Máquina", fontSize = 12.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        if (equipamentos.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Nenhum equipamento registrado.", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(equipamentos) { equipamento ->
                                    // Match corresponding environment name if possible
                                    val localAmbNome = ambientes.find { it.id == equipamento.ambienteId }?.nome ?: "N/I"
                                    
                                    Card(
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable { onNavigateToEquipamentoDetails(equipamento.id) },
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        shape = RoundedCornerShape(4.dp),
                                                        modifier = Modifier.padding(end = 6.dp)
                                                    ) {
                                                        Text(equipamento.tag, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Text("${equipamento.marca} • ${equipamento.capacidadeBtu} BTU/h", fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Gás: ${equipamento.fluidoRefrigerante}", fontSize = 12.sp, color = Color.Gray)
                                                Text("Local: $localAmbNome", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                            }
                                            
                                            // Interactive Status Badge on the right
                                            val badgeColor = when (equipamento.status.lowercase()) {
                                                "ativo" -> Color(0xFFE6F4EA) to Color(0xFF137333)
                                                "inativo" -> Color(0xFFF1F3F4) to Color(0xFF3C4043)
                                                "manutenção", "manutencao" -> Color(0xFFFEF7E0) to Color(0xFFB06000)
                                                else -> Color(0xFFE6F4EA) to Color(0xFF137333)
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(badgeColor.first)
                                                    .clickable {
                                                        val nextStatus = when (equipamento.status.lowercase()) {
                                                            "ativo" -> "Inativo"
                                                            "inativo" -> "Manutenção"
                                                            else -> "Ativo"
                                                        }
                                                        viewModel.salvarEquipamento(equipamento.copy(status = nextStatus))
                                                        Toast.makeText(context, "Status alterado para $nextStatus", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(badgeColor.second))
                                                    Text(
                                                        text = equipamento.status.uppercase(),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = badgeColor.second
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

            } // Closes Column

            // Custom elegant notification bar overlay at top center of ClienteDetalhesScreen
            AnimatedVisibility(
                visible = elegantNotificationMessage != null,
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .zIndex(100f)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F4EA)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, Color(0xFF137333).copy(alpha = 0.3f)),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF137333).copy(alpha = 0.15f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF137333),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = elegantNotificationMessage ?: "",
                            color = Color(0xFF137333),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Redirecionado para visualização em página inteira

    // Dialog Novo Equipamento (Desativado - Redirecionado para visualização em página inteira)
    if (false && showAddEquipamentoDialog) {
        val nextIndex = (equipamentos.size + 1)
        var tag by remember { mutableStateOf("AC-${nextIndex.toString().padStart(2, '0')}") }
        var tipo by remember { mutableStateOf("Split") }
        var marca by remember { mutableStateOf("") }
        var modelo by remember { mutableStateOf("") }
        var capacidade by remember { mutableStateOf("12000") }
        var dataInstalacao by remember { mutableStateOf("") }
        var serial by remember { mutableStateOf("") }
        var observacoes by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Ativo") }
        
        // Tecnicas
        var potenciaW by remember { mutableStateOf("") }
        var tensao by remember { mutableStateOf("220V") }
        var sistema by remember { mutableStateOf("Inverter") }
        var fluido by remember { mutableStateOf("R-410A") }
        var ciclo by remember { mutableStateOf("Frio") }

        var selectedAmbiente by remember { mutableStateOf<Ambiente?>(null) }

        // Dropdowns states
        var tipoDropdownExpanded by remember { mutableStateOf(false) }
        var marcaDropdownExpanded by remember { mutableStateOf(false) }
        var capacidadeDropdownExpanded by remember { mutableStateOf(false) }
        var statusDropdownExpanded by remember { mutableStateOf(false) }
        var selectAmbDropdownExpanded by remember { mutableStateOf(false) }

        var tagError by remember { mutableStateOf(false) }
        var ambError by remember { mutableStateOf(false) }

        val borderColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFCCCCCC),
            unfocusedBorderColor = Color(0xFFE5E5E5),
            disabledBorderColor = Color(0xFFE5E5E5),
            errorBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
            focusedLabelColor = Color.Gray,
            unfocusedLabelColor = Color.Gray,
            disabledLabelColor = Color.Gray,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val fieldTextStyle = LocalTextStyle.current.copy(fontSize = 13.sp)

        val scrollState = rememberLazyListState()
        val kbController = LocalSoftwareKeyboardController.current
        val focManager = LocalFocusManager.current

        LaunchedEffect(scrollState.isScrollInProgress) {
            if (scrollState.isScrollInProgress) {
                kbController?.hide()
                focManager.clearFocus()
            }
        }

        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dataInstalacao = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
            },
            java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
            java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
            java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
        )

        Dialog(
            onDismissRequest = { showAddEquipamentoDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {
                        focManager.clearFocus()
                        kbController?.hide()
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
                            focManager.clearFocus()
                            kbController?.hide()
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header with gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
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
                                        imageVector = Icons.Default.AcUnit,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cadastrar Equipamento",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { showAddEquipamentoDialog = false },
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

                        // Form Scrollable
                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 440.dp)
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // SEÇÃO 1: INFORMAÇÕES BÁSICAS
                            item {
                                HighlightedSectionHeader(
                                    title = "Informações Básicas",
                                    icon = Icons.Default.Info,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Field Tipo
                            item {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = tipo,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Tipo de Equipamento*", fontSize = 11.sp) },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { tipoDropdownExpanded = true },
                                        enabled = false,
                                        textStyle = fieldTextStyle,
                                        colors = borderColors,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    DropdownMenu(
                                        expanded = tipoDropdownExpanded,
                                        onDismissRequest = { tipoDropdownExpanded = false }
                                    ) {
                                        listOf("Split", "Janela", "Central", "Fan Coil", "Cassete", "Piso Teto", "Chiller").forEach { t ->
                                            DropdownMenuItem(
                                                text = { Text(t) },
                                                onClick = {
                                                    tipo = t
                                                    tipoDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Field Marca
                            item {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = marca,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Marca*", fontSize = 11.sp) },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { marcaDropdownExpanded = true },
                                        enabled = false,
                                        textStyle = fieldTextStyle,
                                        colors = borderColors,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    DropdownMenu(
                                        expanded = marcaDropdownExpanded,
                                        onDismissRequest = { marcaDropdownExpanded = false }
                                    ) {
                                        listOf("LG", "Samsung", "Daikin", "Carrier", "Midea", "Gree", "Fujitsu", "Elgin", "Consul", "Springer").forEach { m ->
                                            DropdownMenuItem(
                                                text = { Text(m) },
                                                onClick = {
                                                    marca = m
                                                    marcaDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Field Modelo
                            item {
                                OutlinedTextField(
                                    value = modelo,
                                    onValueChange = { modelo = it },
                                    label = { Text("Modelo", fontSize = 11.sp) },
                                    placeholder = { Text("Ex: ASNW09GDSH0") },
                                    textStyle = fieldTextStyle,
                                    colors = borderColors,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )
                            }

                            // Field Capacidade (BTU/h)
                            item {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = "$capacidade BTU/h",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Capacidade Térmica (BTU/h)*", fontSize = 11.sp) },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { capacidadeDropdownExpanded = true },
                                        enabled = false,
                                        textStyle = fieldTextStyle,
                                        colors = borderColors,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    DropdownMenu(
                                        expanded = capacidadeDropdownExpanded,
                                        onDismissRequest = { capacidadeDropdownExpanded = false }
                                    ) {
                                        listOf("9000", "12000", "18000", "24000", "30000", "36000", "48000", "60000").forEach { cap ->
                                            DropdownMenuItem(
                                                text = { Text("$cap BTU/h") },
                                                onClick = {
                                                    capacidade = cap
                                                    capacidadeDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // SEÇÃO 2: LOCALIZAÇÃO
                            item {
                                HighlightedSectionHeader(
                                    title = "Localização",
                                    icon = Icons.Default.LocationOn,
                                    color = Color(0xFF8E24AA)
                                )
                            }
                            // Select Ambiente dropdown
                            item {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedAmbiente?.nome ?: "Selecionar Ambiente*",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Ambiente Vinculado*", fontSize = 11.sp, color = Color(0xFF8E24AA)) },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF8E24AA)) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectAmbDropdownExpanded = true },
                                        enabled = false,
                                        isError = ambError,
                                        textStyle = fieldTextStyle.copy(color = if (selectedAmbiente != null) MaterialTheme.colorScheme.onSurface else Color(0xFF8E24AA)),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = if (selectedAmbiente != null) MaterialTheme.colorScheme.onSurface else Color(0xFF8E24AA),
                                            disabledBorderColor = if (ambError) MaterialTheme.colorScheme.error else Color(0xFFCC99FF),
                                            disabledLabelColor = Color(0xFF8E24AA),
                                            disabledTrailingIconColor = Color(0xFF8E24AA)
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    DropdownMenu(
                                        expanded = selectAmbDropdownExpanded,
                                        onDismissRequest = { selectAmbDropdownExpanded = false }
                                    ) {
                                        if (ambientes.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Nenhum ambiente cadastrado. Cadastre primeiro.") },
                                                onClick = { selectAmbDropdownExpanded = false }
                                            )
                                        } else {
                                            ambientes.forEach { amb ->
                                                DropdownMenuItem(
                                                    text = { Text(amb.nome) },
                                                    onClick = {
                                                        selectedAmbiente = amb
                                                        ambError = false
                                                        selectAmbDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // SEÇÃO 3: INSTALAÇÃO & DATA
                            item {
                                HighlightedSectionHeader(
                                    title = "Instalação",
                                    icon = Icons.Default.CalendarMonth,
                                    color = Color(0xFFE28743)
                                )
                            }

                            // Field Data Instalacao (abrir calendario native)
                            item {
                                OutlinedTextField(
                                    value = dataInstalacao,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Data da Instalação", fontSize = 11.sp) },
                                    placeholder = { Text("Selecione no calendário...") },
                                    trailingIcon = {
                                        IconButton(onClick = { datePickerDialog.show() }) {
                                            Icon(imageVector = Icons.Default.DateRange, contentDescription = "Selecionar data")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { datePickerDialog.show() },
                                    enabled = false,
                                    textStyle = fieldTextStyle,
                                    colors = borderColors,
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }

                            // Field Numero de Serie
                            item {
                                OutlinedTextField(
                                    value = serial,
                                    onValueChange = { serial = it },
                                    label = { Text("Número de Série / Chapa", fontSize = 11.sp) },
                                    placeholder = { Text("Ex: SN-1823901-X") },
                                    textStyle = fieldTextStyle,
                                    colors = borderColors,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )
                            }

                            // Field Observacoes
                            item {
                                OutlinedTextField(
                                    value = observacoes,
                                    onValueChange = { observacoes = it },
                                    label = { Text("Observações Técnicas / Notas", fontSize = 11.sp) },
                                    placeholder = { Text("Ex: Fica próximo à janela, sol a tarde...") },
                                    textStyle = fieldTextStyle,
                                    colors = borderColors,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    maxLines = 3
                                )
                            }

                            // SEÇÃO 4: STATUS
                            item {
                                HighlightedSectionHeader(
                                    title = "Status de Operação",
                                    icon = Icons.Default.SettingsSuggest,
                                    color = Color(0xFF137333)
                                )
                            }
                            item {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = status,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Status", fontSize = 11.sp) },
                                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { statusDropdownExpanded = true },
                                        enabled = false,
                                        textStyle = fieldTextStyle,
                                        colors = borderColors,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    DropdownMenu(
                                        expanded = statusDropdownExpanded,
                                        onDismissRequest = { statusDropdownExpanded = false }
                                    ) {
                                        listOf("Ativo", "Em Manutenção", "Desativado").forEach { st ->
                                            DropdownMenuItem(
                                                text = { Text(st) },
                                                onClick = {
                                                    status = st
                                                    statusDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // SEÇÃO 5: INFORMAÇÕES TÉCNICAS
                            item {
                                HighlightedSectionHeader(
                                    title = "Informações Técnicas",
                                    icon = Icons.Default.Settings,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            // Field Potencia (W)
                            item {
                                OutlinedTextField(
                                    value = potenciaW,
                                    onValueChange = { potenciaW = it },
                                    label = { Text("Potência (Watts)", fontSize = 11.sp) },
                                    placeholder = { Text("Ex: 1080") },
                                    textStyle = fieldTextStyle,
                                    colors = borderColors,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )
                            }

                            // Field Tensao (110, 220, 380)
                            item {
                                Text("Tensão de Alimentação", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("110V", "220V", "380V").forEach { v ->
                                        val selected = tensao == v
                                        val containerColor = if (selected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        val contentColor = if (selected) MaterialTheme.colorScheme.error else Color.Gray
                                        val borderColor = if (selected) MaterialTheme.colorScheme.error else Color.Transparent
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                                .background(containerColor)
                                                .clickable { tensao = v },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(v, fontWeight = FontWeight.Bold, color = contentColor, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            // Field Sistema (Convencional, Inverter)
                            item {
                                Text("Sistema de Compressor", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Convencional", "Inverter").forEach { s ->
                                        val selected = sistema == s
                                        val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        val contentColor = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
                                        val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                                .background(containerColor)
                                                .clickable { sistema = s },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(s, fontWeight = FontWeight.Bold, color = contentColor, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            // Field Gas refrigerante (R-22, R-410A, R-32)
                            item {
                                Text("Gás Refrigerante", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("R-22", "R-410A", "R-32").forEach { g ->
                                        val selected = fluido == g
                                        val containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        val contentColor = if (selected) MaterialTheme.colorScheme.secondary else Color.Gray
                                        val borderColor = if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                                .background(containerColor)
                                                .clickable { fluido = g },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(g, fontWeight = FontWeight.Bold, color = contentColor, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            // Field Ciclo (Frio, Quente-frio, quente)
                            item {
                                Text("Ciclo de Operação", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Frio", "Quente-Frio", "Quente").forEach { c ->
                                        val selected = ciclo == c
                                        val containerColor = if (selected) Color(0xFFE0F7FA) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        val contentColor = if (selected) Color(0xFF006064) else Color.Gray
                                        val borderColor = if (selected) Color(0xFF006064) else Color.Transparent
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                                .background(containerColor)
                                                .clickable { ciclo = c },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(c, fontWeight = FontWeight.Bold, color = contentColor, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            // TAG / Identificacao
                            item {
                                OutlinedTextField(
                                    value = tag,
                                    onValueChange = { 
                                        tag = it
                                        if (it.isNotEmpty()) tagError = false
                                    },
                                    label = { Text("TAG / Identificação da Máquina *", fontSize = 11.sp) },
                                    placeholder = { Text("Ex: TAC-01, SPLIT-05") },
                                    isError = tagError,
                                    supportingText = if (tagError) {
                                        { Text("A identificação TAG é obrigatória!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) }
                                    } else null,
                                    textStyle = fieldTextStyle,
                                    colors = borderColors,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Dialog Confirm / Dismiss Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showAddEquipamentoDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancelar", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val finalAmb = selectedAmbiente
                                    if (tag.isBlank()) {
                                        tagError = true
                                        Toast.makeText(context, "⚠️ A TAG do equipamento é obrigatória!", Toast.LENGTH_SHORT).show()
                                    } else if (finalAmb == null) {
                                        ambError = true
                                        Toast.makeText(context, "⚠️ Selecione um ambiente para a máquina!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.salvarEquipamento(
                                            Equipamento(
                                                clienteId = clienteId,
                                                ambienteId = finalAmb.id,
                                                tag = tag.trim().uppercase(),
                                                marca = marca,
                                                modelo = modelo.trim(),
                                                numeroSerie = serial.trim(),
                                                tipo = tipo,
                                                capacidadeBtu = capacidade.toIntOrNull() ?: 9000,
                                                fluidoRefrigerante = fluido,
                                                status = status,
                                                dataInstalacao = dataInstalacao,
                                                observacoes = observacoes.trim(),
                                                potenciaW = potenciaW.trim(),
                                                tensao = tensao,
                                                sistema = sistema,
                                                ciclo = ciclo
                                            )
                                        )
                                        Toast.makeText(context, "Módulos de ar condicionado '${tag.trim().uppercase()}' salvo com sucesso!", Toast.LENGTH_LONG).show()
                                        showAddEquipamentoDialog = false
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

    if (isShowingFullProfilePage) {
        androidx.activity.compose.BackHandler {
            isShowingFullProfilePage = false
        }
        Scaffold(
            topBar = {
                MediumTopAppBar(
                    title = {
                        Text(
                            text = cliente.nome,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isShowingFullProfilePage = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    },
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { pad ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cliente.nome.take(2).uppercase(),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = cliente.nomeCompleto.ifEmpty { cliente.nome },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "CLIENTE " + cliente.tipoPessoa.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "INFORMAÇÕES DE CONTATO",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (cliente.documento.isNotEmpty()) {
                            ProfileDetailRow(
                                label = if (cliente.tipoPessoa == "Física") "CPF" else "CNPJ",
                                value = cliente.documento,
                                icon = Icons.Default.Badge
                            )
                        }

                        if (cliente.email.isNotEmpty()) {
                            ProfileDetailRow(
                                label = "E-mail",
                                value = cliente.email,
                                icon = Icons.Default.Email
                            )
                        }

                        if (cliente.telefone.isNotEmpty()) {
                            ProfileDetailRow(
                                label = "Telefone para Contato",
                                value = cliente.telefone,
                                icon = Icons.Default.Phone
                            )
                        }

                        if (cliente.endereco.isNotEmpty()) {
                            ProfileDetailRow(
                                label = "Endereço Operacional",
                                value = cliente.endereco,
                                icon = Icons.Default.LocationOn
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "MÉTRICAS DO CONTRATO",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatWidget(label = "Ambientes", count = ambientes.size, color = MaterialTheme.colorScheme.primary)
                            StatWidget(label = "Máquinas", count = equipamentos.size, color = MaterialTheme.colorScheme.secondary)
                            StatWidget(label = "O.S. Atendidas", count = ordens.count { it.clienteId == cliente.id }, color = Color(0xFF4CAF50))
                        }
                    }
                }

                Button(
                    onClick = {
                        val reportFile = ReportUtils.generateClientFullReportPdf(
                            context = context,
                            cliente = cliente,
                            ambientes = ambientes,
                            equipamentos = equipamentos,
                            ordens = ordens
                        )
                        if (reportFile != null) {
                            ReportUtils.sharePdf(context, reportFile)
                        } else {
                            Toast.makeText(context, "Erro ao gerar PDF", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Gerar Relatório Completo PDF", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (false) {
        var nomeCompleto by remember { mutableStateOf(cliente.nomeCompleto) }
        var nomeExibicaoByUsr by remember { mutableStateOf(cliente.nome) }
        var tipoPessoa by remember { mutableStateOf(cliente.tipoPessoa) } // "Física" or "Jurídica"
        var documentoRaw by remember { mutableStateOf(cliente.documento) }
        var telefoneRaw by remember { mutableStateOf(cliente.telefone) }
        var email by remember { mutableStateOf(cliente.email) }
        var endereco by remember { mutableStateOf(cliente.endereco) }

        var showNameError by remember { mutableStateOf(false) }

        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        val listState = rememberLazyListState()
        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        }

        val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
        val maxDialogHeight = screenHeight - 64.dp

        Dialog(
            onDismissRequest = { showEditClienteDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .heightIn(max = maxDialogHeight)
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header with vibrant premium gradient background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
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
                                        .background(Color.White.copy(alpha = 0.2f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Editar Cliente",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Atualize os dados cadastrais",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                IconButton(
                                    onClick = { showEditClienteDialog = false },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.15f), shape = CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fechar2",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Form fields list
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Fields group: Identificação
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "DADOS DE IDENTIFICAÇÃO",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Nome para Exibição (Obrigatório)
                            item {
                                OutlinedTextField(
                                    value = nomeExibicaoByUsr,
                                    onValueChange = { 
                                        if (it.length <= 30) {
                                            nomeExibicaoByUsr = it
                                            if (it.isNotEmpty()) showNameError = false
                                        }
                                    },
                                    label = { Text("Nome para Exibição *") },
                                    isError = showNameError,
                                    supportingText = {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            if (showNameError) {
                                                Text("O nome para exibição é obrigatório!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                            } else {
                                                Text("Máximo 30 caracteres", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Text("${nomeExibicaoByUsr.length}/30", style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            // Nome Completo / Razão Social (Opcional)
                            item {
                                OutlinedTextField(
                                    value = nomeCompleto,
                                    onValueChange = { nomeCompleto = it },
                                    label = { Text("Nome Completo / Razão Social") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            // Tipo de Pessoa: Física ou Jurídica
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tipo de Cliente *",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { 
                                            tipoPessoa = "Física"
                                            documentoRaw = ""
                                        }
                                    ) {
                                        RadioButton(
                                            selected = tipoPessoa == "Física",
                                            onClick = { 
                                                tipoPessoa = "Física"
                                                documentoRaw = ""
                                            }
                                        )
                                        Text("Física", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { 
                                            tipoPessoa = "Jurídica"
                                            documentoRaw = ""
                                        }
                                    ) {
                                        RadioButton(
                                            selected = tipoPessoa == "Jurídica",
                                            onClick = { 
                                                tipoPessoa = "Jurídica"
                                                documentoRaw = ""
                                            }
                                        )
                                        Text("Jurídica", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            // CPF or CNPJ formatado
                            item {
                                val labelText = if (tipoPessoa == "Física") "CPF" else "CNPJ"
                                val placeholderText = if (tipoPessoa == "Física") "Ex: 000.000.000-00" else "Ex: 00.000.000/0000-00"

                                OutlinedTextField(
                                    value = documentoRaw,
                                    onValueChange = { input ->
                                        val digitsOnly = input.filter { it.isDigit() }
                                        documentoRaw = if (tipoPessoa == "Física") {
                                            formatCpf(digitsOnly)
                                        } else {
                                            formatCnpj(digitsOnly)
                                        }
                                    },
                                    label = { Text(labelText) },
                                    placeholder = { Text(placeholderText) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            // Contato & Localização Header
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HomeWork,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "CONTATO & LOCALIZAÇÃO",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Telefone formatado
                            item {
                                OutlinedTextField(
                                    value = telefoneRaw,
                                    onValueChange = { input ->
                                        val digitsOnly = input.filter { it.isDigit() }
                                        telefoneRaw = formatPhone(digitsOnly)
                                    },
                                    label = { Text("Número para Contato") },
                                    placeholder = { Text("Ex: (99) 99999-9999") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            // E-mail
                            item {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("E-mail para Contato") },
                                    placeholder = { Text("Ex: duto@refrigeracao.com") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            // Cidade e Endereço
                            item {
                                OutlinedTextField(
                                    value = endereco,
                                    onValueChange = { endereco = it },
                                    label = { Text("Cidade e Endereço") },
                                    placeholder = { Text("Ex: Porto Alegre, Rua das Flores, 450") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Footer Action Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showEditClienteDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancelar", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (nomeExibicaoByUsr.isBlank()) {
                                        showNameError = true
                                    } else {
                                        viewModel.atualizarCliente(
                                            cliente.copy(
                                                nome = nomeExibicaoByUsr.trim(),
                                                documento = documentoRaw.trim(),
                                                telefone = telefoneRaw.trim(),
                                                email = email.trim(),
                                                endereco = endereco.trim(),
                                                nomeCompleto = nomeCompleto.trim(),
                                                tipoPessoa = tipoPessoa
                                            )
                                        ) {
                                            showEditClienteDialog = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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

    if (showDeleteConfirmDialog) {
        var confirmationText by remember { mutableStateOf("") }
        val isConfirmEnabled = confirmationText.trim() == "EXCLUIR"

        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Excluir Cliente?", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Esta ação é irreversível e excluirá permanentemente todos os ambientes, equipamentos e ordens associadas a este cliente (${cliente.nome}).",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Para confirmar, digite as letras maiúsculas EXCLUIR abaixo:",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = confirmationText,
                        onValueChange = { confirmationText = it },
                        placeholder = { Text("EXCLUIR") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isConfirmEnabled) {
                            viewModel.excluirCliente(cliente)
                            showDeleteConfirmDialog = false
                            onBack()
                        }
                    },
                    enabled = isConfirmEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Excluir Permanentemente")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
}

@Composable
fun CounterField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { if (value > 0) onValueChange(value - 1) },
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Diminuir",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value.toString(),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.widthIn(min = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(
                onClick = { onValueChange(value + 1) },
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aumentar",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun HighlightedSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = color
        )
    }
}

private fun formatCpf(digits: String): String {
    val clean = digits.take(11)
    val sb = StringBuilder()
    for (i in clean.indices) {
        sb.append(clean[i])
        if (i == 2 || i == 5) {
            sb.append(".")
        } else if (i == 8) {
            sb.append("-")
        }
    }
    return sb.toString()
}

private fun formatCnpj(digits: String): String {
    val clean = digits.take(14)
    val sb = StringBuilder()
    for (i in clean.indices) {
        sb.append(clean[i])
        if (i == 1 || i == 4) {
            sb.append(".")
        } else if (i == 7) {
            sb.append("/")
        } else if (i == 11) {
            sb.append("-")
        }
    }
    return sb.toString()
}

private fun formatPhone(digits: String): String {
    val clean = digits.take(11)
    val sb = StringBuilder()
    if (clean.isNotEmpty()) {
        sb.append("(")
        for (i in clean.indices) {
            if (i == 2) {
                sb.append(") ")
            } else if (i == 7 && clean.length > 10) {
                sb.append("-")
            } else if (i == 6 && clean.length <= 10) {
                sb.append("-")
            }
            sb.append(clean[i])
        }
    }
    return sb.toString()
}

@Composable
private fun ProfileDetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StatWidget(
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .border(2.dp, color.copy(alpha = 0.2f), shape = CircleShape)
                .background(color.copy(alpha = 0.05f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SelectableBadgeRow(
    title: String,
    options: List<String>,
    selectedValue: String,
    onSelected: (String) -> Unit,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = selectedValue == option
                val displayColor = if (isSelected) {
                    when (option) {
                        "Ativo" -> Color(0xFF2E7D32)
                        "Inativo" -> Color(0xFFC62828)
                        "Manutenção" -> Color(0xFFEF6C00)
                        else -> activeColor
                    }
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }

                val contentColor = if (isSelected) {
                    if (option in listOf("Ativo", "Inativo", "Manutenção")) Color.White else textColor
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                }

                val border = if (isSelected) {
                    BorderStroke(1.5.dp, displayColor)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                val icon = when (option) {
                    "Ativo" -> Icons.Default.CheckCircle
                    "Inativo" -> Icons.Default.Cancel
                    "Manutenção" -> Icons.Default.Build
                    "Frio" -> Icons.Default.AcUnit
                    "Quente" -> Icons.Default.WbSunny
                    "Quente/Frio" -> Icons.Default.Thermostat
                    "Inverter" -> Icons.Default.Bolt
                    "Convencional" -> Icons.Default.Settings
                    "R-410A", "R-32", "R-22", "R-407C" -> Icons.Default.Opacity
                    else -> null
                }

                Surface(
                    onClick = { onSelected(option) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) displayColor.copy(alpha = 0.15f) else displayColor,
                    contentColor = if (isSelected) displayColor else contentColor,
                    border = border,
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) displayColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = option,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

