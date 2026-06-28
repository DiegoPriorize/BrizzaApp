package com.priorizedev.brizza

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.priorizedev.brizza.ui.screens.*
import com.priorizedev.brizza.ui.screens.OrdemServicoDetalhesScreen
import com.priorizedev.brizza.ui.theme.MyApplicationTheme
import com.priorizedev.brizza.ui.viewmodel.ClimaGestViewModel
import com.priorizedev.brizza.ui.viewmodel.ClimaGestViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: ClimaGestViewModel by viewModels {
        ClimaGestViewModelFactory((application as ClimaGestApp).repository, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userThemeSelection by viewModel.themeState.collectAsState()
            val isDarkTheme = when (userThemeSelection) {
                null -> androidx.compose.foundation.isSystemInDarkTheme()
                else -> userThemeSelection!!
            }
            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainContainer(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.registrarAcesso()
        if (viewModel.isLoggedIn.value) {
            viewModel.verificarAssinaturaStripe()
        }
    }

}

@Composable
fun MainContainer(viewModel: ClimaGestViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val limitationMessage by viewModel.limitationMessage.collectAsState()

    // Automatic redirection if session ends
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    if (limitationMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearLimitationMessage() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recurso Limitado (Plano FREE)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = limitationMessage!!,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearLimitationMessage()
                        navController.navigate("stripe_subscriptions") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Ver Planos / Upgrade", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearLimitationMessage() }) {
                    Text("Fechar")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // Top-level routes that display bottom navigation bar
    val topLevelRoutes = listOf("dashboard", "clientes", "ordens_servico", "pmoc")
    val showBottomBar = isLoggedIn && (currentRoute in topLevelRoutes || currentRoute?.startsWith("cliente_detalhes") == true)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Central") },
                        label = { Text("Central", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selected = currentRoute == "dashboard",
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        onClick = {
                            if (currentRoute != "dashboard") {
                                navController.navigate("dashboard") {
                                    popUpTo("dashboard") { inclusive = false }
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.People, contentDescription = "Clientes") },
                        label = { Text("Clientes", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selected = currentRoute == "clientes" || currentRoute?.startsWith("cliente_detalhes") == true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        onClick = {
                            if (currentRoute != "clientes") {
                                navController.navigate("clientes") {
                                    popUpTo("dashboard")
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Assignment, contentDescription = "Ordens") },
                        label = { Text("Ordens", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selected = currentRoute == "ordens_servico",
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        onClick = {
                            if (currentRoute != "ordens_servico") {
                                navController.navigate("ordens_servico") {
                                    popUpTo("dashboard")
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Description, contentDescription = "PMOC") },
                        label = { Text("PMOC", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selected = currentRoute == "pmoc",
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        onClick = {
                            if (currentRoute != "pmoc") {
                                navController.navigate("pmoc") {
                                    popUpTo("dashboard")
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) "dashboard" else "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    }
                )
            }

            composable("register") {
                RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate("login") {
                            popUpTo("register") { inclusive = true }
                        }
                    }
                )
            }

            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSection = { section ->
                        navController.navigate(section)
                    }
                )
            }
            
            composable("clientes") {
                ClientesScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = { id ->
                        navController.navigate("cliente_detalhes/$id")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(
                route = "cliente_detalhes/{clienteId}",
                arguments = listOf(navArgument("clienteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val clienteId = backStackEntry.arguments?.getString("clienteId") ?: ""
                ClienteDetalhesScreen(
                    clienteId = clienteId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToAmbienteDetails = { id ->
                        navController.navigate("ambiente_detalhes/$id")
                    },
                    onNavigateToEquipamentoDetails = { id ->
                        navController.navigate("equipamento_detalhes/$id")
                    }
                )
            }

            composable(
                route = "ambiente_detalhes/{ambienteId}",
                arguments = listOf(navArgument("ambienteId") { type = NavType.StringType })
            ) { backStackEntry ->
                val ambienteId = backStackEntry.arguments?.getString("ambienteId") ?: ""
                AmbienteDetalhesScreen(
                    ambienteId = ambienteId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToEquipamentoDetails = { id ->
                        navController.navigate("equipamento_detalhes/$id")
                    }
                )
            }

            composable(
                route = "equipamento_detalhes/{equipamentoId}",
                arguments = listOf(navArgument("equipamentoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val equipamentoId = backStackEntry.arguments?.getString("equipamentoId") ?: ""
                EquipamentoDetalhesScreen(
                    equipamentoId = equipamentoId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToOrdemDetalhes = { id ->
                        navController.navigate("ordem_detalhes/$id")
                    },
                    onNavigateToOrdensServico = {
                        navController.navigate("ordens_servico")
                    }
                )
            }
            
            composable("ordens_servico") {
                OrdensServicoScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = { id ->
                        navController.navigate("ordem_detalhes/$id")
                    },
                    onNavigateToOrdemForm = { oId, eId ->
                        navController.navigate("ordem_form?ordemId=${oId ?: "0"}&equipamentoId=${eId ?: "0"}")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(
                route = "ordem_detalhes/{ordemId}",
                arguments = listOf(navArgument("ordemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val ordemId = backStackEntry.arguments?.getString("ordemId") ?: ""
                OrdemServicoDetalhesScreen(
                    ordemId = ordemId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToClientDetails = { id: String ->
                        navController.navigate("cliente_detalhes/$id")
                    },
                    onNavigateToEditarOrdem = { id: String ->
                        navController.navigate("ordem_form?ordemId=$id&equipamentoId=0")
                    },
                    onNavigateToEquipamentoDetails = { id: String ->
                        navController.navigate("equipamento_detalhes/$id")
                    }
                )
            }

            composable(
                route = "ordem_form?ordemId={ordemId}&equipamentoId={equipamentoId}",
                arguments = listOf(
                    navArgument("ordemId") {
                        type = NavType.StringType
                        defaultValue = "0"
                    },
                    navArgument("equipamentoId") {
                        type = NavType.StringType
                        defaultValue = "0"
                    }
                )
            ) { backStackEntry ->
                val oIdRaw = backStackEntry.arguments?.getString("ordemId") ?: "0"
                val eIdRaw = backStackEntry.arguments?.getString("equipamentoId") ?: "0"
                val oId = if (oIdRaw == "0") null else oIdRaw
                val eId = if (eIdRaw == "0") null else eIdRaw
                OrdemFormScreen(
                    ordemId = oId,
                    preselectedEquipamentoId = eId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("pmoc") {
                PmocScreen(
                    viewModel = viewModel,
                    onNavigateToDetails = { id ->
                        navController.navigate("pmoc_detalhes/$id")
                    },
                    onNavigateToCreate = {
                        navController.navigate("pmoc_novo")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("pmoc_novo") {
                PmocCreateWizard(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(
                route = "pmoc_detalhes/{pmocId}",
                arguments = listOf(navArgument("pmocId") { type = NavType.StringType })
            ) { backStackEntry ->
                val pmocId = backStackEntry.arguments?.getString("pmocId") ?: ""
                PmocDetalhesScreen(
                    pmocId = pmocId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToLogbook = { id ->
                        navController.navigate("pmoc_logbook/$id")
                    }
                )
            }

            composable(
                route = "pmoc_logbook/{pmocId}",
                arguments = listOf(navArgument("pmocId") { type = NavType.StringType })
            ) { backStackEntry ->
                val pmocId = backStackEntry.arguments?.getString("pmocId") ?: ""
                PmocLogbookScreen(
                    pmocId = pmocId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToEquipment = { equipId ->
                        navController.navigate("equipamento_detalhes/$equipId")
                    }
                )
            }
            
            composable("tecnicos") {
                TecnicosScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToDetails = { id ->
                        navController.navigate("tecnico_detalhes/$id")
                    }
                )
            }

            composable(
                route = "tecnico_detalhes/{tecnicoId}",
                arguments = listOf(navArgument("tecnicoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tecnicoId = backStackEntry.arguments?.getString("tecnicoId") ?: ""
                TecnicoDetalhesScreen(
                    tecnicoId = tecnicoId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("perfil") {
                PerfilScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToParametros = { navController.navigate("parametros") },
                    onNavigateToStripeSubscriptions = { navController.navigate("stripe_subscriptions") },
                    onNavigateToConfigEmpresa = { navController.navigate("config_empresa") }
                )
            }

            composable("config_empresa") {
                ConfigEmpresaScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("stripe_subscriptions") {
                StripeSubscriptionsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("parametros") {
                ParametrosScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("pecas") {
                PecasScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("relatorio") {
                RelatorioScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToDashboard = {
                        navController.navigate("indicadores_dashboard")
                    }
                )
            }

            composable("indicadores_dashboard") {
                IndicadoresDashboardScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("programas") {
                ProgramacaoScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToOrdemId = { id -> navController.navigate("ordem_detalhes/$id") }
                )
            }
        }
    }
}
