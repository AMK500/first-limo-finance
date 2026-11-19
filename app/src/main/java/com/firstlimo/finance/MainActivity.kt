package com.firstlimo.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.firstlimo.finance.ui.screens.AuthScreen
import com.firstlimo.finance.ui.screens.DashboardScreen
import com.firstlimo.finance.ui.screens.SettingsScreen
import com.firstlimo.finance.ui.screens.TransactionsHistoryScreen
import com.firstlimo.finance.ui.theme.FirstLimoFinanceTheme
import com.firstlimo.finance.ui.viewmodel.AuthViewModel
import com.firstlimo.finance.ui.viewmodel.FinancialViewModel

class MainActivity : ComponentActivity() {

    private val financialViewModel: FinancialViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FirstLimoFinanceTheme {
                val navController = rememberNavController()
                val currentUserId = authViewModel.currentUserId

                // إذا كان المستخدم مسجل مسبقاً، نربط حسابه بالمحرك المالي
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
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            }
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
