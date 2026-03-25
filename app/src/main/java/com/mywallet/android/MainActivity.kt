package com.mywallet.android

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mywallet.android.data.repository.AuthRepository
import com.mywallet.android.ui.navigation.AppNavGraph
import com.mywallet.android.ui.navigation.Screen
import com.mywallet.android.ui.theme.MyWalletTheme
import com.mywallet.android.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val systemDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        var authChecked by mutableStateOf(false)
        var startDestination by mutableStateOf(Screen.Login.route)

        splashScreen.setKeepOnScreenCondition { !authChecked }

        lifecycleScope.launch {
            val restored = authRepository.tryRestoreSession()
            startDestination = if (restored) Screen.Home.route else Screen.Login.route
            authChecked = true
        }

        setContent {
            val preferences by dataStore.data.collectAsState(initial = null)
            val themeMode = preferences?.get(THEME_MODE_KEY)
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDarkTheme
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            MyWalletTheme(darkTheme = isDarkTheme) {
                if (authChecked) {
                    MyWalletApp(
                        startDestination = startDestination,
                        themeMode = themeMode,
                        onThemeModeChange = { mode ->
                            lifecycleScope.launch {
                                dataStore.edit { it[THEME_MODE_KEY] = mode.name }
                            }
                        },
                    )
                }
            }
        }
    }

    companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}

@Composable
fun MyWalletApp(
    startDestination: String,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.Home.route,
            label = "Dashboard",
            selectedIcon = Icons.Filled.Dashboard,
            unselectedIcon = Icons.Outlined.Dashboard,
        ),
        BottomNavItem(
            route = Screen.Reports.route,
            label = "Reports",
            selectedIcon = Icons.Filled.Description,
            unselectedIcon = Icons.Outlined.Description,
        ),
        BottomNavItem(
            route = Screen.Subscriptions.route,
            label = "Subs",
            selectedIcon = Icons.Filled.CreditCard,
            unselectedIcon = Icons.Outlined.CreditCard,
        ),
        BottomNavItem(
            route = Screen.NetWorth.route,
            label = "Net Worth",
            selectedIcon = Icons.Filled.AccountBalance,
            unselectedIcon = Icons.Outlined.AccountBalance,
        ),
        BottomNavItem(
            route = Screen.Profile.route,
            label = "Profile",
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
        ),
    )

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavGraph(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
        )
    }
}
