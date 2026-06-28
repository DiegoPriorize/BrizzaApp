package com.priorizedev.brizza.ui.screens

import androidx.compose.animation.animateContentSize
import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.priorizedev.brizza.data.model.Cliente
import com.priorizedev.brizza.data.model.Equipamento
import com.priorizedev.brizza.data.model.OrdemServico
import com.priorizedev.brizza.data.model.Peca
import com.priorizedev.brizza.data.model.Tecnico
import com.priorizedev.brizza.ui.viewmodel.ClimaGestViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdemFormScreen(
    ordemId: String?,
    preselectedEquipamentoId: String?,
    viewModel: ClimaGestViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val clientes by viewModel.clientesState.collectAsStateWithLifecycle()
    val tecnicos by viewModel.tecnicosState.collectAsStateWithLifecycle()
    val todosEquipamentos by viewModel.equipamentosState.collectAsStateWithLifecycle()
    val ambientes by viewModel.ambientesState.collectAsStateWithLifecycle()

    // Loaded existing O.S. if present
    val existingOsState = remember(ordemId) {
        if (ordemId != null && ordemId.isNotBlank()) {
            viewModel.getOrdemServicoFlow(ordemId)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.collectAsStateWithLifecycle(initialValue = null)

    var hasLoadedExisting by remember { mutableStateOf(false) }

    // Screen Form States
    var selectedCliente by remember { mutableStateOf<Cliente?>(null) }
    var selectedEquipamentos by remember { mutableStateOf<List<Equipamento>>(emptyList()) }
    var selectedTecnico by remember { mutableStateOf<Tecnico?>(null) }
    var tipoServico by remember { mutableStateOf("Corretiva") }
    var statusState by remember { mutableStateOf("Aberta") }
    var prioridadeState by remember { mutableStateOf("Média") }
    
    var dataChamado by remember { mutableStateOf("") }
    var dataAgendada by remember { mutableStateOf("") }
    var descricaoResumida by remember { mutableStateOf("") }
    var valorServico by remember { mutableStateOf("") }
    var descricaoCompleta by remember { mutableStateOf("") }

    // Medições Realizadas fields
    var measurementsExpanded by remember { mutableStateOf(false) }
    var pecasExpanded by remember { mutableStateOf(false) }
    var fotosExpanded by remember { mutableStateOf(false) }
    var descricoesExpanded by remember { mutableStateOf(false) }
    var valorServicosExpanded by remember { mutableStateOf(false) }
    var correnteEletrica by remember { mutableStateOf("") }
    var temperatura by remember { mutableStateOf("") }
    var pressaoGasAlta by remember { mutableStateOf("") }
    var pressaoGasBaixa by remember { mutableStateOf("") }

    // Photos state list
    var foto1Uri by remember { mutableStateOf("") }
    var foto2Uri by remember { mutableStateOf("") }
    var foto3Uri by remember { mutableStateOf("") }

    var selectedPecas by remember { mutableStateOf<List<Peca>>(emptyList()) }

    // Dropdown open states
    var dropdownCliExpanded by remember { mutableStateOf(false) }
    var dropdownEqExpanded by remember { mutableStateOf(false) }
    var dropdownTecExpanded by remember { mutableStateOf(false) }

    // Validation focus markers
    var showClienteError by remember { mutableStateOf(false) }
    var showTecnicoError by remember { mutableStateOf(false) }
    var showDataChamadoError by remember { mutableStateOf(false) }

    // Track active photo view expansions
    var expandedPhotoUri by remember { mutableStateOf<String?>(null) }
    var expandedPhotoLabel by remember { mutableStateOf("") }

    // Handle initial loading & pre-selection flow
    LaunchedEffect(existingOsState.value, clientes, todosEquipamentos, tecnicos) {
        val o = existingOsState.value
        if (o != null && !hasLoadedExisting) {
            selectedCliente = clientes.find { it.id == o.clienteId }
            selectedEquipamentos = listOfNotNull(todosEquipamentos.find { it.id == o.equipamentoId })
            selectedTecnico = tecnicos.find { it.id == o.tecnicoId }
            tipoServico = o.tipoServico
            statusState = o.status
            prioridadeState = o.prioridade
            dataChamado = o.dataChamado
            dataAgendada = o.dataAgendada
            descricaoResumida = o.descricaoResumida
            valorServico = if (o.valorServico > 0.0) o.valorServico.toString() else ""
            descricaoCompleta = o.descricao
            correnteEletrica = o.correnteEletrica
            temperatura = o.temperatura
            pressaoGasAlta = o.pressaoGasAlta
            pressaoGasBaixa = o.pressaoGasBaixa
            foto1Uri = o.foto1
            foto2Uri = o.foto2
            foto3Uri = o.foto3
            selectedPecas = deserializeSelectedPecas(o.pecasTrocadas)
            hasLoadedExisting = true
        } else if (o == null && !hasLoadedExisting) {
            // New order creation preselected equipment configuration
            if (preselectedEquipamentoId != null && preselectedEquipamentoId.isNotBlank() && preselectedEquipamentoId != "0") {
                val preEq = todosEquipamentos.find { it.id == preselectedEquipamentoId }
                if (preEq != null) {
                    selectedEquipamentos = listOf(preEq)
                    selectedCliente = clientes.find { it.id == preEq.clienteId }
                }
            }
            // Pre-populate call date with today
            if (dataChamado.isEmpty()) {
                val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                dataChamado = today
            }
        }
    }

    // Filtered equipment list for selected client
    val clientEquips = remember(selectedCliente, todosEquipamentos) {
        if (selectedCliente == null) emptyList()
        else todosEquipamentos.filter { it.clienteId == selectedCliente!!.id }
    }

    // Date picker helper closure
    val triggerDatePicker = { onSelected: (String) -> Unit ->
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val dateStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year)
                onSelected(dateStr)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Image Pickers selectors
    val launcherFoto1 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { foto1Uri = com.priorizedev.brizza.data.api.SupabaseSyncClient.copyUriToLocalCache(context, it.toString()) }
    }
    val launcherFoto2 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { foto2Uri = com.priorizedev.brizza.data.api.SupabaseSyncClient.copyUriToLocalCache(context, it.toString()) }
    }
    val launcherFoto3 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { foto3Uri = com.priorizedev.brizza.data.api.SupabaseSyncClient.copyUriToLocalCache(context, it.toString()) }
    }

    // Keyboard dismissal on scroll state trigger
    val scrollState = rememberScrollState()
    val isScrollInProgress = scrollState.isScrollInProgress
    LaunchedEffect(isScrollInProgress) {
        if (isScrollInProgress) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (ordemId == null || ordemId.isBlank()) "Nova Ordem de Serviço" else "Editar Ordem de Serviço #${ordemId}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (ordemId == null || ordemId.isBlank()) "Preencha os campos obrigatórios (*)" else "Atualize os dados e clique em Salvar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
        ) {
            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Identificação de Atendimento (Cliente e Equipamento)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "IDENTIFICAÇÃO DE ATENDIMENTO",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 1. SELECT CLIENT (Required)
                        Column {
                            Text(
                                text = "Selecione o Cliente *",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (showClienteError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { 
                                        dropdownCliExpanded = true 
                                        focusManager.clearFocus()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(
                                        width = if (showClienteError) 2.dp else 1.dp,
                                        color = if (showClienteError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = selectedCliente?.nome ?: "Selecionar cliente da lista",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                DropdownMenu(
                                    expanded = dropdownCliExpanded,
                                    onDismissRequest = { dropdownCliExpanded = false },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    if (clientes.isEmpty()) {
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                                            text = { Text("Nenhum cliente cadastrado") },
                                            onClick = { dropdownCliExpanded = false }
                                        )
                                    } else {
                                        clientes.forEach { cli ->
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(Icons.Default.PersonOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                                text = { Text(cli.nome, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                                onClick = {
                                                    selectedCliente = cli
                                                    selectedEquipamentos = emptyList() // Reset selected equipment when client shifts
                                                    showClienteError = false
                                                    dropdownCliExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            if (showClienteError) {
                                Text(
                                    text = "O cliente é obrigatório!",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                )
                            }
                        }

                        // 2. SELECT SINGLE EQUIPMENT (with environment info)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Equipamento / Máquina",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (selectedEquipamentos.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { 
                                            dropdownEqExpanded = true 
                                            focusManager.clearFocus()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Kitchen,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Selecionar máquina do cliente",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = dropdownEqExpanded,
                                        onDismissRequest = { dropdownEqExpanded = false },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), RoundedCornerShape(16.dp)),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        if (selectedCliente == null) {
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                                text = { Text("Selecione um cliente primeiro") },
                                                onClick = { dropdownEqExpanded = false }
                                            )
                                        } else if (clientEquips.isEmpty()) {
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                                                text = { Text("Nenhum equipamento cadastrado para este cliente") },
                                                onClick = { dropdownEqExpanded = false }
                                            )
                                        } else {
                                            clientEquips.forEach { eq ->
                                                val eqAmbiente = ambientes.find { it.id == eq.ambienteId }
                                                val ambText = if (eqAmbiente != null) " (${eqAmbiente.nome})" else ""
                                                DropdownMenuItem(
                                                    leadingIcon = { Icon(Icons.Default.Kitchen, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                                    text = { Text("${eq.tag} - ${eq.marca}$ambText", fontWeight = FontWeight.Medium) },
                                                    onClick = {
                                                        selectedEquipamentos = listOf(eq)
                                                        dropdownEqExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                selectedEquipamentos.forEach { eq ->
                                    val eqAmbiente = ambientes.find { it.id == eq.ambienteId }
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${eq.tag} - ${eq.marca} (${eq.capacidadeBtu} BTUs)",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (eqAmbiente != null) {
                                                    Row(
                                                        modifier = Modifier.padding(top = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Place,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp),
                                                            tint = MaterialTheme.colorScheme.secondary
                                                        )
                                                        Text(
                                                            text = "Ambiente: ${eqAmbiente.nome}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    selectedEquipamentos = emptyList()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remover",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Informações de Atendimento (Tecnico, Datas, Tipo, Status)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "DETALHES DO ATENDIMENTO",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 1. SELECT RESPONSABLE TECHNICIAN (Required)
                        Column {
                            Text(
                                text = "Técnico Responsável *",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (showTecnicoError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { 
                                        dropdownTecExpanded = true 
                                        focusManager.clearFocus()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(
                                        width = if (showTecnicoError) 2.dp else 1.dp,
                                        color = if (showTecnicoError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Engineering,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = selectedTecnico?.nome ?: "Atribuir técnico responsável",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                DropdownMenu(
                                    expanded = dropdownTecExpanded,
                                    onDismissRequest = { dropdownTecExpanded = false },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    if (tecnicos.isEmpty()) {
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                                            text = { Text("Nenhum técnico cadastrado") },
                                            onClick = { dropdownTecExpanded = false }
                                        )
                                    } else {
                                        tecnicos.forEach { tec ->
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(Icons.Default.Engineering, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                                text = { Text(tec.nome, fontWeight = FontWeight.Medium) },
                                                onClick = {
                                                    selectedTecnico = tec
                                                    showTecnicoError = false
                                                    dropdownTecExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            if (showTecnicoError) {
                                Text(
                                    text = "O técnico responsável é obrigatório!",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                )
                            }
                        }

                        // 2. TIPO DE ATENDIMENTO (Required Selectors with Horizontal Radio Row)
                        Column {
                            Text(
                                text = "Tipo de Serviço *",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Corretiva", "Preventiva", "Instalação", "Orçamento").forEach { t ->
                                    val isSelected = tipoServico == t
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                                                else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { tipoServico = t }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = t,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // 3. DATAS: CHAMADO (Required) & CONCLUSAO (Optional) (Task 2: deixa data do chamado em uma linha e agendamento na linha de baixo, deixar apenas data conclusão)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Data do Chamado *",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (showDataChamadoError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = dataChamado,
                                    onValueChange = { 
                                        dataChamado = it 
                                        if (it.isNotEmpty()) showDataChamadoError = false
                                    },
                                    placeholder = { Text("dd/mm/aaaa") },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    isError = showDataChamadoError,
                                    trailingIcon = {
                                        IconButton(onClick = { triggerDatePicker { dataChamado = it; showDataChamadoError = false } }) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = "Selecionar data", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (showDataChamadoError) {
                                    Text(
                                        text = "Obrigatório",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Data de Conclusão",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = dataAgendada,
                                    onValueChange = { dataAgendada = it },
                                    placeholder = { Text("dd/mm/aaaa") },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { triggerDatePicker { dataAgendada = it } }) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = "Selecionar data",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // 4. STATUS DA O.S. (Required - designed in horizontal Row/scroll to prevent "Pendente" wrapping!) (Task 3: status Em andamento alterado para Pendente, sem negrito, so o selecionado)
                        Column {
                            Text(
                                text = "Status do Atendimento *",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Aberta", "Pendente", "Concluída", "Cancelada").forEach { s ->
                                    val isSelected = statusState.lowercase() == s.lowercase()
                                    val itemColor = when (s) {
                                        "Aberta" -> Color(0xFF2563EB)
                                        "Pendente" -> Color(0xFFF59E0B)
                                        "Concluída" -> Color(0xFF10B981)
                                        else -> Color(0xFF9E9E9E)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                1.dp,
                                                if (isSelected) itemColor else MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .background(
                                                if (isSelected) itemColor.copy(alpha = 0.12f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { statusState = s }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = s,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) itemColor else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // PRIORIDADE
                        Column {
                            Text(
                                text = "Prioridade",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Baixa", "Média", "Alta").forEach { p ->
                                    val isSelected = prioridadeState == p
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { prioridadeState = p }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = p,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // COLLAPSIBLE SECTION 1: Descrições
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.animateContentSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { descricoesExpanded = !descricoesExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "DESCRIÇÕES",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (descricoesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (descricoesExpanded) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Descrição do Sintoma / Chamado (Opcional)
                                OutlinedTextField(
                                    value = descricaoCompleta,
                                    onValueChange = { descricaoCompleta = it },
                                    label = { Text("Ocorrência / Descrição Completa") },
                                    shape = RoundedCornerShape(12.dp),
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Descrição Resumida (Opcional)
                                OutlinedTextField(
                                    value = descricaoResumida,
                                    onValueChange = { descricaoResumida = it },
                                    label = { Text("Breve resumo do serviço executado") },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // COLLAPSIBLE SECTION 2: Medições Técnicas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.animateContentSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { measurementsExpanded = !measurementsExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "MEDIÇÕES TÉCNICAS",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (measurementsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (measurementsExpanded) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 1. Corrente Elétrica
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = correnteEletrica,
                                        onValueChange = { correnteEletrica = it },
                                        label = { Text("Corrente Elétrica (A)") },
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // 2. Temperatura de Saída
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Thermostat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = temperatura,
                                        onValueChange = { temperatura = it },
                                        label = { Text("Temperatura de Saída (°C)") },
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // 3. Pressão de Gás Alta
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = pressaoGasAlta,
                                        onValueChange = { pressaoGasAlta = it },
                                        label = { Text("Pressão de Gás Alta (Psi)") },
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // 4. Pressão de Gás Baixa
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = pressaoGasBaixa,
                                        onValueChange = { pressaoGasBaixa = it },
                                        label = { Text("Pressão de Gás Baixa (Psi)") },
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // COLLAPSIBLE SECTION 3: Peças e Acessórios
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.animateContentSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pecasExpanded = !pecasExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "PEÇAS E ACESSÓRIOS",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (pecasExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (pecasExpanded) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val allRegisteredPecas by viewModel.pecasState.collectAsStateWithLifecycle()

                                Text(
                                    text = "Peças Utilizadas de Reposição *",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Adicionar Peça trigger
                                var pecasDropdownExpanded by remember { mutableStateOf(false) }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { pecasDropdownExpanded = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(44.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Adicionar Peça da Lista", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    DropdownMenu(
                                        expanded = pecasDropdownExpanded,
                                        onDismissRequest = { pecasDropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        if (allRegisteredPecas.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Nenhuma peça cadastrada. Acesse Navegação -> Peças.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) },
                                                onClick = { pecasDropdownExpanded = false }
                                            )
                                        } else {
                                            allRegisteredPecas.forEach { pecaItem ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("${pecaItem.nome} - R$ ${pecaItem.preco}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                            if (pecaItem.marca.isNotEmpty()) {
                                                                Text(pecaItem.marca, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedPecas = selectedPecas + pecaItem
                                                        pecasDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Lista de peças inseridas
                                if (selectedPecas.isEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Nenhuma peça adicionada a esta ordem",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        selectedPecas.forEachIndexed { index, peca ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        // 1º linha: só o nome
                                                        Text(
                                                            text = peca.nome,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        // 2º linha: marca e preço
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "Marca: ${peca.marca.ifEmpty { "Generica" }}",
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                            Text(
                                                                text = String.format("R$ %.2f", peca.preco),
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 13.sp,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(16.dp))

                                                    // X pequeno pra excluir
                                                    IconButton(
                                                        onClick = {
                                                            selectedPecas = selectedPecas.filterIndexed { idx, _ -> idx != index }
                                                        },
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Remover",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(14.dp)
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

                // COLLAPSIBLE SECTION 4: Fotos do Atendimento
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.animateContentSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { fotosExpanded = !fotosExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "FOTOS DO ATENDIMENTO",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (fotosExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (fotosExpanded) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Anexe até 3 fotos do diagnóstico ou serviço",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Slot 1
                                    DiagnosticPhotoFormCard(
                                        label = "Foto 1",
                                        uri = foto1Uri,
                                        modifier = Modifier.weight(1f),
                                        onAdd = { launcherFoto1.launch("image/*") },
                                        onClick = {
                                            expandedPhotoUri = foto1Uri
                                            expandedPhotoLabel = "Foto 1"
                                        },
                                        onRemove = { foto1Uri = "" }
                                    )

                                    // Slot 2
                                    DiagnosticPhotoFormCard(
                                        label = "Foto 2",
                                        uri = foto2Uri,
                                        modifier = Modifier.weight(1f),
                                        onAdd = { launcherFoto2.launch("image/*") },
                                        onClick = {
                                            expandedPhotoUri = foto2Uri
                                            expandedPhotoLabel = "Foto 2"
                                        },
                                        onRemove = { foto2Uri = "" }
                                    )

                                    // Slot 3
                                    DiagnosticPhotoFormCard(
                                        label = "Foto 3",
                                        uri = foto3Uri,
                                        modifier = Modifier.weight(1f),
                                        onAdd = { launcherFoto3.launch("image/*") },
                                        onClick = {
                                            expandedPhotoUri = foto3Uri
                                            expandedPhotoLabel = "Foto 3"
                                        },
                                        onRemove = { foto3Uri = "" }
                                    )
                                }
                            }
                        }
                    }
                }

                // COLLAPSIBLE SECTION 5: Valor dos Serviços
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.animateContentSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { valorServicosExpanded = !valorServicosExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "VALOR DOS SERVIÇOS",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (valorServicosExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (valorServicosExpanded) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Digite o valor total da mão de obra",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = valorServico,
                                    onValueChange = { valorServico = it },
                                    label = { Text("Valor do serviço (R$)") },
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    leadingIcon = { Text("R$ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Buttons Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                border = BorderStroke(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            // Validation checks
                            showClienteError = selectedCliente == null
                            showTecnicoError = selectedTecnico == null
                            showDataChamadoError = dataChamado.isEmpty()

                            if (selectedCliente != null && selectedTecnico != null && dataChamado.isNotEmpty()) {
                                val valDbl = valorServico.toDoubleOrNull() ?: 0.0
                                val equipsToSave = selectedEquipamentos.ifEmpty { listOf(null) }
                                
                                val firstId = ordemId ?: com.priorizedev.brizza.data.model.generateOrdemServicoId()
                                val numeroOrdemBase = existingOsState.value?.numeroOrdem ?: com.priorizedev.brizza.data.model.generateRandomNumeroOrdem()

                                equipsToSave.forEachIndexed { idx, eq ->
                                    val saveOrdem = OrdemServico(
                                        id = if (idx == 0) firstId else com.priorizedev.brizza.data.model.generateOrdemServicoId(),
                                        numeroOrdem = numeroOrdemBase,
                                        clienteId = selectedCliente!!.id,
                                        tecnicoId = selectedTecnico!!.id,
                                        equipamentoId = eq?.id ?: "",
                                        tipoServico = tipoServico,
                                        status = statusState,
                                        prioridade = prioridadeState,
                                        dataChamado = dataChamado,
                                        dataAgendada = dataAgendada,
                                        descricaoResumida = descricaoResumida,
                                        descricao = descricaoCompleta,
                                        valorServico = valDbl,
                                        correnteEletrica = correnteEletrica,
                                        temperatura = temperatura,
                                        pressaoGasAlta = pressaoGasAlta,
                                        pressaoGasBaixa = pressaoGasBaixa,
                                        foto1 = foto1Uri,
                                        foto2 = foto2Uri,
                                        foto3 = foto3Uri,
                                        diagnosticoTecnico = existingOsState.value?.diagnosticoTecnico ?: "",
                                        pecasTrocadas = serializeSelectedPecas(selectedPecas),
                                        assinaturaDigital = existingOsState.value?.assinaturaDigital ?: "",
                                        assinaturaClienteNome = existingOsState.value?.assinaturaClienteNome ?: ""
                                    )
                                    viewModel.salvarOrdemServico(saveOrdem)
                                }
                                Toast.makeText(
                                    context,
                                    if (ordemId == null || ordemId.isBlank()) "Ordem de Serviço criada com sucesso!" else "Ordem de Serviço atualizada com sucesso!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onBack()
                            } else {
                                Toast.makeText(context, "Por favor, corrija os erros no formulário", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salvar", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }

    // Photo Dialog expansion view
    expandedPhotoUri?.let { uri ->
        Dialog(onDismissRequest = { expandedPhotoUri = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = uri,
                            contentDescription = expandedPhotoLabel,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                        )
                        IconButton(
                            onClick = { expandedPhotoUri = null },
                            modifier = Modifier
                                .padding(12.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                        }
                    }
                    Text(
                        text = expandedPhotoLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                        )
                    }
                }
            }
        }
    }

@Composable
fun DiagnosticPhotoFormCard(
    label: String,
    uri: String,
    modifier: Modifier = Modifier,
    onAdd: () -> Unit = {},
    onClick: () -> Unit = {},
    onRemove: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .border(
                width = 1.dp,
                color = if (uri.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (uri.isNotEmpty()) onClick() else onAdd()
            },
        colors = CardDefaults.cardColors(
            containerColor = if (uri.isNotEmpty()) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (uri.isNotEmpty()) {
                AsyncImage(
                    model = uri,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Label Badge at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                // Delete handle
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(22.dp)
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.White, modifier = Modifier.size(12.dp))
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Adicionar foto",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Inserir\n$label",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        lineHeight = 10.sp
                    )
                }
            }
        }
    }
}

// Helpers for robust String Serialization of Parts in Ordem de Serviço (Task 4)
fun serializeSelectedPecas(list: List<Peca>): String {
    return list.joinToString("||") { "${it.id}|${it.nome.replace("|", "")}|${it.preco}|${it.marca.replace("|", "")}" }
}

fun deserializeSelectedPecas(serialized: String): List<Peca> {
    if (serialized.isBlank()) return emptyList()
    return try {
        serialized.split("||").mapNotNull { block ->
            val parts = block.split("|")
            if (parts.size >= 4) {
                Peca(
                    id = parts[0],
                    nome = parts[1],
                    preco = parts[2].toDoubleOrNull() ?: 0.0,
                    marca = parts[3]
                )
            } else if (parts.size >= 3) {
                Peca(
                    id = parts[0],
                    nome = parts[1],
                    preco = parts[2].toDoubleOrNull() ?: 0.0,
                    marca = ""
                )
            } else null
        }
    } catch (e: Exception) {
        emptyList()
    }
}
