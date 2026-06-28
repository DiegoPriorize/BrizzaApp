package com.priorizedev.brizza.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.priorizedev.brizza.data.model.OrdemServico
import com.priorizedev.brizza.ui.viewmodel.ClimaGestViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicadoresDashboardScreen(
    viewModel: ClimaGestViewModel,
    onBack: () -> Unit
) {
    val clientes by viewModel.clientesState.collectAsStateWithLifecycle()
    val tecnicos by viewModel.tecnicosState.collectAsStateWithLifecycle()
    val ordens by viewModel.ordensServicoState.collectAsStateWithLifecycle()
    val equipamentos by viewModel.equipamentosState.collectAsStateWithLifecycle()
    val ambientes by viewModel.ambientesState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("Geral") } // "Geral", "Financeiro", "Serviços"

    // Raw calculated values
    val totalRevenue = remember(ordens) { 
        ordens.filter { it.status == "Concluída" }.sumOf { it.valorServico } 
    }
    val openCount = remember(ordens) { ordens.count { it.status == "Aberta" } }
    val progressCount = remember(ordens) { ordens.count { it.status == "Pendente" || it.status == "Em Andamento" || it.status == "Em andamento" } }
    val completedCount = remember(ordens) { ordens.count { it.status == "Concluída" } }
    val cancelledCount = remember(ordens) { ordens.count { it.status == "Cancelada" } }
    val totalCount = ordens.size

    val ticketMedio = remember(totalRevenue, completedCount) {
        if (completedCount == 0) 0.0 else totalRevenue / completedCount
    }

    val systemEfficacy = remember(completedCount, totalCount) {
        if (totalCount == 0) 100f else (completedCount.toFloat() / totalCount.toFloat()) * 100
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DASHBOARD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "Visão Geral de Performance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            // TAB SELECTOR
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Geral", "Financeiro", "Serviços").forEach { tab ->
                        val isSelected = activeTab == tab
                        val backgroundFactor by animateFloatAsState(if (isSelected) 1f else 0f, label = "tab_bg")
                        val tabColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { activeTab = tab }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = tabColor
                            )
                        }
                    }
                }
            }

            // DYNAMIC CONTENT BASED ON SELECTED TAB
            when (activeTab) {
                "Geral" -> {
                    // Highlights ROW 1
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardMiniCard(
                                modifier = Modifier.weight(1.0f),
                                title = "EFICIÊNCIA",
                                value = String.format("%.1f%%", systemEfficacy),
                                subtitle = "Ordens concluídas",
                                icon = Icons.Default.TrendingUp,
                                glowColor = MaterialTheme.colorScheme.primary
                            )
                            DashboardMiniCard(
                                modifier = Modifier.weight(1.0f),
                                title = "ORDENS ATIVAS",
                                value = (openCount + progressCount).toString(),
                                subtitle = "Pendente/Aberta",
                                icon = Icons.Default.Build,
                                glowColor = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    // Large Radial Donut Graph: Status Distribution
                    item {
                        LegacyStatusDonutChartCard(
                            total = totalCount,
                            open = openCount,
                            inProgress = progressCount,
                            completed = completedCount,
                            cancelled = cancelledCount
                        )
                    }

                    // Key Summary Metrics Row
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "RESUMO OPERACIONAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                GridResumoOperacional(
                                    clientesCount = clientes.size,
                                    tecnicosCount = tecnicos.size,
                                    equipamentosCount = equipamentos.size,
                                    ambientesCount = ambientes.size
                                )
                            }
                        }
                    }
                }

                "Financeiro" -> {
                    // Highlights Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardMiniCard(
                                modifier = Modifier.weight(1.2f),
                                title = "FATURAMENTO TOTAL",
                                value = String.format("R$ %.2f", totalRevenue),
                                subtitle = "Acumulado em O.S.",
                                icon = Icons.Default.MonetizationOn,
                                glowColor = Color(0xFF059669)
                            )
                            DashboardMiniCard(
                                modifier = Modifier.weight(0.8f),
                                title = "TICKET MÉDIO",
                                value = String.format("R$ %.0f", ticketMedio),
                                subtitle = "Por O.S. concluída",
                                icon = Icons.Default.AccountBalanceWallet,
                                glowColor = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // Financial Curve Chart (Area Chart showing earnings per month or similar, simulated by orders)
                    item {
                        RevenueWaveChartCard(ordens = ordens)
                    }

                    // Top Clients by Value
                    item {
                        TopMetricsCard(
                            title = "RANKING DE CLIENTES (TOP VALORES)",
                            icon = Icons.Default.People,
                            metrics = remember(ordens, clientes) {
                                ordens.groupBy { it.clienteId }
                                    .map { (cId, list) ->
                                        val name = clientes.find { it.id == cId }?.nome ?: "Cliente Desconhecido"
                                        val totalVal = list.sumOf { it.valorServico }
                                        Pair(name, totalVal)
                                    }
                                    .sortedByDescending { it.second }
                                    .take(5)
                            },
                            suffix = "R$"
                        )
                    }
                }

                "Serviços" -> {
                    // Highlights
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardMiniCard(
                                modifier = Modifier.weight(1.0f),
                                title = "CONCLUÍDAS",
                                value = completedCount.toString(),
                                subtitle = "Serviços executados",
                                icon = Icons.Default.CheckCircle,
                                glowColor = Color(0xFF059669)
                            )
                            DashboardMiniCard(
                                modifier = Modifier.weight(1.0f),
                                title = "TIPO MAIS COMUM",
                                value = remember(ordens) {
                                    ordens.groupBy { it.tipoServico }
                                        .maxByOrNull { it.value.size }?.key ?: "Nenhum"
                                },
                                subtitle = "Maior recorrência",
                                icon = Icons.Default.Star,
                                glowColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Interactive Horizontal Bar Chart for types of service
                    item {
                        ServiceTypeBarChartCard(ordens = ordens)
                    }

                    // Best Performers: Technicians
                    item {
                        TopMetricsCard(
                            title = "DESEMPENHO DOS TÉCNICOS (O.S. CONCLUÍDAS)",
                            icon = Icons.Default.Engineering,
                            metrics = remember(ordens, tecnicos) {
                                ordens.filter { it.status == "Concluída" }
                                    .groupBy { it.tecnicoId }
                                    .map { (tId, list) ->
                                        val name = tecnicos.find { it.id == tId }?.nome ?: "Técnico Desconhecido"
                                        Pair(name, list.size.toDouble())
                                    }
                                    .sortedByDescending { it.second }
                                    .take(5)
                            },
                            suffix = "",
                            isInteger = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardMiniCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    glowColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Draw a subtle colored glow point on the top right
                    drawCircle(
                        color = glowColor.copy(alpha = 0.08f),
                        radius = 80.dp.toPx(),
                        center = Offset(size.width - 20.dp.toPx(), 20.dp.toPx())
                    )
                }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = glowColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LegacyStatusDonutChartCard(
    total: Int,
    open: Int,
    inProgress: Int,
    completed: Int,
    cancelled: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "STATUS DAS ATIVIDADES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (total == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nenhuma Ordem Cadastrada",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Custom Donut canvas
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            val innerSize = size.copy(width = size.width - strokeWidth, height = size.height - strokeWidth)
                            val centerOffset = Offset(strokeWidth/2, strokeWidth/2)
                            
                            val angles = listOf(
                                (completed.toFloat() / total) * 360f,
                                (inProgress.toFloat() / total) * 360f,
                                (open.toFloat() / total) * 360f,
                                (cancelled.toFloat() / total) * 360f
                            )
                            val colors = listOf(
                                Color(0xFF059669), // Concluída
                                Color(0xFF2563EB), // Em Andamento
                                Color(0xFFD97706), // Aberta
                                Color(0xFFDC2626)  // Cancelada
                            )

                            var currentStartAngle = -90f
                            for (i in angles.indices) {
                                if (angles[i] > 0f) {
                                    drawArc(
                                        color = colors[i],
                                        startAngle = currentStartAngle,
                                        sweepAngle = angles[i] - 2f, // Subtle gap
                                        useCenter = false,
                                        topLeft = centerOffset,
                                        size = innerSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                    currentStartAngle += angles[i]
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = total.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Total O.S.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Side indicators
                    Column(
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusIndicatorRow(
                            label = "Concluídas",
                            count = completed,
                            color = Color(0xFF059669),
                            percentage = (completed.toFloat() / total) * 100
                        )
                        StatusIndicatorRow(
                            label = "Pendentes",
                            count = inProgress,
                            color = Color(0xFF2563EB),
                            percentage = (inProgress.toFloat() / total) * 100
                        )
                        StatusIndicatorRow(
                            label = "Abertas",
                            count = open,
                            color = Color(0xFFD97706),
                            percentage = (open.toFloat() / total) * 100
                        )
                        StatusIndicatorRow(
                            label = "Canceladas",
                            count = cancelled,
                            color = Color(0xFFDC2626),
                            percentage = (cancelled.toFloat() / total) * 100
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicatorRow(
    label: String,
    count: Int,
    color: Color,
    percentage: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$count (${String.format("%.0f%%", percentage)})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
fun GridResumoOperacional(
    clientesCount: Int,
    tecnicosCount: Int,
    equipamentosCount: Int,
    ambientesCount: Int
) {
    val items = listOf(
        QuadItem("Clientes", clientesCount.toString(), Icons.Default.People, MaterialTheme.colorScheme.primary),
        QuadItem("Técnicos", tecnicosCount.toString(), Icons.Default.Face, MaterialTheme.colorScheme.secondary),
        QuadItem("Ambientes", ambientesCount.toString(), Icons.Default.LocationOn, MaterialTheme.colorScheme.tertiary),
        QuadItem("Equipamentos", equipamentosCount.toString(), Icons.Default.Settings, MaterialTheme.colorScheme.error)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuadCard(modifier = Modifier.weight(1f), item = items[0])
            QuadCard(modifier = Modifier.weight(1f), item = items[1])
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuadCard(modifier = Modifier.weight(1f), item = items[2])
            QuadCard(modifier = Modifier.weight(1f), item = items[3])
        }
    }
}

data class QuadItem(val title: String, val value: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color)

@Composable
fun QuadCard(modifier: Modifier, item: QuadItem) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(item.color.copy(alpha = 0.06f))
            .border(1.dp, item.color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = item.title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = item.value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Icon(imageVector = item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun RevenueWaveChartCard(ordens: List<OrdemServico>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "HISTÓRICO FINANCEIRO (O.S. RECENTES)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))

            val completedOrders: List<OrdemServico> = remember(ordens) {
                ordens.filter { it.status == "Concluída" }.sortedBy { it.dataCriacao }
            }

            if (completedOrders.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Dados insuficientes para gerar ondas de faturamento",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                val points = remember(completedOrders) {
                    completedOrders.map { it.valorServico.toFloat() }
                }
                val maxVal = points.maxOrNull() ?: 100f
                val minVal = points.minOrNull() ?: 0f
                val range = if (maxVal == minVal) 100f else maxVal - minVal

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(top = 10.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val spacing = width / (points.size - 1)

                    val path = Path()
                    val fillPath = Path()

                    points.forEachIndexed { index, valPoint ->
                        val x = index * spacing
                        // Invert coordinates since canvas 0,0 is top-left
                        val normalizedVal = (valPoint - minVal) / range
                        val y = height - (normalizedVal * height * 0.8f) - (height * 0.1f)

                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, height)
                            fillPath.lineTo(x, y)
                        } else {
                            // Draw smooth curves control points
                            val prevX = (index - 1) * spacing
                            val prevValNormal = (points[index - 1] - minVal) / range
                            val prevY = height - (prevValNormal * height * 0.8f) - (height * 0.1f)
                            
                            val controlX1 = prevX + (spacing / 2)
                            val controlY1 = prevY
                            val controlX2 = prevX + (spacing / 2)
                            val controlY2 = y

                            path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                            fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                        }

                        if (index == points.size - 1) {
                            fillPath.lineTo(x, height)
                            fillPath.close()
                        }
                    }

                    // Solid gradient area fill
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF059669).copy(alpha = 0.25f),
                                Color(0xFF059669).copy(alpha = 0.0f)
                            )
                        )
                    )

                    // Draw line
                    drawPath(
                        path = path,
                        color = Color(0xFF059669),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw circles at data points
                    points.forEachIndexed { index, valPoint ->
                        val x = index * spacing
                        val normalizedVal = (valPoint - minVal) / range
                        val y = height - (normalizedVal * height * 0.8f) - (height * 0.1f)

                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color(0xFF059669),
                            radius = 3.dp.toPx(),
                            center = Offset(x, y),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Mín: R$ ${minVal.toInt()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Máx: R$ ${maxVal.toInt()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceTypeBarChartCard(ordens: List<OrdemServico>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SERVIÇOS POR RECORRÊNCIA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))

            val types = remember(ordens) {
                listOf("Preventiva", "Corretiva", "Instalação", "Orçamento", "Limpeza", "Higienização").map { type ->
                    val cnt = ordens.count { it.tipoServico == type }
                    Pair(type, cnt)
                }.sortedByDescending { it.second }
            }

            val maxCount = remember(types) { types.maxOfOrNull { it.second } ?: 1 }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                types.forEach { (type, count) ->
                    val animatedProgress = remember { Animatable(0f) }
                    LaunchedEffect(count) {
                        animatedProgress.animateTo(
                            targetValue = if (maxCount == 0) 0f else count.toFloat() / maxCount,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = type, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = count.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = animatedProgress.value)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopMetricsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    metrics: List<Pair<String, Double>>,
    suffix: String,
    isInteger: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            if (metrics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Sem dados disponíveis", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    metrics.forEachIndexed { index, metric ->
                        val medalColor = when (index) {
                            0 -> Color(0xFFFFD700) // Gold
                            1 -> Color(0xFFC0C0C0) // Silver
                            2 -> Color(0xFFCD7F32) // Bronze
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(medalColor.copy(alpha = 0.15f))
                                        .border(1.dp, medalColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (index + 1).toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index < 3) medalColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = metric.first,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            val valueStr = if (isInteger) {
                                metric.second.toInt().toString()
                            } else {
                                String.format("%.2f", metric.second)
                            }
                            
                            Text(
                                text = if (suffix == "R$") "R$ $valueStr" else "$valueStr $suffix",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
