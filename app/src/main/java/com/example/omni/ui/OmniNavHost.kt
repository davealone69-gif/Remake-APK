package com.example.omni.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

sealed class OmniScreen(val route: String, val title: String, val icon: ImageVector) {
    object CommandCentre : OmniScreen("command_centre", "Command Centre", Icons.Filled.Psychology)
    object ProjectBuilder : OmniScreen("project_builder", "Project Builder", Icons.Filled.FolderZip)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: CommandCentreViewModel = viewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val screens = listOf(OmniScreen.CommandCentre, OmniScreen.ProjectBuilder)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "OMNI SWARM BUILDER",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
                )
                HorizontalDivider()
                screens.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(OmniScreen.CommandCentre.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(OmniScreen.CommandCentre.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = OmniScreen.CommandCentre.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(OmniScreen.CommandCentre.route) {
                    CommandCentreScreen(
                        viewModel = viewModel,
                        onNavigateToProjectBuilder = { navController.navigate(OmniScreen.ProjectBuilder.route) },
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
                composable(OmniScreen.ProjectBuilder.route) {
                    ProjectBuilderScreen(
                        viewModel = viewModel,
                        onNavigateToCommandCentre = { navController.navigate(OmniScreen.CommandCentre.route) },
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
            }
        }
    }
}
