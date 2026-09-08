package com.ribminet.obill.data

enum class PaymentStatus { PAID, PENDING, CANCELLED, UNPAID }

enum class PaymentChannel { INSTANT, MANUAL }

enum class ComplaintStatus { TERKIRIM, SELESAI, DIPROSES, MENUNGGU }

data class UserProfile(
    val fullName: String,
    val customerId: String,
    val username: String,
    val email: String,
    val whatsapp: String,
    val address: String,
    val profileCompletion: Int,
    val packageName: String,
    val packageSpeed: String,
    val packagePrice: Long,
    val serviceActive: Boolean,
    val signalDbm: Double,
    val ssid: String,
    val ssidPassword: String,
    val activeUntil: String,
    val nextDueDate: String,
    val photoUrl: String = "",
    val walletBalance: Long = 0L,
)

data class InternetPackage(
    val id: String,
    val name: String,
    val speed: String,
    val price: Long,
    val features: List<String>,
    val popular: Boolean = false,
    val current: Boolean = false,
)

data class Bill(
    val id: String,
    val periodLabel: String,
    val amount: Long,
    val status: PaymentStatus,
    val channel: PaymentChannel,
    val dateTime: String,
    val payCode: String,
    val gatewayId: String,
    val method: String,
    val targetBank: String,
    val targetAccount: String,
    val targetOwner: String,
)

data class OutstandingBill(
    val year: Int,
    val monthLabel: String,
    val amount: Long,
)

data class PaymentMethodOption(
    val id: String,
    val name: String,
    val group: String,
    val short: String,
    val subtitle: String = "",
    val iconKey: String = "",
)

data class BankAccount(
    val bank: String,
    val number: String,
    val owner: String,
)

data class Complaint(
    val id: String,
    val category: String,
    val problem: String,
    val description: String,
    val status: ComplaintStatus,
    val dateTime: String,
)

data class ConnectedClient(
    val name: String,
    val ip: String,
    val mac: String,
    val band: String,
    val signal: Int,
    val online: Boolean,
)

data class NewsItem(
    val title: String,
    val date: String,
    val summary: String,
)
