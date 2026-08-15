package com.ayushojha.levain

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ayushojha.levain.ui.bake.BakeHomeScreen
import com.ayushojha.levain.ui.bake.BakeScreen
import com.ayushojha.levain.ui.bake.LiveBakeScreen
import com.ayushojha.levain.ui.calculator.CalculatorScreen
import com.ayushojha.levain.ui.dashboard.DashboardScreen
import com.ayushojha.levain.ui.feeding.FeedingScreen
import com.ayushojha.levain.ui.more.MoreScreen
import com.ayushojha.levain.ui.observation.ObservationScreen
import com.ayushojha.levain.ui.settings.SettingsScreen
import com.ayushojha.levain.ui.starter.StarterDetailScreen
import com.ayushojha.levain.ui.starter.StarterEditorScreen
import com.ayushojha.levain.ui.theme.LevainTheme
import com.ayushojha.levain.ui.wizard.StarterWizardScreen
import com.ayushojha.levain.ui.wizard.TroubleshootingScreen

/** The three places the app is about. */
private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Bake("tab/bake", "Bake", Icons.Filled.LocalFireDepartment),
    Starters("tab/starters", "Starters", Icons.Filled.Spa),
    More("tab/more", "More", Icons.Filled.MoreHoriz),
}

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* the app works either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            LevainTheme {
                val nav = rememberNavController()
                val entry by nav.currentBackStackEntryAsState()
                val route = entry?.destination?.hierarchy?.firstOrNull()?.route
                val onTab = Tab.entries.any { it.route == route }

                Scaffold(
                    bottomBar = { if (onTab) BottomBar(nav) },
                    containerColor = MaterialTheme.colorScheme.background,
                ) { padding ->
                    NavHost(
                        navController = nav,
                        startDestination = Tab.Bake.route,
                        modifier = Modifier.padding(padding),
                    ) {
                        composable(Tab.Bake.route) {
                            BakeHomeScreen(onOpenBake = { id -> nav.navigate("bake/$id") })
                        }

                        composable(Tab.Starters.route) {
                            DashboardScreen(
                                onAddStarter = { nav.navigate("wizard") },
                                onOpenStarter = { id -> nav.navigate("starter/$id") },
                                onOpenTools = { nav.navigate("tools") },
                                onOpenTroubleshoot = { nav.navigate("troubleshoot") },
                                onOpenSettings = { nav.navigate("settings") },
                                onFirstRun = { nav.navigate("wizard") },
                            )
                        }

                        composable(Tab.More.route) {
                            MoreScreen(
                                onOpenTools = { nav.navigate("tools") },
                                onOpenDoctor = { nav.navigate("troubleshoot") },
                                onOpenBackup = { nav.navigate("settings") },
                            )
                        }

                        composable(
                            "bake/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType }),
                        ) { e ->
                            LiveBakeScreen(
                                bakeId = e.arguments!!.getLong("id"),
                                onBack = { nav.popBackStack() },
                                onFinished = { nav.popBackStack() },
                            )
                        }

                        composable("wizard") {
                            StarterWizardScreen(onDone = { createdId ->
                                nav.popBackStack()
                                createdId?.let { nav.navigate("starter/$it") }
                            })
                        }
                        composable("tools") { CalculatorScreen(onBack = { nav.popBackStack() }) }
                        composable("troubleshoot") { TroubleshootingScreen(onBack = { nav.popBackStack() }) }
                        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }

                        composable("starter/new") {
                            StarterEditorScreen(starterId = null, onDone = { nav.popBackStack() })
                        }
                        composable(
                            "starter/{id}/edit",
                            arguments = listOf(navArgument("id") { type = NavType.LongType }),
                        ) { e ->
                            StarterEditorScreen(
                                starterId = e.arguments!!.getLong("id"),
                                onDone = { nav.popBackStack() },
                            )
                        }
                        composable(
                            "starter/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType }),
                        ) { e ->
                            val id = e.arguments!!.getLong("id")
                            StarterDetailScreen(
                                starterId = id,
                                onBack = { nav.popBackStack() },
                                onEdit = { nav.navigate("starter/$id/edit") },
                                onLogFeeding = { nav.navigate("starter/$id/feed") },
                                onEditFeeding = { fId -> nav.navigate("starter/$id/feed?feedingId=$fId") },
                                onLogObservation = { nav.navigate("starter/$id/observe") },
                                onLogBake = { nav.navigate("starter/$id/bake") },
                            )
                        }
                        composable(
                            "starter/{id}/feed?feedingId={feedingId}",
                            arguments = listOf(
                                navArgument("id") { type = NavType.LongType },
                                navArgument("feedingId") { type = NavType.LongType; defaultValue = -1L },
                            ),
                        ) { e ->
                            FeedingScreen(
                                starterId = e.arguments!!.getLong("id"),
                                feedingId = e.arguments!!.getLong("feedingId").takeIf { it >= 0 },
                                onDone = { nav.popBackStack() },
                            )
                        }
                        composable(
                            "starter/{id}/observe",
                            arguments = listOf(navArgument("id") { type = NavType.LongType }),
                        ) { e ->
                            ObservationScreen(
                                starterId = e.arguments!!.getLong("id"),
                                onDone = { nav.popBackStack() },
                            )
                        }
                        composable(
                            "starter/{id}/bake",
                            arguments = listOf(navArgument("id") { type = NavType.LongType }),
                        ) { e ->
                            BakeScreen(
                                starterId = e.arguments!!.getLong("id"),
                                onDone = { nav.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController) {
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        Tab.entries.forEach { tab ->
            val selected = current?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
