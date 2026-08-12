package com.caffeine.tracker.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddDrink : Screen("add_drink")
    data object AddBackfill : Screen("add_backfill")
    data object History : Screen("history")
    data object Stats : Screen("stats")
    data object Settings : Screen("settings")
}
