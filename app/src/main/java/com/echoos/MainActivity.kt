package com.echoos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.echoos.ui.screens.CreateAutomationScreen
import com.echoos.ui.screens.LoginScreen
import com.echoos.ui.screens.ProfileScreen
import com.echoos.ui.screens.DashboardScreen
import com.echoos.ui.screens.HistoryScreen
import com.echoos.ui.screens.IntelligenceScreen
import com.echoos.ui.screens.PermissionCenterScreen
import com.echoos.ui.screens.PlannerScreen
import com.echoos.ui.theme.EchoTheme
import com.echoos.viewmodel.EchoViewModel

class MainActivity : ComponentActivity() {

    private val vm: EchoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.checkBackend()
        setContent {
            EchoTheme {
                val user by vm.user.collectAsState()
                if (user == null) {
                    // Not signed in → local login screen (no server, no password).
                    LoginScreen(vm)
                    return@EchoTheme
                }
                val nav = rememberNavController()
                val backStack by nav.currentBackStackEntryAsState()
                val route = backStack?.destination?.route

                val tabs = listOf(
                    Triple("dashboard", "Home", Icons.Filled.Home),
                    Triple("create", "Create", Icons.Filled.AutoAwesome),
                    Triple("intelligence", "EchoLens", Icons.Filled.Lightbulb),
                    Triple("planner", "Planner", Icons.Filled.Today),
                    Triple("permissions", "Control", Icons.Filled.Security),
                    Triple("profile", "You", Icons.Filled.Person),
                )

                Scaffold(bottomBar = {
                    NavigationBar {
                        tabs.forEach { (r, label, icon) ->
                            NavigationBarItem(
                                selected = route == r,
                                onClick = {
                                    nav.navigate(r) {
                                        popUpTo("dashboard"); launchSingleTop = true
                                    }
                                },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) })
                        }
                    }
                }) { padding ->
                    NavHost(nav, startDestination = "dashboard",
                        modifier = Modifier.padding(padding)) {
                        composable("dashboard") {
                            DashboardScreen(vm, onOpenHistory = { nav.navigate("history") })
                        }
                        composable("create") { CreateAutomationScreen(vm) }
                        composable("intelligence") { IntelligenceScreen(vm) }
                        composable("planner") { PlannerScreen(vm) }
                        composable("permissions") { PermissionCenterScreen(vm) }
                        composable("profile") { ProfileScreen(vm) }
                        composable("history") { HistoryScreen(vm) }
                    }
                }
            }
        }
    }
}
