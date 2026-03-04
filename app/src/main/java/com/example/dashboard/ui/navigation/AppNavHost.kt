package com.example.dashboard.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dashboard.ui.AppViewModel
import com.example.dashboard.ui.components.BottomNavBar
import com.example.dashboard.ui.kakeibo.add.AddTransactionScreen
import com.example.dashboard.ui.kakeibo.budget.BudgetScreen
import com.example.dashboard.ui.kakeibo.dashboard.DashboardScreen
import com.example.dashboard.ui.kakeibo.csvimport.ImportScreen
import com.example.dashboard.ui.kakeibo.settings.SettingsScreen
import com.example.dashboard.ui.kakeibo.transactions.TransactionsScreen

@Composable
fun AppNavHost(
    appViewModel: AppViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val unclassifiedCount by appViewModel.unclassifiedCount.collectAsState()

    Scaffold(
        bottomBar = {
            BottomNavBar(
                navController = navController,
                unclassifiedCount = unclassifiedCount
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }
            composable(Screen.Transactions.route) {
                TransactionsScreen()
            }
            composable(Screen.Import.route) {
                ImportScreen()
            }
            composable(Screen.AddTransaction.route) {
                AddTransactionScreen(navController = navController)
            }
            composable(Screen.Budget.route) {
                BudgetScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
