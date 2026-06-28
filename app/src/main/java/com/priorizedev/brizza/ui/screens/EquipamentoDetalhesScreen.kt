package com.priorizedev.brizza.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.priorizedev.brizza.data.model.Equipamento
import com.priorizedev.brizza.data.model.Ambiente
import com.priorizedev.brizza.data.model.OrdemServico
import com.priorizedev.brizza.ui.viewmodel.ClimaGestViewModel
import java.text.SimpleDateFormat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.widget.Toast
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipamentoDetalhesScreen(
    equipamentoId: String,
    viewModel: ClimaGestViewModel,
    onBack: () -> Unit,
    onNavigateToOrdemDetalhes: (String) -> Unit,
    onNavigateToOrdensServico: () -> Unit
) {
    val equipamentoState = viewModel.getEquipamentoByIdFlow(equipamentoId).collectAsStateWithLifecycle(initialValue = null)
    val ordensState = viewModel.getOrdensPorEquipamentoFlow(equipamentoId).collectAsStateWithLifecycle(initialValue = emptyList())
    val clientes by viewModel.clientesState.collectAsStateWithLifecycle()
    val tecnicos by viewModel.tecnicosState.collectAsStateWithLifecycle()
    
    var specsExpanded by remember { mutableStateOf(true) }
    var locationExpanded by remember { mutableStateOf(true) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showAddOrdemDialog by remember { mutableStateOf(false) }
    var showEditEquipamentoDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    val equipamento = equipamentoState.value

    val ambienteState = remember(equipamento?.ambienteId) {
        val ambId = equipamento?.ambienteId ?: ""
        if (ambId.isNotBlank()) {
            viewModel.getAmbienteFlow(ambId)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.collectAsStateWithLifecycle(initialValue = null)

    if (showEditEquipamentoDialog && equipamento != null) {
        var tag by remember { mutableStateOf(equipamento.tag) }
        var tipo by remember { mutableStateOf(equipamento.tipo) }
        var marca by remember { mutableStateOf(equipamento.marca) }
        var modelo by remember { mutableStateOf(equipamento.modelo) }
        var capacidade by remember { mutableStateOf(equipamento.capacidadeBtu.toString()) }
        var dataInstalacao by remember { mutableStateOf(equipamento.dataInstalacao) }
        var serial by remember { mutableStateOf(equipamento.numeroSerie) }
        var observacoes by remember { mutableStateOf(equipamento.observacoes) }
        var status by remember { mutableStateOf(equipamento.status) }
        
        var potenciaW by remember { mutableStateOf(equipamento.potenciaW) }
        var tensao by remember { mutableStateOf(equipamento.tensao) }
        var sistema by remember { mutableStateOf(equipamento.sistema) }
        var fluido by remember { mutableStateOf(equipamento.fluidoRefrigerante) }
        var ciclo by remember { mutableStateOf(equipamento.ciclo) }

        var tipoDropdownExpanded by remember { mutableStateOf(false) }
        var marcaDropdownExpanded by remember { mutableStateOf(false) }
        var capacidadeDropdownExpanded by remember { mutableStateOf(false) }
        var statusDropdownExpanded by remember { mutableStateOf(false) }

        var tagError by remember { mutableStateOf(false) }

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
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        }

        val context = androidx.compose.ui.platform.LocalContext.current
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
                title = { Text("Adicionar Nova Marca", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = novaMarcaInput,
                        onValueChange = { novaMarcaInput = it },
                        label = { Text("Nome da Marca*") },
                        placeholder = { Text("Ex: Daitsu, Electrolux") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
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
                        }
                    ) {
                        Text("Adicionar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showNewBrandInput = false
                        novaMarcaInput = ""
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Editar Equipamento",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { showEditEquipamentoDialog = false }) {
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

                        item {
                            HighlightedSectionHeader(
                                title = "Informações Básicas",
                                icon = Icons.Default.Info,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

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

                        item {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = marca,
                                    onValueChange = {
                                        marca = it
                                        marcaDropdownExpanded = true
                                    },
                                    label = { Text("Marca*", fontSize = 11.sp) },
                                    trailingIcon = {
                                        IconButton(onClick = { marcaDropdownExpanded = !marcaDropdownExpanded }) {
                                            Icon(
                                                imageVector = if (marcaDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                contentDescription = "Expandir"
                                            )
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
                                        .heightIn(max = 280.dp)
                                ) {
                                    if (filteredMarcasList.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Sem marcas correspondentes") },
                                            onClick = {}
                                        )
                                    } else {
                                        filteredMarcasList.forEach { m ->
                                            DropdownMenuItem(
                                                text = { Text(m) },
                                                onClick = {
                                                    marca = m
                                                    marcaDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Text("Cadastrar Nova Marca...", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        },
                                        onClick = {
                                            marcaDropdownExpanded = false
                                            showNewBrandInput = true
                                        }
                                    )
                                }
                            }
                        }

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

                        item {
                            HighlightedSectionHeader(
                                title = "Detalhes Técnicos",
                                icon = Icons.Default.Build,
                                color = Color(0xFFE65100)
                            )
                        }

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

                        item {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = status,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Status Operacional*", fontSize = 11.sp) },
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
                                    listOf("Ativo", "Inativo", "Manutenção").forEach { st ->
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

                        item {
                            SelectableBadgeRow(
                                title = "Voltagem / Tensão Elétrica*",
                                options = listOf("220V", "110V", "380V"),
                                selectedValue = tensao,
                                onSelected = { tensao = it },
                                activeColor = MaterialTheme.colorScheme.primary,
                                textColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        item {
                            SelectableBadgeRow(
                                title = "Tecnologia do Sistema*",
                                options = listOf("Inverter", "Convencional"),
                                selectedValue = sistema,
                                onSelected = { sistema = it },
                                activeColor = MaterialTheme.colorScheme.primary,
                                textColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }

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

                        item {
                            OutlinedTextField(
                                value = potenciaW,
                                onValueChange = { potenciaW = it },
                                label = { Text("Potência de Funcionamento (Watts)", fontSize = 11.sp) },
                                placeholder = { Text("Ex: 1050") },
                                textStyle = fieldTextStyle,
                                colors = borderColors,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = dataInstalacao,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Data de Instalação", fontSize = 11.sp) },
                                trailingIcon = {
                                    IconButton(onClick = { datePickerDialog.show() }) {
                                        Icon(Icons.Default.DateRange, contentDescription = "Selecionar data")
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

                        item {
                            OutlinedTextField(
                                value = observacoes,
                                onValueChange = { observacoes = it },
                                label = { Text("Observações Técnicas / Histórico", fontSize = 11.sp) },
                                placeholder = { Text("Ex: Instalado no suporte de parede reforçado, livre de umidade.") },
                                textStyle = fieldTextStyle,
                                colors = borderColors,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                maxLines = 4
                            )
                        }

                        item { Spacer(modifier = Modifier.height(10.dp)) }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showEditEquipamentoDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (tag.isBlank()) {
                                    tagError = true
                                    Toast.makeText(context, "A TAG do equipamento é obrigatória!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val updated = equipamento.copy(
                                        tag = tag.trim().uppercase(),
                                        marca = marca.trim(),
                                        modelo = modelo.trim(),
                                        numeroSerie = serial.trim(),
                                        tipo = tipo,
                                        capacidadeBtu = capacidade.toIntOrNull() ?: equipamento.capacidadeBtu,
                                        fluidoRefrigerante = fluido,
                                        status = status,
                                        dataInstalacao = dataInstalacao,
                                        observacoes = observacoes.trim(),
                                        potenciaW = potenciaW.trim(),
                                        tensao = tensao,
                                        sistema = sistema,
                                        ciclo = ciclo
                                    )
                                    viewModel.salvarEquipamento(updated)
                                    showEditEquipamentoDialog = false
                                    Toast.makeText(context, "Equipamento editador com sucesso!", Toast.LENGTH_SHORT).show()
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
    } else {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ficha do Equipamento",
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
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Mais Opções",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Adicionar Ordem de Serviço") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.requestNewOrderFlow(equipamento)
                                    onNavigateToOrdensServico()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Editar Equipamento") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    showEditEquipamentoDialog = true
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
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
        if (equipamento == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Carregando informações...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            val ambiente = ambienteState.value
            val cliente = clientes.find { it.id == equipamento.clienteId }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // HIGHLIGHT CARD: Modern white card with Brand & BTUs side-by-side, TAG below, and Status on the right
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(22.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(22.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Brand and BTUs side-by-side without snowflake (asterisk) icon - larger font
                                Text(
                                    text = "${equipamento.marca} ${equipamento.capacidadeBtu}",
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                // Ambiente under Brand and BTUs
                                Text(
                                    text = ambiente?.nome ?: "Ambiente não cadastrado",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Spacer(modifier = Modifier.height(1.dp))
                                
                                // Tag under brand and BTUs, close to the other texts
                                Text(
                                    text = "TAG: ${equipamento.tag}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Status Badge on the right side
                            val statusColors = when (equipamento.status.lowercase()) {
                                "ativo" -> Color(0xFF4CAF50) to Color(0xFFE8F5E9)
                                "inativo" -> Color(0xFF9E9E9E) to Color(0xFFF5F5F5)
                                "manutenção", "manutencao" -> Color(0xFFFF9800) to Color(0xFFFFF3E0)
                                else -> Color(0xFF4CAF50) to Color(0xFFE8F5E9)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(statusColors.second)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(statusColors.first)
                                    )
                                    Text(
                                        text = equipamento.status.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColors.first
                                    )
                                }
                            }
                        }
                    }
                }

                // Section: Informações Técnicas (Technical Specifics)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { specsExpanded = !specsExpanded }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Especificações Técnicas",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = if (specsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (specsExpanded) "Recolher" else "Expandir",
                            tint = Color(0xFF2563EB)
                        )
                    }
                }

                if (specsExpanded) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                TechnicalSpecItem(
                                    label = "Modelo",
                                    value = equipamento.modelo.ifEmpty { "Não Informado" },
                                    icon = Icons.Default.Description
                                )
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                TechnicalSpecItem(
                                    label = "Tipo",
                                    value = equipamento.tipo,
                                    icon = Icons.Default.Kitchen
                                )
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                TechnicalSpecItem(
                                    label = "Gás Refrigerante",
                                    value = equipamento.fluidoRefrigerante,
                                    icon = Icons.Default.Opacity
                                )
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                TechnicalSpecItem(
                                    label = "Ciclo",
                                    value = equipamento.ciclo,
                                    icon = Icons.Default.Sync
                                )
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                TechnicalSpecItem(
                                    label = "Sistema",
                                    value = equipamento.sistema,
                                    icon = Icons.Default.Settings
                                )
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                TechnicalSpecItem(
                                    label = "Tensão / Potência",
                                    value = "${equipamento.tensao}V / ${if (equipamento.potenciaW.isEmpty()) "N/I" else "${equipamento.potenciaW}W"}",
                                    icon = Icons.Default.Bolt
                                )
                            }
                        }
                    }
                }

                // Section: Localização e Responsabilidade
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { locationExpanded = !locationExpanded }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Instalação & Localização",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = if (locationExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (locationExpanded) "Recolher" else "Expandir",
                            tint = Color(0xFF2563EB)
                        )
                    }
                }

                if (locationExpanded) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                TechnicalSpecItem(
                                    label = "Cliente",
                                    value = cliente?.nome ?: "Não Informado",
                                    icon = Icons.Default.Business,
                                    valueUnderneath = true
                                )
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                TechnicalSpecItem(
                                    label = "Ambiente",
                                    value = ambiente?.nome ?: "Não Informado",
                                    icon = Icons.Default.Room
                                )
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                TechnicalSpecItem(
                                    label = "Data de Instalação",
                                    value = equipamento.dataInstalacao.ifEmpty { "Não Registrada" },
                                    icon = Icons.Default.CalendarToday
                                )
                            }
                        }
                    }
                }

                // Observations
                if (equipamento.observacoes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Observações Técnicas / Notas",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                        )
                    }
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = equipamento.observacoes,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                // Bottom Section: Work Orders List (Ordens de Serviço do Equipamento)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Histórico de Ordens de Serviço",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "${ordensState.value.size}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (ordensState.value.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Nenhuma Ordem de Serviço registrada para esta máquina.",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(ordensState.value) { os ->
                        val statusColor = when (os.status) {
                            "Aberta" -> Color(0xFF2563EB)
                            "Em Andamento", "Em andamento", "Pendente" -> Color(0xFFF59E0B)
                            "Concluída", "Concluído", "Concluido" -> Color(0xFF10B981)
                            else -> Color(0xFF6B7280)
                        }
                        
                        val statusLabel = when (os.status) {
                            "Aberta" -> "Aberta"
                            "Em Andamento", "Em andamento", "Pendente" -> "Pendente"
                            "Concluída", "Concluído", "Concluido" -> "Concluída"
                            else -> os.status
                        }

                        val tecnico = tecnicos.find { it.id == os.tecnicoId }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onNavigateToOrdemDetalhes(os.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                            ) {
                                // Left colored stripe/border matching status
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .fillMaxHeight()
                                        .background(statusColor)
                                )

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        // Title: Service Type | Scheduled Date
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val displayTipo = if (os.tipoServico.uppercase() == "PROGRAMADA") "Programada" else os.tipoServico
                                            val tipoColor = if (displayTipo == "Programada") Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurface
                                            Text(
                                                text = displayTipo,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = tipoColor
                                            )
                                            Text(
                                                text = "| ${os.dataAgendada}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color.Gray
                                            )
                                        }
 
                                        // Brand & BTU
                                        Text(
                                            text = "${equipamento.marca} ${equipamento.capacidadeBtu} BTU/h",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
 
                                        // Tech Name
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Téc:",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = tecnico?.nome ?: "Não Atribuído",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
 
                                    // Status Badge on the right
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(statusColor)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // End spacing in LazyColumn
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
    }

    if (showDeleteConfirmDialog && equipamento != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Excluir Equipamento") },
            text = { Text("Aviso importante: Ao excluir o equipamento \"${equipamento.marca} (${equipamento.tag})\", todas as ordens de serviço vinculadas a ele também serão excluídas definitivamente. Deseja prosseguir?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.excluirEquipamentoComOrdens(equipamento, ordensState.value)
                        showDeleteConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showEditEquipamentoDialog && equipamento != null) {
        // old dialog code replaced
    }

    if (showAddOrdemDialog && equipamento != null) {
        var selectedTecnico by remember { mutableStateOf(tecnicos.firstOrNull()) }
        var tipoServico by remember { mutableStateOf("Corretiva") }
        var prioridade by remember { mutableStateOf("Média") }
        var descricao by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Aberta") }
        var dataAgendada by remember {
            mutableStateOf(
                java.text.SimpleDateFormat(
                    "dd/MM/yyyy",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())
            )
        }

        var tecExpanded by remember { mutableStateOf(false) }
        var tipoExpanded by remember { mutableStateOf(false) }
        var prioExpanded by remember { mutableStateOf(false) }
        val context = androidx.compose.ui.platform.LocalContext.current

        AlertDialog(
            onDismissRequest = { showAddOrdemDialog = false },
            title = { Text("Nova Ordem de Serviço", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedTecnico?.nome ?: "Selecionar Técnico...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Técnico Responsável") },
                            trailingIcon = {
                                IconButton(onClick = { tecExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tecExpanded = true }
                        )
                        DropdownMenu(
                            expanded = tecExpanded,
                            onDismissRequest = { tecExpanded = false }
                        ) {
                            if (tecnicos.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Nenhum técnico cadastrado") },
                                    onClick = { tecExpanded = false }
                                )
                            } else {
                                tecnicos.forEach { tec ->
                                    DropdownMenuItem(
                                        text = { Text(tec.nome) },
                                        onClick = {
                                            selectedTecnico = tec
                                            tecExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = tipoServico,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de Serviço") },
                            trailingIcon = {
                                IconButton(onClick = { tipoExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tipoExpanded = true }
                        )
                        DropdownMenu(
                            expanded = tipoExpanded,
                            onDismissRequest = { tipoExpanded = false }
                        ) {
                            listOf("Preventiva", "Corretiva", "Instalação", "Orçamento").forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t) },
                                    onClick = {
                                        tipoServico = t
                                        tipoExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = prioridade,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Prioridade") },
                            trailingIcon = {
                                IconButton(onClick = { prioExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { prioExpanded = true }
                        )
                        DropdownMenu(
                            expanded = prioExpanded,
                            onDismissRequest = { prioExpanded = false }
                        ) {
                            listOf("Baixa", "Média", "Alta").forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p) },
                                    onClick = {
                                        prioridade = p
                                        prioExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = dataAgendada,
                        onValueChange = { dataAgendada = it },
                        label = { Text("Data Agendada") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = descricao,
                        onValueChange = { descricao = it },
                        label = { Text("Descrição / Sintomas") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedTecnico == null) {
                            Toast.makeText(context, "Por favor, cadastre ou selecione um Técnico!", Toast.LENGTH_SHORT).show()
                        } else if (descricao.isBlank()) {
                            Toast.makeText(context, "O campo Descrição é obrigatório!", Toast.LENGTH_SHORT).show()
                        } else {
                            val novaOrdem = com.priorizedev.brizza.data.model.OrdemServico(
                                id = com.priorizedev.brizza.data.model.generateOrdemServicoId(),
                                clienteId = equipamento.clienteId,
                                tecnicoId = selectedTecnico!!.id,
                                equipamentoId = equipamento.id,
                                tipoServico = tipoServico,
                                dataAgendada = dataAgendada.trim(),
                                descricao = descricao.trim(),
                                status = status,
                                prioridade = prioridade
                            )
                            viewModel.salvarOrdemServico(novaOrdem)
                            showAddOrdemDialog = false
                            Toast.makeText(context, "Ordem de serviço aberta com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Criar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddOrdemDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TechnicalSpecItem(
    label: String,
    value: String,
    icon: ImageVector,
    valueUnderneath: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
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
