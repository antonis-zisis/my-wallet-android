package com.antoniszisis.mywallet.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.antoniszisis.mywallet.ui.auth.LoginScreen
import com.antoniszisis.mywallet.ui.home.HomeScreen
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

        composable(Screen.NetWorth.route) {
            NetWorthScreen(
                onNavigateToDetail = { snapshotId ->
                    navController.navigate(Screen.NetWorthDetail.createRoute(snapshotId))
                }
            )
        }

        composable(
            route = Screen.NetWorthDetail.route,
            arguments = listOf(navArgument("snapshotId") { type = NavType.StringType })
        ) { backStackEntry ->
            val snapshotId = backStackEntry.arguments?.getString("snapshotId") ?: return@composable
            NetWorthDetailScreen(
                snapshotId = snapshotId,
                onNavigateBack = { navController.popBackStack() },
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
