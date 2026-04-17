package com.antoniszisis.mywallet.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object Reports : Screen("reports")
    data object ReportDetail : Screen("reports/{reportId}") {
        fun createRoute(reportId: String) = "reports/$reportId"
    }
    data object Subscriptions : Screen("subscriptions")
    data object NetWorth : Screen("net-worth")
    data object NetWorthDetail : Screen("net-worth/{snapshotId}") {
        fun createRoute(snapshotId: String) = "net-worth/$snapshotId"
    }
    data object Profile : Screen("profile")
}
