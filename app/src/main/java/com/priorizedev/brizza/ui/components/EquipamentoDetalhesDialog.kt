package com.priorizedev.brizza.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.priorizedev.brizza.data.model.Equipamento

@Composable
fun EquipamentoDetalhesDialog(
    equipamento: Equipamento,
    ambienteNome: String = "Não Informado",
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 32.dp)
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { /* Consome cliques na card */ },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header with gradient and title
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
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ficha do Equipamento",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = equipamento.tag,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color.White.copy(alpha = 0.15f), shape = CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Content Area
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // General Row with TAG and Status
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = equipamento.tag,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Status Badge
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
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
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

                        // Section: Identificação
                        item {
                            DetailCardSection(title = "Identificação & Modelo", icon = Icons.Default.Layers) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DetailItem(label = "Marca", value = equipamento.marca)
                                    DetailItem(label = "Modelo", value = equipamento.modelo.ifEmpty { "Não Informado" })
                                    DetailItem(label = "Tipo", value = equipamento.tipo)
                                    DetailItem(label = "Número de Série / Chapa", value = equipamento.numeroSerie.ifEmpty { "Não Informado" })
                                }
                            }
                        }

                        // Section: Especificações Técnicas
                        item {
                            DetailCardSection(title = "Especificações Técnicas", icon = Icons.Default.Settings) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DetailItem(label = "Capacidade", value = "${equipamento.capacidadeBtu} BTU/h")
                                    DetailItem(label = "Gás Refrigerante", value = equipamento.fluidoRefrigerante)
                                    DetailItem(label = "Sistema", value = equipamento.sistema)
                                    DetailItem(label = "Tensão", value = "${equipamento.tensao}V")
                                    DetailItem(label = "Ciclo", value = equipamento.ciclo)
                                    DetailItem(label = "Potência (Watts)", value = equipamento.potenciaW.ifEmpty { "Não Informado" })
                                }
                            }
                        }

                        // Section: Instalação & Localização
                        item {
                            DetailCardSection(title = "Instalação & Local", icon = Icons.Default.LocationOn) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DetailItem(label = "Ambiente Vinculado", value = ambienteNome)
                                    DetailItem(label = "Data da Instalação", value = equipamento.dataInstalacao.ifEmpty { "Não Informada" })
                                }
                            }
                        }

                        // Section: Observações Técnicas
                        if (equipamento.observacoes.isNotEmpty()) {
                            item {
                                DetailCardSection(title = "Observações Técnicas / Notas", icon = Icons.Default.Comment) {
                                    Text(
                                        text = equipamento.observacoes,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Bottom close button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                    ) {
                        Text("Fechar", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailCardSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            content()
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
