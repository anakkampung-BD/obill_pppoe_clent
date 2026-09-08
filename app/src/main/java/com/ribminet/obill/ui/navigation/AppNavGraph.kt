package com.ribminet.obill.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ribminet.obill.AppViewModel
import com.ribminet.obill.PpobViewModel
import com.ribminet.obill.WalletViewModel
import com.ribminet.obill.OrderFlow
import com.ribminet.obill.data.remote.payableTotal
import com.ribminet.obill.data.ComplaintStatus
import com.ribminet.obill.data.Complaint
import com.ribminet.obill.ui.components.AppBottomBar
import com.ribminet.obill.ui.components.PullToRefresh
import com.ribminet.obill.ui.guide.LocalGuideTargetRegistry
import com.ribminet.obill.ui.guide.UserGuideOverlay
import com.ribminet.obill.ui.guide.rememberGuideTargetRegistry
import kotlinx.coroutines.delay
import com.ribminet.obill.ui.screens.ChangePackageScreen
import com.ribminet.obill.ui.screens.ChangePasswordScreen
import com.ribminet.obill.ui.screens.ClientMonitoringScreen
import com.ribminet.obill.ui.screens.CreateReportScreen
import com.ribminet.obill.ui.screens.DashboardScreen
import com.ribminet.obill.ui.screens.EditBiodataScreen
import com.ribminet.obill.ui.screens.FiberMonitoringScreen
import com.ribminet.obill.ui.screens.HelpScreen
import com.ribminet.obill.ui.screens.LegalDocScreen
import com.ribminet.obill.ui.screens.LegalDocType
import com.ribminet.obill.ui.screens.LoginScreen
import com.ribminet.obill.ui.screens.NotificationsScreen
import com.ribminet.obill.ui.screens.OrdersScreen
import com.ribminet.obill.ui.screens.OutstandingScreen
import com.ribminet.obill.ui.screens.PaymentHistoryScreen
import com.ribminet.obill.ui.screens.PaymentInstructionScreen
import com.ribminet.obill.ui.screens.PaymentMethodScreen
import com.ribminet.obill.ui.screens.PpobHistoryScreen
import com.ribminet.obill.ui.screens.PpobScreen
import com.ribminet.obill.ui.screens.PpobTransactionDetailScreen
import com.ribminet.obill.ui.screens.ProfileScreen
import com.ribminet.obill.ui.screens.ReportScreen
import com.ribminet.obill.ui.screens.TwoFactorScreen
import com.ribminet.obill.ui.screens.WalletTopUpScreen
import com.ribminet.obill.ui.screens.WifiSettingsScreen

