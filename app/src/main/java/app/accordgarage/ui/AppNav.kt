package app.accordgarage.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.accordgarage.data.Container
import app.accordgarage.ui.screens.AboutScreen
import app.accordgarage.ui.screens.BackupScreen
import app.accordgarage.ui.screens.DocumentEditScreen
import app.accordgarage.ui.screens.DocumentsScreen
import app.accordgarage.ui.screens.FuelEditScreen
import app.accordgarage.ui.screens.GuidesScreen
import app.accordgarage.ui.screens.HomeScreen
import app.accordgarage.ui.screens.JobScreen
import app.accordgarage.ui.screens.LogScreen
import app.accordgarage.ui.screens.MaintenanceScreen
import app.accordgarage.ui.screens.ManualPageScreen
import app.accordgarage.ui.screens.ManualScreen
import app.accordgarage.ui.screens.MinderScreen
import app.accordgarage.ui.screens.MoreScreen
import app.accordgarage.ui.screens.OilEditScreen
import app.accordgarage.ui.screens.RecallsScreen
import app.accordgarage.ui.screens.RoadsideScreen
import app.accordgarage.ui.screens.ServiceEditScreen
import app.accordgarage.ui.screens.SpecsScreen
import app.accordgarage.ui.screens.TutorialScreen
import app.accordgarage.ui.screens.WarningLightScreen
import app.accordgarage.ui.screens.WarningLightsScreen
import app.accordgarage.ui.theme.Inter
import app.accordgarage.ui.theme.LocalGarage

val LocalContainer = staticCompositionLocalOf<Container> { error("No container") }

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Garage", Icons.Outlined.Speed),
    Tab("log", "Log", Icons.Outlined.History),
    Tab("manual", "Manual", Icons.AutoMirrored.Outlined.MenuBook),
    Tab("guides", "Guides", Icons.Outlined.Build),
    Tab("more", "More", Icons.Outlined.MoreHoriz),
)

/** Opens a web page or another app; shows a short message if nothing can handle it. */
fun Context.openLink(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No app can open that link", Toast.LENGTH_SHORT).show()
    }
}

fun NavHostController.manualSearch(query: String) = navigate("manual?q=${Uri.encode(query)}")

@Composable
fun AppNav(lightTheme: Boolean, onToggleTheme: (Boolean) -> Unit) {
    val nav = rememberNavController()
    val g = LocalGarage.current
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route?.substringBefore('?')
    val showBar = tabs.any { it.route == route }

    Scaffold(
        containerColor = g.bg,
        bottomBar = {
            if (showBar) {
                Column {
                    HorizontalDivider(color = g.surface2, thickness = 1.dp)
                    NavigationBar(containerColor = g.bar, tonalElevation = 0.dp, modifier = Modifier.height(72.dp)) {
                        tabs.forEach { tab ->
                            val selected = route == tab.route
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
                                label = { Text(tab.label, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = g.accent,
                                    selectedTextColor = g.accent,
                                    unselectedIconColor = g.muted,
                                    unselectedTextColor = g.muted,
                                    indicatorColor = g.accent.copy(alpha = 0.12f),
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().background(g.bg).padding(inner)) {
            NavHost(nav, startDestination = "home") {
                composable("home") { HomeScreen(nav) }
                composable("log?tab={tab}", arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })) {
                    LogScreen(nav, it.arguments?.getInt("tab") ?: 0)
                }
                composable("manual?q={q}", arguments = listOf(navArgument("q") { type = NavType.StringType; defaultValue = "" })) {
                    ManualScreen(nav, it.arguments?.getString("q") ?: "")
                }
                composable("guides") { GuidesScreen(nav) }
                composable("more") { MoreScreen(nav, lightTheme, onToggleTheme) }

                composable(
                    "service/{id}?jobs={jobs}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("jobs") { type = NavType.StringType; defaultValue = "" },
                    ),
                ) { ServiceEditScreen(nav, it.arguments?.getLong("id") ?: 0L, it.arguments?.getString("jobs") ?: "") }
                composable("fuel/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                    FuelEditScreen(nav, it.arguments?.getLong("id") ?: 0L)
                }
                composable("oil/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                    OilEditScreen(nav, it.arguments?.getLong("id") ?: 0L)
                }
                composable("minder") { MinderScreen(nav) }
                composable("maintenance") { MaintenanceScreen(nav) }
                composable("job/{jobId}", arguments = listOf(navArgument("jobId") { type = NavType.StringType })) {
                    JobScreen(nav, it.arguments?.getString("jobId") ?: "")
                }
                composable(
                    "page/{docId}/{page}?q={q}",
                    arguments = listOf(
                        navArgument("docId") { type = NavType.LongType },
                        navArgument("page") { type = NavType.IntType },
                        navArgument("q") { type = NavType.StringType; defaultValue = "" },
                    ),
                ) {
                    ManualPageScreen(nav, it.arguments?.getLong("docId") ?: 0L, it.arguments?.getInt("page") ?: 1, it.arguments?.getString("q") ?: "")
                }
                composable("tutorial/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    TutorialScreen(nav, it.arguments?.getString("id") ?: "")
                }
                composable("recalls") { RecallsScreen(nav) }
                composable("documents") { DocumentsScreen(nav) }
                composable("document/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                    DocumentEditScreen(nav, it.arguments?.getLong("id") ?: 0L)
                }
                composable("specs") { SpecsScreen(nav) }
                composable("roadside") { RoadsideScreen(nav) }
                composable("lights") { WarningLightsScreen(nav) }
                composable("light/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                    WarningLightScreen(nav, it.arguments?.getString("id") ?: "")
                }
                composable("backup") { BackupScreen(nav) }
                composable("about") { AboutScreen(nav) }
            }
        }
    }
}
