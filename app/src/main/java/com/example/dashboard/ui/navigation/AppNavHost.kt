package com.example.dashboard.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dashboard.ui.AppViewModel
import com.example.dashboard.ui.components.BottomNavBar

/**
 * アプリ全体の NavGraph。
 * 各画面 Composable は Step 11〜16 で実装予定。
 */
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
                PlaceholderScreen("ダッシュボード（Step 11で実装）")
            }
            composable(Screen.Transactions.route) {
                PlaceholderScreen("取引一覧（Step 12で実装）")
            }
            composable(Screen.Import.route) {
                PlaceholderScreen("CSVインポート（Step 13で実装）")
            }
            composable(Screen.AddTransaction.route) {
                PlaceholderScreen("手動入力（Step 14で実装）")
            }
            composable(Screen.Budget.route) {
                PlaceholderScreen("予算管理（Step 15で実装）")
            }
            composable(Screen.Settings.route) {
                PlaceholderScreen("設定（Step 16で実装）")
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(label)
    }
}
