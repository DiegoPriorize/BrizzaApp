package com.priorizedev.brizza.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.lazy.rememberLazyListState
import com.priorizedev.brizza.data.model.Ambiente
import com.priorizedev.brizza.data.model.Equipamento
import com.priorizedev.brizza.ui.components.EquipamentoDetalhesDialog
import com.priorizedev.brizza.ui.viewmodel.ClimaGestViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbienteDetalhesScreen(
    ambienteId: String,
    viewModel: ClimaGestViewModel,
    onBack: () -> Unit,
    onNavigateToEquipamentoDetails: (String) -> Unit
) {
    val context = LocalContext.current
    val ambienteState = viewModel.getAmbienteFlow(ambienteId).collectAsStateWithLifecycle(initialValue = null)
    val equipamentos by viewModel.getEquipamentosPorAmbienteFlow(ambienteId).collectAsStateWithLifecycle(initialValue = emptyList())
    val databaseMarcas by viewModel.marcasEquipamentosState.collectAsStateWithLifecycle()

    var showAddEquipamentoDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditAmbienteDialog by remember { mutableStateOf(false) }

    val ambiente = ambienteState.value ?: return

    if (showEditAmbienteDialog) {
        var nomeAmb by remember { mutableStateOf(ambiente.nome) }
        var areaAmb by remember { mutableStateOf(ambiente.areaM2.toString()) }
        var cargaAmb by remember { mutableStateOf(ambiente.cargaTermicaBtu.toString()) }
        
        var fixos by remember { mutableIntStateOf(ambiente.fixos) }
        var flutuantes by remember { mutableIntStateOf(ambiente.flutuantes) }
        var janelas by remember { mutableIntStateOf(ambiente.janelas) }
        var portas by remember { mutableIntStateOf(ambiente.portas) }
        var fontesCalor by remember { mutableIntStateOf(ambiente.fontesCalor) }
        var incidenciaSolar by remember { mutableStateOf(ambiente.incidenciaSolar) }

        var diferenteEndereco by remember { mutableStateOf(ambiente.endereco.isNotBlank()) }
        var endInput by remember { mutableStateOf(ambiente.endereco) }

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
                            text = "Editar Ambiente",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { showEditAmbienteDialog = false }) {
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
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                            onClick = { showEditAmbienteDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (nomeAmb.isBlank()) {
                                    showNameError = true
                                    Toast.makeText(context, "Nome do ambiente não pode ser vazio!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val updated = ambiente.copy(
                                        nome = nomeAmb.trim(),
                                        areaM2 = areaAmb.replace(',', '.').toDoubleOrNull() ?: ambiente.areaM2,
                                        cargaTermicaBtu = cargaAmb.toIntOrNull() ?: ambiente.cargaTermicaBtu,
                                        fixos = fixos,
                                        flutuantes = flutuantes,
                                        janelas = janelas,
                                        portas = portas,
                                        fontesCalor = fontesCalor,
                                        incidenciaSolar = incidenciaSolar,
                                        endereco = if (diferenteEndereco) endInput.trim() else ""
                                    )
                                    viewModel.salvarAmbiente(updated)
                                    showEditAmbienteDialog = false
                                    Toast.makeText(context, "Ambiente atualizado!", Toast.LENGTH_SHORT).show()
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
                                modifier = Modifier.size(18.dp)
                            )
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
                        text = "Detalhes do Ambiente",
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
                                contentDescription = "Menu",
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
                                text = { Text("Adicionar Equipamento") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    showAddEquipamentoDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Editar Ambiente") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    showEditAmbienteDialog = true
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card with large name, area and heat load
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = ambiente.nome,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Área Total: ${ambiente.areaM2} m²",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Carga Térmica: ${ambiente.cargaTermicaBtu} BTU/h",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (ambiente.endereco.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Endereço",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Endereço: ${ambiente.endereco}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Grid of Attributes
            item {
                Text(
                    text = "CARACTERÍSTICAS TÉRMICAS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // row 1: Ocupantes (Fixos e Flutuantes)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            AttributeCell(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Groups,
                                label = "Fixos",
                                value = "${ambiente.fixos}"
                            )
                            AttributeCell(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Person,
                                label = "Flutuantes",
                                value = "${ambiente.flutuantes}"
                            )
                        }
                        
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        
                        // row 2: Janelas e Portas
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            AttributeCell(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Apps,
                                label = "Janelas",
                                value = "${ambiente.janelas}"
                            )
                            AttributeCell(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.MeetingRoom,
                                label = "Portas",
                                value = "${ambiente.portas}"
                            )
                        }
                        
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        
                        // row 3: Fontes de calor & Incidencia Solar
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            AttributeCell(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Whatshot,
                                label = "Fontes de Calor",
                                value = "${ambiente.fontesCalor}"
                            )
                            AttributeCell(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.WbSunny,
                                label = "Incidência Solar",
                                value = ambiente.incidenciaSolar
                            )
                        }
                    }
                }
            }

            // AC Equipment Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MAQUINAS NO AMBIENTE (${equipamentos.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Button(
                        onClick = { showAddEquipamentoDialog = true },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adicionar", fontSize = 12.sp)
                    }
                }
            }

            if (equipamentos.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhum equipamento cadastrado neste ambiente.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                items(equipamentos) { equipamento ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToEquipamentoDetails(equipamento.id) },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
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
                                        Text(
                                            text = equipamento.tag,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "${equipamento.marca} (${equipamento.tipo})",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("S/N: ${equipamento.numeroSerie}", fontSize = 12.sp, color = Color.Gray)
                                Text("Carga: ${equipamento.capacidadeBtu} BTU/h | Gás: ${equipamento.fluidoRefrigerante}", fontSize = 12.sp, color = Color.Gray)
                            }
                            
                            // Interactive status badge
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
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



        if (showAddEquipamentoDialog) {
            val nextIndex = equipamentos.size + 1
            var tag by remember { mutableStateOf("AC-${nextIndex.toString().padStart(2, '0')}") }
            var tipo by remember { mutableStateOf("Split") }
            var marca by remember { mutableStateOf("") }
            var modelo by remember { mutableStateOf("") }
            var capacidade by remember { mutableStateOf("12000") }
            var dataInstalacao by remember { mutableStateOf("") }
            var serial by remember { mutableStateOf("") }
            var observacoes by remember { mutableStateOf("") }
            var status by remember { mutableStateOf("Ativo") }

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
            
            // Tecnicas
            var potenciaW by remember { mutableStateOf("") }
            var tensao by remember { mutableStateOf("220V") }
            var sistema by remember { mutableStateOf("Inverter") }
            var fluido by remember { mutableStateOf("R-410A") }
            var ciclo by remember { mutableStateOf("Frio") }

            // Dropdowns states
            var tipoDropdownExpanded by remember { mutableStateOf(false) }
            var marcaDropdownExpanded by remember { mutableStateOf(false) }
            var capacidadeDropdownExpanded by remember { mutableStateOf(false) }
            var statusDropdownExpanded by remember { mutableStateOf(false) }

            var tagError by remember { mutableStateOf(false) }

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
                                            label = { Text("Tipo de Equipamento", fontSize = 11.sp) },
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

                                // Field Marca and button below
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
                                                        text = { Text("Sem marcas correspondentes") },
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
                                            label = { Text("Capacidade Térmica (BTU/h)", fontSize = 11.sp) },
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
                                item {
                                    OutlinedTextField(
                                        value = ambiente.nome,
                                        onValueChange = {},
                                        label = { Text("Ambiente Vinculado", fontSize = 11.sp) },
                                        readOnly = true,
                                        enabled = false,
                                        textStyle = fieldTextStyle,
                                        colors = borderColors,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp)
                                    )
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
                                        if (tag.isBlank()) {
                                            tagError = true
                                            Toast.makeText(context, "⚠️ A TAG do equipamento é obrigatória!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.salvarEquipamento(
                                                Equipamento(
                                                    clienteId = ambiente.clienteId,
                                                    ambienteId = ambiente.id,
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

            if (showNewBrandInput) {
                Dialog(onDismissRequest = { showNewBrandInput = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = "Label Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            
                            Text(
                                text = "Adicionar Nova Marca",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Text(
                                text = "Cadastre uma nova marca personalizada para os seus equipamentos. Ela ficará salva localmente na sua conta de usuário.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            OutlinedTextField(
                                value = novaMarcaInput,
                                onValueChange = { novaMarcaInput = it },
                                label = { Text("Nome da Marca*") },
                                placeholder = { Text("Ex: Daitsu, Electrolux") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { 
                                        showNewBrandInput = false
                                        novaMarcaInput = ""
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancelar")
                                }
                                
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
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Text("Adicionar Marca", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Excluir Ambiente") },
            text = { Text("Tem certeza que deseja excluir o ambiente \"${ambiente.nome}\"? Esta ação é definitiva e removerá este ambiente do sistema.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.excluirAmbiente(ambiente)
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

@Composable
fun AttributeCell(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}
