package com.sktech.wastetrack.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object Dashboard : Screen("dashboard")
    data object ScrapLog : Screen("scrap_log")
    data object ScrapHistory : Screen("scrap_history")
    data object ScrapClassify : Screen("scrap_classify")
    data object TransferList : Screen("transfer_list")
    data object QRGenerate : Screen("qr_generate/{scrapEntryId}") {
        fun createRoute(scrapEntryId: String) = "qr_generate/$scrapEntryId"
    }
    data object QRScan : Screen("qr_scan")
    data object BidMarket : Screen("bid_market")
    data object BidDetail : Screen("bid_detail/{bidRequestId}") {
        fun createRoute(bidRequestId: String) = "bid_detail/$bidRequestId"
    }
    data object Compliance : Screen("compliance")
    data object CertificateView : Screen("certificate_view/{certificateId}") {
        fun createRoute(certificateId: String) = "certificate_view/$certificateId"
    }
    data object BinMonitor : Screen("bin_monitor")
    data object LedgerScan : Screen("ledger_scan")
    data object Analytics : Screen("analytics")
    data object Settings : Screen("settings")
    data object FleetTracker : Screen("fleet_tracker")
    data object GatePass : Screen("gate_pass/{transferId}") {
        fun createRoute(transferId: String) = "gate_pass/$transferId"
    }
}
