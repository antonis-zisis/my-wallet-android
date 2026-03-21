package com.mywallet.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mywallet.android.ui.auth.LoginScreen
import com.mywallet.android.ui.home.HomeScreen
import com.mywallet.android.ui.networth.NetWorthDetailScreen
import com.mywallet.android.ui.networth.NetWorthScreen
import com.mywallet.android.ui.profile.ProfileScreen
import com.mywallet.android.ui.reports.ReportDetailScreen
import com.mywallet.android.ui.reports.ReportsScreen
import com.mywallet.android.ui.subscriptions.SubscriptionsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
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
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
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
                }
            )
        }
    }
}
