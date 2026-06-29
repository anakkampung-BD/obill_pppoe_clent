package com.ribminet.obill.util

fun rupiah(amount: Long): String {
    val s = amount.toString().reversed().chunked(3).joinToString(".").reversed()
    return "Rp $s"
}

fun initialsOf(name: String?): String {
    if (name.isNullOrBlank()) return "-"
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "-"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
