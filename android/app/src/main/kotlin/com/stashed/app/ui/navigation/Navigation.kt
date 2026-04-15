package com.stashed.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stashed.app.billing.BillingManager
import com.stashed.app.data.local.PreferencesManager
import com.stashed.app.ui.list.MemoryListScreen
import com.stashed.app.ui.onboarding.OnboardingScreen
import com.stashed.app.ui.save.SaveScreen
import com.stashed.app.ui.search.SearchScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class Screen(val route: String, val label: String) {
    data object Onboarding : Screen("onboarding", "Onboarding")
    data object Save : Screen("save", "Stash")
    data object Search : Screen("search", "Search")
    data object List : Screen("list", "All")
}

private val bottomNavItems = listOf(Screen.Save, Screen.Search, Screen.List)

@HiltViewModel
class NavigationViewModel @Inject constructor(
    val preferencesManager: PreferencesManager,
    val billingManager: BillingManager,
) : ViewModel() {
    val isOnboardingComplete = preferencesManager.isOnboardingComplete
        .stateIn(
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main),
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null, // null = loading, don't render yet
        )
}

@Composable
fun StashedNavHost(viewModel: NavigationViewModel = hiltViewModel()) {
    val isOnboardingComplete by viewModel.isOnboardingComplete.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val scope = rememberCoroutineScope()

    // Wait for DataStore to load before rendering
    val onboardingDone = isOnboardingComplete ?: return

    val startDestination = if (onboardingDone) Screen.Save.route else Screen.Onboarding.route
    val showBottomBar = currentDestination?.route != Screen.Onboarding.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        Screen.Save -> Icons.Default.Add
                                        Screen.Search -> Icons.Default.Search
                                        Screen.List -> Icons.Default.List
                                        else -> Icons.Default.Add
                                    },
                                    contentDescription = screen.label,
                                )
                            },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        scope.launch {
                            viewModel.preferencesManager.setOnboardingComplete()
                            navController.navigate(Screen.Save.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    },
                )
            }
            composable(Screen.Save.route) { SaveScreen(innerPadding, viewModel.billingManager) }
            composable(Screen.Search.route) { SearchScreen(innerPadding) }
            composable(Screen.List.route) { MemoryListScreen(innerPadding) }
        }
    }
}
