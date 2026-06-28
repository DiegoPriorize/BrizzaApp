package com.priorizedev.brizza.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.priorizedev.brizza.data.model.Cliente
import com.priorizedev.brizza.data.model.PmocReport
import com.priorizedev.brizza.data.model.PmocLogbookEntry
import com.priorizedev.brizza.data.model.Tecnico
import com.priorizedev.brizza.ui.components.SignaturePad
import com.priorizedev.brizza.ui.viewmodel.ClimaGestViewModel
import com.priorizedev.brizza.util.ReportUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PmocScreen(
    viewModel: ClimaGestViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onBack: () -> Unit
) {
    val pmocs by viewModel.pmocReportsState.collectAsStateWithLifecycle()
    val clientes by viewModel.clientesState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PMOC Planos de Controle",
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
            if (clientes.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onNavigateToCreate,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Gerar Novo PMOC")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = "O PMOC (Plano de Manutenção, Operação e Controle) é obrigatório por lei para estabelecimentos comerciais com climatização. Abaixo, acompanhe os relatórios emitidos.",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (pmocs.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(68.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Nenhum relatório PMOC gerado.",
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    if (clientes.isEmpty()) {
                        Text(
                            text = "Cadastre um cliente primeiro para operar a central PMOC.",
                            fontSize = 12.sp,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                    } else {
                        Text(
                            text = "Toque no botão '+' para gerar un plano mensal.",
                            fontSize = 12.sp,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pmocs) { pmoc ->
                        val clientName = clientes.find { it.id == pmoc.clienteId }?.nome ?: "Cliente Excluído"
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { onNavigateToDetails(pmoc.id) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pmoc.numeroPmoc,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Cliente: $clientName", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Mês de Ref: ${pmoc.mesRef}", fontSize = 12.sp, color = Color.Gray)
                                    Text("Data de Emissão: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(pmoc.dataGeracao))}", fontSize = 11.sp, color = Color.Gray)
                                }
                                
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Ver Detalhes",
                                    tint = MaterialTheme.colorScheme.primary
                                )
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
fun PmocCreateWizard(
    viewModel: ClimaGestViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clientes by viewModel.clientesState.collectAsStateWithLifecycle()
    val tecnicos by viewModel.tecnicosState.collectAsStateWithLifecycle()

    var step by remember { mutableIntStateOf(1) }

    // Step 1: Base info & dynamic equipment selection
    var selectedCliente by remember { mutableStateOf<Cliente?>(null) }
    var dropdownCliExpanded by remember { mutableStateOf(false) }
    var selectedTecnico by remember { mutableStateOf<Tecnico?>(null) }
    var dropdownTecExpanded by remember { mutableStateOf(false) }
    var mesRef by remember { mutableStateOf("") }
    var numeroPmoc by remember { mutableStateOf("") }

    val clientEquipments = remember(selectedCliente, viewModel.equipamentosState.collectAsStateWithLifecycle().value) {
        if (selectedCliente != null) {
            viewModel.equipamentosState.value.filter { it.clienteId == selectedCliente!!.id }
        } else {
            emptyList()
        }
    }

    val selectedEquipmentsSet = remember { mutableStateOf<Set<String>>(emptySet()) }

    // Auto-select all equipments by default on client change
    LaunchedEffect(selectedCliente) {
        if (selectedCliente != null) {
            val equips = viewModel.equipamentosState.value.filter { it.clienteId == selectedCliente!!.id }
            selectedEquipmentsSet.value = equips.map { it.id }.toSet()
        } else {
            selectedEquipmentsSet.value = emptySet()
        }
    }

    // Environments reactive list
    val ambientesListState = remember(selectedCliente) {
        if (selectedCliente != null) {
            viewModel.getAmbientesPorClienteFlow(selectedCliente!!.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // Step 2: Routines & Periodicities
    var newRoutineTitle by remember { mutableStateOf("") }
    var newRoutinePeriodicity by remember { mutableStateOf("Mensal") }
    var dropdownNewRoutinePeriodExpanded by remember { mutableStateOf(false) }
    var periodicityMenuExpandedIndex by remember { mutableStateOf<Int?>(null) }

    val routinesList = remember {
        mutableStateListOf(
            Pair("Limpeza e inspeção dos filtros de ar", "Mensal"),
            Pair("Limpeza da bandeja de condensado", "Mensal"),
            Pair("Limpeza das aletas e serpentina do evaporador", "Mensal"),
            Pair("Verificação e limpeza do dreno de condensado", "Mensal"),
            Pair("Inspeção do motor e turbina do ventilador", "Trimestral"),
            Pair("Aplicação de biocida / higienizador", "Trimestral"),
            Pair("Verificação elétrica da unidade interna", "Semestral"),
            Pair("Limpeza das aletas e serpentina do condensador", "Trimestral"),
            Pair("Inspeção do motor e hélice do ventilador", "Trimestral"),
            Pair("Verificação de vazamento de gás refrigerante", "Trimestral"),
            Pair("Medição de pressão do ciclo frigorífico", "Semestral"),
            Pair("Verificação e aperto das conexões elétricas", "Semestral"),
            Pair("Lubrificação de partes móveis", "Semestral"),
            Pair("Teste funcional geral (modo frio, quente, ventilação, timer e controle)", "Trimestral"),
            Pair("Verificação da fixação e vedação da unidade", "Semestral"),
            Pair("Recarga de gás refrigerante", "Sob ocorrência")
        )
    }

    // Step 3: Operational Procedures (Editable title and description)
    val opsList = remember {
        mutableStateListOf(
            Pair("1. Acionamento e desligamento", "O sistema de climatização deve ser acionado mediante o controle remoto ou painel de operação do equipamento, observando as seguintes orientações:\n\n- Acionar o equipamento somente quando o ambiente estiver ocupado ou com antecedência máxima de 30 minutos antes da ocupação.\n- Desligar o equipamento ao final do expediente ou quando o ambiente permanecer desocupado por período superior a [X] horas.\n- Aguardar no mínimo 3 (três) minutos entre o desligamento e um novo acionamento, a fim de proteger o compressor.\n- Em caso de queda de energia, aguardar o restabelecimento e somente religar após intervalo mínimo de 3 minutos."),
            Pair("2. Setpoint e faixas de operação", "A operação do sistema deve respeitar as faixas de temperatura e umidade estabelecidas pela NBR 16.401-2 e recomendadas pelo Ministério da Saúde:\n\n- Temperatura de bulbo seco: entre 23°C e 26°C no período de verão; entre 21°C e 23°C no período de inverno (quando aplicável).\n- Umidade relativa do ar: entre 40% e 65%.\n- Renovação de ar: garantir entrada mínima de ar externo conforme especificado no projeto do sistema.\n\nÉ vedado o ajuste de temperatura abaixo de 20°C ou acima de 28°C, salvo orientação expressa do responsável técnico."),
            Pair("3. Ventilação e renovação de ar", "Para garantir a qualidade do ar interno e o correto funcionamento do sistema:\n\n- Manter portas e janelas fechadas durante o funcionamento do equipamento de climatização.\n- Não obstruir as grelhas de insuflamento, retorno de ar ou tomadas de ar externo com móveis, equipamentos ou objetos.\n- Verificar periodicamente se as entradas de ar externo estão desobstruídas e limpas.\n- Em ambientes com alta ocupação, garantir que a renovação de ar mínima prevista em projeto esteja sendo cumprida."),
            Pair("4. Responsabilidades de operação", "A operação cotidiana do sistema de climatização é de responsabilidade de:\n\n- Operador(es) habilitado(s): [Nome(s) do(s) operador(es)]\n- Cargo/função: [Cargo]\n- Contato: [Telefone / e-mail]\n\nQualquer anormalidade observada durante a operação — ruídos, odores, vazamentos ou falhas — deve ser comunicada imediatamente ao responsável técnico antes de qualquer tentativa de intervention.\n\n- Responsável técnico (RT): [Nome do RT]\n- Registro: [CREA/CFT nº]\n- Contato em horário comercial: [Telefone]"),
            Pair("5. Programação de horários", "O sistema de climatização deve operar dentro dos seguintes horários, ajustados conforme a ocupação do estabelecimento:\n\n- Dias úteis: [Horário de início] às [Horário de término]\n- Sábados: [Horário de início] às [Horário de término] / Não opera\n- Domingos e feriados: [Horário de início] às [Horário de término] / Não opera\n\nQuando disponível, utilizar o temporizador (timer) do próprio equipamento para automatizar o desligamento ao final do expediente. Alterações na programação devem ser comunicadas ao RT."),
            Pair("6. Registro de funcionamento", "O operador responsável deve registrar mensalmente as seguintes informações no logbook do PMOC:\n\n- Temperatura medida no ambiente (bulbo seco) e comparação com o setpoint programado.\n- Ocorrências de desligamento automático, alarmes ou falhas observadas.\n- Qualquer alteração nas condições de operação (mudança de layout, aumento de ocupação, obras no local).\n- Data e nome do operador que realizou o registro.\n\nO registro deve ser mantido disponível no local de instalação para fins de fiscalização pela Vigilância Sanitária, conforme exigência da Portaria MS nº 3.523/98 e Lei nº 13.589/2018.")
        )
    }

    // Step 4: Emergency Procedures (Editable title and description)
    val emsList = remember {
        mutableStateListOf(
            Pair("E1. Vazamento de água (condensado)", "Situação: Água gotejando da unidade interna, acúmulo no piso ou manchas de umidade próximas ao equipamento.\n\nAção imediata:\n\n- Desligar o equipamento pelo controle remoto ou pelo disjuntor dedicado.\n- Proteger equipamentos elétricos e documentos próximos ao vazamento.\n- Acionar imediatamente o responsável técnico: [Nome do RT] — [Telefone]\n- Não religar o equipamento até que a causa do vazamento seja identificada e corrigida pelo RT.\n- Registrar a ocorrência no logbook, informando data, hora e condições observadas."),
            Pair("E2. Odor, fumaça ou superaquecimento", "Situação: Cheiro de queimado, fumaça visível ou equipamento com temperatura superficial anormalmente elevada.\n\nAção imediata:\n\n- Desligar o equipamento imediatamente pelo disjuntor (não pelo controle remoto).\n- Se houver fumaça ou risco de incêndio, evacuar o ambiente e acionar o Corpo de Bombeiros: 193.\n- Não utilizar extintores de água em equipamentos elétricos energizados.\n- Acionar o responsável técnico: [Nome do RT] — [Telefone]\n- Não religar o equipamento sob nenhuma hipótese antes da inspeção técnica.\n- Registrar a ocorrência no logbook."),
            Pair("E3. Falha elétrica ou desligamento automático", "Situação: O equipamento desliga sozinho, exibe código de erro no display ou não responde ao acionamento.\n\nAção imediata:\n\n- Anotar o código de erro exibido no display (se houver).\n- Aguardar mínimo de 3 minutos e tentar um novo acionamento.\n- Se o equipamento desligar novamente ou não ligar, acionar o responsável técnico: [Nome do RT] — [Telefone]\n- Não forçar o funcionamento por meios alternativos (bypass de proteção, reset forçado).\n- Em caso de queda geral de energia, aguardar o restabelecimento e observar o intervalo mínimo de 3 minutos antes de religar.\n- Registrar a ocorrência no logbook."),
            Pair("E4. Suspeita de vazamento de gás refrigerante", "Situação: Cheiro adocicado ou levemente étereo próximo ao equipamento; formação de gelo na tubulação; perda de capacidade de refrigeração sem causa aparente.\n\nAção imediata:\n\n- Desligar o equipamento pelo disjuntor — não pelo controle remoto.\n- Ventilar o ambiente abrindo portas e janelas imediatamente.\n- Não acionar interruptores, tomadas ou qualquer equipamento elétrico no ambiente (risco de ignição).\n- Evacuar o ambiente e aguardar a ventilação completa.\n- Acionar com urgência o responsável técnico habilitado: [Nome do RT] — [Telefone]\n- Não religar o equipamento até inspeção, identificação e correção do vazamento por profissional habilitado com equipamento adequado (detector eletrônico).\n- Registrar a ocorrência no logbook.\n\nAtenção: a recarga de gás refrigerante só pode ser realizada por técnico habilitado com certificação junto ao IBAMA (Lei nº 12.187/2009)."),
            Pair("E5. Qualidade do ar comprometida", "Situação: Ocupantes do ambiente apresentam sintomas como tosse persistente, irritação nas vias aéreas, alergias, tontura ou mal-estar sem causa aparente; odor desagradável proveniente do equipamento.\n\nAção imediata:\n\n- Interromper o funcionamento do sistema de climatização.\n- Ventilar o ambiente naturalmente abrindo portas e janelas.\n- Remover os ocupantes que apresentem sintomas e, se necessário, acionar o SAMU: 192.\n- Acionar o responsável técnico para inspeção microbiológica urgente: [Nome do RT] — [Telefone]\n- Não religar o sistema antes de inspeção completa, limpeza e higienização por profissional habilitado.\n- Registrar todos os sintomas relatados, número de pessoas afetadas, data e hora no logbook.\n\nEsta situação pode caracterizar a \"Síndrome do Edifício Doente\", prevista na Portaria MS nº 3.523/98. A omissão pode gerar responsabilidade civil e sanitária ao proprietário ou gestor do imóvel."),
            Pair("E6. Contatos de emergência", "Os contatos abaixo devem estar afixados próximos ao painel de operação ou em local de fácil acesso a todos os ocupantes:\n\n- Responsável técnico (RT): [Nome do RT] — [Telefone]\n- Empresa de manutenção: [Nome da empresa] — [Telefone]\n- SAMU: 192\n- Corpo de Bombeiros: 193\n- Defesa Civil: 199\n- Vigilância Sanitária Municipal: [Telefone]")
        )
    }

    // Step 5: ANVISA items and Signatures
    var statusFiltro by remember { mutableStateOf("C") }
    var obsFiltro by remember { mutableStateOf("") }
    var statusSerp by remember { mutableStateOf("C") }
    var obsSerp by remember { mutableStateOf("") }
    var statusBand by remember { mutableStateOf("C") }
    var obsBand by remember { mutableStateOf("") }
    var statusMot by remember { mutableStateOf("C") }
    var obsMot by remember { mutableStateOf("") }
    var statusEle by remember { mutableStateOf("C") }
    var obsEle by remember { mutableStateOf("") }
    var statusGas by remember { mutableStateOf("C") }
    var obsGas by remember { mutableStateOf("") }
    var statusIsol by remember { mutableStateOf("C") }
    var obsIsol by remember { mutableStateOf("") }

    var sigTecbase64 by remember { mutableStateOf("") }
    var sigClibase64 by remember { mutableStateOf("") }

    // Auto generate PMOC doc number
    LaunchedEffect(selectedCliente) {
        if (selectedCliente != null) {
            val count = (1000..9999).random()
            val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
            numeroPmoc = "PMOC-$year-$count"
            mesRef = SimpleDateFormat("MMMM/yyyy", Locale("pt", "BR")).format(Date()).replaceFirstChar { it.uppercase() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Novo PMOC (Etapa $step/5)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step > 1) step-- else onBack()
                    }) {
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
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 0.dp
                )
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (step == 1) {
                    // STEP 1 UI: General Settings and Client Equipments Selection
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            // Header matching standard "cadastro de equipamentos"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    )
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color.White.copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                                    }
                                    Column {
                                        Text("Dados Gerais do PMOC", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                        Text("Dados básicos de identificação e escopo", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                    }
                                }
                            }

                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Cliente Selector Drop-Down
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { dropdownCliExpanded = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(selectedCliente?.nome ?: "Selecionar Cliente Proprietário*")
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = dropdownCliExpanded,
                                        onDismissRequest = { dropdownCliExpanded = false }
                                    ) {
                                        clientes.forEach { cli ->
                                            DropdownMenuItem(
                                                text = { Text(cli.nome) },
                                                onClick = {
                                                    selectedCliente = cli
                                                    dropdownCliExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Técnico Selector Drop-Down
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { dropdownTecExpanded = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(selectedTecnico?.nome ?: "Técnico Responsável do PMOC*")
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = dropdownTecExpanded,
                                        onDismissRequest = { dropdownTecExpanded = false }
                                    ) {
                                        if (tecnicos.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Sem técnicos cadastrados. Crie um perfil primeiro!") },
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

                                OutlinedTextField(
                                    value = mesRef,
                                    onValueChange = { mesRef = it },
                                    label = { Text("Mês de Referência*") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = numeroPmoc,
                                    onValueChange = { numeroPmoc = it },
                                    label = { Text("Número de Registro do PMOC*") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = obsFiltro,
                                    onValueChange = { obsFiltro = it },
                                    label = { Text("Número da ART ou TRT (Opcional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (selectedCliente != null) {
                        Text("Equipamentos Sob Controle (PMOC)*", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        // Single check for Select All / Deselect All
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val allSelected = clientEquipments.isNotEmpty() && selectedEquipmentsSet.value.size == clientEquipments.size
                                    selectedEquipmentsSet.value = if (allSelected) {
                                        emptySet()
                                    } else {
                                        clientEquipments.map { it.id }.toSet()
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val allSelected = clientEquipments.isNotEmpty() && selectedEquipmentsSet.value.size == clientEquipments.size
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { checked ->
                                    selectedEquipmentsSet.value = if (checked == true) {
                                        clientEquipments.map { it.id }.toSet()
                                    } else {
                                        emptySet()
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Marcar Todos", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }

                        if (selectedEquipmentsSet.value.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Text(
                                        "Preocupante: Nenhum equipamento selecionado para este plano de PMOC!",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (clientEquipments.isEmpty()) {
                            Text("Este cliente não possui equipamentos cadastrados.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        } else {
                            clientEquipments.forEach { eq ->
                                val ambientName = ambientesListState.value.firstOrNull { it.id == eq.ambienteId }?.nome ?: "Desconhecido"
                                val isChecked = selectedEquipmentsSet.value.contains(eq.id)
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedEquipmentsSet.value = if (isChecked) {
                                            selectedEquipmentsSet.value - eq.id
                                        } else {
                                            selectedEquipmentsSet.value + eq.id
                                        }
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isChecked) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                selectedEquipmentsSet.value = if (checked == true) {
                                                    selectedEquipmentsSet.value + eq.id
                                                } else {
                                                    selectedEquipmentsSet.value - eq.id
                                                }
                                            }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "TAG: ${eq.tag}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(text = "${eq.marca} ${eq.modelo} (${eq.tipo}) - ${eq.capacidadeBtu} BTUs", fontSize = 12.sp)
                                            Text(text = "Ambiente: $ambientName", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                } else if (step == 2) {
                    // STEP 2 UI: Active Routines list and dynamic editing with periodicity changes
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    )
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color.White.copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Build, contentDescription = null, tint = Color.White)
                                    }
                                    Column {
                                        Text("Rotinas de Manutenção (Plano)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                        Text("Adicione, remova ou altere as periodicidades das rotinas", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                    }
                                }
                            }

                            // Dynamic add form
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Cadastrar Nova Rotina", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = newRoutineTitle,
                                    onValueChange = { newRoutineTitle = it },
                                    label = { Text("Descrição da Rotina") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { dropdownNewRoutinePeriodExpanded = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(newRoutinePeriodicity)
                                        }
                                        DropdownMenu(
                                            expanded = dropdownNewRoutinePeriodExpanded,
                                            onDismissRequest = { dropdownNewRoutinePeriodExpanded = false }
                                        ) {
                                            listOf("Mensal", "Bimestral", "Trimestral", "Semestral", "Anual", "Sob ocorrência").forEach { p ->
                                                DropdownMenuItem(
                                                    text = { Text(p) },
                                                    onClick = {
                                                        newRoutinePeriodicity = p
                                                        dropdownNewRoutinePeriodExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (newRoutineTitle.isNotBlank()) {
                                                routinesList.add(Pair(newRoutineTitle.trim(), newRoutinePeriodicity))
                                                newRoutineTitle = ""
                                            } else {
                                                Toast.makeText(context, "Insira um título para a rotina!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Adicionar")
                                    }
                                }
                            }
                        }
                    }

                    Text("Rotinas Ativas no PMOC (${routinesList.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    routinesList.forEachIndexed { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.first, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box {
                                        OutlinedButton(
                                            onClick = { periodicityMenuExpandedIndex = index },
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text(item.second, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                        DropdownMenu(
                                            expanded = periodicityMenuExpandedIndex == index,
                                            onDismissRequest = { periodicityMenuExpandedIndex = null }
                                        ) {
                                            listOf("Mensal", "Bimestral", "Trimestral", "Semestral", "Anual", "Sob ocorrência").forEach { p ->
                                                DropdownMenuItem(
                                                    text = { Text(p) },
                                                    onClick = {
                                                        routinesList[index] = Pair(item.first, p)
                                                        periodicityMenuExpandedIndex = null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                IconButton(onClick = { routinesList.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color.Red.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }

                } else if (step == 3) {
                    // STEP 3 UI: Operational Procedures
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                                }
                                Column {
                                    Text("Procedimentos Operacionais (Etapa 3/5)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                    Text("Caixas de texto editáveis de título e descrição operacional", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    opsList.forEachIndexed { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = item.first,
                                    onValueChange = { opsList[index] = Pair(it, item.second) },
                                    label = { Text("Título do Procedimento") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                )
                                OutlinedTextField(
                                    value = item.second,
                                    onValueChange = { opsList[index] = Pair(item.first, it) },
                                    label = { Text("Descrição / Instruções de Operação") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 4,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                            }
                        }
                    }

                } else if (step == 4) {
                    // STEP 4 UI: Emergency Procedures
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                                }
                                Column {
                                    Text("Procedimentos Emergenciais (Etapa 4/5)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                    Text("Instruções e contatos editáveis para casos de sinistro", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    emsList.forEachIndexed { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = item.first,
                                    onValueChange = { emsList[index] = Pair(it, item.second) },
                                    label = { Text("Título do Procedimento") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                )
                                OutlinedTextField(
                                    value = item.second,
                                    onValueChange = { emsList[index] = Pair(item.first, it) },
                                    label = { Text("Procedimento / Medidas Emergenciais") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 4,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                            }
                        }
                    }

                } else if (step == 5) {
                    // STEP 5 UI: Confirm Information & Signatures
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    )
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color.White.copy(alpha = 0.2f), shape = androidx.compose.foundation.shape.CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Done, contentDescription = null, tint = Color.White)
                                    }
                                    Column {
                                        Text("Confirmar Informações", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                        Text("Confirme os dados cadastrais do PMOC e recolha as assinaturas", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                    }
                                }
                            }

                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Cadastro do PMOC",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(text = "Cliente/Contratante: ${selectedCliente?.nome ?: "Nenhum"}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Text(text = "Endereço: ${selectedCliente?.endereco ?: "Nenhum"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                        Text(text = "Mês de Referência: $mesRef", fontSize = 14.sp)
                                        Text(text = "Número do PMOC: $numeroPmoc", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        if (obsFiltro.isNotEmpty()) {
                                            Text(text = "ART / TRT Nº: $obsFiltro", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text(text = "Equipamentos Selecionados: ${selectedEquipmentsSet.value.size}", fontSize = 13.sp)
                                        Text(text = "Rotinas Customizadas: ${routinesList.size}", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Assinaturas Digitais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    SignaturePad(
                        labelText = "Assinatura do Responsável Técnico*",
                        onSignatureCaptured = { sigTecbase64 = it }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SignaturePad(
                        labelText = "Assinatura do Cliente Autorizado*",
                        onSignatureCaptured = { sigClibase64 = it }
                    )
                }
            }

            // Button controls at the base
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (step > 1) {
                                step--
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Text(if (step == 1) "Cancelar" else "Voltar")
                    }

                    Button(
                        onClick = {
                            if (step == 1) {
                                if (selectedCliente != null && selectedTecnico != null && mesRef.isNotEmpty() && numeroPmoc.isNotEmpty()) {
                                    if (selectedEquipmentsSet.value.isEmpty()) {
                                        Toast.makeText(context, "Por favor, selecione ao menos um equipamento para o controle de PMOC!", Toast.LENGTH_LONG).show()
                                    } else {
                                        step = 2
                                    }
                                } else {
                                    Toast.makeText(context, "Preencha todos os campos obrigatórios primeiro!", Toast.LENGTH_SHORT).show()
                                }
                            } else if (step in 2..4) {
                                step++
                            } else if (step == 5) {
                                if (selectedCliente != null) {
                                    if (!viewModel.premiumAtivo.value) {
                                        viewModel.triggerLimitation("Limite do Plano FREE! A geração de relatórios de PMOC é exclusiva do plano PREMIUM.")
                                    } else {
                                        // Serialize dynamic steps fields
                                        val eqIdsString = selectedEquipmentsSet.value.joinToString(",")
                                        val routinesString = routinesList.joinToString("\n") { "${it.first}|${it.second}" }
                                        val opsString = opsList.joinToString("\n===\n") { "${it.first}:::${it.second}" }
                                        val emsString = emsList.joinToString("\n===\n") { "${it.first}:::${it.second}" }

                                        val finalPmoc = PmocReport(
                                            clienteId = selectedCliente!!.id,
                                            tecnicoId = selectedTecnico!!.id,
                                            mesRef = mesRef,
                                            numeroPmoc = numeroPmoc,
                                            filtroStatus = statusFiltro,
                                            filtroObs = obsFiltro,
                                            serpentinaStatus = statusSerp,
                                            serpentinaObs = obsSerp,
                                            bandejaStatus = statusBand,
                                            bandejaObs = obsBand,
                                            motorStatus = statusMot,
                                            motorObs = obsMot,
                                            eletricoStatus = statusEle,
                                            eletricoObs = obsEle,
                                            gasVoltStatus = statusGas,
                                            gasVoltObs = obsGas,
                                            isolamentoStatus = statusIsol,
                                            isolamentoObs = obsIsol,
                                            assinaturaDigitalTecnico = sigTecbase64,
                                            assinaturaDigitalCliente = sigClibase64,
                                            equipamentosSelecionadosIds = eqIdsString,
                                            rotinasJson = routinesString,
                                            procedimentosOperacionaisJson = opsString,
                                            procedimentosEmergenciaisJson = emsString
                                        )

                                        // Save PMOC
                                        viewModel.salvarPmocReport(finalPmoc)

                                        // Dynamic client's equipments under control for PDF Generation
                                        val selectedIdsList = selectedEquipmentsSet.value
                                        val equips = viewModel.equipamentosState.value.filter { it.id in selectedIdsList }

                                        // Write Multipage PDF
                                        val file = ReportUtils.generatePmocPdf(
                                            context = context,
                                            pmoc = finalPmoc,
                                            cliente = selectedCliente!!,
                                            tecnico = selectedTecnico, ambientes = ambientesListState.value,
                                            equipamentos = equips
                                        )

                                        if (file != null) {
                                            ReportUtils.sharePdf(context, file)
                                            onBack()
                                        } else {
                                            Toast.makeText(context, "Erro ao gerar PDF do PMOC.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Text(if (step == 5) "Gerar PMOC" else "Avançar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PmocDetalhesScreen(
    pmocId: String,
    viewModel: ClimaGestViewModel,
    onBack: () -> Unit,
    onNavigateToLogbook: (String) -> Unit
) {
    val context = LocalContext.current
    val pmocState = viewModel.getPmocReportFlow(pmocId).collectAsStateWithLifecycle(initialValue = null)
    
    val pmoc = pmocState.value ?: return

    val clientes by viewModel.clientesState.collectAsStateWithLifecycle()
    val tecnicos by viewModel.tecnicosState.collectAsStateWithLifecycle()

    val cliente = clientes.find { it.id == pmoc.clienteId }
    val tecnico = tecnicos.find { it.id == pmoc.tecnicoId }

    val clientAmbientesState = remember(cliente) {
        if (cliente != null) {
            viewModel.getAmbientesPorClienteFlow(cliente.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val logbookEntriesState = remember(pmocId) {
        viewModel.getLogbookEntriesByPmocFlow(pmocId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // All available equipments
    val equipmentsState by viewModel.equipamentosState.collectAsStateWithLifecycle()
    
    val selectedIds = pmoc.equipamentosSelecionadosIds.split(",")
        .filter { it.isNotEmpty() }
    
    val filteredEquips = if (selectedIds.isNotEmpty()) {
        equipmentsState.filter { it.id in selectedIds }
    } else {
        if (cliente != null) {
            equipmentsState.filter { it.clienteId == cliente.id }
        } else {
            emptyList()
        }
    }

    val totalEquipsCount = filteredEquips.size
    val totalAmbientesCount = filteredEquips.map { it.ambienteId }.distinct().size
    val somaTotalBtus = filteredEquips.sumOf { it.capacidadeBtu }
    val equivalenciaTR = somaTotalBtus / 12000.0

    // Digital Signatures loaded indicators
    val hasTecSig = pmoc.assinaturaDigitalTecnico.isNotEmpty()
    val hasCliSig = pmoc.assinaturaDigitalCliente.isNotEmpty()
    val isFullySigned = hasTecSig && hasCliSig

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detalhes do PMOC",
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
                    IconButton(
                        onClick = {
                            viewModel.excluirPmocReport(pmoc)
                            onBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Deletar PMOC",
                            tint = MaterialTheme.colorScheme.error
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
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 0.dp
                )
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // STATUS & REGISTRO CARD WITH DYNAMIC BADGE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PMOC REGISTRO", 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = pmoc.numeroPmoc, 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Referência: ${pmoc.mesRef}", 
                                fontSize = 13.sp, 
                                color = Color.Gray
                            )
                        }
                        
                        // Status Badge
                        Surface(
                            color = if (isFullySigned) Color(0xFFE8F5E9) else Color(0xFFFFECEF),
                            contentColor = if (isFullySigned) Color(0xFF2E7D32) else Color(0xFFC62828),
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = if (isFullySigned) "Em vigor / Assinado" else "Pendente Assinatura",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // BUTTON FOR LOGBOOK ACCESS
            Button(
                onClick = { onNavigateToLogbook(pmoc.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Build, contentDescription = "Logbook", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Acessar Logbook de Atividades", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }

            // DADOS DO PROPRIETÁRIO (CLIENTE)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Proprietário do Estabelecimento", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Divider(modifier = Modifier.padding(vertical = 10.dp))
                    Text("Razão Social: ${cliente?.nome ?: "Desconhecido"}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Documento CNPJ/CPF: ${cliente?.documento ?: "N/I"}", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Telefone/Contato: ${cliente?.telefone ?: "N/I"}", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Endereço: ${cliente?.endereco ?: "N/I"}", fontSize = 13.sp)
                }
            }

            // DADOS DO RESPONSÁVEL TÉCNICO DE CAMPO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Responsável Técnico de Campo", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Divider(modifier = Modifier.padding(vertical = 10.dp))
                    if (tecnico != null) {
                        Text("Nome Técnico: ${tecnico.nome}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("CPF: ${tecnico.cpf.ifEmpty { "N/I" }}", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Registro CREA/CFT: ${tecnico.creaCft.ifEmpty { "N/I" }}", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Contato E-mail: ${tecnico.email.ifEmpty { "N/I" }}", fontSize = 13.sp)
                        if (pmoc.filtroObs.trim().isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "ART / TRT Nº: ${pmoc.filtroObs}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    } else {
                        Text("Sem técnico campo cadastrado.", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }

            // RESUMO DA INFRAESTRUTURA CLIMATIZADA
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dimensão & Capacidade Geral", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Divider(modifier = Modifier.padding(vertical = 10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Equipamentos", fontSize = 11.sp, color = Color.Gray)
                            Text("$totalEquipsCount und.", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ambientes", fontSize = 11.sp, color = Color.Gray)
                            Text("$totalAmbientesCount amb.", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Capacidade Total", fontSize = 11.sp, color = Color.Gray)
                            Text(String.format("%, d BTU/h", somaTotalBtus), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Equivalência TR", fontSize = 11.sp, color = Color.Gray)
                            Text(String.format("%.2f TR", equivalenciaTR), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Federal requirement alert
                    Surface(
                        color = if (somaTotalBtus > 60000) Color(0xFFFFECEF) else Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (somaTotalBtus > 60000) Icons.Default.Warning else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (somaTotalBtus > 60000) Color(0xFFC62828) else Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (somaTotalBtus > 60000) 
                                    "Sistema acima de 60k BTU/h — PMOC obrigatório conforme Lei 13.589/2018."
                                else 
                                    "Sistema abaixo da cota legal de 60k BTU/h, porém com manutenção recomendada.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (somaTotalBtus > 60000) Color(0xFFC62828) else Color(0xFF2E7D32),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Assinatura e Validação do PMOC", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            
            // State for interactive capture
            var sigTecbase64 by remember { mutableStateOf("") }
            var sigClibase64 by remember { mutableStateOf("") }
            
            var showTecSigning by remember { mutableStateOf(false) }
            var showCliSigning by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Technical signing box
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Responsável Técnico", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Surface(
                            color = if (hasTecSig) Color(0xFFE8F5E9) else Color(0xFFFFECEF),
                            contentColor = if (hasTecSig) Color(0xFF2E7D32) else Color(0xFFC62828),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (hasTecSig) "Assinado ✔" else "Pendente ✖",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (hasTecSig) {
                        val bitmap = remember(pmoc.assinaturaDigitalTecnico) { ReportUtils.base64ToBitmap(pmoc.assinaturaDigitalTecnico) }
                        if (bitmap != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Assinatura Técnico",
                                        modifier = Modifier.fillMaxSize().padding(8.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = { showTecSigning = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Substituir Assinatura", fontSize = 12.sp)
                        }
                    } else if (showTecSigning) {
                        SignaturePad(
                            labelText = "Assinar como Técnico",
                            onSignatureCaptured = { sigTecbase64 = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showTecSigning = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    if (sigTecbase64.isNotEmpty()) {
                                        val updated = pmoc.copy(assinaturaDigitalTecnico = sigTecbase64)
                                        viewModel.salvarPmocReport(updated)
                                        showTecSigning = false
                                        sigTecbase64 = ""
                                    } else {
                                        Toast.makeText(context, "Por favor desenhe sua assinatura", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Salvar", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = { showTecSigning = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Registrar Assinatura do Técnico", fontSize = 13.sp)
                        }
                    }
                }
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                
                // 2. Client signing box
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Representante do Cliente", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Surface(
                            color = if (hasCliSig) Color(0xFFE8F5E9) else Color(0xFFFFECEF),
                            contentColor = if (hasCliSig) Color(0xFF2E7D32) else Color(0xFFC62828),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (hasCliSig) "Assinado ✔" else "Pendente ✖",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (hasCliSig) {
                        val bitmap = remember(pmoc.assinaturaDigitalCliente) { ReportUtils.base64ToBitmap(pmoc.assinaturaDigitalCliente) }
                        if (bitmap != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Assinatura Cliente",
                                        modifier = Modifier.fillMaxSize().padding(8.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = { showCliSigning = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Substituir Assinatura", fontSize = 12.sp)
                        }
                    } else if (showCliSigning) {
                        SignaturePad(
                            labelText = "Assinar como Representante do Cliente",
                            onSignatureCaptured = { sigClibase64 = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showCliSigning = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    if (sigClibase64.isNotEmpty()) {
                                        val updated = pmoc.copy(assinaturaDigitalCliente = sigClibase64)
                                        viewModel.salvarPmocReport(updated)
                                        showCliSigning = false
                                        sigClibase64 = ""
                                    } else {
                                        Toast.makeText(context, "Por favor desenhe sua assinatura", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Salvar", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = { showCliSigning = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Registrar Assinatura do Cliente", fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Export button
            Button(
                onClick = {
                    if (!viewModel.premiumAtivo.value) {
                        viewModel.triggerLimitation("Limite do Plano FREE! A geração de relatórios de PMOC é exclusiva do plano PREMIUM.")
                    } else {
                        if (cliente != null) {
                            val file = ReportUtils.generatePmocPdf(
                                context = context,
                                pmoc = pmoc,
                                cliente = cliente,
                                tecnico = tecnico, 
                                ambientes = clientAmbientesState.value,
                                equipamentos = filteredEquips,
                                logbookEntries = logbookEntriesState.value
                            )
                            if (file != null) {
                                ReportUtils.sharePdf(context, file)
                            } else {
                                Toast.makeText(context, "Erro ao exportar PDF", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Cliente associado foi removido do banco", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar e Compartilhar PDF")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PmocLogbookScreen(
    pmocId: String,
    viewModel: ClimaGestViewModel,
    onBack: () -> Unit,
    onNavigateToEquipment: (String) -> Unit
) {
    val context = LocalContext.current
    val pmocState = viewModel.getPmocReportFlow(pmocId).collectAsStateWithLifecycle(initialValue = null)
    val pmoc = pmocState.value ?: return

    val clientes by viewModel.clientesState.collectAsStateWithLifecycle()
    val cliente = clientes.find { it.id == pmoc.clienteId } ?: return

    val equipmentsState by viewModel.equipamentosState.collectAsStateWithLifecycle()
    
    val clientAmbientesState by remember(cliente) {
        viewModel.getAmbientesPorClienteFlow(cliente.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val selectedIds = pmoc.equipamentosSelecionadosIds.split(",")
        .filter { it.isNotEmpty() }
    
    val filteredEquips = if (selectedIds.isNotEmpty()) {
        equipmentsState.filter { it.id in selectedIds }
    } else {
        equipmentsState.filter { it.clienteId == cliente.id }
    }

    val dbEntriesState by viewModel.getLogbookEntriesByPmocFlow(pmocId).collectAsStateWithLifecycle(initialValue = emptyList())

    val routines = remember(pmoc.rotinasJson) {
        if (pmoc.rotinasJson.isNotEmpty()) {
            pmoc.rotinasJson.split("\n").filter { it.contains("|") }.map {
                val parts = it.split("|", limit = 2)
                Pair(parts[0], parts[1])
            }
        } else {
            listOf(
                Pair("Limpeza e inspeção dos filtros de ar", "Mensal"),
                Pair("Limpeza da bandeja de condensado", "Mensal"),
                Pair("Limpeza das aletas e serpentina do evaporador", "Trimestral"),
                Pair("Verificação e limpeza do dreno de condensado", "Mensal"),
                Pair("Inspeção do dreno, motor e turbina do ventilador", "Trimestral"),
                Pair("Aplicação de biocida / higienizador", "Trimestral"),
                Pair("Verificação elétrica da unidade interna", "Semestral"),
                Pair("Limpeza das aletas e serpentina do condensador", "Trimestral"),
                Pair("Inspeção do motor e hélice do ventilador/condensador", "Trimestral"),
                Pair("Verificação de vazamento de gás refrigerante", "Trimestral"),
                Pair("Medição de pressão do ciclo frigorífico", "Semestral"),
                Pair("Verificação e aperto das conexões elétricas", "Semestral"),
                Pair("Lubrificação de partes móveis", "Semestral"),
                Pair("Teste funcional geral (frio, quente, ventilação)", "Trimestral"),
                Pair("Verificação da fixação e vedação", "Semestral")
            )
        }
    }

    var selectedEquipId by remember { mutableStateOf<String?>(null) }
    if (selectedEquipId == null && filteredEquips.isNotEmpty()) {
        selectedEquipId = filteredEquips.first().id
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Logbook do PMOC", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Ref: ${pmoc.mesRef} | ${cliente.nome}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                windowInsets = WindowInsets(top = 0.dp)
            )
        }
    ) { innerPadding ->
        if (filteredEquips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding), 
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum equipamento associado a este PMOC.", fontWeight = FontWeight.Medium, color = Color.Gray)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Horizontal list / tabs of equipments
                ScrollableTabRow(
                    selectedTabIndex = filteredEquips.indexOfFirst { it.id == selectedEquipId }.coerceAtLeast(0),
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    filteredEquips.forEach { eq ->
                        Tab(
                            selected = selectedEquipId == eq.id,
                            onClick = { selectedEquipId = eq.id },
                            text = { 
                                Text(
                                    text = eq.tag, 
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedEquipId == eq.id) MaterialTheme.colorScheme.primary else Color.Gray
                                ) 
                            }
                        )
                    }
                }

                val currentEq = filteredEquips.find { it.id == selectedEquipId }
                if (currentEq != null) {
                    
                    val currentAmbiente = clientAmbientesState.find { it.id == currentEq.ambienteId }
                    val ambienteNome = currentAmbiente?.nome ?: "Não Informado"

                    // Dedicated visually styled equipment sheet card with complete details
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Equipment TAG as a beautiful Badge
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = currentEq.tag,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currentEq.marca,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Environment info
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Ambiente: $ambienteNome",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Capacity info
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Carga: ${currentEq.capacidadeBtu} BTU/h",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            // Visual icon button for accessing the complete equipment sheet
                            IconButton(
                                onClick = { onNavigateToEquipment(currentEq.id) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Launch,
                                    contentDescription = "Ver Ficha Completa",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Activity list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(routines) { rot ->
                            val activityText = rot.first
                            val activityFreq = rot.second
                            
                            val dbEntry = dbEntriesState.find { it.equipamentoId == currentEq.id && it.atividade == activityText }
                            
                            val isCompleted = dbEntry?.concluido ?: false
                            val dateStr = dbEntry?.dataRealizacao ?: ""

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(activityText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                color = when (activityFreq) {
                                                    "Mensal" -> Color(0xFFE3F2FD)
                                                    "Trimestral" -> Color(0xFFFFF3E0)
                                                    "Semestral" -> Color(0xFFEDE7F6)
                                                    else -> Color(0xFFF5F5F5)
                                                },
                                                contentColor = when (activityFreq) {
                                                    "Mensal" -> Color(0xFF1E88E5)
                                                    "Trimestral" -> Color(0xFFF57C00)
                                                    "Semestral" -> Color(0xFF5E35B1)
                                                    else -> Color.DarkGray
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    activityFreq, 
                                                    style = MaterialTheme.typography.bodySmall, 
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Checkbox(
                                            checked = isCompleted,
                                            onCheckedChange = { checked ->
                                                val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                                val entryToSave = PmocLogbookEntry(
                                                    id = dbEntry?.id ?: com.priorizedev.brizza.data.model.generateRandomId(),
                                                    pmocId = pmocId,
                                                    equipamentoId = currentEq.id,
                                                    atividade = activityText,
                                                    periodicidade = activityFreq,
                                                    dataRealizacao = if (checked) (if (dateStr.isEmpty()) today else dateStr) else "",
                                                    concluido = checked
                                                )
                                                viewModel.salvarLogbookEntry(entryToSave)
                                            }
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    // Row to input the completed date
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = dateStr,
                                            onValueChange = { newVal ->
                                                val entryToSave = PmocLogbookEntry(
                                                    id = dbEntry?.id ?: com.priorizedev.brizza.data.model.generateRandomId(),
                                                    pmocId = pmocId,
                                                    equipamentoId = currentEq.id,
                                                    atividade = activityText,
                                                    periodicidade = activityFreq,
                                                    dataRealizacao = newVal,
                                                    concluido = isCompleted || newVal.isNotEmpty()
                                                )
                                                viewModel.salvarLogbookEntry(entryToSave)
                                            },
                                            label = { Text("Data Realizada", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        
                                        // Quick stamp date button
                                        Button(
                                            onClick = {
                                                val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                                val entryToSave = PmocLogbookEntry(
                                                    id = dbEntry?.id ?: com.priorizedev.brizza.data.model.generateRandomId(),
                                                    pmocId = pmocId,
                                                    equipamentoId = currentEq.id,
                                                    atividade = activityText,
                                                    periodicidade = activityFreq,
                                                    dataRealizacao = today,
                                                    concluido = true
                                                )
                                                viewModel.salvarLogbookEntry(entryToSave)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp)
                                        ) {
                                            Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Hoje", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
