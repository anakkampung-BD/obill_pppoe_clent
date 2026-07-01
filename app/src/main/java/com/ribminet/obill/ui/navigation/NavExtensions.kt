package com.ribminet.obill.ui.navigation

import androidx.navigation.NavHostController

/** Navigasi antar tab utama — sama seperti bottom bar (bersihkan layar overlay di atasnya). */
fun NavHostController.navigateMainTab(route: String) {
    require(route in Routes.MAIN_TABS) { "Bukan tab utama: $route" }
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavHostController.navigateToRoute(route: String) {
    if (route in Routes.MAIN_TABS) navigateMainTab(route)
    else navigate(route) { launchSingleTop = true }
}
