package com.aa.autoresponder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aa.autoresponder.ui.screens.DeveloperScreen
import com.aa.autoresponder.ui.screens.HomeScreen
import com.aa.autoresponder.ui.screens.LogsScreen
import com.aa.autoresponder.ui.screens.RulesScreen
import com.aa.autoresponder.ui.screens.SettingsScreen
import com.aa.autoresponder.ui.theme.AppThemeMode
import com.aa.autoresponder.ui.theme.AutoResponderTheme
import com.aa.autoresponder.util.Prefs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                val ctx = LocalContext.current
                val themeModeStr by Prefs.themeMode(ctx).collectAsState(initial = "SYSTEM")
                val themeMode = runCatching { AppThemeMode.valueOf(themeModeStr) }.getOrDefault(AppThemeMode.SYSTEM)

                AutoResponderTheme(mode = themeMode) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        AppRoot()
                    }
                }
            }
        }
    }
}

private data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val navItems = listOf(
    NavItem("home", "الرئيسية", Icons.Default.Home),
    NavItem("rules", "قواعد الردود", Icons.Default.List),
    NavItem("logs", "السجل", Icons.Default.History),
    NavItem("settings", "الإعدادات", Icons.Default.Settings),
    NavItem("developer", "معلومات المطور", Icons.Default.Code),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val navController: NavHostController = rememberNavController()
    var menuExpanded by remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"
    val currentLabel = navItems.firstOrNull { it.route == currentRoute }?.label ?: "الرد التلقائي"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(currentLabel, fontWeight = FontWeight.SemiBold) },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "القائمة")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            navItems.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.label) },
                                    leadingIcon = { Icon(item.icon, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            enterTransition = {
                androidx.compose.animation.fadeIn(tween(280))
            },
            exitTransition = {
                androidx.compose.animation.fadeOut(tween(180))
            }
        ) {
            composable("home") { HomeScreen() }
            composable("rules") { RulesScreen() }
            composable("logs") { LogsScreen() }
            composable("settings") { SettingsScreen() }
            composable("developer") { DeveloperScreen() }
        }
    }
}