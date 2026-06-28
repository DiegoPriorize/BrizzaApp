package com.priorizedev.brizza.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.priorizedev.brizza.data.model.Cliente
import com.priorizedev.brizza.data.model.Ambiente
import com.priorizedev.brizza.data.model.Equipamento
import com.priorizedev.brizza.data.model.OrdemServico
import com.priorizedev.brizza.data.model.Tecnico
import com.priorizedev.brizza.ui.components.SignaturePad
import com.priorizedev.brizza.ui.viewmodel.ClimaGestViewModel
import com.priorizedev.brizza.util.ReportUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OrdensServicoScreen(
    viewModel: ClimaGestViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToOrdemForm: (String?, String?) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    val ordens by viewModel.ordensServicoState.collectAsStateWithLifecycle()
    val clientes by viewModel.clientesState.collectAsStateWithLifecycle()
    val tecnicos by viewModel.tecnicosState.collectAsStateWithLifecycle()
    val equipamentos by viewModel.equipamentosState.collectAsStateWithLifecycle()
    val ambientes by viewModel.ambientesState.collectAsStateWithLifecycle()
    
    var statusFilter by remember { mutableStateOf("Todas") }
    var apenasProgramadas by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedClienteId by remember { mutableStateOf<String?>(null) }
    var selectedTecnicoId by remember { mutableStateOf<String?>(null) }
    var selectedDateStr by remember { mutableStateOf<String?>(null) }

    var filtersExpanded by remember { mutableStateOf(false) }
    var dateRangePreset by remember { mutableStateOf<String?>(null) } // "1m", "3m", "6m", "1y", or "personalizado"
    var customStartDate by remember { mutableStateOf<String?>(null) }
    var customEndDate by remember { mutableStateOf<String?>(null) }

    var clienteDropdownExpanded by remember { mutableStateOf(false) }
    var tecnicoDropdownExpanded by remember { mutableStateOf(false) }

    val shouldOpenNewOrderFlow by viewModel.shouldOpenNewOrderFlow.collectAsStateWithLifecycle()
    val preselectedEquip by viewModel.preselectedEquipamentoForNewOrder.collectAsStateWithLifecycle()

    LaunchedEffect(shouldOpenNewOrderFlow, preselectedEquip) {
        if (shouldOpenNewOrderFlow && preselectedEquip != null) {
            val equipId = preselectedEquip!!.id
            viewModel.clearNewOrderFlow()
            onNavigateToOrdemForm(null, equipId)
        }
    }

    val context = LocalContext.current
    val datePickerDialog = remember {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formattedDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                selectedDateStr = formattedDate
                dateRangePreset = null
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val customStartPickerDialog = remember {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                customStartDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val customEndPickerDialog = remember {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                customEndDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val filteredOrdens = ordens.filter { os ->
        val matchesStatus = statusFilter == "Todas" || 
                (statusFilter.lowercase() == "pendente" && (os.status.lowercase() == "pendente" || os.status.lowercase() == "em andamento")) ||
                os.status.lowercase() == statusFilter.lowercase()
        val matchesCliente = selectedClienteId == null || os.clienteId == selectedClienteId
        val matchesTecnico = selectedTecnicoId == null || os.tecnicoId == selectedTecnicoId
        
        val matchesDate = if (dateRangePreset != null) {
            val osDate = try {
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val dateStr = if (os.dataChamado.isNotBlank()) os.dataChamado else if (os.dataAgendada.isNotBlank()) os.dataAgendada else ""
                val cleanDateStr = dateStr.substringBefore(" ").trim()
                if (cleanDateStr.isNotBlank()) format.parse(cleanDateStr) else null
            } catch (e: Exception) {
                null
            }

            if (osDate != null) {
                val startCal = Calendar.getInstance()
                if (dateRangePreset == "1m") {
                    startCal.add(Calendar.MONTH, -1)
                } else if (dateRangePreset == "3m") {
                    startCal.add(Calendar.MONTH, -3)
                } else if (dateRangePreset == "6m") {
                    startCal.add(Calendar.MONTH, -6)
                } else if (dateRangePreset == "1y") {
                    startCal.add(Calendar.YEAR, -1)
                } else if (dateRangePreset == "personalizado") {
                    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val customStart = try { customStartDate?.let { format.parse(it) } } catch(e: Exception) { null }
                    val customEnd = try { customEndDate?.let { format.parse(it) } } catch(e: Exception) { null }
                    
                    val matchesStart = customStart == null || !osDate.before(customStart)
                    val matchesEnd = customEnd == null || !osDate.after(customEnd)
                    matchesStart && matchesEnd
                } else {
                    true
                }

                if (dateRangePreset != "personalizado") {
                    !osDate.before(startCal.time)
                } else {
                    true
                }
            } else {
                false
            }
        } else {
            selectedDateStr == null || 
            os.dataChamado.contains(selectedDateStr!!) || 
            os.dataAgendada.contains(selectedDateStr!!)
        }

        val clientName = clientes.find { it.id == os.clienteId }?.nome ?: ""
        val tecnicoName = tecnicos.find { it.id == os.tecnicoId }?.nome ?: ""
        val equip = equipamentos.find { it.id == os.equipamentoId }
        val ambName = ambientes.find { it.id == equip?.ambienteId }?.nome ?: ""
        val matchesSearch = searchQuery.isBlank() || 
            clientName.contains(searchQuery, ignoreCase = true) ||
            tecnicoName.contains(searchQuery, ignoreCase = true) ||
            os.numeroOrdem.contains(searchQuery, ignoreCase = true) ||
            ambName.contains(searchQuery, ignoreCase = true) ||
            (equip?.marca?.contains(searchQuery, ignoreCase = true) ?: false) ||
            (equip?.modelo?.contains(searchQuery, ignoreCase = true) ?: false) ||
            (equip?.tag?.contains(searchQuery, ignoreCase = true) ?: false) ||
            os.descricao.contains(searchQuery, ignoreCase = true) ||
            os.descricaoResumida.contains(searchQuery, ignoreCase = true) ||
            os.tipoServico.contains(searchQuery, ignoreCase = true)

        val matchesProgramada = !apenasProgramadas || os.tipoServico.equals("Programada", ignoreCase = true) || os.tipoServico.equals("PROGRAMADA", ignoreCase = true)

        matchesStatus && matchesCliente && matchesTecnico && matchesDate && matchesSearch && matchesProgramada
    }.sortedByDescending { it.dataCriacao }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Gerenciar Ordens de Serviço",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
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
            if (clientes.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToOrdemForm(null, null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nova O.S.", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Minimalist Search and Filter Toggle Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Pesquise por cliente, OS, ambiente, marca, técnico...", fontSize = 11.sp, maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Pesquisar",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpar pesquisa",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                )

                val activeFiltersCount = (if (statusFilter != "Todas") 1 else 0) +
                        (if (selectedClienteId != null) 1 else 0) +
                        (if (selectedTecnicoId != null) 1 else 0) +
                        (if (dateRangePreset != null || selectedDateStr != null) 1 else 0)

                IconButton(
                    onClick = { filtersExpanded = !filtersExpanded },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (filtersExpanded || activeFiltersCount > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    BadgedBox(
                        badge = {
                            if (activeFiltersCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ) {
                                    Text("$activeFiltersCount", fontSize = 9.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (filtersExpanded) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = "Filtros",
                            tint = if (filtersExpanded || activeFiltersCount > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = filtersExpanded,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Group 1: Status da O.S.
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Status da O.S.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Todas", "Aberta", "Pendente", "Concluída", "Cancelada").forEach { status ->
                                    val isSelected = statusFilter == status
                                    val statusColor = when (status) {
                                        "Aberta" -> Color(0xFF1976D2)
                                        "Pendente" -> Color(0xFFF57C00)
                                        "Concluída" -> Color(0xFF0F9D58)
                                        "Cancelada" -> Color(0xFFDC2626)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    SuggestionChip(
                                        onClick = { statusFilter = status },
                                        label = { Text(status, fontSize = 11.sp) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (isSelected) statusColor.copy(alpha = 0.12f) else Color.Transparent,
                                            labelColor = if (isSelected) statusColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        ),
                                        border = SuggestionChipDefaults.suggestionChipBorder(
                                            enabled = true,
                                            borderColor = if (isSelected) statusColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            borderWidth = if (isSelected) 1.5.dp else 1.dp
                                        )
                                    )
                                }
                            }
                        }

                        // Group 2: Responsabilidade
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Responsabilidade",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box {
                                    val clientNameSelected = if (selectedClienteId != null) {
                                        clientes.find { it.id == selectedClienteId }?.nome ?: "Cliente"
                                    } else null

                                    FilterChip(
                                        selected = clientNameSelected != null,
                                        onClick = { clienteDropdownExpanded = true },
                                        label = {
                                            Text(
                                                text = clientNameSelected ?: "Cliente",
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 110.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            if (clientNameSelected != null) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Limpar",
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable { selectedClienteId = null }
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    )
                                    DropdownMenu(
                                        expanded = clienteDropdownExpanded,
                                        onDismissRequest = { clienteDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Todos os Clientes", fontSize = 13.sp) },
                                            onClick = {
                                                selectedClienteId = null
                                                clienteDropdownExpanded = false
                                            }
                                        )
                                        clientes.forEach { cli ->
                                            DropdownMenuItem(
                                                text = { Text(cli.nome, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedClienteId = cli.id
                                                    clienteDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Box {
                                    val techNameSelected = if (selectedTecnicoId != null) {
                                        tecnicos.find { it.id == selectedTecnicoId }?.nome ?: "Técnico"
                                    } else null

                                    FilterChip(
                                        selected = techNameSelected != null,
                                        onClick = { tecnicoDropdownExpanded = true },
                                        label = {
                                            Text(
                                                text = techNameSelected ?: "Técnico",
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 110.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            if (techNameSelected != null) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Limpar",
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable { selectedTecnicoId = null }
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    )
                                    DropdownMenu(
                                        expanded = tecnicoDropdownExpanded,
                                        onDismissRequest = { tecnicoDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Todos os Técnicos", fontSize = 13.sp) },
                                            onClick = {
                                                selectedTecnicoId = null
                                                tecnicoDropdownExpanded = false
                                            }
                                        )
                                        tecnicos.forEach { tec ->
                                            DropdownMenuItem(
                                                text = { Text(tec.nome, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedTecnicoId = tec.id
                                                    tecnicoDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Group 3: Período / Intervalo
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Período / Intervalo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val periodOptions = listOf(
                                    null to "Qualquer data",
                                    "1m" to "1 mês",
                                    "3m" to "3 meses",
                                    "6m" to "6 meses",
                                    "1y" to "1 ano",
                                    "personalizado" to "Personalizar"
                                )
                                periodOptions.forEach { (preset, label) ->
                                    val isSelected = dateRangePreset == preset
                                    SuggestionChip(
                                        onClick = {
                                            dateRangePreset = preset
                                            if (preset != "personalizado") {
                                                customStartDate = null
                                                customEndDate = null
                                            }
                                        },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            labelColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = SuggestionChipDefaults.suggestionChipBorder(
                                            enabled = true,
                                            borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            borderWidth = if (isSelected) 1.5.dp else 1.dp
                                        )
                                    )
                                }
                            }
                        }

                        if (dateRangePreset == "personalizado") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { customStartPickerDialog.show() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = customStartDate ?: "Início",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text("até", fontSize = 11.sp, color = Color.Gray)

                                Button(
                                    onClick = { customEndPickerDialog.show() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = customEndDate ?: "Fim",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Group 4: Tipo de Serviço (Programadas)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { apenasProgramadas = !apenasProgramadas }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = apenasProgramadas,
                                onCheckedChange = { apenasProgramadas = it },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Exibir apenas O.S. Programadas",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    statusFilter = "Todas"
                                    apenasProgramadas = false
                                    selectedClienteId = null
                                    selectedTecnicoId = null
                                    selectedDateStr = null
                                    dateRangePreset = null
                                    customStartDate = null
                                    customEndDate = null
                                    searchQuery = ""
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.FilterListOff, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Limpar Filtros", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            TextButton(
                                onClick = { filtersExpanded = false },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Fechar", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            if (filteredOrdens.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentLate,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Nenhuma O.S. encontrada!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (clientes.isEmpty()) "Cadastre pelo menos um Cliente para criar Ordens de Serviço."
                                   else "Use o botão '+' no canto inferior para registrar sua primeira ordem técnica.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredOrdens) { os ->
                        val clientName = clientes.find { it.id == os.clienteId }?.nome ?: "Cliente Excluído"
                        val tecnicoName = tecnicos.find { it.id == os.tecnicoId }?.nome ?: "Técnico Não Definido"
                        val equip = equipamentos.find { it.id == os.equipamentoId }
                        val amb = ambientes.find { it.id == equip?.ambienteId }
                        
                        val statusIndicatorColor = when (os.status) {
                            "Aberta" -> Color(0xFF1976D2)
                            "Em Andamento", "Em andamento", "Pendente" -> Color(0xFFF57C00)
                            "Concluída" -> Color(0xFF0F9D58)
                            else -> Color(0xFF78909C)
                        }

                        var cardMenuExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onNavigateToDetails(os.id) },
                                        onLongClick = { cardMenuExpanded = true }
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, statusIndicatorColor.copy(alpha = 0.20f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                ) {
                                    // Status color bar
                                    Box(
                                        modifier = Modifier
                                            .width(5.dp)
                                            .fillMaxHeight()
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        statusIndicatorColor,
                                                        statusIndicatorColor.copy(alpha = 0.62f)
                                                    )
                                                )
                                            )
                                    )

                                    // Main Content Column
                               Column(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .weight(1f)
                                    ) {
                                        // Row 1: Tipo de Serviço & Status Badge
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val displayTipoText = if (os.tipoServico.uppercase() == "PROGRAMADA") "Programada" else os.tipoServico
                                            val tipoColor = if (displayTipoText == "Programada") Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurface
                                            Text(
                                                text = displayTipoText,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = tipoColor
                                            )

                                            Spacer(modifier = Modifier.weight(1f))

                                            StatusBadge(status = os.status)
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Row 2: OS Number & Cliente Name
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "OS: #${os.numeroOrdem}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            
                                            Text(
                                                text = "•  Cliente: $clientName",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        // Row 3: Equipment and Environment
                                        if (equip != null) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val btuFormatted = when (val cap = equip.capacidadeBtu) {
                                                null, 0 -> ""
                                                else -> " " + String.format(Locale.getDefault(), "%,d BTU/h", cap).replace(",", ".")
                                            }
                                            val ambLabel = amb?.nome ?: "Geral"
                                            Text(
                                                text = "${equip.marca}$btuFormatted  •  $ambLabel",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Simple separator line
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )

                                        // Row 4: Date & Technician
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val isCompleted = os.status.lowercase() in listOf("concluída", "concluida")
                                            val dateLabel = if (isCompleted) "Fim:" else "Visita:"
                                            val dateValue = if (isCompleted) {
                                                if (os.dataAgendada.isNotEmpty()) os.dataAgendada else os.dataChamado
                                            } else {
                                                if (os.dataChamado.isNotEmpty()) os.dataChamado else os.dataAgendada
                                            }

                                            Text(
                                                text = "$dateLabel $dateValue",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Handyman,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(10.dp),
                                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                                )
                                                Text(
                                                    text = tecnicoName,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.widthIn(max = 140.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = cardMenuExpanded,
                                onDismissRequest = { cardMenuExpanded = false },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                                    text = { Text("Visualizar Ordem", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                                    onClick = {
                                        cardMenuExpanded = false
                                        onNavigateToDetails(os.id)
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) },
                                    text = { Text("Editar Ordem", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                                    onClick = {
                                        cardMenuExpanded = false
                                        onNavigateToOrdemForm(os.id, "0")
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                                    text = { Text("Duplicar Ordem", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                                    onClick = {
                                        cardMenuExpanded = false
                                        val newNum = com.priorizedev.brizza.data.model.generateRandomNumeroOrdem()
                                        val newId = com.priorizedev.brizza.data.model.generateOrdemServicoId()
                                        val duplicatedOs = os.copy(
                                            id = newId,
                                            numeroOrdem = newNum,
                                            status = "Aberta"
                                        )
                                        viewModel.salvarOrdemServico(duplicatedOs)
                                        Toast.makeText(context, "Ordem duplicada! Nova OS: #$newNum", Toast.LENGTH_LONG).show()
                                        onNavigateToOrdemForm(newId, "0")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DummyLegacyBlock(viewModel: ClimaGestViewModel) {
    val clientes = emptyList<com.priorizedev.brizza.data.model.Cliente>()
    if (false) {
        val triggerPreselectedByFlow = remember { mutableStateOf<com.priorizedev.brizza.data.model.Equipamento?>(null) }
        val showAddDialog = remember { mutableStateOf(false) }
        val context = LocalContext.current
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current

        var selectedCliente by remember {
            mutableStateOf<Cliente?>(null)
        }
        var dropdownCliExpanded by remember { mutableStateOf(false) }

        var selectedEquipamento by remember {
            mutableStateOf<Equipamento?>(triggerPreselectedByFlow.value)
        }
        var dropdownEqExpanded by remember { mutableStateOf(false) }

        var selectedTecnico by remember { mutableStateOf<Tecnico?>(null) }
        var dropdownTecExpanded by remember { mutableStateOf(false) }

        var tipoServico by remember { mutableStateOf("Corretiva") }
        var dataChamado by remember { mutableStateOf("") }
        var dataAgendada by remember { mutableStateOf("") }
        var descricaoResumida by remember { mutableStateOf("") }
        var valorServico by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("Aberta") }
        var prioridade by remember { mutableStateOf("Média") }
        var descricaoCompleta by remember { mutableStateOf("") }

        // Medições Realizadas (Expansível / Collapsible)
        var measurementsExpanded by remember { mutableStateOf(false) }
        var correnteEletrica by remember { mutableStateOf("") }
        var temperatura by remember { mutableStateOf("") }
        var pressaoGasAlta by remember { mutableStateOf("") }
        var pressaoGasBaixa by remember { mutableStateOf("") }

        // Imagens do atendimento (Maximum 3)
        var foto1Uri by remember { mutableStateOf("") }
        var foto2Uri by remember { mutableStateOf("") }
        var foto3Uri by remember { mutableStateOf("") }

        val tecnicos by viewModel.tecnicosState.collectAsStateWithLifecycle()
        
        // Dynamically fetch customer equipment list when client is chosen
        val clientEquipsState = remember(selectedCliente) {
            if (selectedCliente == null) kotlinx.coroutines.flow.flowOf(emptyList<Equipamento>())
            else viewModel.getEquipamentosPorClienteFlow(selectedCliente!!.id)
        }
        val clientEquips by clientEquipsState.collectAsStateWithLifecycle(initialValue = emptyList())

        // Image selector launchers
        val launcherFoto1 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) { foto1Uri = com.priorizedev.brizza.data.api.SupabaseSyncClient.copyUriToLocalCache(context, uri.toString()) }
        }
        val launcherFoto2 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) { foto2Uri = com.priorizedev.brizza.data.api.SupabaseSyncClient.copyUriToLocalCache(context, uri.toString()) }
        }
        val launcherFoto3 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) { foto3Uri = com.priorizedev.brizza.data.api.SupabaseSyncClient.copyUriToLocalCache(context, uri.toString()) }
        }

        // Calendar Trigger Helpers
        val showDatePicker: (onDateSelected: (String) -> Unit) -> Unit = { callback ->
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    callback(String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        Dialog(
            onDismissRequest = { showAddDialog.value = false; triggerPreselectedByFlow.value = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showAddDialog.value = false; triggerPreselectedByFlow.value = null },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusManager.clearFocus() },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header com gradiente azul moderno
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
                                    ),
                                    shape = RoundedCornerShape(topStart = 23.dp, topEnd = 23.dp)
                                )
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color.White.copy(alpha = 0.2f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Assignment,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Nova Ordem de Serviço",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Cadastrar no sistema",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                IconButton(
                                    onClick = { showAddDialog.value = false; triggerPreselectedByFlow.value = null },
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

                        // Form Scrollable Column
                        val scrollState = rememberScrollState()
                        val isScrollInProgress = scrollState.isScrollInProgress
                        LaunchedEffect(isScrollInProgress) {
                            if (isScrollInProgress) {
                                focusManager.clearFocus()
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(max = 480.dp)
                                .verticalScroll(scrollState)
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Identificação do Cliente & Equipamento
                            HighlightedSectionHeader(
                                icon = Icons.Default.PersonSearch,
                                title = "Identificação de Atendimento"
                            )

                    // 1. Selector Cliente (Obrigatório)
                    Column {
                        FieldLabel(text = "Selecione o Cliente *")
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { dropdownCliExpanded = true },
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
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(selectedCliente?.nome ?: "Selecionar cliente da lista")
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = dropdownCliExpanded,
                                onDismissRequest = { dropdownCliExpanded = false }
                            ) {
                                clientes.forEach { cli ->
                                    DropdownMenuItem(
                                        text = { Text(cli.nome, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            selectedCliente = cli
                                            selectedEquipamento = null // reset
                                            dropdownCliExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Selector Equipamento (Obrigatório)
                    if (selectedCliente != null) {
                        Column {
                            FieldLabel(text = "Selecione o Equipamento")
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { dropdownEqExpanded = true },
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
                                            Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(selectedEquipamento?.tag ?: "Selecionar equipamento")
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                DropdownMenu(
                                    expanded = dropdownEqExpanded,
                                    onDismissRequest = { dropdownEqExpanded = false }
                                ) {
                                    if (clientEquips.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Nenhum equipamento para este cliente.", style = MaterialTheme.typography.bodySmall) },
                                            onClick = { dropdownEqExpanded = false }
                                        )
                                    }
                                    clientEquips.forEach { eq ->
                                        DropdownMenuItem(
                                            text = { Text("${eq.tag} - ${eq.marca} (${eq.tipo})") },
                                            onClick = {
                                                selectedEquipamento = eq
                                                dropdownEqExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Técnico Responsável Selector (moved inside Section 1)
                    Column {
                        FieldLabel(text = "Técnico Responsável *")
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { dropdownTecExpanded = true },
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
                                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(selectedTecnico?.nome ?: "Selecionar técnico")
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = dropdownTecExpanded,
                                onDismissRequest = { dropdownTecExpanded = false }
                            ) {
                                if (tecnicos.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Nenhum técnico cadastrado ainda.") },
                                        onClick = { dropdownTecExpanded = false }
                                    )
                                }
                                tecnicos.forEach { tec ->
                                    DropdownMenuItem(
                                        text = { Text(tec.nome) },
                                        onClick = {
                                            selectedTecnico = tec
                                            dropdownTecExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Datas & Tipo de Atendimento
                    HighlightedSectionHeader(
                        icon = Icons.Default.Event,
                        title = "Datas e Tipo de Serviço"
                    )

                    // 3. Date do chamado & Date da visita (with picker buttons)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel(text = "Data do Chamado *")
                            OutlinedTextField(
                                value = dataChamado,
                                onValueChange = { dataChamado = it },
                                label = { Text("dd/mm/aaaa") },
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker { dataChamado = it } }) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = "Abrir Calendário", tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel(text = "Data da Visita")
                            OutlinedTextField(
                                value = dataAgendada,
                                onValueChange = { dataAgendada = it },
                                label = { Text("dd/mm/aaaa") },
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker { dataAgendada = it } }) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = "Abrir Calendário", tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // 5. Tipo de Serviço Selector
                    Column {
                        FieldLabel(text = "Tipo de Atendimento *")
                        var dropdownTipoExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { dropdownTipoExpanded = true },
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
                                        Icon(Icons.Default.Handyman, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(tipoServico)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = dropdownTipoExpanded,
                                onDismissRequest = { dropdownTipoExpanded = false }
                            ) {
                                listOf("Preventiva", "Corretiva", "Instalação", "Orçamento").forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t) },
                                        onClick = {
                                            tipoServico = t
                                            dropdownTipoExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Informações e Status
                    HighlightedSectionHeader(
                        icon = Icons.Default.Description,
                        title = "Informações e Status"
                    )

                    // 6. Descrição Resumida (MAX 30 CHARS WITH VALIDATION/COUNTER)
                    Column {
                        FieldLabel(text = "Descrição Resumida (Máx 30 caracteres)")
                        OutlinedTextField(
                            value = descricaoResumida,
                            onValueChange = { 
                                if (it.length <= 30) {
                                    descricaoResumida = it
                                }
                            },
                            placeholder = { Text("Ex: Troca de Capacitor") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (descricaoResumida.length >= 30) "Limite atingido" else "",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${descricaoResumida.length}/30",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        )
                    }

                    // 7. Custo Estimado
                    Column {
                        FieldLabel(text = "Custo Estimado (R$)")
                        OutlinedTextField(
                            value = valorServico,
                            onValueChange = { valorServico = it },
                            placeholder = { Text("Ex: 150.00") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }

                    // 8. Status selector (Chips)
                    Column {
                        FieldLabel(text = "Status da Ordem *")
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Aberta", "Pendente").forEach { st ->
                                    val isSelected = status == st
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .clickable { status = st }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = st,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Concluída", "Cancelada").forEach { st ->
                                    val isSelected = status == st
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .clickable { status = st }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = st,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 9. Prioridade selector (Chips)
                    Column {
                        FieldLabel(text = "Prioridade do Atendimento")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Baixa", "Média", "Alta").forEach { prio ->
                                val isSelected = prioridade == prio
                                val chipColor = when(prio) {
                                    "Alta" -> Color(0xFFEF5350)
                                    "Média" -> Color(0xFFFFB74D)
                                    else -> Color(0xFF90A4AE)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) chipColor
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                        .clickable { prioridade = prio }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = prio,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 10. Descrição Completa
                    Column {
                        FieldLabel(text = "Descrição Completa dos Problemas relatados")
                        OutlinedTextField(
                            value = descricaoCompleta,
                            onValueChange = { descricaoCompleta = it },
                            placeholder = { Text("Escreva detalhadamente o diagnóstico prévio e observações do cliente...") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }

                    // 11. Collapsible group "Medições Realizadas"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column {
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
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "Medições Realizadas",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = if (measurementsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }

                            AnimatedVisibility(visible = measurementsExpanded) {
                                Column(
                                    modifier = Modifier
                                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = correnteEletrica,
                                            onValueChange = { correnteEletrica = it },
                                            label = { Text("Corrente Elétrica") },
                                            placeholder = { Text("Ex: 12.3 A") },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = temperatura,
                                            onValueChange = { temperatura = it },
                                            label = { Text("Temperatura") },
                                            placeholder = { Text("Ex: 18 °C") },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = pressaoGasAlta,
                                            onValueChange = { pressaoGasAlta = it },
                                            label = { Text("Pressão Gás Alta") },
                                            placeholder = { Text("Ex: 240 PSI") },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = pressaoGasBaixa,
                                            onValueChange = { pressaoGasBaixa = it },
                                            label = { Text("Pressão Gás Baixa") },
                                            placeholder = { Text("Ex: 65 PSI") },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 12. Option to insert up to 3 images with slot templates
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel(text = "Fotos do Diagnóstico (Máximo 3)")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Slot 1
                            ImageSlot(
                                imageUri = foto1Uri,
                                label = "Foto 1",
                                onSelect = { launcherFoto1.launch("image/*") },
                                onClear = { foto1Uri = "" },
                                modifier = Modifier.weight(1f)
                            )

                            // Slot 2
                            ImageSlot(
                                imageUri = foto2Uri,
                                label = "Foto 2",
                                onSelect = { launcherFoto2.launch("image/*") },
                                onClear = { foto2Uri = "" },
                                modifier = Modifier.weight(1f)
                            )

                            // Slot 3
                            ImageSlot(
                                imageUri = foto3Uri,
                                label = "Foto 3",
                                onSelect = { launcherFoto3.launch("image/*") },
                                onClear = { foto3Uri = "" },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Demo Simulation Option for UI Validation
                        TextButton(
                            onClick = {
                                foto1Uri = "sim_foto1"
                                foto2Uri = "sim_foto2"
                                foto3Uri = "sim_foto3"
                                Toast.makeText(context, "Fotos simuladas injetadas com sucesso!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simular Upload de Fotos Tecnicas", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                        }

                        // Divisor antes dos botões de ação
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Botões de ação alinhados à direita
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    showAddDialog.value = false
                                    triggerPreselectedByFlow.value = null
                                },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Cancelar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    val cli = selectedCliente
                                    val tec = selectedTecnico
                                    val equip = selectedEquipamento
                                    if (cli != null && tec != null && dataChamado.isNotEmpty() && tipoServico.isNotEmpty() && status.isNotEmpty()) {
                                        viewModel.salvarOrdemServico(
                                            OrdemServico(id = com.priorizedev.brizza.data.model.generateOrdemServicoId(), 
                                                clienteId = cli.id,
                                                tecnicoId = tec.id,
                                                equipamentoId = equip?.id ?: "",
                                                tipoServico = tipoServico,
                                                dataChamado = dataChamado,
                                                dataAgendada = dataAgendada,
                                                descricao = descricaoCompleta,
                                                status = status,
                                                prioridade = prioridade,
                                                descricaoResumida = descricaoResumida,
                                                correnteEletrica = correnteEletrica,
                                                temperatura = temperatura,
                                                pressaoGasAlta = pressaoGasAlta,
                                                pressaoGasBaixa = pressaoGasBaixa,
                                                valorServico = valorServico.toDoubleOrNull() ?: 0.0,
                                                foto1 = foto1Uri,
                                                foto2 = foto2Uri,
                                                foto3 = foto3Uri
                                            )
                                        )
                                        showAddDialog.value = false
                                        triggerPreselectedByFlow.value = null
                                        Toast.makeText(context, "OS criada com sucesso!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Por favor, preencha os campos obrigatórios (*): Cliente, Técnico, Data do chamado, Tipo de atendimento e Status.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Salvar O.S.", fontWeight = FontWeight.Bold)
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
fun OrdemServicoDetalhesScreen(
    ordemId: String,
    viewModel: ClimaGestViewModel,
    onBack: () -> Unit,
    onNavigateToClientDetails: (String) -> Unit = {},
    onNavigateToEditarOrdem: (String) -> Unit = {},
    onNavigateToEquipamentoDetails: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val osState = viewModel.getOrdemServicoFlow(ordemId).collectAsStateWithLifecycle(initialValue = null)
    val clientes by viewModel.clientesState.collectAsStateWithLifecycle()
    val tecnicos by viewModel.tecnicosState.collectAsStateWithLifecycle()
    
    val os = osState.value ?: return
    val cliente = clientes.find { it.id == os.clienteId }
    val tecnico = tecnicos.find { it.id == os.tecnicoId }
    
    // Fetch Associated Equipamento dynamically
    val appConfiguredEquips = viewModel.equipamentosState.collectAsStateWithLifecycle().value
    val equipamento = appConfiguredEquips.find { it.id == os.equipamentoId }

    // Fetch Associated Ambiente dynamically
    val ambiente = remember(equipamento) {
        if (equipamento != null) {
            viewModel.getAmbienteFlow(equipamento.ambienteId)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.collectAsStateWithLifecycle(initialValue = null).value

    // Screen Fields State
    var statusState by remember(os) { mutableStateOf(os.status) }
    var prioridadeState by remember(os) { mutableStateOf(os.prioridade) }
    var diagnosticoState by remember(os) { mutableStateOf(os.diagnosticoTecnico) }
    var pecasState by remember(os) { mutableStateOf(os.pecasTrocadas) }
    var valorState by remember(os) { mutableStateOf(os.valorServico.toString()) }
    var correnteState by remember(os) { mutableStateOf(os.correnteEletrica) }
    var temperaturaState by remember(os) { mutableStateOf(os.temperatura) }
    var pressaoAltaState by remember(os) { mutableStateOf(os.pressaoGasAlta) }
    var pressaoBaixaState by remember(os) { mutableStateOf(os.pressaoGasBaixa) }
    var clienteNomeAssinatura by remember(os) { mutableStateOf(os.assinaturaClienteNome) }
    var signatureBase64 by remember(os) { mutableStateOf(os.assinaturaDigital) }
    var dataConclusaoState by remember(os) { mutableStateOf(os.dataAgendada) }

    val showDatePicker: (onDateSelected: (String) -> Unit) -> Unit = { callback ->
        val calendar = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                callback(String.format(java.util.Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year))
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Measurements display collapse state
    var measurementsExpanded by remember { mutableStateOf(true) }
    var clienteExpanded by remember { mutableStateOf(false) }
    var equipamentoExpanded by remember { mutableStateOf(false) }
    var gerenciarAndamentoExpanded by remember { mutableStateOf(false) }
    var pecasExpanded by remember { mutableStateOf(false) }
    var fotosExpanded by remember { mutableStateOf(false) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var expandedPhotoUri by remember { mutableStateOf<String?>(null) }
    var expandedPhotoLabel by remember { mutableStateOf("") }

    val launchDet1 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val localPath = com.priorizedev.brizza.data.api.SupabaseSyncClient.copyUriToLocalCache(context, it.toString())
            viewModel.salvarOrdemServico(os.copy(foto1 = localPath))
        }
    }
    val launchDet2 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val localPath = com.priorizedev.brizza.data.api.SupabaseSyncClient.copyUriToLocalCache(context, it.toString())
            viewModel.salvarOrdemServico(os.copy(foto2 = localPath))
        }
    }
    val launchDet3 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val localPath = com.priorizedev.brizza.data.api.SupabaseSyncClient.copyUriToLocalCache(context, it.toString())
            viewModel.salvarOrdemServico(os.copy(foto3 = localPath))
        }
    }

    val focusManager = LocalFocusManager.current
    val lazyListState = rememberLazyListState()
    val isScrollInProgress = lazyListState.isScrollInProgress
    LaunchedEffect(isScrollInProgress) {
        if (isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Visualizar OS #${os.numeroOrdem}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
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
                        var detailsMenuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { detailsMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Mais opções",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        DropdownMenu(
                            expanded = detailsMenuExpanded,
                            onDismissRequest = { detailsMenuExpanded = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                                text = { Text("Gerar PDF", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                                onClick = {
                                    detailsMenuExpanded = false
                                    if (cliente != null) {
                                        val updVal = valorState.toDoubleOrNull() ?: 0.0
                                        val updatedOs = os.copy(
                                            status = statusState,
                                            prioridade = prioridadeState,
                                            diagnosticoTecnico = diagnosticoState,
                                            pecasTrocadas = pecasState,
                                            valorServico = updVal,
                                            correnteEletrica = correnteState,
                                            temperatura = temperaturaState,
                                            pressaoGasAlta = pressaoAltaState,
                                            pressaoGasBaixa = pressaoBaixaState,
                                            assinaturaClienteNome = clienteNomeAssinatura,
                                            assinaturaDigital = signatureBase64,
                                            dataAgendada = dataConclusaoState
                                        )
                                        viewModel.salvarOrdemServico(updatedOs)

                                        val pdfFile = ReportUtils.generateOrdemServicoPdf(
                                            context = context,
                                            os = updatedOs,
                                            cliente = cliente,
                                            tecnico = tecnico,
                                            equipamento = equipamento,
                                            ambiente = ambiente
                                        )
                                        if (pdfFile != null) {
                                            ReportUtils.sharePdf(context, pdfFile)
                                        } else {
                                            Toast.makeText(context, "Erro ao gerar PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Cliente inválido para gerar PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) },
                                text = { Text("Editar Ordem", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                                onClick = {
                                    detailsMenuExpanded = false
                                    onNavigateToEditarOrdem(os.id)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                                text = { Text("Duplicar Ordem", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                                onClick = {
                                    detailsMenuExpanded = false
                                    val newNum = com.priorizedev.brizza.data.model.generateRandomNumeroOrdem()
                                    val newId = com.priorizedev.brizza.data.model.generateOrdemServicoId()
                                    val duplicatedOs = os.copy(
                                        id = newId,
                                        numeroOrdem = newNum,
                                        status = "Aberta"
                                    )
                                    viewModel.salvarOrdemServico(duplicatedOs)
                                    Toast.makeText(context, "Ordem duplicada! Nova OS: #$newNum", Toast.LENGTH_LONG).show()
                                    onNavigateToEditarOrdem(newId)
                                }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) },
                                text = { Text("Excluir ordem", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                                onClick = {
                                    detailsMenuExpanded = false
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
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HEADER INFO CARD: O.S. STATUS SUMMARY
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Tipo de Serviço",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val displayTipo = if (os.tipoServico.uppercase() == "PROGRAMADA") "Programada" else os.tipoServico
                            val tipoColor = if (displayTipo == "Programada") Color(0xFF7C3AED) else MaterialTheme.colorScheme.onSurface
                            Text(
                                text = displayTipo,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = tipoColor
                            )
                        }
                        StatusBadge(status = statusState)
                    }
                }
            }

            // 2. CARD DO CLIENTE - COLLAPSIBLE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { clienteExpanded = !clienteExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "CLIENTE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = cliente?.nome ?: "Desconhecido",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (clienteExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (clienteExpanded) "Recolher" else "Expandir",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (clienteExpanded) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Telefone Row
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Telefone", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = cliente?.telefone ?: "Telefone não informado",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Endereço Row
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Endereço", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = cliente?.endereco ?: "Endereço não informado",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { onNavigateToClientDetails(os.clienteId) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ver Detalhes do Cliente", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. CARD DA MÁQUINA / EQUIPAMENTO - COLLAPSIBLE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { equipamentoExpanded = !equipamentoExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Kitchen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "EQUIPAMENTO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                    val btuFormatted = when (val cap = equipamento?.capacidadeBtu) {
                                        null, 0 -> ""
                                        else -> " " + String.format(Locale.getDefault(), "%,d BTU/h", cap).replace(",", ".")
                                    }
                                    Text(
                                        text = "${equipamento?.marca ?: "Desconhecido"}$btuFormatted",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = ambiente?.nome ?: "Geral",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (equipamentoExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (equipamentoExpanded) "Recolher" else "Expandir",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (equipamentoExpanded) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Tag Row
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("TAG / Identificador", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = equipamento?.tag ?: "Geral / Sem Identificador",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Modelo Serie info
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Série & Modelo", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${equipamento?.modelo ?: "N/I"} - Nº Série: ${equipamento?.numeroSerie ?: "N/I"}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (equipamento != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { onNavigateToEquipamentoDetails(equipamento.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ver Ficha do Equipamento", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 4. DETALHES DA ORDEM (Sintoma, Data Chamado, Valor)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DETALHES DO ATENDIMENTO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Datas info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Data do Chamado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = os.dataChamado.ifBlank { "Não informada" },
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Valor do Atendimento",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val precoFormatted = try {
                                    String.format(Locale.getDefault(), "R$ %.2f", valorState.toDoubleOrNull() ?: os.valorServico)
                                } catch (e: Exception) {
                                    "R$ ${valorState}"
                                }
                                Text(
                                    text = precoFormatted,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Ocorrência / Sintoma Relatado",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = os.descricao.ifBlank { "Nenhuma descrição informada." },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // 5. GERENCIAR ANDAMENTO DA OS - COLLAPSIBLE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { gerenciarAndamentoExpanded = !gerenciarAndamentoExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Handyman,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "GERENCIAR ANDAMENTO DA OS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (gerenciarAndamentoExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = gerenciarAndamentoExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Priority selector
                                Column {
                                    FieldLabel(text = "Definir Prioridade")
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("Baixa", "Média", "Alta").forEach { prio ->
                                            val isSelected = prioridadeState == prio
                                            val color = when(prio) {
                                                "Alta" -> Color(0xFFEF5350)
                                                "Média" -> Color(0xFFFFB74D)
                                                else -> Color(0xFF90A4AE)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                                    .clickable { prioridadeState = prio }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(prio, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }

                                // Status Editor
                                Column {
                                    FieldLabel(text = "Status de Execução")
                                    Column {
                                        listOf(
                                            listOf("Aberta", "Pendente"),
                                            listOf("Concluída", "Cancelada")
                                        ).forEachIndexed { index, rowList ->
                                            if (index > 0) Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                rowList.forEach { st ->
                                                    val isSelected = statusState == st
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                                            .clickable { statusState = st }
                                                            .padding(vertical = 10.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = st,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Text Field Inputs
                                OutlinedTextField(
                                    value = diagnosticoState,
                                    onValueChange = { diagnosticoState = it },
                                    label = { Text("Diagnóstico") },
                                    placeholder = { Text("Ex: Identificado vazamento no flange, refeito vácuo e carga de gás R-410A.") },
                                    leadingIcon = { Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )

                                OutlinedTextField(
                                    value = dataConclusaoState,
                                    onValueChange = { dataConclusaoState = it },
                                    label = { Text("Data de Conclusão") },
                                    placeholder = { Text("Ex: 15/06/2026") },
                                    leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingIcon = {
                                        IconButton(onClick = { showDatePicker { dataConclusaoState = it } }) {
                                            Icon(Icons.Default.DateRange, contentDescription = "Selecionar data")
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 5B. PROGRAMAR PREVENTIVA FUTURA - COLLAPSIBLE
            item {
                var programarPreventivaExpanded by remember { mutableStateOf(false) }
                var selectedPeriodoPreventiva by remember { mutableStateOf("3 meses") }
                var notificarClientePreventiva by remember { mutableStateOf(true) }
                var notificarAppPreventiva by remember { mutableStateOf(true) }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { programarPreventivaExpanded = !programarPreventivaExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "PROGRAMAR PREVENTIVA",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (programarPreventivaExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = programarPreventivaExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                val aOrdemFoiConcluida = (os.status == "Concluída" || statusState == "Concluída" || os.status == "Concluido" || statusState == "Concluido")

                                if (!aOrdemFoiConcluida) {
                                    // Alert info card if current order is not concluded
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Operação Bloqueada",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "É preciso concluir a ordem atual antes de programar outra.",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Escolha um período para o próximo agendamento preventivo automático:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Period Selector Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("3 meses", "6 meses", "1 ano", "2 anos").forEach { period ->
                                            val isSelected = selectedPeriodoPreventiva == period
                                            val chipBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            val chipTextCol = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(chipBg)
                                                    .clickable { selectedPeriodoPreventiva = period }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = period,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = chipTextCol
                                                )
                                            }
                                        }
                                    }

                                    // Calculated date preview
                                    val futureDate = remember(selectedPeriodoPreventiva) {
                                        val df = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                        val baseDate = try {
                                            df.parse(os.dataAgendada) ?: java.util.Date()
                                        } catch (e: Exception) {
                                            java.util.Date()
                                        }
                                        val calendar = java.util.Calendar.getInstance()
                                        calendar.time = baseDate
                                        when (selectedPeriodoPreventiva) {
                                            "3 meses" -> calendar.add(java.util.Calendar.MONTH, 3)
                                            "6 meses" -> calendar.add(java.util.Calendar.MONTH, 6)
                                            "1 ano" -> calendar.add(java.util.Calendar.YEAR, 1)
                                            "2 anos" -> calendar.add(java.util.Calendar.YEAR, 2)
                                        }
                                        df.format(calendar.time)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                            .padding(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Event,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Próxima Visita Projetada: $futureDate",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    // Notificações switches
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Notificar Cliente (E-mail/Zap)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Switch(
                                            checked = notificarClientePreventiva,
                                            onCheckedChange = { notificarClientePreventiva = it }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsActive,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Notificar no Aplicativo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Switch(
                                            checked = notificarAppPreventiva,
                                            onCheckedChange = { notificarAppPreventiva = it }
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val calculatedDate = futureDate
                                            val newOrdemId = com.priorizedev.brizza.data.model.generateOrdemServicoId()
                                            
                                            // Create automatic duplicate Ordem de Serviço for the scheduled date
                                            val automaticOS = OrdemServico(
                                                id = newOrdemId,
                                                clienteId = os.clienteId,
                                                tecnicoId = os.tecnicoId,
                                                equipamentoId = os.equipamentoId,
                                                tipoServico = "Preventiva",
                                                dataChamado = calculatedDate,
                                                dataAgendada = calculatedDate,
                                                descricao = "Preventiva agendada para: $calculatedDate. Programada a partir da conclusão da O.S. #${os.numeroOrdem}.",
                                                status = "Aberta",
                                                prioridade = os.prioridade,
                                                descricaoResumida = "Prev. $selectedPeriodoPreventiva",
                                                usuarioEmail = os.usuarioEmail
                                            )

                                            viewModel.salvarOrdemServico(automaticOS)

                                            Toast.makeText(context, "Nova O.S. de Preventiva gerada para $calculatedDate com sucesso!", Toast.LENGTH_LONG).show()
                                            programarPreventivaExpanded = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Agendar Preventiva")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. COLLAPSIBLE MEDIÇÕES REALIZADAS
            item {
                var editMeasurementsExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editMeasurementsExpanded = !editMeasurementsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "MEDIÇÕES REALIZADAS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (editMeasurementsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = editMeasurementsExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = correnteState,
                                        onValueChange = { correnteState = it },
                                        label = { Text("Corrente (A)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        placeholder = { Text("Ex: 12.5") },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    )
                                    OutlinedTextField(
                                        value = temperaturaState,
                                        onValueChange = { temperaturaState = it },
                                        label = { Text("Temp (Cº)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        placeholder = { Text("Ex: 11.8") },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = pressaoAltaState,
                                        onValueChange = { pressaoAltaState = it },
                                        label = { Text("P. Alta (PSI)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        placeholder = { Text("Ex: 350") },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    )
                                    OutlinedTextField(
                                        value = pressaoBaixaState,
                                        onValueChange = { pressaoBaixaState = it },
                                        label = { Text("P. Baixa (PSI)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        placeholder = { Text("Ex: 120") },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. COLLAPSIBLE PEÇAS E ACESSÓRIOS UTILIZADOS
            item {
                var dropdownExpanded by remember { mutableStateOf(false) }
                val allRegisteredPecas by viewModel.pecasState.collectAsStateWithLifecycle()
                val currentPecasList = remember(pecasState) { deserializeSelectedPecas(pecasState) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pecasExpanded = !pecasExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "PEÇAS E ACESSÓRIOS UTILIZADOS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (pecasExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = pecasExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Add button
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { dropdownExpanded = true },
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
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        if (allRegisteredPecas.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Nenhuma peça cadastrada no ClimaGest", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) },
                                                onClick = { dropdownExpanded = false }
                                            )
                                        } else {
                                            val unselectedPecas = allRegisteredPecas.filterNot { registered -> 
                                                currentPecasList.any { it.id == registered.id } 
                                            }
                                            if (unselectedPecas.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("Todas as peças já foram adicionadas", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                                                    onClick = { dropdownExpanded = false }
                                                )
                                            } else {
                                                unselectedPecas.forEach { pecaItem ->
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
                                                            val newList = currentPecasList + pecaItem
                                                            pecasState = serializeSelectedPecas(newList)
                                                            dropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                if (currentPecasList.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Nenhuma peça adicionada.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    currentPecasList.forEach { peca ->
                                        PecaCardWithDelete(peca = peca, onDelete = {
                                            val newList = currentPecasList.filterNot { it.id == peca.id }
                                            pecasState = serializeSelectedPecas(newList)
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. COLLAPSIBLE FOTOS TÉCNICAS DO ATENDIMENTO
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { fotosExpanded = !fotosExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "FOTOS TÉCNICAS DO ATENDIMENTO",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (fotosExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = fotosExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DiagnosticPhotoView(
                                        label = "Foto 1",
                                        uri = os.foto1,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            expandedPhotoUri = os.foto1
                                            expandedPhotoLabel = "Foto 1"
                                        },
                                        onAddClick = { launchDet1.launch("image/*") },
                                        onRemoveClick = if (os.foto1.isNotEmpty()) { { viewModel.salvarOrdemServico(os.copy(foto1 = "")) } } else null
                                    )
                                    DiagnosticPhotoView(
                                        label = "Foto 2",
                                        uri = os.foto2,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            expandedPhotoUri = os.foto2
                                            expandedPhotoLabel = "Foto 2"
                                        },
                                        onAddClick = { launchDet2.launch("image/*") },
                                        onRemoveClick = if (os.foto2.isNotEmpty()) { { viewModel.salvarOrdemServico(os.copy(foto2 = "")) } } else null
                                    )
                                    DiagnosticPhotoView(
                                        label = "Foto 3",
                                        uri = os.foto3,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            expandedPhotoUri = os.foto3
                                            expandedPhotoLabel = "Foto 3"
                                        },
                                        onAddClick = { launchDet3.launch("image/*") },
                                        onRemoveClick = if (os.foto3.isNotEmpty()) { { viewModel.salvarOrdemServico(os.copy(foto3 = "")) } } else null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Signature capture canvas Widget
            item {
                var signatureExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { signatureExpanded = !signatureExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "ASSINATURA DO CLIENTE",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (signatureExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = signatureExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                SignaturePad(
                                    labelText = "Assinatura Digital do Cliente",
                                    onSignatureCaptured = { base64 -> signatureBase64 = base64 }
                                )
                            }
                        }
                    }
                }
            }

            // Actions Block (Saves, PDF exports)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val updVal = valorState.toDoubleOrNull() ?: 0.0
                            val updatedOs = os.copy(
                                status = statusState,
                                prioridade = prioridadeState,
                                diagnosticoTecnico = diagnosticoState,
                                pecasTrocadas = pecasState,
                                valorServico = updVal,
                                correnteEletrica = correnteState,
                                temperatura = temperaturaState,
                                pressaoGasAlta = pressaoAltaState,
                                pressaoGasBaixa = pressaoBaixaState,
                                assinaturaClienteNome = clienteNomeAssinatura,
                                assinaturaDigital = signatureBase64,
                                dataAgendada = dataConclusaoState
                            )
                            viewModel.salvarOrdemServico(updatedOs)
                            Toast.makeText(context, "Todas as informações foram salvas com sucesso!", Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Atualizar ordem", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (cliente != null) {
                                // Dynamic Save first
                                val updVal = valorState.toDoubleOrNull() ?: 0.0
                                val updatedOs = os.copy(
                                    status = statusState,
                                    prioridade = prioridadeState,
                                    diagnosticoTecnico = diagnosticoState,
                                    pecasTrocadas = pecasState,
                                    valorServico = updVal,
                                    correnteEletrica = correnteState,
                                    temperatura = temperaturaState,
                                    pressaoGasAlta = pressaoAltaState,
                                    pressaoGasBaixa = pressaoBaixaState,
                                    assinaturaClienteNome = clienteNomeAssinatura,
                                    assinaturaDigital = signatureBase64,
                                    dataAgendada = dataConclusaoState
                                )
                                viewModel.salvarOrdemServico(updatedOs)

                                val pdfFile = ReportUtils.generateOrdemServicoPdf(
                                    context = context,
                                    os = updatedOs,
                                    cliente = cliente,
                                    tecnico = tecnico,
                                    equipamento = equipamento,
                                    ambiente = ambiente
                                )
                                if (pdfFile != null) {
                                    ReportUtils.sharePdf(context, pdfFile)
                                } else {
                                    Toast.makeText(context, "Erro ao gerar PDF do checklist", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gerar e exportar PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Excluir Ordem de Serviço") },
            text = { Text("Deseja realmente excluir esta ordem de serviço? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.excluirOrdemServico(os)
                        Toast.makeText(context, "OS #${os.numeroOrdem} excluída com sucesso.", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
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

    if (expandedPhotoUri != null) {
        Dialog(onDismissRequest = { expandedPhotoUri = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(expandedPhotoLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { expandedPhotoUri = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }
                    if (expandedPhotoUri!!.startsWith("sim_")) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(60.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(expandedPhotoLabel, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("Foto Técnica Simulada de Suporte", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                            }
                        }
                    } else {
                        coil.compose.SubcomposeAsyncImage(
                            model = expandedPhotoUri,
                            contentDescription = expandedPhotoLabel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            loading = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                    Button(
                        onClick = { expandedPhotoUri = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fechar")
                    }
                }
            }
        }
    }
}

// --- SUB-COMPOSABLES FOR THE SCREEN ---

@Composable
fun InfoTextRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (backColor, txColor) = when(priority) {
        "Alta" -> Color(0xFFFEE2E2) to Color(0xFFEF5350)
        "Média" -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        else -> Color(0xFFF1F5F9) to Color(0xFF64748B)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = priority,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = txColor
        )
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
    )
}

@Composable
fun MeasurementDetailBadge(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value.ifEmpty { "Sem Medição" },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ImageSlot(
    imageUri: String,
    label: String,
    onSelect: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (imageUri.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            )
            .border(
                width = 1.dp,
                color = if (imageUri.isNotEmpty()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() },
        contentAlignment = Alignment.Center
    ) {
        if (imageUri.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (imageUri.startsWith("sim_")) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Text("Simulada", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    coil.compose.AsyncImage(
                        model = imageUri,
                        contentDescription = "Miniatura de Upload",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                
                // Remove overlay button in upper corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .background(Color.Black.copy(alpha = 0.62f), shape = CircleShape)
                        .clickable { onClear() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpar foto",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
                
                // Caption overlay at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DiagnosticPhotoView(
    label: String,
    uri: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onRemoveClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(105.dp)
            .border(
                1.dp,
                if (uri.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (uri.isNotEmpty()) onClick() else onAddClick()
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
                if (uri.startsWith("sim_")) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Simulada", color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp)
                        }
                    }
                } else {
                    coil.compose.SubcomposeAsyncImage(
                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                            .data(uri)
                            .size(200, 200)
                            .crossfade(true)
                            .build(),
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        loading = {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }

                // Header / Footer Overlay Label
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }

                // Delete photo overlay button
                if (onRemoveClick != null) {
                    IconButton(
                        onClick = onRemoveClick,
                        modifier = Modifier
                            .size(26.dp)
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remover", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Adicionar foto",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
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

@Composable
fun StatusBadge(status: String) {
    val displayStatus = when(status) {
        "Em Andamento", "EM_ANDAMENTO", "Em andamento" -> "Pendente"
        "PENDENTE" -> "Pendente"
        "CONCLUIDA", "Concluído", "Concluido" -> "Concluída"
        else -> status
    }
    val (backgroundColor, contentColor) = when(displayStatus) {
        "Aberta", "ABERTA" -> Color(0xFF1976D2) to Color.White
        "Pendente" -> Color(0xFFF57C00) to Color.White
        "Concluída" -> Color(0xFF0F9D58) to Color.White
        else -> Color(0xFF78909C) to Color.White
    }
    val displayText = displayStatus
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = displayText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun HighlightedSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp)
            .background(color.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun CardDetailCell(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NoIconCardDetailCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SmallPecaCard(peca: com.priorizedev.brizza.data.model.Peca) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // First row: Name
            Text(
                text = peca.nome,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Second row: Marca + Preço
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Marca: ${peca.marca.ifEmpty { "Genérica" }}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = String.format("R$ %.2f", peca.preco),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


@Composable
fun PecaCardWithDelete(
    peca: com.priorizedev.brizza.data.model.Peca,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 1ª Linha: Nome (Left) + Botão X (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = peca.nome,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remover",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            // 2ª Linha: Marca (Left) + Preço (Right - bem na direita do card)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Marca: ${peca.marca.ifEmpty { "Genérica" }}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = String.format("R$ %.2f", peca.preco),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}
