package com.priorizedev.brizza.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.priorizedev.brizza.ui.viewmodel.ClimaGestViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StripeSubscriptionsScreen(
    viewModel: ClimaGestViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // States from viewmodel
    val premiumAtivo by viewModel.premiumAtivo.collectAsStateWithLifecycle()
    val userPlan by viewModel.userPlan.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val stripeChecking by viewModel.stripeChecking.collectAsStateWithLifecycle()
    val dataRegistro by viewModel.dataRegistro.collectAsStateWithLifecycle()

    // Local subscription intervals selector (Monthly vs Annual)
    var isAnnualPlanSelected by remember { mutableStateOf(false) }

    // Next simulated renewal billing date
    val nextBillingDate = remember(userPlan, dataRegistro) {
        val cal = Calendar.getInstance()
        if (dataRegistro.isNotBlank()) {
            try {
                val sdfInput = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                val cleanDateStr = if (dataRegistro.length >= 10) dataRegistro.substring(0, 10) else dataRegistro
                val parsedDate = sdfInput.parse(cleanDateStr)
                if (parsedDate != null) {
                    cal.time = parsedDate
                }
            } catch (e: Exception) {
                // Keep today's date as fallback
            }
        }
        if (userPlan == "ANUAL") {
            cal.add(Calendar.YEAR, 1)
        } else {
            cal.add(Calendar.MONTH, 1)
        }
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        sdf.format(cal.time)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (premiumAtivo) "Configurações do Plano" else "Assinatura do Plano",
                        fontWeight = FontWeight.Black,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                if (premiumAtivo) {
                    // =====================================================
                    // =============== PREMIUM USER AREA ===================
                    // =====================================================
                    
                    // Subscription Info Details (Beautiful, compact specs sheet styling)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Dados da Assinatura",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            SubscriptionDetailRow(
                                label = "Status",
                                value = if (premiumAtivo) "Ativo" else "Inativo",
                                icon = Icons.Default.Info
                            )

                            SubscriptionDetailRow(
                                label = "Plano",
                                value = if (userPlan == "ANUAL") "PRO • Anual" else "PRO • Mensal",
                                icon = Icons.Default.Verified
                            )

                            SubscriptionDetailRow(
                                label = "Valor",
                                value = if (userPlan == "ANUAL") "R$ 189,90 / ano" else "R$ 19,90 / mês",
                                icon = Icons.Default.ConfirmationNumber
                            )

                            SubscriptionDetailRow(
                                label = "Cobrança",
                                value = nextBillingDate,
                                icon = Icons.Default.CalendarToday
                            )

                            SubscriptionDetailRow(
                                label = "Pagamento",
                                value = "Cartão / Pix",
                                icon = Icons.Default.CreditCard
                            )

                            SubscriptionDetailRow(
                                label = "Renovação",
                                value = "Sim",
                                icon = Icons.Default.Autorenew
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action buttons for active subscriber
                    Button(
                        onClick = {
                            viewModel.obterPortalFaturamentoStripe { result ->
                                when (result) {
                                    is com.priorizedev.brizza.data.api.PortalSessionResult.Success -> {
                                        Toast.makeText(context, "Redirecionando ao Portal Stripe...", Toast.LENGTH_SHORT).show()
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.portalUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Erro ao abrir navegador: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    is com.priorizedev.brizza.data.api.PortalSessionResult.NotConfigured -> {
                                        Toast.makeText(context, "Sem credenciais. Usando modo simulação.", Toast.LENGTH_SHORT).show()
                                        viewModel.verificarAssinaturaStripe { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    is com.priorizedev.brizza.data.api.PortalSessionResult.Error -> {
                                        Toast.makeText(context, "Modo de faturamento local ativo.", Toast.LENGTH_SHORT).show()
                                        viewModel.togglePremiumMock()
                                        Toast.makeText(context, "Modo Simulado: Assinatura gratuita ativada.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gerenciar Assinatura",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.verificarAssinaturaStripe { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        enabled = !stripeChecking
                    ) {
                        if (stripeChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Sincronizando...", fontSize = 14.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sincronizar com Stripe",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                } else {
                    // =====================================================
                    // ================= FREE USER AREA ====================
                    // =====================================================

                    // Header Text
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Eleve sua Gestão",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Faça o upgrade para o BrizaaApp PRO e emita cronogramas de PMOC de forma ágil, segura e profissional.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }

                    // Benefits Card (Modern & Elegant feature panel with detailed spacing)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "O que você terá no plano PRO:",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            val benefitsList = listOf(
                                "Clientes e marcas ILIMITADAS",
                                "Equipamentos e ambientes ilimitados",
                                "Adição de mecânicos e técnicos de campo",
                                "Cronogramas de PMOC liberados",
                                "Exportar relatórios PDF com logotipo",
                                "Nuvem em tempo real e suporte prioritário"
                            )

                            benefitsList.forEach { benefit ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = benefit,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    // Segmented Selector Switcher (Mensal vs Anual) with neat design
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Monthly Option Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (!isAnnualPlanSelected) MaterialTheme.colorScheme.primary 
                                        else Color.Transparent
                                    )
                                    .clickable { isAnnualPlanSelected = false }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Mensal",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (!isAnnualPlanSelected) MaterialTheme.colorScheme.onPrimary 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Annual Option Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isAnnualPlanSelected) MaterialTheme.colorScheme.primary 
                                        else Color.Transparent
                                    )
                                    .clickable { isAnnualPlanSelected = true }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Anual",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isAnnualPlanSelected) MaterialTheme.colorScheme.onPrimary 
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFEF3C7))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "-20%",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Price Presentation Card (Modern minimalist layout)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isAnnualPlanSelected) "PAGAMENTO ANUAL" else "PAGAMENTO MENSAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isAnnualPlanSelected) "R$ 189,90" else "R$ 19,90",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAnnualPlanSelected) "/ano" else "/mês",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            if (isAnnualPlanSelected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Equivale a R$ 15,82/mês • Economize R$ 48,90 ao ano",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Subscribe checkout button
                    Button(
                        onClick = {
                            viewModel.iniciarCheckoutStripe(isAnnualPlan = isAnnualPlanSelected) { result ->
                                when (result) {
                                    is com.priorizedev.brizza.data.api.CheckoutSessionResult.Success -> {
                                        Toast.makeText(context, "Abrindo formulário do Stripe...", Toast.LENGTH_SHORT).show()
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.checkoutUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Erro ao abrir navegador: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    is com.priorizedev.brizza.data.api.CheckoutSessionResult.NotConfigured -> {
                                        viewModel.togglePremiumMock()
                                        Toast.makeText(context, "Demonstração ativa: Novo plano PRO simulado!", Toast.LENGTH_LONG).show()
                                        onBack()
                                    }
                                    is com.priorizedev.brizza.data.api.CheckoutSessionResult.Error -> {
                                        viewModel.togglePremiumMock()
                                        Toast.makeText(context, "Modo simulação ativado: Conta PRO liberada!", Toast.LENGTH_LONG).show()
                                        onBack()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !stripeChecking
                    ) {
                        if (stripeChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Processando...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                    text = "Assinar Plano agora",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionDetailRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (label == "Status") {
            val isAtivo = value == "Ativo"
            val badgeBgColor = if (isAtivo) Color(0xFF10B981) else Color(0xFFEF4444)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBgColor.copy(alpha = 0.15f))
                    .border(1.dp, badgeBgColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = value.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = badgeBgColor,
                    letterSpacing = 0.5.sp
                )
            }
        } else if (label == "Pagamento") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Visa •••• 4242",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
