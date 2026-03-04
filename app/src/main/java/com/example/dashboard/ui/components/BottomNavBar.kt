package com.example.dashboard.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dashboard.ui.navigation.Screen

private data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

private val NAV_ITEMS = listOf(
    NavItem(Screen.Dashboard,    Icons.Default.Home,      "ホーム"),
    NavItem(Screen.Transactions, Icons.Default.List,      "明細"),
    NavItem(Screen.Import,       Icons.Default.Add,       "入力"),
    NavItem(Screen.Budget,       Icons.Default.DateRange, "予算"),
    NavItem(Screen.Settings,     Icons.Default.Settings,  "設定")
)

@Composable
fun BottomNavBar(
    navController: NavController,
    unclassifiedCount: Int = 0
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        NAV_ITEMS.forEach { item ->
            val selected = currentRoute == item.screen.route
            // 「明細」タブに未分類バッジを表示
            val badge = if (item.screen == Screen.Transactions && unclassifiedCount > 0)
                unclassifiedCount else 0

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    if (badge > 0) {
                        BadgedBox(badge = { Badge { Text(badge.toString()) } }) {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    } else {
                        Icon(item.icon, contentDescription = item.label)
                    }
                },
                label = { Text(item.label) }
            )
        }
    }
}
