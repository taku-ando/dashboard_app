package com.example.dashboard.ui.navigation

/**
 * ボトムナビゲーションおよび画面間遷移で使用するルート定義。
 */
sealed class Screen(val route: String) {
    object Dashboard    : Screen("kakeibo/dashboard")
    object Transactions : Screen("kakeibo/transactions")
    object Import       : Screen("kakeibo/import")
    object AddTransaction : Screen("kakeibo/add")
    object Budget       : Screen("kakeibo/budget")
    object Settings     : Screen("kakeibo/settings")
}
