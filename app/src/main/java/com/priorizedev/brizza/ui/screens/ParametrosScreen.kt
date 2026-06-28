package com.priorizedev.brizza.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.priorizedev.brizza.ui.viewmodel.ClimaGestViewModel
import com.priorizedev.brizza.data.model.MarcaEquipamento

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametrosScreen(
    viewModel: ClimaGestViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val marcas by viewModel.marcasEquipamentosState.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    
    var novaMarcaNome by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Parâmetros do Sistema",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Form to Add Brand
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Nova Marca de Equipamento",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                             value = novaMarcaNome,
                             onValueChange = { novaMarcaNome = it },
                             placeholder = { Text("Nova marca", fontSize = 13.sp) },
                             modifier = Modifier.weight(1f).height(48.dp),
                             singleLine = true,
                             shape = RoundedCornerShape(12.dp),
                             colors = OutlinedTextFieldDefaults.colors(
                                 focusedBorderColor = MaterialTheme.colorScheme.primary,
                                 unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                 focusedContainerColor = MaterialTheme.colorScheme.surface,
                                 unfocusedContainerColor = MaterialTheme.colorScheme.surface
                             ),
                             textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                        )
                        
                        Button(
                            onClick = {
                                val cleanName = novaMarcaNome.trim()
                                if (cleanName.isNotBlank()) {
                                    val equalExists = marcas.any { it.nome.equals(cleanName, ignoreCase = true) }
                                    if (equalExists) {
                                        Toast.makeText(context, "Esta marca já existe!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        if (!viewModel.premiumAtivo.value) {
                                            viewModel.triggerLimitation("Limite do Plano FREE! Não é possível adicionar novas marcas de equipamentos no plano gratuito. Adquira o plano PREMIUM.")
                                        } else {
                                            viewModel.salvarMarcaEquipamento(cleanName)
                                            novaMarcaNome = ""
                                            Toast.makeText(context, "Marca adicionada com sucesso!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Digite um nome válido!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Adicionar", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Adicionar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Brands List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Marcas Cadastradas (${marcas.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Scrollable Brands List
            if (marcas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma marca cadastrada.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(marcas, key = { it.id }) { marca ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = marca.nome,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                if (marca.usuarioEmail == userEmail && userEmail.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            viewModel.excluirMarcaEquipamento(marca)
                                            Toast.makeText(context, "Marca excluída!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir Marca",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
