package com.ayushojha.levain

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ayushojha.levain.ui.bake.BakeScreen
import com.ayushojha.levain.ui.dashboard.DashboardScreen
import com.ayushojha.levain.ui.feeding.FeedingScreen
import com.ayushojha.levain.ui.observation.ObservationScreen
import com.ayushojha.levain.ui.calculator.CalculatorScreen
import com.ayushojha.levain.ui.starter.StarterDetailScreen
import com.ayushojha.levain.ui.starter.StarterEditorScreen
import com.ayushojha.levain.ui.theme.LevainTheme
import com.ayushojha.levain.ui.wizard.StarterWizardScreen
import com.ayushojha.levain.ui.wizard.TroubleshootingScreen

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* dashboard works either way */ }

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
                NavHost(navController = nav, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            onAddStarter = { nav.navigate("wizard") },
                            onOpenStarter = { id -> nav.navigate("starter/$id") },
                            onOpenTools = { nav.navigate("tools") },
                            onOpenTroubleshoot = { nav.navigate("troubleshoot") },
                        )
                    }
                    composable("wizard") {
                        StarterWizardScreen(onDone = { createdId ->
                            nav.popBackStack()
                            createdId?.let { nav.navigate("starter/$it") }
                        })
                    }
                    composable("tools") {
                        CalculatorScreen(onBack = { nav.popBackStack() })
                    }
                    composable("troubleshoot") {
                        TroubleshootingScreen(onBack = { nav.popBackStack() })
                    }
                    composable(
                        "starter/{id}/edit",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        StarterEditorScreen(
                            starterId = entry.arguments!!.getLong("id"),
                            onDone = { nav.popBackStack() },
                        )
                    }
                    composable(
                        "starter/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        val id = entry.arguments!!.getLong("id")
                        StarterDetailScreen(
                            starterId = id,
                            onBack = { nav.popBackStack() },
                            onEdit = { nav.navigate("starter/$id/edit") },
                            onLogFeeding = { nav.navigate("starter/$id/feed") },
                            onLogObservation = { nav.navigate("starter/$id/observe") },
                            onLogBake = { nav.navigate("starter/$id/bake") },
                        )
                    }
                    composable(
                        "starter/{id}/feed",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        FeedingScreen(
                            starterId = entry.arguments!!.getLong("id"),
                            onDone = { nav.popBackStack() },
                        )
                    }
                    composable(
                        "starter/{id}/observe",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        ObservationScreen(
                            starterId = entry.arguments!!.getLong("id"),
                            onDone = { nav.popBackStack() },
                        )
                    }
                    composable(
                        "starter/{id}/bake",
                        arguments = listOf(navArgument("id") { type = NavType.LongType }),
                    ) { entry ->
                        BakeScreen(
                            starterId = entry.arguments!!.getLong("id"),
                            onDone = { nav.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
