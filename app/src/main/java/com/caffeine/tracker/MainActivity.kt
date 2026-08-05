package com.caffeine.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.glance.appwidget.updateAll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.caffeine.tracker.data.repository.SettingsRepository
import com.caffeine.tracker.ui.adddrink.AddDrinkScreen
import com.caffeine.tracker.ui.adddrink.AddDrinkViewModel
import com.caffeine.tracker.ui.history.HistoryScreen
import com.caffeine.tracker.ui.history.HistoryViewModel
import com.caffeine.tracker.ui.home.HomeScreen
import com.caffeine.tracker.ui.home.HomeViewModel
import com.caffeine.tracker.ui.navigation.Screen
import com.caffeine.tracker.ui.settings.SettingsScreen
import com.caffeine.tracker.ui.stats.StatsScreen
import com.caffeine.tracker.ui.stats.StatsViewModel
import com.caffeine.tracker.ui.theme.CaffeineTrackerTheme
import com.caffeine.tracker.worker.WidgetUpdateWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WidgetUpdateWorker.enqueue(this)

        setContent {
            CaffeineTrackerTheme {
                MainApp(settingsRepository)
            }
        }
    }
}

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen,
)

private val bottomNavItems = listOf(
    BottomNavItem("首页", Icons.Default.Home, Screen.Home),
    BottomNavItem("历史", Icons.Default.List, Screen.History),
    BottomNavItem("统计", Icons.Default.BarChart, Screen.Stats),
    BottomNavItem("设置", Icons.Default.Settings, Screen.Settings),
)

@Composable
fun MainApp(settingsRepository: SettingsRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val vm: HomeViewModel = hiltViewModel()
                HomeScreen(
                    onAddClick = { navController.navigate(Screen.AddDrink.route) },
                    viewModel = vm,
                )
            }
            composable(Screen.AddDrink.route) {
                val vm: AddDrinkViewModel = hiltViewModel()
                val context = androidx.compose.ui.platform.LocalContext.current
                val scope = rememberCoroutineScope()
                AddDrinkScreen(
                    viewModel = vm,
                    onSaved = {
                        scope.launch {
                            com.caffeine.tracker.widget.GlanceCaffeineWidget().updateAll(context)
                        }
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.History.route) {
                val vm: HistoryViewModel = hiltViewModel()
                HistoryScreen(viewModel = vm)
            }
            composable(Screen.Stats.route) {
                val vm: StatsViewModel = hiltViewModel()
                StatsScreen(viewModel = vm)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(settingsRepository = settingsRepository)
            }
        }
    }
}