@Composable
fun AppNavGraph(vm: AppViewModel) {
    val nav = rememberNavController()
    val start = if (vm.loggedIn) Routes.HOME else Routes.LOGIN
    val guideRegistry = rememberGuideTargetRegistry()

    LaunchedEffect(vm.loggedIn) {
        if (vm.loggedIn) vm.startNotificationPoll() else vm.stopNotificationPoll()
    }

    LaunchedEffect(vm.pendingPushRoute) {
        val route = vm.consumePushRoute() ?: return@LaunchedEffect
        if (nav.currentBackStackEntry?.destination?.route == route) return@LaunchedEffect
        nav.navigateToRoute(route)
    }

    LaunchedEffect(vm.guideSession?.guide?.id, vm.guideSession?.stepIndex) {
        val step = vm.guideSession?.step ?: return@LaunchedEffect
        delay(120)
        when {
            step.route == Routes.PAYMENT_METHOD -> {
                vm.startBillFlow()
                vm.loadPaymentMethods()
                nav.navigateToRoute(step.route)
            }
            step.route == Routes.ORDERS -> {
                vm.loadOrders()
                nav.navigateToRoute(step.route)
            }
            step.route == Routes.OUTSTANDING -> {
                vm.startBillFlow()
                vm.refreshBilling()
                nav.navigateToRoute(step.route)
            }
            step.mainTab -> nav.navigateMainTab(step.route)
            else -> nav.navigateToRoute(step.route)
        }
    }

    val animDuration = 320
    val zoomScale = 0.88f
    CompositionLocalProvider(LocalGuideTargetRegistry provides guideRegistry) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = nav,
                startDestination = start,
                enterTransition = {
                    fadeIn(animationSpec = tween(animDuration)) +
                        scaleIn(initialScale = zoomScale, animationSpec = tween(animDuration))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(animDuration)) +
                        scaleOut(targetScale = zoomScale, animationSpec = tween(animDuration))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(animDuration)) +
                        scaleIn(initialScale = zoomScale, animationSpec = tween(animDuration))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(animDuration)) +
                        scaleOut(targetScale = zoomScale, animationSpec = tween(animDuration))
                },
            ) {
        composable(Routes.LOGIN) {
            LoginScreen(vm = vm, onLoggedIn = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }

        mainTabs(nav, vm)

        composable(Routes.OUTSTANDING) {
            LaunchedEffect(Unit) { vm.startBillFlow(); vm.refreshBilling() }
            PullToRefresh(refreshing = vm.billLoading, onRefresh = { vm.refreshBilling() }) {
                OutstandingScreen(
                    loading = vm.billLoading,
                    error = vm.billError,
                    bill = vm.billDto,
                    openOrder = vm.billOpenOrder,
                    guideMode = vm.guideSession != null,
                    onBack = { nav.popBackStack() },
                    onPay = {
                        vm.startBillFlow()
                        vm.createOrder("qris_dinamis", null) {
                            nav.navigate(Routes.PAYMENT_INSTRUCTION)
                        }
                    },
                    paySubmitting = vm.orderSubmitting,
                    onViewOrder = {
                        vm.currentOrder = vm.billOpenOrder
                        vm.resumeBillQrisPayment()
                        nav.navigate(Routes.PAYMENT_INSTRUCTION)
                    },
                )
            }
        }

        composable(Routes.PAYMENT_METHOD) {
            LaunchedEffect(Unit) { if (vm.methods.isEmpty()) vm.loadPaymentMethods() }
            val isUpgrade = vm.orderFlow == OrderFlow.UPGRADE
            val amount = if (isUpgrade)
                vm.packages.firstOrNull { it.id == vm.selectedUpgradeProfileId?.toString() }?.price ?: 0L
            else vm.billDto?.payableTotal() ?: 0L
            val bill = if (isUpgrade) null else vm.billDto
            val label = if (isUpgrade) vm.selectedUpgradeName else (vm.billDto?.profileName ?: "-")
            PullToRefresh(refreshing = vm.methodsLoading, onRefresh = { vm.loadPaymentMethods() }) {
                PaymentMethodScreen(
                    amount = amount,
                    packageLabel = label,
                    bill = bill,
                    methods = vm.methods,
                    loading = vm.methodsLoading,
                    submitting = vm.orderSubmitting,
                    error = vm.orderError,
                    alert = vm.alert,
                    onDismissAlert = { vm.dismissAlert() },
                    onBack = { nav.popBackStack() },
                    onClose = { nav.popBackStack(Routes.HOME, inclusive = false) },
                    onPay = { method ->
                        vm.createOrder(method.id, null) {
                            nav.navigate(Routes.PAYMENT_INSTRUCTION)
                        }
                    }
                )
            }
        }

        composable(Routes.PAYMENT_INSTRUCTION) {
            val guideOrder = vm.guideDemoOrder
            PaymentInstructionScreen(
                order = if (vm.guideSession != null) (guideOrder ?: vm.currentOrder) else vm.currentOrder,
                submitting = vm.orderSubmitting,
                statusRefreshing = vm.orderStatusRefreshing,
                error = vm.orderError,
                alert = vm.alert,
                onDismissAlert = { vm.dismissAlert() },
                onConfirm = { ref -> vm.confirmOrder(ref) {} },
                onCancel = { vm.cancelOrder { nav.popBackStack(Routes.HOME, inclusive = false) } },
                onRefreshStatus = { vm.refreshCurrentOrder() },
                onBack = { nav.popBackStack() },
                onHome = {
                    vm.loadMe(); vm.loadPayments(); vm.refreshWallet()
                    nav.popBackStack(Routes.HOME, inclusive = false)
                },
                billPaymentExpiresAtMs = vm.billPaymentExpiresAtMs,
            )
        }

        composable(Routes.CREATE_REPORT) {
            CreateReportScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onSubmit = { category, problem, description ->
                    val now = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("id", "ID"))
                        .format(java.util.Date())
                    vm.addComplaint(
                        Complaint(
                            id = "ADU-${System.currentTimeMillis()}",
                            category = category.ifBlank { "Lainnya" }.uppercase(),
                            problem = problem.ifBlank { "-" }.uppercase(),
                            description = description.ifBlank { "-" },
                            status = ComplaintStatus.TERKIRIM,
                            dateTime = now
                        )
                    )
                },
                onComplaintSent = {
                    nav.navigate(Routes.REPORT) {
                        popUpTo(Routes.CREATE_REPORT) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.EDIT_BIODATA) {
            LaunchedEffect(Unit) { vm.loadMe() }
            EditBiodataScreen(vm = vm, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
        }
        composable(Routes.CHANGE_PASSWORD) { ChangePasswordScreen(onBack = { nav.popBackStack() }, onSave = { nav.popBackStack() }) }
        composable(Routes.CHANGE_PACKAGE) {
            LaunchedEffect(Unit) { vm.loadUpgradeOptions() }
            PullToRefresh(refreshing = vm.upgradeLoading, onRefresh = { vm.loadUpgradeOptions() }) {
                ChangePackageScreen(
                    packages = vm.packages,
                    loading = vm.upgradeLoading,
                    error = vm.upgradeError,
                    pendingChange = vm.pendingChange,
                    packagesSource = vm.packagesSource,
                    submitting = vm.packageChangeSubmitting,
                    alert = vm.alert,
                    onDismissAlert = { vm.dismissAlert() },
                    onBack = { nav.popBackStack() },
                    onRequestChange = { pkg ->
                        val id = pkg.id.toIntOrNull()
                        if (id != null) vm.requestPackageChange(id) {}
                    },
                    onCancelPending = { vm.cancelPackageChange {} },
                )
            }
        }
        composable(Routes.WIFI_SETTINGS) {
            LaunchedEffect(Unit) { vm.loadDevice(); vm.loadDeviceClients() }
            PullToRefresh(
                refreshing = vm.deviceLoading || vm.deviceClientsLoading,
                onRefresh = { vm.loadDevice(); vm.loadDeviceClients() }
            ) {
                WifiSettingsScreen(vm = vm, onBack = { nav.popBackStack() })
            }
        }
        composable(Routes.CLIENT_MONITORING) {
            LaunchedEffect(Unit) { vm.loadDevice(); vm.loadDeviceClients() }
            PullToRefresh(
                refreshing = vm.deviceClientsLoading,
                onRefresh = { vm.loadDevice(); vm.loadDeviceClients() }
            ) {
                ClientMonitoringScreen(vm = vm, onBack = { nav.popBackStack() })
            }
        }
        composable(Routes.FIBER_MONITORING) {
            LaunchedEffect(Unit) { vm.loadDevice(); vm.loadRxHistory(7) }
            PullToRefresh(
                refreshing = vm.deviceLoading || vm.rxHistoryLoading,
                onRefresh = { vm.loadDevice(); vm.loadRxHistory(7) }
            ) {
                FiberMonitoringScreen(vm = vm, onBack = { nav.popBackStack() })
            }
        }
        composable(Routes.ORDERS) {
            LaunchedEffect(Unit) { vm.loadOrders() }
            PullToRefresh(refreshing = vm.ordersLoading, onRefresh = { vm.loadOrders() }) {
                OrdersScreen(
                    items = vm.unifiedOrders,
                    loading = vm.ordersLoading,
                    error = vm.ordersError,
                    guideMode = vm.guideSession != null,
                    onBack = { nav.popBackStack() },
                    onOpen = { item ->
                        when (item.source) {
                            com.ribminet.obill.data.remote.HistoryOrderSource.PPPOE -> {
                                item.order?.let { order ->
                                    vm.currentOrder = order
                                    nav.navigate(Routes.PAYMENT_INSTRUCTION)
                                }
                            }
                            com.ribminet.obill.data.remote.HistoryOrderSource.PPOB -> {
                                item.ppobRefId?.let { ref ->
                                    nav.navigate(Routes.ppobDetail(ref))
                                }
                            }
                        }
                    },
                )
            }
        }
        composable(Routes.HELP) {
            HelpScreen(
                onBack = { nav.popBackStack() },
                onStartGuide = { guideId ->
                    nav.popBackStack()
                    vm.startUserGuide(guideId)
                },
            )
        }
        composable(Routes.NOTIFICATIONS) {
            LaunchedEffect(Unit) { vm.loadNotifications() }
            NotificationsScreen(
                notifications = vm.notifications,
                loading = vm.notificationsLoading,
                onBack = { nav.popBackStack() },
                onRefresh = { vm.loadNotifications() },
                onMarkRead = { vm.markNotificationRead(it) },
                onMarkAllRead = { vm.markAllNotificationsRead() },
            )
        }
        composable(Routes.TWO_FACTOR) { TwoFactorScreen(onBack = { nav.popBackStack() }) }
        composable(
            route = Routes.LEGAL_DOC,
            arguments = listOf(navArgument("docId") { type = NavType.StringType }),
        ) { entry ->
            val docId = entry.arguments?.getString("docId").orEmpty()
            val type = when (docId) {
                "privacy" -> LegalDocType.PRIVACY
                else -> LegalDocType.TERMS
            }
            LegalDocScreen(type = type, onBack = { nav.popBackStack() })
        }
        composable(Routes.PPOB) {
            val ppobVm: PpobViewModel = viewModel()
            PpobScreen(
                vm = ppobVm,
                onBack = { nav.popBackStack() },
                onOpenHistory = { nav.navigate(Routes.PPOB_HISTORY) },
                onOpenDetail = { refId -> nav.navigate(Routes.ppobDetail(refId)) },
            )
        }
        composable(Routes.PPOB_HISTORY) {
            val ppobVm: PpobViewModel = viewModel()
            PpobHistoryScreen(
                vm = ppobVm,
                onBack = { nav.popBackStack() },
                onOpenDetail = { refId -> nav.navigate(Routes.ppobDetail(refId)) },
            )
        }
        composable(
            route = Routes.PPOB_DETAIL,
            arguments = listOf(navArgument("refId") { type = NavType.StringType }),
        ) { entry ->
            val refId = entry.arguments?.getString("refId") ?: return@composable
            val ppobVm: PpobViewModel = viewModel()
            PpobTransactionDetailScreen(
                refId = refId,
                vm = ppobVm,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.WALLET_TOPUP) {
            val walletVm: WalletViewModel = viewModel()
            WalletTopUpScreen(
                vm = walletVm,
                onBack = { nav.popBackStack() },
                onBalanceUpdated = { bal -> vm.updateWalletBalance(bal) },
            )
        }
            }

            vm.guideSession?.let { session ->
                UserGuideOverlay(
                    session = session,
                    targetBounds = guideRegistry.bounds[session.step.target],
                    onNext = { vm.nextGuideStep() },
                    onSkip = { vm.skipUserGuide() },
                )
            }
        }
    }
}

private fun NavGraphBuilder.mainTabs(nav: NavHostController, vm: AppViewModel) {
    val bottomBar: @Composable (String) -> Unit = { current ->
        AppBottomBar(current = current, onNavigate = { route -> nav.navigateMainTab(route) })
    }

    composable(Routes.HOME) {
        LaunchedEffect(Unit) {
            vm.loadMe(); vm.refreshBilling(); vm.loadDevice(); vm.loadDeviceClients()
            vm.refreshAnnouncements(); vm.refreshWallet()
        }
        val connectedCount = if (vm.deviceClients.isEmpty() && vm.deviceClientsLoading) null else vm.lanClients.size
        val homeRefreshing = vm.meLoading || vm.billLoading || vm.deviceLoading || vm.deviceClientsLoading || vm.announcementsLoading
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                PullToRefresh(
                    refreshing = homeRefreshing,
                    onRefresh = {
                        vm.loadMe(); vm.refreshBilling(); vm.loadPayments()
                        vm.loadDevice(); vm.loadDeviceClients()
                        vm.refreshWallet(); vm.refreshAnnouncements(force = true)
                    }
                ) {
                    DashboardScreen(
                        user = vm.profile,
                        bill = vm.billDto,
                        openOrder = vm.billOpenOrder,
                        device = vm.device,
                        deviceLoading = vm.deviceLoading,
                        connectedCount = connectedCount,
                        connectedLoading = vm.deviceClientsLoading,
                        showBillingStatus = vm.billingBannerVisible,
                        onDismissBillingStatus = { vm.dismissBillingBanner() },
                        onOpenBilling = { nav.navigate(Routes.OUTSTANDING) },
                        onChangePackage = { nav.navigate(Routes.CHANGE_PACKAGE) },
                        onComplaint = { nav.navigate(Routes.CREATE_REPORT) },
                        onHelp = { nav.navigate(Routes.HELP) },
                        onPpob = { nav.navigate(Routes.PPOB) },
                        onWalletTopUp = { nav.navigate(Routes.WALLET_TOPUP) },
                        onWifi = { nav.navigate(Routes.WIFI_SETTINGS) },
                        onClients = { nav.navigate(Routes.CLIENT_MONITORING) },
                        onFiber = { nav.navigate(Routes.FIBER_MONITORING) },
                        onNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                        unreadNotifications = vm.unreadCount,
                        announcements = vm.announcements.toList(),
                        onOpenAnnouncement = { vm.openAnnouncementDetail(it) },
                    )
                }
            }
            bottomBar(Routes.HOME)
        }
    }

    composable(Routes.HISTORY) {
        LaunchedEffect(Unit) { vm.loadPayments() }
        PullToRefresh(refreshing = vm.paymentsLoading, onRefresh = { vm.loadPayments() }) {
            PaymentHistoryScreen(
                payments = vm.payments,
                loading = vm.paymentsLoading,
                onDelete = { vm.removePayment(it) },
                bottomBar = { bottomBar(Routes.HISTORY) }
            )
        }
    }

    composable(Routes.REPORT) {
        ReportScreen(
            complaints = vm.complaints,
            onCreate = { nav.navigate(Routes.CREATE_REPORT) },
            onDelete = { vm.removeComplaint(it) },
            alert = vm.alert,
            onDismissAlert = { vm.dismissAlert() },
            bottomBar = { bottomBar(Routes.REPORT) }
        )
    }

    composable(Routes.PROFILE) {
        PullToRefresh(refreshing = vm.meLoading, onRefresh = { vm.loadMe() }) {
            ProfileScreen(
                user = vm.profile,
                onEditBiodata = { nav.navigate(Routes.EDIT_BIODATA) },
                onNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                onOrders = { nav.navigate(Routes.ORDERS) },
                onHelp = { nav.navigate(Routes.HELP) },
                onDoc = { type ->
                    val id = if (type == LegalDocType.PRIVACY) "privacy" else "terms"
                    nav.navigate(Routes.legalDoc(id))
                },
                onLogout = {
                    vm.logout { nav.navigate(Routes.LOGIN) { popUpTo(0) } }
                },
                bottomBar = { bottomBar(Routes.PROFILE) }
            )
        }
    }
}
