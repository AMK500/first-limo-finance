package com.firstlimo.finance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlimo.finance.data.local.CustomerEntity
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.ui.viewmodel.AuthViewModel
import com.firstlimo.finance.ui.viewmodel.DebtsViewModel
import com.firstlimo.finance.ui.viewmodel.FinancialViewModel

enum class MainTab(val title: String) {
    DEBTS("دفتر الديون"),
    CASH("دفتر النقدية"),
    MORE("المزيد")
}

@Composable
fun MainScreen(
    financialViewModel: FinancialViewModel,
    debtsViewModel: DebtsViewModel,
    authViewModel: AuthViewModel,
    onNavigateToCustomerDetail: (CustomerEntity) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onLoggedOut: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(MainTab.CASH) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                // تبويب دفتر الديون
                NavigationBarItem(
                    selected = selectedTab == MainTab.DEBTS,
                    onClick = { selectedTab = MainTab.DEBTS },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == MainTab.DEBTS) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                            contentDescription = "دفتر الديون"
                        )
                    },
                    label = {
                        Text(
                            text = "دفتر الديون",
                            fontWeight = if (selectedTab == MainTab.DEBTS) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentBlue,
                        selectedTextColor = AccentBlue,
                        indicatorColor = AccentBlue.copy(alpha = 0.15f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                // تبويب دفتر النقدية (المالية)
                NavigationBarItem(
                    selected = selectedTab == MainTab.CASH,
                    onClick = { selectedTab = MainTab.CASH },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == MainTab.CASH) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "دفتر النقدية"
                        )
                    },
                    label = {
                        Text(
                            text = "دفتر النقدية",
                            fontWeight = if (selectedTab == MainTab.CASH) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentGold,
                        selectedTextColor = AccentGold,
                        indicatorColor = AccentGold.copy(alpha = 0.15f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                // تبويب المزيد (الإعدادات)
                NavigationBarItem(
                    selected = selectedTab == MainTab.MORE,
                    onClick = { selectedTab = MainTab.MORE },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == MainTab.MORE) Icons.Filled.GridView else Icons.Outlined.GridView,
                            contentDescription = "المزيد"
                        )
                    },
                    label = {
                        Text(
                            text = "المزيد",
                            fontWeight = if (selectedTab == MainTab.MORE) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldCollection,
                        selectedTextColor = EmeraldCollection,
                        indicatorColor = EmeraldCollection.copy(alpha = 0.15f),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                MainTab.DEBTS -> {
                    DebtsScreen(
                        viewModel = debtsViewModel,
                        onCustomerClick = onNavigateToCustomerDetail
                    )
                }
                MainTab.CASH -> {
                    DashboardScreen(
                        viewModel = financialViewModel,
                        onNavigateToHistory = onNavigateToHistory,
                        onNavigateToAnalytics = onNavigateToAnalytics,
                        onNavigateToSettings = { selectedTab = MainTab.MORE }
                    )
                }
                MainTab.MORE -> {
                    SettingsScreen(
                        financialViewModel = financialViewModel,
                        authViewModel = authViewModel,
                        onBack = { selectedTab = MainTab.CASH },
                        onNavigateToAnalytics = onNavigateToAnalytics,
                        onLoggedOut = onLoggedOut
                    )
                }
            }
        }
    }
}
