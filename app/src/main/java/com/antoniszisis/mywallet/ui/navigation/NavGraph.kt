package com.antoniszisis.mywallet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.antoniszisis.mywallet.ui.auth.LoginScreen
import com.antoniszisis.mywallet.ui.home.HomeScreen
import com.antoniszisis.mywallet.ui.networth.CreateNetWorthSnapshotScreen
import com.antoniszisis.mywallet.ui.networth.EditNetWorthSnapshotScreen
import com.antoniszisis.mywallet.ui.networth.NetWorthDetailScreen
import com.antoniszisis.mywallet.ui.networth.NetWorthScreen
import com.antoniszisis.mywallet.ui.profile.ProfileScreen
import com.antoniszisis.mywallet.ui.reports.ReportDetailScreen
import com.antoniszisis.mywallet.ui.reports.ReportsScreen
import com.antoniszisis.mywallet.ui.subscriptions.SubscriptionsScreen
import com.antoniszisis.mywallet.ui.theme.ThemeMode

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToReportDetail = { reportId ->
                    navController.navigate(Screen.ReportDetail.createRoute(reportId))
                },
                onNavigateToNetWorthDetail = { snapshotId ->
                    navController.navigate(Screen.NetWorthDetail.createRoute(snapshotId))
                },
                onNavigateToSubscriptions = {
                    navController.navigate(Screen.Subscriptions.route)
                },
                onNavigateToNetWorth = {
                    navController.navigate(Screen.NetWorth.route)
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                },
            )
        }

        composable(Screen.Reports.route) {
            ReportsScreen(
                onNavigateToDetail = { reportId ->
                    navController.navigate(Screen.ReportDetail.createRoute(reportId))
                }
            )
        }

        composable(
            route = Screen.ReportDetail.route,
            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId") ?: return@composable
            ReportDetailScreen(
                reportId = reportId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Subscriptions.route) {
            SubscriptionsScreen()
        }

        composable(Screen.NetWorth.route) { backStackEntry ->
            val snapshotCreated = backStackEntry.savedStateHandle
                .getStateFlow("snapshotCreated", false)
                .collectAsState()

            NetWorthScreen(
                onNavigateToDetail = { snapshotId ->
                    navController.navigate(Screen.NetWorthDetail.createRoute(snapshotId))
                },
                onNavigateToCreate = {
                    navController.navigate(Screen.CreateNetWorthSnapshot.route)
                },
                needsRefresh = snapshotCreated.value,
                onRefreshConsumed = {
                    backStackEntry.savedStateHandle["snapshotCreated"] = false
                },
            )
        }

        composable(Screen.CreateNetWorthSnapshot.route) {
            CreateNetWorthSnapshotScreen(
                onNavigateBack = { navController.popBackStack() },
                onSuccess = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("snapshotCreated", true)
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = Screen.NetWorthDetail.route,
            arguments = listOf(navArgument("snapshotId") { type = NavType.StringType })
        ) { backStackEntry ->
            val snapshotId = backStackEntry.arguments?.getString("snapshotId") ?: return@composable
            val snapshotUpdated = backStackEntry.savedStateHandle
                .getStateFlow("snapshotUpdated", false)
                .collectAsState()
            NetWorthDetailScreen(
                snapshotId = snapshotId,
                onNavigateBack = { navController.popBackStack() },
                needsRefresh = snapshotUpdated.value,
                onRefreshConsumed = { backStackEntry.savedStateHandle["snapshotUpdated"] = false },
                onNavigateToEdit = {
                    navController.navigate(Screen.EditNetWorthSnapshot.createRoute(snapshotId))
                },
            )
        }

        composable(
            route = Screen.EditNetWorthSnapshot.route,
            arguments = listOf(navArgument("snapshotId") { type = NavType.StringType })
        ) { backStackEntry ->
            val snapshotId = backStackEntry.arguments?.getString("snapshotId") ?: return@composable
            EditNetWorthSnapshotScreen(
                snapshotId = snapshotId,
                onNavigateBack = { navController.popBackStack() },
                onSuccess = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("snapshotUpdated", true)
                    navController.popBackStack()
                },
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
            )
        }
    }
}
