package com.ribminet.obill.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ribminet.obill.AppViewModel
import com.ribminet.obill.OrderFlow
import com.ribminet.obill.data.remote.payableTotal
import com.ribminet.obill.data.ComplaintStatus
import com.ribminet.obill.data.Complaint
import com.ribminet.obill.ui.components.AppBottomBar
import com.ribminet.obill.ui.components.PullToRefresh
import com.ribminet.obill.ui.screens.ChangePackageScreen
import com.ribminet.obill.ui.screens.ChangePasswordScreen
import com.ribminet.obill.ui.screens.ClientMonitoringScreen
import com.ribminet.obill.ui.screens.CreateReportScreen
import com.ribminet.obill.ui.screens.DashboardScreen
import com.ribminet.obill.ui.screens.EditBiodataScreen
import com.ribminet.obill.ui.screens.FiberMonitoringScreen
import com.ribminet.obill.ui.screens.HelpScreen
import com.ribminet.obill.ui.screens.LoginScreen
import com.ribminet.obill.ui.screens.NotificationsScreen
import com.ribminet.obill.ui.screens.OrdersScreen
import com.ribminet.obill.ui.screens.OutstandingScreen
import com.ribminet.obill.ui.screens.PaymentHistoryScreen
import com.ribminet.obill.ui.screens.PaymentInstructionScreen
import com.ribminet.obill.ui.screens.PaymentMethodScreen
import com.ribminet.obill.ui.screens.ProfileScreen
import com.ribminet.obill.ui.screens.ReportScreen
import com.ribminet.obill.ui.screens.SimpleDocScreen
import com.ribminet.obill.ui.screens.TwoFactorScreen
import com.ribminet.obill.ui.screens.WifiSettingsScreen

@Composable
fun AppNavGraph(vm: AppViewModel) {
    val nav = rememberNavController()
    val start = if (vm.loggedIn) Routes.HOME else Routes.LOGIN

    LaunchedEffect(vm.loggedIn) {
        if (vm.loggedIn) vm.startNotificationPoll() else vm.stopNotificationPoll()
    }

    LaunchedEffect(vm.pendingPushRoute) {
        val route = vm.consumePushRoute() ?: return@LaunchedEffect
        if (nav.currentBackStackEntry?.destination?.route == route) return@LaunchedEffect
        nav.navigateToRoute(route)
    }

    val animDuration = 320
    NavHost(
        navController = nav,
        startDestination = start,
        enterTransition = {
            slideInHorizontally(animationSpec = tween(animDuration)) { it / 5 } + fadeIn(tween(animDuration))
        },
        exitTransition = {
            slideOutHorizontally(animationSpec = tween(animDuration)) { -it / 8 } + fadeOut(tween(animDuration))
        },
        popEnterTransition = {
            slideInHorizontally(animationSpec = tween(animDuration)) { -it / 5 } + fadeIn(tween(animDuration))
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(animDuration)) { it / 5 } + fadeOut(tween(animDuration))
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
                    onBack = { nav.popBackStack() },
                    onPay = {
                        vm.startBillFlow()
                        vm.loadPaymentMethods()
                        nav.navigate(Routes.PAYMENT_METHOD)
                    },
                    onViewOrder = {
                        vm.currentOrder = vm.billOpenOrder
                        nav.navigate(Routes.PAYMENT_INSTRUCTION)
                    }
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
            PaymentInstructionScreen(
                order = vm.currentOrder,
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
                    vm.loadMe(); vm.loadPayments()
                    nav.popBackStack(Routes.HOME, inclusive = false)
                }
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
                    orders = vm.orders,
                    loading = vm.ordersLoading,
                    onBack = { nav.popBackStack() },
                    onOpen = { order ->
                        vm.currentOrder = order
                        nav.navigate(Routes.PAYMENT_INSTRUCTION)
                    }
                )
            }
        }
        composable(Routes.HELP) { HelpScreen(onBack = { nav.popBackStack() }) }
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
        composable(Routes.SIMPLE_DOC) { SimpleDocScreen(title = vm.docTitle, onBack = { nav.popBackStack() }) }
    }
}

private fun NavGraphBuilder.mainTabs(nav: NavHostController, vm: AppViewModel) {
    val bottomBar: @Composable (String) -> Unit = { current ->
        AppBottomBar(current = current, onNavigate = { route -> nav.navigateMainTab(route) })
    }

    composable(Routes.HOME) {
        LaunchedEffect(Unit) { vm.loadMe(); vm.refreshBilling(); vm.loadDevice(); vm.loadDeviceClients() }
        val connectedCount = if (vm.deviceClients.isEmpty() && vm.deviceClientsLoading) null else vm.lanClients.size
        val homeRefreshing = vm.meLoading || vm.billLoading || vm.deviceLoading || vm.deviceClientsLoading
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                PullToRefresh(
                    refreshing = homeRefreshing,
                    onRefresh = {
                        vm.loadMe(); vm.refreshBilling(); vm.loadPayments()
                        vm.loadDevice(); vm.loadDeviceClients()
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
                        onWifi = { nav.navigate(Routes.WIFI_SETTINGS) },
                        onClients = { nav.navigate(Routes.CLIENT_MONITORING) },
                        onFiber = { nav.navigate(Routes.FIBER_MONITORING) },
                        onNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                        unreadNotifications = vm.unreadCount,
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
                onDoc = { title -> vm.docTitle = title; nav.navigate(Routes.SIMPLE_DOC) },
                onLogout = {
                    vm.logout { nav.navigate(Routes.LOGIN) { popUpTo(0) } }
                },
                bottomBar = { bottomBar(Routes.PROFILE) }
            )
        }
    }
}
