package com.firstlimo.finance

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.firstlimo.finance.data.local.CustomerEntity
import com.firstlimo.finance.ui.screens.*
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.ui.viewmodel.AuthViewModel
import com.firstlimo.finance.ui.viewmodel.DebtsViewModel
import com.firstlimo.finance.ui.viewmodel.FinancialViewModel
import com.firstlimo.finance.utils.BiometricHelper

class MainActivity : FragmentActivity() {

    companion object {
        var isInternalActionActive = false
    }

    private val financialViewModel: FinancialViewModel by viewModels()
    private val debtsViewModel: DebtsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    // حالة فتح التطبيق بالبصمة
    private val isAppUnlocked = mutableStateOf(false)
    private var isPromptShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BiometricHelper.init(this)

        if (!BiometricHelper.isBiometricEnabled(this)) {
            isAppUnlocked.value = true
        }

        setContent {
            FirstLimoFinanceTheme {
                val isBiometricEnabled by BiometricHelper.isBiometricEnabledFlow.collectAsState()
                val unlocked by isAppUnlocked

                val needsLock = isBiometricEnabled && !unlocked

                if (needsLock) {
                    // شاشة القفل بالبصمة
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(EmeraldCollection.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = EmeraldCollection,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Text(
                                text = "التطبيق مقفل لحماية بياناتك",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "يرجى تأكيد هويتك باستخدام البصمة أو رمز الهاتف",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    promptBiometric()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldCollection)
                            ) {
                                Text("إلغاء القفل بالبصمة", color = DarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    val navController = rememberNavController()
                    val currentUserId = authViewModel.currentUserId
                    var activeCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

                    LaunchedEffect(currentUserId) {
                        if (currentUserId != null) {
                            financialViewModel.setUserId(currentUserId)
                            debtsViewModel.setUserId(currentUserId)
                        }
                    }

                    val startDestination = if (currentUserId != null) "dashboard" else "auth"

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable("auth") {
                            AuthScreen(
                                authViewModel = authViewModel,
                                onAuthSuccess = { uid ->
                                    financialViewModel.setUserId(uid)
                                    debtsViewModel.setUserId(uid)
                                    navController.navigate("dashboard") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("dashboard") {
                            MainScreen(
                                financialViewModel = financialViewModel,
                                debtsViewModel = debtsViewModel,
                                authViewModel = authViewModel,
                                onNavigateToCustomerDetail = { customer ->
                                    activeCustomer = customer
                                    debtsViewModel.selectCustomer(customer.id)
                                    navController.navigate("customer_detail")
                                },
                                onNavigateToHistory = {
                                    navController.navigate("history")
                                },
                                onNavigateToAnalytics = {
                                    navController.navigate("analytics")
                                },
                                onLoggedOut = {
                                    navController.navigate("auth") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("customer_detail") {
                            if (activeCustomer != null) {
                                CustomerDetailScreen(
                                    customer = activeCustomer!!,
                                    viewModel = debtsViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            } else {
                                navController.popBackStack()
                            }
                        }

                        composable("analytics") {
                            AnalyticsScreen(
                                viewModel = financialViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("history") {
                            TransactionsHistoryScreen(
                                viewModel = financialViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                financialViewModel = financialViewModel,
                                authViewModel = authViewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateToAnalytics = {
                                    navController.navigate("analytics")
                                },
                                onLoggedOut = {
                                    navController.navigate("auth") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (BiometricHelper.isBiometricEnabled(this)) {
            if (!isAppUnlocked.value) {
                promptBiometric()
            }
        } else {
            isAppUnlocked.value = true
        }
    }

    override fun onStop() {
        super.onStop()
        // عند مغادرة التطبيق (الخروج للشاشة الرئيسية أو تطبيق آخر دون إغلاقه نهائياً)، يعود لطلب البصمة
        if (!isInternalActionActive && BiometricHelper.isBiometricEnabled(this)) {
            isAppUnlocked.value = false
        }
    }

    private fun promptBiometric() {
        if (isPromptShowing) return
        isPromptShowing = true
        BiometricHelper.showBiometricPrompt(
            activity = this,
            onSuccess = {
                isAppUnlocked.value = true
                isPromptShowing = false
            },
            onError = {
                isPromptShowing = false
            }
        )
    }
}
