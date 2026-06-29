package com.ribminet.obill

enum class AlertType { SUCCESS, ERROR, WARNING, INFO }

data class AppAlert(
    val type: AlertType,
    val title: String,
    val message: String,
    val confirmText: String = "OK",
)
