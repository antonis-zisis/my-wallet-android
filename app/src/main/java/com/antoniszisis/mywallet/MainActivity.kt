package com.antoniszisis.mywallet

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.antoniszisis.mywallet.data.repository.AuthRepository
import com.antoniszisis.mywallet.ui.navigation.AppNavGraph
import com.antoniszisis.mywallet.ui.navigation.Screen
import com.antoniszisis.mywallet.ui.theme.LocalHideAmounts
import com.antoniszisis.mywallet.ui.theme.MyWalletTheme
import com.antoniszisis.mywallet.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import javax.inject.Inject

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/** Sentinel route for the "More" bottom nav item — it opens [ModalBottomSheet] instead of navigating. */
const val MORE_ROUTE = "more"

data class MoreMenuItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
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
            val hideAmounts = preferences?.get(HIDE_AMOUNTS_KEY) ?: false

            MyWalletTheme(darkTheme = isDarkTheme) {
                CompositionLocalProvider(LocalHideAmounts provides hideAmounts) {
                    if (authChecked) {
                        MyWalletApp(
                            startDestination = startDestination,
                            themeMode = themeMode,
                            onThemeModeChange = { mode ->
                                lifecycleScope.launch {
                                    dataStore.edit { it[THEME_MODE_KEY] = mode.name }
                                }
                            },
                            hideAmounts = hideAmounts,
                            onHideAmountsChange = { hide ->
                                lifecycleScope.launch {
                                    dataStore.edit { it[HIDE_AMOUNTS_KEY] = hide }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val HIDE_AMOUNTS_KEY = booleanPreferencesKey("hide_amounts")
    }
}

@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {
    val sessionExpired: SharedFlow<Unit> = authRepository.sessionExpired
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWalletApp(
    startDestination: String,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    hideAmounts: Boolean,
    onHideAmountsChange: (Boolean) -> Unit,
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        appViewModel.sessionExpired.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var showMoreSheet by remember { mutableStateOf(false) }

    val moreMenuItems = listOf(
        MoreMenuItem(
            route = Screen.NetWorth.route,
            label = "Net Worth",
            icon = Icons.Filled.AccountBalance,
        ),
        MoreMenuItem(
            route = Screen.Profile.route,
            label = "Profile",
            icon = Icons.Filled.Person,
        ),
    )
    val overflowRoutes = moreMenuItems.map { it.route }

    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.Home.route,
            label = "Overview",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
        ),
        BottomNavItem(
            route = Screen.Reports.route,
            label = "Reports",
            selectedIcon = Icons.Filled.Description,
            unselectedIcon = Icons.Outlined.Description,
        ),
        BottomNavItem(
            route = Screen.Subscriptions.route,
            label = "Subscriptions",
            selectedIcon = Icons.Filled.Subscriptions,
            unselectedIcon = Icons.Outlined.Subscriptions,
        ),
        BottomNavItem(
            route = Screen.Contracts.route,
            label = "Contracts",
            selectedIcon = Icons.AutoMirrored.Filled.Assignment,
            unselectedIcon = Icons.AutoMirrored.Outlined.Assignment,
        ),
        BottomNavItem(
            route = MORE_ROUTE,
            label = "More",
            selectedIcon = Icons.Filled.MoreHoriz,
            unselectedIcon = Icons.Outlined.MoreHoriz,
        ),
    )

    val showBottomBar = currentDestination?.route in
        (bottomNavItems.map { it.route } - MORE_ROUTE + overflowRoutes)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = if (item.route == MORE_ROUTE) {
                            currentDestination?.route in overflowRoutes
                        } else {
                            currentDestination?.hierarchy?.any { it.route == item.route } == true
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (item.route == MORE_ROUTE) {
                                    showMoreSheet = true
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
            hideAmounts = hideAmounts,
            onHideAmountsChange = onHideAmountsChange,
        )
    }

    if (showMoreSheet) {
        ModalBottomSheet(onDismissRequest = { showMoreSheet = false }) {
            moreMenuItems.forEach { item ->
                ListItem(
                    headlineContent = { Text(item.label) },
                    leadingContent = { Icon(item.icon, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showMoreSheet = false
                        navController.navigate(item.route) {
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
}
