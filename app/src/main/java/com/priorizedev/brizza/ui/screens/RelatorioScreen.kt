package com.priorizedev.brizza.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.priorizedev.brizza.data.model.Cliente
import com.priorizedev.brizza.data.model.OrdemServico
import com.priorizedev.brizza.data.model.Tecnico
import com.priorizedev.brizza.ui.viewmodel.ClimaGestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioScreen(
    viewModel: ClimaGestViewModel,
    onBack: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val clientes by viewModel.clientesState.collectAsStateWithLifecycle()
    val tecnicos by viewModel.tecnicosState.collectAsStateWithLifecycle()
    val ordens by viewModel.ordensServicoState.collectAsStateWithLifecycle()
    val equipamentos by viewModel.equipamentosState.collectAsStateWithLifecycle()
    val ambientes by viewModel.ambientesState.collectAsStateWithLifecycle()

    // Key Filters
    var selectedStatus by remember { mutableStateOf("Todos") }
    var selectedTipo by remember { mutableStateOf("Todos") }
    var selectedClienteId by remember { mutableStateOf("Todos") }
    var selectedTecnicoId by remember { mutableStateOf("Todos") }
    var searchQuery by remember { mutableStateOf("") }
    var filtersExpanded by remember { mutableStateOf(false) }

    // Dropdowns Expanded Statuses
    var showStatusDropdown by remember { mutableStateOf(false) }
    var showTipoDropdown by remember { mutableStateOf(false) }
    var showClienteDropdown by remember { mutableStateOf(false) }
    var showTecnicoDropdown by remember { mutableStateOf(false) }

    // Filtering logic
    val filteredOrdens = remember(ordens, selectedStatus, selectedTipo, selectedClienteId, selectedTecnicoId, searchQuery) {
        ordens.filter { ordem ->
            val matchesStatus = selectedStatus == "Todos" || 
                    (selectedStatus == "Pendente" && (ordem.status == "Pendente" || ordem.status == "Em Andamento" || ordem.status == "Em andamento")) ||
                    ordem.status == selectedStatus
            val matchesTipo = selectedTipo == "Todos" || ordem.tipoServico == selectedTipo
            val matchesCliente = selectedClienteId == "Todos" || ordem.clienteId == selectedClienteId
            val matchesTecnico = selectedTecnicoId == "Todos" || ordem.tecnicoId == selectedTecnicoId
            
            val clienteName = clientes.find { it.id == ordem.clienteId }?.nome ?: ""
            val tecnicoName = tecnicos.find { it.id == ordem.tecnicoId }?.nome ?: ""
            val matchesSearch = searchQuery.isBlank() || 
                    ordem.numeroOrdem.contains(searchQuery, ignoreCase = true) ||
                    clienteName.contains(searchQuery, ignoreCase = true) ||
                    tecnicoName.contains(searchQuery, ignoreCase = true) ||
                    ordem.descricao.contains(searchQuery, ignoreCase = true)

            matchesStatus && matchesTipo && matchesCliente && matchesTecnico && matchesSearch
        }
    }

    // Advanced Metrics calculations based on filtered set
    val totalRevenue = remember(filteredOrdens) { filteredOrdens.filter { it.status == "Concluída" || it.status == "Concluído" }.sumOf { it.valorServico } }
    val openCount = remember(filteredOrdens) { filteredOrdens.count { it.status == "Aberta" } }
    val inProgressCount = remember(filteredOrdens) { filteredOrdens.count { it.status == "Pendente" || it.status == "Em Andamento" || it.status == "Em andamento" } }
    val concludedCount = remember(filteredOrdens) { filteredOrdens.count { it.status == "Concluída" || it.status == "Concluído" } }
    val cancelledCount = remember(filteredOrdens) { filteredOrdens.count { it.status == "Cancelada" } }
    val totalFilteredCount = filteredOrdens.size

    val averageTicket = remember(totalRevenue, concludedCount) { 
        if (concludedCount == 0) 0.0 else totalRevenue / concludedCount 
    }

    // Status breakdown list helper
    val statusBreakdown = remember(filteredOrdens) {
        val total = filteredOrdens.size
        listOf("Aberta", "Pendente", "Concluída", "Cancelada").map { status ->
            val count = filteredOrdens.count { 
                it.status.lowercase() == status.lowercase() || 
                (status == "Pendente" && (it.status == "Em Andamento" || it.status == "Em andamento")) ||
                (status == "Concluída" && (it.status == "Concluído" || it.status == "concluido"))
            }
            val percentage = if (total == 0) 0f else count.toFloat() / total
            val color = when (status) {
                "Aberta" -> Color(0xFFD97706)
                "Pendente" -> Color(0xFF2563EB)
                "Concluída" -> Color(0xFF059669)
                "Cancelada" -> Color(0xFFDC2626)
                else -> Color.Gray
            }
            Triple(status, count, Pair(percentage, color))
        }
    }

    // Tipo breakdown list helper
    val tipoBreakdown = remember(filteredOrdens) {
        val total = filteredOrdens.size
        listOf("Preventiva", "Corretiva", "Instalação", "Orçamento", "Limpeza", "Higienização").map { tipo ->
            val count = filteredOrdens.count { it.tipoServico == tipo }
            val percentage = if (total == 0) 0f else count.toFloat() / total
            val color = when (tipo) {
                "Preventiva" -> Color(0xFF4F46E5)
                "Corretiva" -> Color(0xFFE11D48)
                "Instalação" -> Color(0xFF0D9488)
                "Orçamento" -> Color(0xFF7C3AED)
                "Limpeza" -> Color(0xFF0891B2)
                "Higienização" -> Color(0xFF0284C7)
                else -> Color.Gray
            }
            Triple(tipo, count, Pair(percentage, color))
        }
    }

    // Clients breakdown list helper (Top 5)
    val clientBreakdown = remember(filteredOrdens, clientes) {
        filteredOrdens.groupBy { it.clienteId }
            .map { (clienteId, list) ->
                val name = clientes.find { it.id == clienteId }?.nome ?: "Desconhecido"
                val count = list.size
                val totalValue = list.sumOf { it.valorServico }
                Pair(name, Pair(count, totalValue))
            }
            .sortedByDescending { it.second.first }
            .take(5)
    }

    // Technicians breakdown list helper (Top 5)
    val tecnicoBreakdown = remember(filteredOrdens, tecnicos) {
        filteredOrdens.groupBy { it.tecnicoId }
            .map { (tecnicoId, list) ->
                val name = tecnicos.find { it.id == tecnicoId }?.nome ?: "Desconhecido"
                val count = list.size
                val completed = list.count { it.status == "Concluída" }
                Pair(name, Pair(count, completed))
            }
            .sortedByDescending { it.second.first }
            .take(5)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Indicadores",
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
                    TextButton(
                        onClick = onNavigateToDashboard,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Dashboard",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
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
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // SECTION 1: FILTER MANAGER CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { filtersExpanded = !filtersExpanded }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FILTRAR INDICADORES",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                // Active filters badge
                                val activeCount = listOf(
                                    selectedStatus != "Todos",
                                    selectedTipo != "Todos",
                                    selectedClienteId != "Todos",
                                    selectedTecnicoId != "Todos",
                                    searchQuery.isNotBlank()
                                ).count { it }
                                if (activeCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50))
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = activeCount.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = if (filtersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (filtersExpanded) "Recolher filtros" else "Expandir filtros",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(
                            visible = filtersExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))

                                // Text Search Query Field
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Filtrar por O.S., Cliente ou Técnico...", fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Filters Grid Row 1
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Status selector
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { showStatusDropdown = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = "Status: $selectedStatus",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showStatusDropdown,
                                            onDismissRequest = { showStatusDropdown = false }
                                        ) {
                                            val statuses = listOf("Todos", "Aberta", "Pendente", "Concluída", "Cancelada")
                                            statuses.forEach { st ->
                                                DropdownMenuItem(
                                                    text = { Text(st) },
                                                    onClick = {
                                                        selectedStatus = st
                                                        showStatusDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Type selector
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { showTipoDropdown = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = "Tipo: $selectedTipo",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showTipoDropdown,
                                            onDismissRequest = { showTipoDropdown = false }
                                        ) {
                                            val tipos = listOf("Todos", "Preventiva", "Corretiva", "Instalação", "Orçamento", "Limpeza", "Higienização")
                                            tipos.forEach { tp ->
                                                DropdownMenuItem(
                                                    text = { Text(tp) },
                                                    onClick = {
                                                        selectedTipo = tp
                                                        showTipoDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Filters Grid Row 2
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Cliente selector
                                    Box(modifier = Modifier.weight(1f)) {
                                        val currentClientName = clientes.find { it.id == selectedClienteId }?.nome ?: "Todos"
                                        OutlinedButton(
                                            onClick = { showClienteDropdown = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = "Cliente: $currentClientName",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showClienteDropdown,
                                            onDismissRequest = { showClienteDropdown = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Todos") },
                                                onClick = {
                                                    selectedClienteId = "Todos"
                                                    showClienteDropdown = false
                                                }
                                            )
                                            clientes.forEach { cl ->
                                                DropdownMenuItem(
                                                    text = { Text(cl.nome) },
                                                    onClick = {
                                                        selectedClienteId = cl.id
                                                        showClienteDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Técnico selector
                                    Box(modifier = Modifier.weight(1f)) {
                                        val currentTecnicoName = tecnicos.find { it.id == selectedTecnicoId }?.nome ?: "Todos"
                                        OutlinedButton(
                                            onClick = { showTecnicoDropdown = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = "Técnico: $currentTecnicoName",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showTecnicoDropdown,
                                            onDismissRequest = { showTecnicoDropdown = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Todos") },
                                                onClick = {
                                                    selectedTecnicoId = "Todos"
                                                    showTecnicoDropdown = false
                                                }
                                            )
                                            tecnicos.forEach { tc ->
                                                DropdownMenuItem(
                                                    text = { Text(tc.nome) },
                                                    onClick = {
                                                        selectedTecnicoId = tc.id
                                                        showTecnicoDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Clear filter pill
                                if (selectedStatus != "Todos" || selectedTipo != "Todos" || selectedClienteId != "Todos" || selectedTecnicoId != "Todos" || searchQuery.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Limpar Filtros",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .clickable {
                                                selectedStatus = "Todos"
                                                selectedTipo = "Todos"
                                                selectedClienteId = "Todos"
                                                selectedTecnicoId = "Todos"
                                                searchQuery = ""
                                            }
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: CONSOLIDATED FINANCIAL BOARD
            item {
                Text(
                    text = "DESEMPENHO FINANCEIRO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Total revenue card
                    Card(
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Faturamento Concluído",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("R$ %.2f", totalRevenue),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Faturamento real das O.S. com status de Concluída.",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                lineHeight = 11.sp
                            )
                        }
                    }

                    // Ticket medio card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Ticket Médio",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("R$ %.0f", averageTicket),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Valor médio por O.S.",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // SECTION 3: SYSTEM METRICS BOARD (Totals summary)
            item {
                Text(
                    text = "SUMÁRIO DOS ATIVOS CADASTRADOS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

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
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Clientes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(clientes.size.toString(), fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ambientes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(ambientes.size.toString(), fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Equipamentos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(equipamentos.size.toString(), fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Técnicos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(tecnicos.size.toString(), fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // SECTION 4: STATUS DISTRIBUTION BAR METER
            item {
                Text(
                    text = "DADOS ANALÍTICOS DE ATENDIMENTO - O.S.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Distribuição por Status",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Total de O.S.: $totalFilteredCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        statusBreakdown.forEach { (status, count, pctColor) ->
                            val (percentage, color) = pctColor
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(color)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(status, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Text(
                                        text = "$count (${String.format("%.1f", percentage * 100)}%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { percentage },
                                    color = color,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            // SECTION 5: TYPES SERVICES DISTRIBUTION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ordem de Serviço por Tipo de Trabalho",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        tipoBreakdown.forEach { (tipo, count, pctColor) ->
                            val (percentage, color) = pctColor
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(color)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tipo, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Text(
                                        text = "$count (${String.format("%.1f", percentage * 100)}%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { percentage },
                                    color = color,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            // SECTION 6: TOP CLIENTS VOLUMES (NUMERICS ONLY)
            item {
                Text(
                    text = "RANQUEAMENTO DOS ATIVOS (TOP 5)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Maiores Clientes por Volume de O.S.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (clientBreakdown.isEmpty()) {
                            Text(
                                text = "Nenhum cliente com O.S. nos filtros selecionados.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            clientBreakdown.forEachIndexed { index, (name, stats) ->
                                val (count, valTotal) = stats
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.5f)) {
                                        Text(
                                            text = "${index + 1}°",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.width(24.dp)
                                        )
                                        Text(
                                            text = name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "$count O.S.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = String.format("R$ %.0f", valTotal),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (index < clientBreakdown.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 7: STAFF PERFORMANCE (NUMERICS ONLY)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Produtividade por Equipe Técnica",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (tecnicoBreakdown.isEmpty()) {
                            Text(
                                text = "Nenhum técnico com O.S. nos filtros selecionados.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            // Table Header Row with subtle background style
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pos.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(0.15f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Técnico",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(0.55f)
                                )
                                Text(
                                    text = "Concluídas",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(0.30f),
                                    textAlign = TextAlign.End
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            tecnicoBreakdown.forEachIndexed { index, (name, stats) ->
                                val (total, completed) = stats
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}°",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(0.15f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(0.55f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = completed.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669),
                                        modifier = Modifier.weight(0.30f),
                                        textAlign = TextAlign.End
                                    )
                                }
                                if (index < tecnicoBreakdown.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
