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
import com.firstlimo.finance.ui.screens.*
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.ui.viewmodel.AuthViewModel
import com.firstlimo.finance.ui.viewmodel.FinancialViewModel
import com.firstlimo.finance.utils.BiometricHelper

class MainActivity : FragmentActivity() {

    private val financialViewModel: FinancialViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FirstLimoFinanceTheme {
                val isBiometricEnabled = remember { BiometricHelper.isBiometricEnabled(this@MainActivity) }
                var isAuthenticatedByBiometric by remember { mutableStateOf(!isBiometricEnabled) }

                LaunchedEffect(Unit) {
                    if (isBiometricEnabled && !isAuthenticatedByBiometric) {
                        BiometricHelper.showBiometricPrompt(
                            activity = this@MainActivity,
                            onSuccess = { isAuthenticatedByBiometric = true },
                            onError = { }
                        )
                    }
                }

                if (isBiometricEnabled && !isAuthenticatedByBiometric) {
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
                                    BiometricHelper.showBiometricPrompt(
                                        activity = this@MainActivity,
                                        onSuccess = { isAuthenticatedByBiometric = true },
                                        onError = { }
                                    )
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

                    LaunchedEffect(currentUserId) {
                        if (currentUserId != null) {
                            financialViewModel.setUserId(currentUserId)
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
                                    navController.navigate("dashboard") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = financialViewModel,
                                onNavigateToHistory = {
                                    navController.navigate("history")
                                },
                                onNavigateToAnalytics = {
                                    navController.navigate("analytics")
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                }
                            )
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
}
