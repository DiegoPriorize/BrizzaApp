package com.priorizedev.brizza.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.priorizedev.brizza.data.model.Ambiente
import com.priorizedev.brizza.data.model.Cliente
import com.priorizedev.brizza.data.model.Equipamento
import com.priorizedev.brizza.data.model.OrdemServico
import com.priorizedev.brizza.data.model.Tecnico
import com.priorizedev.brizza.ui.viewmodel.ClimaGestViewModel
import androidx.activity.compose.BackHandler
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

fun parseEquipamentosList(serialized: String): List<Triple<String, String, String>> {
    if (serialized.isBlank()) return emptyList()
    return serialized.split(";").filter { it.isNotBlank() }.map { part ->
        val fields = part.split("|")
        val id = fields.getOrNull(0) ?: ""
        val status = fields.getOrNull(1) ?: "Pendente"
        val date = fields.getOrNull(2) ?: ""
        Triple(id, status, date)
    }
}

fun serializeEquipamentosList(list: List<Triple<String, String, String>>): String {
    return list.joinToString(";") { "${it.first}|${it.second}|${it.third}" }
}

fun resetEquipamentosSerialized(serialized: String): String {
    val list = parseEquipamentosList(serialized)
    val resetList = list.map { Triple(it.first, "Pendente", "") }
    return serializeEquipamentosList(resetList)
}

