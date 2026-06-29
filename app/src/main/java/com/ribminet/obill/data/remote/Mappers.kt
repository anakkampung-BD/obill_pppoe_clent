package com.ribminet.obill.data.remote

import com.ribminet.obill.data.Bill
import com.ribminet.obill.data.BankAccount
import com.ribminet.obill.data.InternetPackage
import com.ribminet.obill.data.PaymentChannel
import com.ribminet.obill.data.PaymentMethodOption
import com.ribminet.obill.data.PaymentStatus
import com.ribminet.obill.data.UserProfile
import java.text.SimpleDateFormat
import java.util.Locale

private val ID = Locale("id", "ID")
private val MONTHS_SHORT = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

/** "2026-07-01" atau "2026-07-01 10:00:00" -> "01 Jul 2026" */
fun formatDateId(raw: String?): String {
    if (raw.isNullOrBlank()) return "-"
    val datePart = raw.trim().split(" ").firstOrNull() ?: return raw
    val seg = datePart.split("-")
    if (seg.size < 3) return raw
    val y = seg[0]
    val m = seg[1].toIntOrNull() ?: return raw
    val d = seg[2].toIntOrNull() ?: return raw
    if (m !in 1..12) return raw
    return "%02d %s %s".format(d, MONTHS_SHORT[m - 1], y)
}

/** "2026-07-01 10:05:00" -> "01 Jul 2026 10:05" */
fun formatDateTimeId(raw: String?): String {
    if (raw.isNullOrBlank()) return "-"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val out = SimpleDateFormat("dd MMM yyyy HH:mm", ID)
        out.format(parser.parse(raw.trim())!!)
    } catch (_: Exception) {
        formatDateId(raw)
    }
}

/** "20M/20M" -> "20 Mbps" */
fun rateToSpeed(rate: String?): String {
    if (rate.isNullOrBlank()) return "-"
    val num = rate.trim().takeWhile { it.isDigit() }
    return if (num.isNotBlank()) "$num Mbps" else rate
}

fun CustomerDto.toUserProfile(): UserProfile {
    val filled = listOf(customerName, phone, email, address).count { !it.isNullOrBlank() }
    val completion = ((filled / 4f) * 100).toInt()
    return UserProfile(
        fullName = customerName ?: "-",
        customerId = customerCode ?: usernamePppoe ?: "-",
        username = usernamePppoe ?: "-",
        email = email ?: "",
        whatsapp = phone ?: "",
        address = address ?: "",
        profileCompletion = completion,
        packageName = profileName ?: "-",
        packageSpeed = rateToSpeed(rateLimit),
        packagePrice = price ?: 0L,
        serviceActive = (statusLangganan == "active") || (activationState == "active"),
        signalDbm = 0.0, // tidak tersedia di API (lihat dok endpoint monitoring)
        ssid = "-",       // tidak tersedia di API (lihat dok endpoint wifi)
        ssidPassword = "",
        activeUntil = formatDateId(expiredAt),
        nextDueDate = formatDateId(nextPayment?.dueDate ?: expiredAt),
        photoUrl = photoProfileUrl ?: "",
    )
}

fun PaymentDto.toBill(): Bill = Bill(
    id = invoiceNo ?: id?.toString() ?: "-",
    periodLabel = periodeLabel?.let { "Tagihan $it" } ?: "Pembayaran",
    amount = amount ?: 0L,
    status = PaymentStatus.PAID,
    channel = PaymentChannel.MANUAL,
    dateTime = formatDateTimeId(paidAt),
    payCode = invoiceNo ?: "-",
    gatewayId = referenceNo ?: "-",
    method = paymentMethod ?: "-",
    targetBank = "-",
    targetAccount = "-",
    targetOwner = "-",
)

fun PackageDto.toInternetPackage(): InternetPackage = InternetPackage(
    id = profileId?.toString() ?: "-",
    name = profileName ?: "-",
    speed = rateToSpeed(rateLimit),
    price = price ?: 0L,
    features = if (keterangan.isNullOrBlank()) emptyList() else listOf(keterangan),
    popular = isUpgrade == true,
    current = isCurrent == true,
)

/** Map metode pembayaran API -> opsi UI + daftar rekening (untuk transfer bank). */
fun PayChannelDto.toPaymentMethodOption(): PaymentMethodOption {
    val group = when (type) {
        "cash" -> "TUNAI"
        "bank_transfer" -> "TRANSFER BANK"
        "qris", "qr" -> "QRIS DANA"
        else -> "LAINNYA"
    }
    val short = when (type) {
        "cash" -> "CASH"
        "bank_transfer" -> "BANK"
        "qris", "qr" -> "DANA"
        else -> (method ?: "").uppercase()
    }

    // Untuk transfer bank, tampilkan langsung nama rekening dari server API.
    val acc = accounts?.firstOrNull()
    val name: String
    val subtitle: String
    val iconKey: String
    when (type) {
        "bank_transfer" -> {
            val banks = accounts?.mapNotNull { it.bank?.takeIf { b -> b.isNotBlank() } }?.distinct().orEmpty()
            name = banks.joinToString(", ").ifBlank { label ?: "Transfer Bank" }
            subtitle = when {
                (accounts?.size ?: 0) > 1 -> "${accounts?.size} rekening tersedia"
                acc != null -> listOfNotNull(
                    acc.accountNumber?.takeIf { it.isNotBlank() },
                    acc.accountName?.takeIf { it.isNotBlank() }?.let { "a.n $it" },
                ).joinToString("  •  ")
                else -> ""
            }
            iconKey = brandIconKey(banks.firstOrNull() ?: method)
        }
        "qris", "qr" -> {
            name = label ?: "QRIS DANA"
            subtitle = merchantName?.takeIf { it.isNotBlank() }.orEmpty()
            iconKey = "dana"
        }
        "cash" -> {
            name = label ?: "Tunai"
            subtitle = ""
            iconKey = "cash"
        }
        else -> {
            name = label ?: method ?: "-"
            subtitle = ""
            iconKey = brandIconKey(method)
        }
    }

    return PaymentMethodOption(
        id = method ?: "",
        name = name,
        group = group,
        short = short,
        subtitle = subtitle,
        iconKey = iconKey,
    )
}

/** Tentukan ikon brand internal dari nama bank/metode. */
private fun brandIconKey(raw: String?): String {
    val v = raw?.lowercase()?.trim() ?: return "bank"
    return when {
        v.contains("bca") -> "bca"
        v.contains("dana") -> "dana"
        else -> "bank"
    }
}

fun PayChannelDto.toBankAccounts(): List<BankAccount> =
    accounts?.map { BankAccount(it.bank ?: "-", it.accountNumber ?: "-", it.accountName ?: "-") } ?: emptyList()
