package com.ribminet.obill.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val HISTORY = "history"
    const val REPORT = "report"
    const val PROFILE = "profile"

    val MAIN_TABS = setOf(HOME, HISTORY, REPORT, PROFILE)

    const val OUTSTANDING = "outstanding"
    const val PAYMENT_METHOD = "payment_method"
    const val PAYMENT_CONFIRM = "payment_confirm"
    const val PAYMENT_INSTRUCTION = "payment_instruction"
    const val PAYMENT_RECEIPT = "payment_receipt"
    const val ORDERS = "orders"

    const val CREATE_REPORT = "create_report"

    const val EDIT_BIODATA = "edit_biodata"
    const val CHANGE_PASSWORD = "change_password"
    const val CHANGE_PACKAGE = "change_package"
    const val WIFI_SETTINGS = "wifi_settings"
    const val CLIENT_MONITORING = "client_monitoring"
    const val FIBER_MONITORING = "fiber_monitoring"
    const val HELP = "help"
    const val NOTIFICATIONS = "notifications"
    const val PPOB = "ppob"
    const val PPOB_HISTORY = "ppob_history"
    const val PPOB_DETAIL = "ppob_detail/{refId}"
    const val WALLET_TOPUP = "wallet_topup"

    fun ppobDetail(refId: String) = "ppob_detail/$refId"
    const val TWO_FACTOR = "two_factor"
    const val LEGAL_DOC = "legal_doc/{docId}"

    fun legalDoc(docId: String) = "legal_doc/$docId"
}
