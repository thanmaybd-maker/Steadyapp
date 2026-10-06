package com.thanu.steady.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.thanu.steady.di.AppContainer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Composable
fun SteadyAppNavigation(appContainer: AppContainer) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                val screens = listOf("Today", "Break", "Safety", "Review", "Settings")
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Text(screen.first().toString()) },
                        label = { Text(screen) },
                        selected = currentDestination?.route == screen,
                        onClick = {
                            navController.navigate(screen) {
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
            startDestination = "Today",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("Today") { 
                val todayViewModel: TodayViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return TodayViewModel({ appContainer.database }) as T
                        }
                    }
                )
                TodayScreen(viewModel = todayViewModel) 
            }
            composable("Break") { 
                val breakViewModel: BreakViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return BreakViewModel(appContainer.timerRepository, appContainer.alarmAdapter,
                                appContainer.notificationAdapter, appContainer.preferencesRepository) as T
                        }
                    }
                )
                BreakScreen(viewModel = breakViewModel) 
            }
            composable("Safety") { 
                val safetyViewModel: SafetyViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SafetyViewModel(appContainer.privateSafetyRepository, appContainer.clock) as T
                        }
                    }
                )
                SafetyScreen(viewModel = safetyViewModel) 
            }
            composable("Review") { 
                val reviewViewModel: ReviewViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ReviewViewModel({ appContainer.database }) as T
                        }
                    }
                )
                ReviewScreen(viewModel = reviewViewModel) 
            }
            composable("Settings") { 
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SettingsViewModel(appContainer.recoveryRepository, appContainer.documentAdapter,
                                appContainer.alarmAdapter, appContainer.notificationAdapter, appContainer.clock,
                                appContainer::deleteLocalData) as T
                        }
                    }
                )
                SettingsScreen(viewModel = settingsViewModel, onImportCompleted = {
                    listOf("Today", "Break", "Safety", "Review", "Settings").forEach { navController.clearBackStack(it) }
                    navController.navigate("Today") {
                        popUpTo(navController.graph.id) { inclusive = false; saveState = false }
                        launchSingleTop = true
                    }
                })
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title)
    }
}