fun calcularProximaData(ultimaDataStr: String, frequencia: String): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val date = try {
        sdf.parse(ultimaDataStr)
    } catch (e: Exception) {
        null
    } ?: return ultimaDataStr
    
    val cal = Calendar.getInstance()
    cal.time = date
    
    when (frequencia) {
        "3 meses" -> cal.add(Calendar.MONTH, 3)
        "6 meses" -> cal.add(Calendar.MONTH, 6)
        "1 ano" -> cal.add(Calendar.YEAR, 1)
        "2 anos" -> cal.add(Calendar.YEAR, 2)
        else -> cal.add(Calendar.MONTH, 3)
    }
    
    return sdf.format(cal.time)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramacaoScreen(
    viewModel: ClimaGestViewModel,
    onBack: () -> Unit,
    onNavigateToOrdemId: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val userThemeSelection by viewModel.themeState.collectAsStateWithLifecycle()
    val isDark = when (userThemeSelection) {
        null -> androidx.compose.foundation.isSystemInDarkTheme()
        else -> userThemeSelection!!
    }
    val dataTextColor = MaterialTheme.colorScheme.onSurface
    
    val clientes by viewModel.clientesState.collectAsStateWithLifecycle()
    val ambientes by viewModel.ambientesState.collectAsStateWithLifecycle()
    val equipamentos by viewModel.equipamentosState.collectAsStateWithLifecycle()
    val tecnicos by viewModel.tecnicosState.collectAsStateWithLifecycle()
    val ordens by viewModel.ordensServicoState.collectAsStateWithLifecycle()
    val programasPreventivos by viewModel.programasPreventivosState.collectAsStateWithLifecycle()

    // Tab state: 0 = Cronograma, 1 = Programas
    var selectedTab by remember { mutableStateOf(0) }

    // Screen states
    var showSchedulingForm by remember { mutableStateOf(false) }
    var activeProgramForDetails by remember { mutableStateOf<com.priorizedev.brizza.data.model.ProgramaPreventivo?>(null) }

    // Form inputs
    var selectedCliente by remember { mutableStateOf<Cliente?>(null) }
    var selectedAmbiente by remember { mutableStateOf<Ambiente?>(null) }
    var selectedEquipamento by remember { mutableStateOf<Equipamento?>(null) }
    var selectedTipoServico by remember { mutableStateOf("Higienização") } // "Limpeza", "Higienização", "Preventiva", "Corretiva"
    var selectedDateStr by remember { mutableStateOf("") }
    var selectedTimeStr by remember { mutableStateOf("08:00") }
    var selectedTecnico by remember { mutableStateOf<Tecnico?>(null) }
    var descInput by remember { mutableStateOf("") }

    // Dropdown expanded states
    var showClientDropdown by remember { mutableStateOf(false) }
    var showAmbienteDropdown by remember { mutableStateOf(false) }
    var showEquipmentDropdown by remember { mutableStateOf(false) }
    var showTipoDropdown by remember { mutableStateOf(false) }
    var showTecnicoDropdown by remember { mutableStateOf(false) }

    // States for create program form
    var showCreateProgramForm by remember { mutableStateOf(false) }
    var progSelectedCliente by remember { mutableStateOf<Cliente?>(null) }
    var progSelectedPeriodo by remember { mutableStateOf("3 meses") }
    var progSelectedDateStr by remember { mutableStateOf("") }
    var progSelectedTecnico by remember { mutableStateOf<Tecnico?>(null) }
    var progSelectedEquipaments by remember { mutableStateOf<Set<String>>(emptySet()) }
    var progNotificarCliente by remember { mutableStateOf(true) }
    var progNotificarApp by remember { mutableStateOf(true) }
    var showRenewDialog by remember { mutableStateOf(false) }
    var programToRenew by remember { mutableStateOf<com.priorizedev.brizza.data.model.ProgramaPreventivo?>(null) }
    var showProgClientDropdown by remember { mutableStateOf(false) }
    var showProgTecnicoDropdown by remember { mutableStateOf(false) }

    // Date picker setup
    val calendar = remember { Calendar.getInstance() }
    val progDatePickerDialog = remember {
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val formattedMonth = if (month + 1 < 10) "0${month + 1}" else "${month + 1}"
                val formattedDay = if (dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                progSelectedDateStr = "$formattedDay/$formattedMonth/$year"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val formattedMonth = if (month + 1 < 10) "0${month + 1}" else "${month + 1}"
                val formattedDay = if (dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                selectedDateStr = "$formattedDay/$formattedMonth/$year"
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Filtered lists for scheduling form
    val filteredAmbientes = remember(selectedCliente, ambientes) {
        if (selectedCliente == null) emptyList()
        else ambientes.filter { it.clienteId == selectedCliente?.id }
    }

    val filteredEquipamentos = remember(selectedAmbiente, equipamentos) {
        if (selectedAmbiente == null) emptyList()
        else equipamentos.filter { it.ambienteId == selectedAmbiente?.id }
    }

    // Agenda: list of all preventative / cleaning / sanitization ordens scheduled
    val scheduledMaintenances = remember(ordens) {
        ordens.filter {
            it.tipoServico in listOf("Programada", "PROGRAMADA")
        }.sortedWith(compareByDescending<OrdemServico> { it.status == "Aberta" || it.status == "Pendente" || it.status == "Em Andamento" || it.status == "Em andamento" }.thenByDescending { it.dataAgendada })
    }

    if (activeProgramForDetails == null) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Programas",
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                if (selectedTab == -1) {
                    // TAB 0: CRONOGRAMAS

                    // A. SCHEDULING FORM EXPANDABLE WINDOW
                    item {
                        AnimatedVisibility(visible = showSchedulingForm) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "AGENDAR MANUTENÇÃO",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { showSchedulingForm = false },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 1. SELECT CLIENTE
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = selectedCliente?.nome ?: "Selecionar Cliente",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Cliente") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showClientDropdown = true },
                                            enabled = false,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                disabledTextColor = dataTextColor,
                                                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                disabledLabelColor = MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clickable { showClientDropdown = true }
                                        )
                                        DropdownMenu(
                                            expanded = showClientDropdown,
                                            onDismissRequest = { showClientDropdown = false },
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            if (clientes.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("Nenhum cliente cadastrado") },
                                                    onClick = { showClientDropdown = false }
                                                )
                                            } else {
                                                clientes.forEach { cl ->
                                                    DropdownMenuItem(
                                                        text = { Text(cl.nome, color = dataTextColor) },
                                                        onClick = {
                                                            selectedCliente = cl
                                                            selectedAmbiente = null
                                                            selectedEquipamento = null
                                                            showClientDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 2. SELECT AMBIENTE
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = selectedAmbiente?.nome ?: "Selecionar Ambiente",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Ambiente") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { if (selectedCliente != null) showAmbienteDropdown = true },
                                            enabled = false,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                disabledTextColor = dataTextColor,
                                                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                disabledLabelColor = MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clickable { if (selectedCliente != null) showAmbienteDropdown = true }
                                        )
                                        DropdownMenu(
                                            expanded = showAmbienteDropdown,
                                            onDismissRequest = { showAmbienteDropdown = false },
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            if (filteredAmbientes.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("Nenhum ambiente encontrado") },
                                                    onClick = { showAmbienteDropdown = false }
                                                )
                                            } else {
                                                filteredAmbientes.forEach { amb ->
                                                    DropdownMenuItem(
                                                        text = { Text(amb.nome, color = dataTextColor) },
                                                        onClick = {
                                                            selectedAmbiente = amb
                                                            selectedEquipamento = null
                                                            showAmbienteDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 3. SELECT EQUIPAMENTO
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = if (selectedEquipamento != null) "${selectedEquipamento!!.marca} ${selectedEquipamento!!.modelo}" else "Selecionar Equipamento",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Equipamento") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { if (selectedAmbiente != null) showEquipmentDropdown = true },
                                            enabled = false,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                disabledTextColor = dataTextColor,
                                                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                disabledLabelColor = MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clickable { if (selectedAmbiente != null) showEquipmentDropdown = true }
                                        )
                                        DropdownMenu(
                                            expanded = showEquipmentDropdown,
                                            onDismissRequest = { showEquipmentDropdown = false },
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            if (filteredEquipamentos.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("Nenhum equipamento neste ambiente") },
                                                    onClick = { showEquipmentDropdown = false }
                                                )
                                            } else {
                                                filteredEquipamentos.forEach { eq ->
                                                    DropdownMenuItem(
                                                        text = { Text("${eq.marca} • ${eq.modelo} (${eq.capacidadeBtu} BTU)", color = dataTextColor) },
                                                        onClick = {
                                                            selectedEquipamento = eq
                                                            showEquipmentDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 4. CHOOSE TYPE (Limpeza vs Higienização vs Preventiva vs Corretiva)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = selectedTipoServico,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Tipo de Serviço") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showTipoDropdown = true },
                                            enabled = false,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                disabledTextColor = dataTextColor,
                                                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                disabledLabelColor = MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clickable { showTipoDropdown = true }
                                        )
                                        DropdownMenu(
                                            expanded = showTipoDropdown,
                                            onDismissRequest = { showTipoDropdown = false },
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            val tipos = listOf("Limpeza", "Higienização", "Preventiva", "Corretiva")
                                            tipos.forEach { tip ->
                                                DropdownMenuItem(
                                                    text = { Text(tip, color = dataTextColor) },
                                                    onClick = {
                                                        selectedTipoServico = tip
                                                        showTipoDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 5. DATE & TIME
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = selectedDateStr,
                                            onValueChange = { selectedDateStr = it },
                                            label = { Text("Data") },
                                            placeholder = { Text("dd/MM/yyyy") },
                                            modifier = Modifier.weight(1.3f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            trailingIcon = {
                                                IconButton(onClick = { datePickerDialog.show() }) {
                                                    Icon(Icons.Default.CalendarToday, contentDescription = "Data", tint = MaterialTheme.colorScheme.primary)
                                                }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = dataTextColor,
                                                unfocusedTextColor = dataTextColor,
                                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            )
                                        )

                                        OutlinedTextField(
                                            value = selectedTimeStr,
                                            onValueChange = { selectedTimeStr = it },
                                            label = { Text("Hora") },
                                            placeholder = { Text("08:00") },
                                            modifier = Modifier.weight(0.7f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = dataTextColor,
                                                unfocusedTextColor = dataTextColor,
                                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 6. RESPONSABLE TECNICO
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = selectedTecnico?.nome ?: "Selecionar Técnico",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Técnico Responsável") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showTecnicoDropdown = true },
                                            enabled = false,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                disabledTextColor = dataTextColor,
                                                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                disabledLabelColor = MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clickable { showTecnicoDropdown = true }
                                        )
                                        DropdownMenu(
                                            expanded = showTecnicoDropdown,
                                            onDismissRequest = { showTecnicoDropdown = false },
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            if (tecnicos.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("Nenhum técnico cadastrado") },
                                                    onClick = { showTecnicoDropdown = false }
                                                )
                                            } else {
                                                tecnicos.forEach { tc ->
                                                    DropdownMenuItem(
                                                        text = { Text(tc.nome, color = dataTextColor) },
                                                        onClick = {
                                                            selectedTecnico = tc
                                                            showTecnicoDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 7. DESCRICAO
                                    OutlinedTextField(
                                        value = descInput,
                                        onValueChange = { descInput = it },
                                        label = { Text("Observações adicionais (Opcional)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = dataTextColor,
                                            unfocusedTextColor = dataTextColor,
                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // SUBMIT BUTTON FORM
                                    Button(
                                        onClick = {
                                            if (selectedCliente == null || selectedAmbiente == null || selectedEquipamento == null || selectedDateStr.isBlank() || selectedTecnico == null) {
                                                Toast.makeText(context, "Erro: Preencha todos os campos obrigatórios!", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }

                                            val newOrdem = OrdemServico(
                                                clienteId = selectedCliente!!.id,
                                                tecnicoId = selectedTecnico!!.id,
                                                equipamentoId = selectedEquipamento!!.id,
                                                tipoServico = "Programada",
                                                dataAgendada = "$selectedDateStr às $selectedTimeStr",
                                                dataChamado = selectedDateStr,
                                                descricao = descInput.ifBlank { "Manutenção periódica para o equipamento ${selectedEquipamento!!.marca} no ambiente ${selectedAmbiente!!.nome}." },
                                                status = "Aberta"
                                            )

                                            viewModel.salvarOrdemServico(newOrdem)
                                            Toast.makeText(context, "Manutenção agendada com Sucesso!", Toast.LENGTH_SHORT).show()
                                            
                                            // Reset fields
                                            selectedCliente = null
                                            selectedAmbiente = null
                                            selectedEquipamento = null
                                            selectedDateStr = ""
                                            selectedTimeStr = "08:00"
                                            selectedTecnico = null
                                            descInput = ""
                                            showSchedulingForm = false
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                                    ) {
                                        Icon(Icons.Default.Done, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Inserir na Programação", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    // B. AGENDA HEADER
                    item {
                        Text(
                            text = "CRONOGRAMAS PROGRAMADOS (${scheduledMaintenances.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    // C. RENDER AGENDA LISTING
                    if (scheduledMaintenances.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.EventBusy,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Nenhum agendamento de limpeza, higienização ou preventiva programado.",
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            color = dataTextColor
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(scheduledMaintenances) { os ->
                            val cl = clientes.find { it.id == os.clienteId }
                            val tc = tecnicos.find { it.id == os.tecnicoId }
                            val eq = equipamentos.find { it.id == os.equipamentoId }
                            val amb = eq?.let { eqItem -> ambientes.find { it.id == eqItem.ambienteId } }

                            val isProgramada = os.tipoServico.equals("Programada", ignoreCase = true) || os.tipoServico.equals("PROGRAMADA", ignoreCase = true)
                            val displayTipoText = if (isProgramada) "Programada" else os.tipoServico
                            val badgeColor = when {
                                isProgramada -> if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED)
                                os.tipoServico == "Higienização" -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                os.tipoServico == "Limpeza" -> if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488)
                                os.tipoServico == "Preventiva" -> if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToOrdemId(os.id) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Dynamic badge indicating Limpeza vs Higienizacao vs Preventiva
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(badgeColor.copy(alpha = 0.12f))
                                                .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = displayTipoText.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Event,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = os.dataAgendada,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = dataTextColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = cl?.nome ?: "Cliente Desconhecido",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = dataTextColor
                                    )

                                    if (eq != null) {
                                        Text(
                                            text = "Equipamento: ${eq.marca} • ${eq.modelo} (${eq.capacidadeBtu} BTU/h)",
                                            fontSize = 11.sp,
                                            color = dataTextColor
                                        )
                                    }
                                    if (amb != null) {
                                        Text(
                                            text = "Setor/Ambiente: ${amb.nome}",
                                            fontSize = 11.sp,
                                            color = dataTextColor
                                        )
                                    }
                                    if (tc != null) {
                                        Text(
                                            text = "Técnico Responsável: ${tc.nome}",
                                            fontSize = 11.sp,
                                            color = dataTextColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = os.descricao,
                                            fontSize = 11.sp,
                                            color = dataTextColor,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        val statusColor = when (os.status) {
                                            "Aberta" -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                                            "Em Andamento", "Em andamento", "Pendente" -> if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                                            "Concluída" -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                            else -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                                        }
                                        Text(
                                            text = if (os.status == "Em Andamento" || os.status == "Em andamento") "Pendente" else os.status,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                }
                if (selectedTab != -1) {
                    // TAB 1: PROGRAMAS PREVENTIVOS REGISTROS

                    if (showCreateProgramForm) {
                        // ----------------------------------------------------
                        // CREATION FORM SECTION FOR NEW PREVENTIVE PROGRAM
                        // ----------------------------------------------------
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Form Title
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Build,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "Novo Programa Preventivo",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(
                                            onClick = { showCreateProgramForm = false },
                                            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    // 1. SELECT CLIENTE
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Cliente Proprietário*", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = progSelectedCliente?.nome ?: "Selecionar Cliente",
                                                onValueChange = {},
                                                readOnly = true,
                                                modifier = Modifier.fillMaxWidth().clickable { showProgClientDropdown = true },
                                                enabled = false,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    disabledTextColor = dataTextColor,
                                                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                    disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                                                ),
                                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            Box(
                                                modifier = Modifier.matchParentSize().clickable { showProgClientDropdown = true }
                                            )
                                            DropdownMenu(
                                                expanded = showProgClientDropdown,
                                                onDismissRequest = { showProgClientDropdown = false },
                                                modifier = Modifier.fillMaxWidth(0.85f)
                                            ) {
                                                if (clientes.isEmpty()) {
                                                    DropdownMenuItem(
                                                        text = { Text("Nenhum cliente cadastrado") },
                                                        onClick = { showProgClientDropdown = false }
                                                    )
                                                } else {
                                                    clientes.forEach { cl ->
                                                        DropdownMenuItem(
                                                            text = { Text(cl.nome, color = dataTextColor) },
                                                            onClick = {
                                                                progSelectedCliente = cl
                                                                progSelectedEquipaments = emptySet() // Reset equipment multi-selection
                                                                showProgClientDropdown = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 2. PRIMEIRA VISITA (DATA AGENDADA)
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Data da Última Manutenção*", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        OutlinedTextField(
                                            value = progSelectedDateStr,
                                            onValueChange = {},
                                            placeholder = { Text("Clique para escolher a data", fontSize = 13.sp) },
                                            readOnly = true,
                                            modifier = Modifier.fillMaxWidth().clickable { progDatePickerDialog.show() },
                                            enabled = false,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                disabledTextColor = dataTextColor,
                                                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                                            ),
                                            trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }

                                    // 3. SELECT TECNICO RESPONSAVEL
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Técnico Responsável*", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = progSelectedTecnico?.nome ?: "Selecionar Técnico",
                                                onValueChange = {},
                                                readOnly = true,
                                                modifier = Modifier.fillMaxWidth().clickable { showProgTecnicoDropdown = true },
                                                enabled = false,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    disabledTextColor = dataTextColor,
                                                    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                    disabledTrailingIconColor = MaterialTheme.colorScheme.primary
                                                ),
                                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            Box(
                                                modifier = Modifier.matchParentSize().clickable { showProgTecnicoDropdown = true }
                                            )
                                            DropdownMenu(
                                                expanded = showProgTecnicoDropdown,
                                                onDismissRequest = { showProgTecnicoDropdown = false },
                                                modifier = Modifier.fillMaxWidth(0.85f)
                                            ) {
                                                if (tecnicos.isEmpty()) {
                                                    DropdownMenuItem(
                                                        text = { Text("Nenhum técnico cadastrado") },
                                                        onClick = { showProgTecnicoDropdown = false }
                                                    )
                                                } else {
                                                    tecnicos.forEach { tc ->
                                                        DropdownMenuItem(
                                                            text = { Text(tc.nome, color = dataTextColor) },
                                                            onClick = {
                                                                progSelectedTecnico = tc
                                                                showProgTecnicoDropdown = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 4. PERIOD / FREQUENCY SELECTOR
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Frequência de Visitas*", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf("3 meses", "6 meses", "1 ano", "2 anos").forEach { period ->
                                                val isSelected = progSelectedPeriodo == period
                                                val chipBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                val chipTextCol = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(chipBg)
                                                        .clickable { progSelectedPeriodo = period }
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
                                    }

                                    // 5. EQUIPAMENTOS CHECKLIST (Similar to PMOC)
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Equipamentos do Contrato*",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        if (progSelectedCliente == null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
                                                    .padding(14.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Selecione um cliente para carregar os equipamentos.",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        } else {
                                            val clientEquips = equipamentos.filter { it.clienteId == progSelectedCliente!!.id }
                                            
                                            if (clientEquips.isEmpty()) {
                                                Text(
                                                    text = "Este cliente não possui equipamentos cadastrados.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            } else {
                                                val allSelected = clientEquips.isNotEmpty() && progSelectedEquipaments.size == clientEquips.size
                                                
                                                // Marcar todos row
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            progSelectedEquipaments = if (allSelected) {
                                                                emptySet()
                                                            } else {
                                                                clientEquips.map { it.id }.toSet()
                                                            }
                                                        }
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = allSelected,
                                                        onCheckedChange = { checked ->
                                                            progSelectedEquipaments = if (checked == true) {
                                                                clientEquips.map { it.id }.toSet()
                                                            } else {
                                                                emptySet()
                                                            }
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Marcar Todos Equips (${clientEquips.size})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                }

                                                // List vertical containing each equipment card
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    clientEquips.forEach { eq ->
                                                        val isChecked = progSelectedEquipaments.contains(eq.id)
                                                        val ambientName = ambientes.find { it.id == eq.ambienteId }?.nome ?: "Desconhecido"
                                                        
                                                        Card(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    progSelectedEquipaments = if (isChecked) {
                                                                        progSelectedEquipaments - eq.id
                                                                    } else {
                                                                        progSelectedEquipaments + eq.id
                                                                    }
                                                                },
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                                            ),
                                                            border = BorderStroke(
                                                                0.5.dp,
                                                                if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                            ),
                                                            shape = RoundedCornerShape(12.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                            ) {
                                                                Checkbox(
                                                                    checked = isChecked,
                                                                    onCheckedChange = { checked ->
                                                                        progSelectedEquipaments = if (checked == true) {
                                                                            progSelectedEquipaments + eq.id
                                                                        } else {
                                                                            progSelectedEquipaments - eq.id
                                                                        }
                                                                    }
                                                                )
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(text = "${eq.marca} • ${eq.modelo} (${eq.tipo})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = dataTextColor)
                                                                    Text(text = "TAG: ${eq.tag} | ${eq.capacidadeBtu} BTU", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                    Text(text = "Setor: $ambientName", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 6. NOTIFICATION ALERTS
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Notificar Cliente (E-mail/Zap)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                            Switch(
                                                checked = progNotificarCliente,
                                                onCheckedChange = { progNotificarCliente = it },
                                                modifier = Modifier.scale(0.85f)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Notificar no Aplicativo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                            Switch(
                                                checked = progNotificarApp,
                                                onCheckedChange = { progNotificarApp = it },
                                                modifier = Modifier.scale(0.85f)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    // 7. ACTION BUTTONS
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { showCreateProgramForm = false },
                                            modifier = Modifier.weight(0.7f).height(44.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Voltar", fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = {
                                                // Validation check
                                                if (progSelectedCliente == null) {
                                                    Toast.makeText(context, "Por favor, selecione o cliente!", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (progSelectedDateStr.isEmpty()) {
                                                    Toast.makeText(context, "Por favor, informe a data inicial!", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (progSelectedTecnico == null) {
                                                    Toast.makeText(context, "Por favor, selecione o técnico responsável!", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (progSelectedEquipaments.isEmpty()) {
                                                    Toast.makeText(context, "Por favor, selecione ao menos um equipamento!", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }

                                                // Save logic
                                                val clientEquips = equipamentos.filter { it.id in progSelectedEquipaments }
                                                val serialParts = clientEquips.map { "${it.id}|Pendente|" }
                                                val serializedString = serialParts.joinToString(";")

                                                val newProgram = com.priorizedev.brizza.data.model.ProgramaPreventivo(
                                                    id = com.priorizedev.brizza.data.model.generateRandomId(),
                                                    clienteId = progSelectedCliente!!.id,
                                                    ambienteId = "",
                                                    equipamentoId = "",
                                                    dataAgendada = calcularProximaData(progSelectedDateStr, progSelectedPeriodo),
                                                    periodo = progSelectedPeriodo,
                                                    ordemServicoOriginalId = "",
                                                    ordemServicoCriadaId = "",
                                                    ativo = true,
                                                    cancelado = false,
                                                    notificarCliente = progNotificarCliente,
                                                    notificarApp = progNotificarApp,
                                                    usuarioEmail = progSelectedCliente!!.usuarioEmail,
                                                    status = "Pendente",
                                                    tecnicoId = progSelectedTecnico!!.id,
                                                    equipamentosSerialized = serializedString
                                                )

                                                viewModel.salvarProgramaPreventivo(newProgram)

                                                Toast.makeText(context, "Programa preventivo criado com sucesso contendo ${clientEquips.size} equipamento(s)!", Toast.LENGTH_LONG).show()
                                                showCreateProgramForm = false
                                                
                                                // Reset fields
                                                progSelectedCliente = null
                                                progSelectedDateStr = ""
                                                progSelectedTecnico = null
                                                progSelectedEquipaments = emptySet()
                                            },
                                            modifier = Modifier.weight(1.3f).height(44.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Criar Programação", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOTAL DE PROGRAMAS (${programasPreventivos.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { showCreateProgramForm = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Criar Programa",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Criar Programa", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (programasPreventivos.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Nenhum programa preventivo cadastrado ainda.\n\nAbra uma Ordem de Serviço concluída e toque em 'PROGRAMAR PREVENTIVA' para iniciar um novo programa.",
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center,
                                                color = dataTextColor
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            items(programasPreventivos) { prog ->
                            val cl = clientes.find { it.id == prog.clienteId }
                            val tech = tecnicos.find { it.id == prog.tecnicoId }
                            val parsedEquips = parseEquipamentosList(prog.equipamentosSerialized)
                            val totalEquipsCount = parsedEquips.size
                            val concludedEquipsCount = parsedEquips.count { it.second == "Concluído" }

                            val isFinished = prog.status == "Concluído" || !prog.ativo

                            val statusLabel = when {
                                prog.cancelado -> "Cancelado"
                                isFinished -> "Concluído"
                                else -> "Ativo (${concludedEquipsCount}/${totalEquipsCount})"
                            }
                            val statusBg = when {
                                prog.cancelado -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                                isFinished -> if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A)
                                else -> if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeProgramForDetails = prog },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(statusBg.copy(alpha = 0.12f))
                                                .border(1.dp, statusBg.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = statusLabel.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusBg
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Timelapse,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Período: ${prog.periodo}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = dataTextColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = cl?.nome ?: "Cliente Desconhecido",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = dataTextColor
                                    )

                                    if (tech != null) {
                                        Text(
                                            text = "Técnico Responsável: ${tech.nome}",
                                            fontSize = 11.sp,
                                            color = dataTextColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Equipamentos do Programa: $concludedEquipsCount de $totalEquipsCount concluídos",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Próxima visita: ${prog.dataAgendada}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Quick Notification Status Indicators
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (prog.notificarCliente) Icons.Default.MarkEmailRead else Icons.Default.Cancel,
                                                contentDescription = null,
                                                tint = if (prog.notificarCliente) Color(0xFF0D9488) else Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Notif. Cliente",
                                                fontSize = 10.sp,
                                                color = dataTextColor
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (prog.notificarApp) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                                contentDescription = null,
                                                tint = if (prog.notificarApp) Color(0xFF4F46E5) else Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Notif. App",
                                                fontSize = 10.sp,
                                                color = dataTextColor
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
    }
    }

    // Full Screen presentation view for a selected ProgramaPreventivo (Highly Polished & Exquisite UI)
    activeProgramForDetails?.let { prog ->
        val cl = clientes.find { it.id == prog.clienteId }
        val tech = tecnicos.find { it.id == prog.tecnicoId }
        val parsedEquips = parseEquipamentosList(prog.equipamentosSerialized)

        var menuExpanded by remember { mutableStateOf(false) }
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }

        BackHandler {
            activeProgramForDetails = null
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Excluir Programa?", fontWeight = FontWeight.Bold) },
                text = { Text("Deseja realmente excluir este programa preventivo de manutenção? Esta ação não poderá ser desfeita.") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.excluirProgramaPreventivo(prog)
                            activeProgramForDetails = null
                            Toast.makeText(context, "Programa preventivo excluído com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Detalhes do Programa",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { activeProgramForDetails = null }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        },
                        windowInsets = WindowInsets(top = 0.dp),
                        actions = {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Mais opções",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    text = { Text("Gerar PDF", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                                    onClick = {
                                        menuExpanded = false
                                        val pdfFile = com.priorizedev.brizza.util.ReportUtils.generateProgramaPreventivoPdf(
                                            context = context,
                                            prog = prog,
                                            cliente = cl,
                                            tecnico = tech,
                                            equipamentos = equipamentos,
                                            ambientes = ambientes
                                        )
                                        if (pdfFile != null) {
                                            com.priorizedev.brizza.util.ReportUtils.sharePdf(context, pdfFile)
                                        } else {
                                            Toast.makeText(context, "Erro ao gerar PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    text = { Text("Excluir", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                                    onClick = {
                                        menuExpanded = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    )
                }
            ) { innerPad ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPad)
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Progress & Insight Summary Card (Modern High-Fidelity UI Component)
                    val totalEquipsCount = parsedEquips.size
                    val concludedEquipsCount = parsedEquips.count { it.second == "Concluído" }
                    val progressFraction = if (totalEquipsCount > 0) concludedEquipsCount.toFloat() / totalEquipsCount else 0F
                    val progressPct = (progressFraction * 100).toInt()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Progresso Geral",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = "$concludedEquipsCount de $totalEquipsCount concluídos ($progressPct%)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Este programa possui $totalEquipsCount equipamentos programados para manutenção periódica.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Status Badge row
                    val isFinished = prog.status == "Concluído" || !prog.ativo
                    val statusText = when {
                        prog.cancelado -> "Cancelado"
                        isFinished -> "Concluído"
                        else -> "Ativo"
                    }
                    val statusColor = when {
                        prog.cancelado -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                        isFinished -> if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A)
                        else -> if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(statusColor.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                            .border(BorderStroke(1.dp, statusColor.copy(alpha = 0.15f)), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Text(
                                text = "Status:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                             )
                        }
                        Text(
                            text = statusText.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Section: Client & Technician info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Customer info ROW
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                               ) {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                               }
                                Column {
                                    Text("CLIENTE PARCEIRO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.8.sp)
                                    Text(cl?.nome ?: "Não atribuído", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // Technical responsible
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                               ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                               }
                                Column {
                                    Text("TÉCNICO RESPONSÁVEL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, letterSpacing = 0.8.sp)
                                    Text(tech?.nome ?: "Não atribuído", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Section: Timeline & Frequency details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Periodicidade
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("PERIODICIDADE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Text(prog.periodo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        
                        // Proxima visita
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("PRÓXIMA DATA", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE28743), letterSpacing = 0.5.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFFE28743), modifier = Modifier.size(16.dp))
                                    Text(prog.dataAgendada, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    // SECTION: Equipment list with completion toggle inside this program
                    Text(
                        text = "EQUIPAMENTOS DO PROGRAMA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )

                    parsedEquips.forEach { itemTriple ->
                        val eqId = itemTriple.first
                        val eqStatus = itemTriple.second
                        val eqDate = itemTriple.third

                        val eqItem = equipamentos.find { it.id == eqId }
                        val eqAmb = eqItem?.let { item -> ambientes.find { it.id == item.ambienteId } }

                        val isCompleted = eqStatus == "Concluído"
                        val listAccentColor = if (isCompleted) Color(0xFF16A34A) else Color(0xFFD97706)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isCompleted) Color(0xFF16A34A).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCompleted) Color(0xFF16A34A).copy(alpha = 0.03f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = eqItem?.let { "${it.marca} ${it.capacidadeBtu} BTU/h" } ?: "Equipamento Removido",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        if (eqAmb != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Place,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Setor: ${eqAmb.nome}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        // Status badge for individual equipment
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(listAccentColor.copy(alpha = 0.1f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Box(modifier = Modifier.size(6.dp).background(listAccentColor, CircleShape))
                                                Text(
                                                    text = if (isCompleted) "Revisado em: $eqDate" else "Pendente",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = listAccentColor
                                                )
                                            }
                                        }
                                    }

                                    // Action status conversion
                                    if (!isFinished && !prog.cancelado) {
                                        if (eqStatus != "Concluído") {
                                            Button(
                                                onClick = {
                                                    // Request date via DatePickerDialog
                                                    val tempCal = Calendar.getInstance()
                                                    DatePickerDialog(
                                                        context,
                                                        { _, year, month, dayOfMonth ->
                                                            val formattedMonth = if (month + 1 < 10) "0${month + 1}" else "${month + 1}"
                                                            val formattedDay = if (dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                                                            val chosenDate = "$formattedDay/$formattedMonth/$year"
                                                            
                                                            val updatedList = parsedEquips.map {
                                                                if (it.first == eqId) Triple(eqId, "Concluído", chosenDate) else it
                                                            }
                                                            val updatedProg = prog.copy(
                                                                equipamentosSerialized = serializeEquipamentosList(updatedList)
                                                            )
                                                            viewModel.salvarProgramaPreventivo(updatedProg)
                                                            activeProgramForDetails = updatedProg
                                                            Toast.makeText(context, "Equipamento concluído com sucesso!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        tempCal.get(Calendar.YEAR),
                                                        tempCal.get(Calendar.MONTH),
                                                        tempCal.get(Calendar.DAY_OF_MONTH)
                                                    ).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Concluir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            // Reset back to pendente
                                            OutlinedButton(
                                                onClick = {
                                                    val updatedList = parsedEquips.map {
                                                        if (it.first == eqId) Triple(eqId, "Pendente", "") else it
                                                    }
                                                    val updatedProg = prog.copy(
                                                        equipamentosSerialized = serializeEquipamentosList(updatedList)
                                                    )
                                                    viewModel.salvarProgramaPreventivo(updatedProg)
                                                    activeProgramForDetails = updatedProg
                                                    Toast.makeText(context, "Equipamento restaurado para pendente.", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Resetar", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // SECTION: Program final completion block
                    if (!isFinished && !prog.cancelado) {
                        val allConcluded = parsedEquips.isNotEmpty() && parsedEquips.all { it.second == "Concluído" }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (allConcluded) Color(0xFF16A34A).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(
                                1.dp, 
                                if (allConcluded) Color(0xFF16A34A).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (!allConcluded) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Revise todos os aparelhos acima para finalizar o programa.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Tudo pronto! Você pode fechar o programa e gerar as Ordens de Serviço.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        programToRenew = prog; showRenewDialog = true; if (false) { // 1. Set program state as concluded
                                        val updatedProg = prog.copy(
                                            status = "Concluído",
                                            ativo = false
                                        )
                                        viewModel.salvarProgramaPreventivo(updatedProg)
                                        
                                        // 2. Create OS for each companion equipment inside this program
                                        parsedEquips.forEach { itemTriple ->
                                            val eqId = itemTriple.first
                                            val dateVal = itemTriple.third
                                            val eqItem = equipamentos.find { it.id == eqId }
                                            val envName = eqItem?.let { item -> ambientes.find { it.id == item.ambienteId }?.nome } ?: "Geral"
                                            val newOrdemId = com.priorizedev.brizza.data.model.generateOrdemServicoId()
                                            
                                            val automaticOS = OrdemServico(
                                                id = newOrdemId,
                                                clienteId = prog.clienteId,
                                                tecnicoId = prog.tecnicoId,
                                                equipamentoId = eqId,
                                                tipoServico = "Preventiva",
                                                dataChamado = dateVal,
                                                dataAgendada = dateVal,
                                                descricao = "Visita técnica preventiva realizada com sucesso. Equipamento: ${eqItem?.marca ?: ""} ${eqItem?.modelo ?: ""}. Setor: $envName. Visita agendada para o dia ${prog.dataAgendada}.",
                                                status = "Concluída",
                                                prioridade = "Média",
                                                descricaoResumida = "Prev. ${prog.periodo}",
                                                usuarioEmail = prog.usuarioEmail
                                            )
                                            viewModel.salvarOrdemServico(automaticOS)
                                        }
                                        
                                        activeProgramForDetails = null
                                        Toast.makeText(context, "Programa concluído com sucesso! Ordens de serviço concluídas criadas em Ordens de Serviço.", Toast.LENGTH_LONG).show() }
                                    },
                                    enabled = allConcluded,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (allConcluded) Color(0xFF16A34A) else Color.Gray.copy(alpha = 0.3f),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Finalizar e Gerar Ordens de Serviço", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 2º alteração: Se estiver concluído, mostrar a opção de Renovar, criando uma nova.
                    if (isFinished && !prog.cancelado) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(
                                1.dp, 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Este programa de prevenção já foi concluído. Deseja realizar a renovação e iniciar um novo programa no próximo ano?",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = {
                                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        val newDateStr = try {
                                            val d = sdf.parse(prog.dataAgendada)
                                            val cal = Calendar.getInstance()
                                            cal.time = d ?: Date()
                                            cal.add(Calendar.YEAR, 1)
                                            sdf.format(cal.time)
                                        } catch (e: Exception) {
                                            val cal = Calendar.getInstance()
                                            cal.add(Calendar.YEAR, 1)
                                            sdf.format(cal.time)
                                        }
                                        
                                        val resetSerialized = resetEquipamentosSerialized(prog.equipamentosSerialized)
                                        
                                        val renewedProg = prog.copy(
                                            id = com.priorizedev.brizza.data.model.generateRandomId(),
                                            dataAgendada = newDateStr,
                                            status = "Ativo",
                                            ativo = true,
                                            equipamentosSerialized = resetSerialized
                                        )
                                        viewModel.salvarProgramaPreventivo(renewedProg)
                                        
                                        activeProgramForDetails = null
                                        Toast.makeText(context, "Programa renovado com sucesso! Nova programação ativa criada com início em $newDateStr.", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Renovar Programação (+1 Ano)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Toggles: Alerts Preference Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "PREFERÊNCIAS DE ALERTA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 2.dp),
                                letterSpacing = 0.5.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Notificar Cliente", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Enviar e-mail para o responsável pelo cliente", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = prog.notificarCliente,
                                    onCheckedChange = { isChecked ->
                                        val updated = prog.copy(notificarCliente = isChecked)
                                        viewModel.salvarProgramaPreventivo(updated)
                                        activeProgramForDetails = updated
                                        Toast.makeText(context, "Notificação ao cliente atualizada!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Alerta Interno", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Notificar no painel/aplicativo da empresa", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = prog.notificarApp,
                                    onCheckedChange = { isChecked ->
                                        val updated = prog.copy(notificarApp = isChecked)
                                        viewModel.salvarProgramaPreventivo(updated)
                                        activeProgramForDetails = updated
                                        Toast.makeText(context, "Notificação interna atualizada!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }
                    }

                    // Secondary Control Actions: Active/Deactive and Cancel
                    if (!prog.cancelado && prog.status != "Concluído") {
                        Text(
                            text = "CONTROLES DO PROGRAMA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (prog.ativo) {
                                Button(
                                    onClick = {
                                        val updatedProg = prog.copy(ativo = false)
                                        viewModel.salvarProgramaPreventivo(updatedProg)
                                        
                                        activeProgramForDetails = updatedProg
                                        Toast.makeText(context, "Programa Preventivo desativado temporariamente.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pausar / Inativar", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val updatedProg = prog.copy(ativo = true)
                                        viewModel.salvarProgramaPreventivo(updatedProg)
                                        
                                        activeProgramForDetails = updatedProg
                                        Toast.makeText(context, "Programa Preventivo reativado com sucesso!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reativar", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    val updatedProg = prog.copy(cancelado = true, ativo = false)
                                    viewModel.salvarProgramaPreventivo(updatedProg)

                                    activeProgramForDetails = updatedProg
                                    Toast.makeText(context, "Programa Preventivo cancelado permanentemente.", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancelar", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRenewDialog && programToRenew != null) {
        val prog = programToRenew!!
        AlertDialog(
            onDismissRequest = { 
                showRenewDialog = false
                programToRenew = null
            },
            title = {
                Text(
                    "Renovar Preventiva?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    "Deseja renovar este programa de preventivas por mais 1 ano?\n\nSerá criada uma nova programação com início em um ano, com os mesmos aparelhos e frequência.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 1. Conclude parent program
                        val updatedProg = prog.copy(
                            status = "Concluído",
                            ativo = false
                        )
                        viewModel.salvarProgramaPreventivo(updatedProg)
                        
                        // 2. Generate OS as Concluída
                        val parsedEquips = parseEquipamentosList(prog.equipamentosSerialized)
                        parsedEquips.forEach { itemTriple ->
                            val eqId = itemTriple.first
                            val dateVal = itemTriple.third
                            val eqItem = equipamentos.find { it.id == eqId }
                            val envName = eqItem?.let { item -> ambientes.find { it.id == item.ambienteId }?.nome } ?: "Geral"
                            val newOrdemId = com.priorizedev.brizza.data.model.generateOrdemServicoId()
                            
                            val automaticOS = OrdemServico(
                                id = newOrdemId,
                                clienteId = prog.clienteId,
                                tecnicoId = prog.tecnicoId,
                                equipamentoId = eqId,
                                tipoServico = "Preventiva",
                                dataChamado = dateVal,
                                dataAgendada = dateVal,
                                descricao = "Visita técnica preventiva realizada com sucesso. Equipamento: ${eqItem?.marca ?: ""} ${eqItem?.modelo ?: ""}. Setor: $envName. Visita agendada para o dia ${prog.dataAgendada}.",
                                status = "Concluída",
                                prioridade = "Média",
                                descricaoResumida = "Prev. ${prog.periodo}",
                                usuarioEmail = prog.usuarioEmail
                            )
                            viewModel.salvarOrdemServico(automaticOS)
                        }
                        
                        // 3. Renew for +1 year
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val newDateStr = try {
                            val d = sdf.parse(prog.dataAgendada)
                            val cal = Calendar.getInstance()
                            cal.time = d ?: Date()
                            cal.add(Calendar.YEAR, 1)
                            sdf.format(cal.time)
                        } catch (e: Exception) {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.YEAR, 1)
                            sdf.format(cal.time)
                        }
                        
                        val resetSerialized = resetEquipamentosSerialized(prog.equipamentosSerialized)
                        
                        val renewedProg = prog.copy(
                            id = com.priorizedev.brizza.data.model.generateRandomId(),
                            dataAgendada = newDateStr,
                            status = "Ativo",
                            ativo = true,
                            equipamentosSerialized = resetSerialized
                        )
                        viewModel.salvarProgramaPreventivo(renewedProg)
                        
                        showRenewDialog = false
                        programToRenew = null
                        activeProgramForDetails = null
                        Toast.makeText(context, "Programa concluído e renovado por mais 1 ano com sucesso!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Sim, Renovar (+1 Ano)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            // Conclude without renewing
                            val updatedProg = prog.copy(
                                status = "Concluído",
                                ativo = false
                            )
                            viewModel.salvarProgramaPreventivo(updatedProg)
                            
                            val parsedEquips = parseEquipamentosList(prog.equipamentosSerialized)
                            parsedEquips.forEach { itemTriple ->
                                val eqId = itemTriple.first
                                val dateVal = itemTriple.third
                                val eqItem = equipamentos.find { it.id == eqId }
                                val envName = eqItem?.let { item -> ambientes.find { it.id == item.ambienteId }?.nome } ?: "Geral"
                                val newOrdemId = com.priorizedev.brizza.data.model.generateOrdemServicoId()
                                
                                val automaticOS = OrdemServico(
                                    id = newOrdemId,
                                    clienteId = prog.clienteId,
                                    tecnicoId = prog.tecnicoId,
                                    equipamentoId = eqId,
                                    tipoServico = "Preventiva",
                                    dataChamado = dateVal,
                                    dataAgendada = dateVal,
                                    descricao = "Visita técnica preventiva realizada com sucesso. Equipamento: ${eqItem?.marca ?: ""} ${eqItem?.modelo ?: ""}. Setor: $envName. Visita agendada para o dia ${prog.dataAgendada}.",
                                    status = "Concluída",
                                    prioridade = "Média",
                                    descricaoResumida = "Prev. ${prog.periodo}",
                                    usuarioEmail = prog.usuarioEmail
                                )
                                viewModel.salvarOrdemServico(automaticOS)
                            }
                            
                            showRenewDialog = false
                            programToRenew = null
                            activeProgramForDetails = null
                            Toast.makeText(context, "Programa finalizado com sucesso!", Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Text("Não, Apenas Finalizar")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(
                        onClick = {
                            showRenewDialog = false
                            programToRenew = null
                        }
                    ) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            }
        )
    }
}
