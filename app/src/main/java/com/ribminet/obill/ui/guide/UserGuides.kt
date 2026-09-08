package com.ribminet.obill.ui.guide

import com.ribminet.obill.data.remote.OrderDto
import com.ribminet.obill.data.remote.PayChannelDto
import com.ribminet.obill.ui.navigation.Routes

enum class GuideTarget {
    HOME_QUICK_TAGIHAN,
    HOME_QUICK_GANTI_PAKET,
    HOME_QUICK_PENGADUAN,
    HOME_NETWORK_SSID,
    HOME_NETWORK_RX,
    BOTTOM_PROFILE,
    BOTTOM_REPORT,
    PROFILE_EDIT_BIODATA,
    PROFILE_ORDERS,
    OUTSTANDING_SUMMARY,
    OUTSTANDING_PAY,
    PAYMENT_METHODS_LIST,
    PAYMENT_CONTINUE,
    PAYMENT_INSTRUCTION_DETAIL,
    PAYMENT_CONFIRM_BTN,
    PAYMENT_AWAITING_STATUS,
    PAYMENT_CHECK_STATUS,
    WIFI_ACTIONS,
    WIFI_FORM,
    WIFI_SAVE,
    CHANGE_PACKAGE_LIST,
    REPORT_FAB,
    CREATE_REPORT_FORM,
    CREATE_REPORT_SUBMIT,
    ORDERS_LIST,
    EDIT_BIODATA_FORM,
    EDIT_BIODATA_SAVE,
}

data class GuideStep(
    val target: GuideTarget,
    val title: String,
    val message: String,
    val route: String,
    val mainTab: Boolean = false,
)

data class UserGuide(
    val id: String,
    val steps: List<GuideStep>,
)

data class GuideSession(
    val guide: UserGuide,
    val stepIndex: Int = 0,
) {
    val step: GuideStep get() = guide.steps[stepIndex]
    val isLast: Boolean get() = stepIndex >= guide.steps.lastIndex
    val progressLabel: String get() = "${stepIndex + 1}/${guide.steps.size}"
}

/** Pesanan contoh untuk preview panduan (tidak dikirim ke server). */
object GuideDemoData {
    val pendingOrder = OrderDto(
        id = -9001,
        orderNo = "DEMO-PAY-001",
        orderType = "renewal",
        orderTypeLabel = "Perpanjang",
        toProfileName = "Paket Demo 20 Mbps",
        amount = 150_000L,
        paymentMethod = "bank_transfer",
        status = "pending",
        statusLabel = "Menunggu Pembayaran",
        createdAt = "2026-09-06 10:00:00",
        paymentInstruction = PayChannelDto(
            type = "bank_transfer",
            label = "Transfer Bank",
            note = "Transfer ke rekening di bawah, lalu konfirmasi di aplikasi.",
            accounts = listOf(
                com.ribminet.obill.data.remote.BankAccountDto(
                    bank = "Bank Contoh",
                    accountNumber = "1234567890",
                    accountName = "PT Contoh Network",
                ),
            ),
        ),
    )

    val awaitingOrder = pendingOrder.copy(
        id = -9002,
        orderNo = "DEMO-PAY-002",
        status = "awaiting_confirmation",
        statusLabel = "Menunggu Konfirmasi",
        referenceNo = "REF-DEMO-123",
    )
}

object UserGuides {
    const val PAY_BILL = "pay_bill"
    const val PAYMENT_METHODS = "payment_methods"
    const val AFTER_PAY_CONFIRM = "after_pay_confirm"
    const val CHANGE_PACKAGE = "change_package"
    const val ORDER_STATUS = "order_status"
    const val SLOW_INTERNET = "slow_internet"
    const val WIFI_SETTINGS = "wifi_settings"
    const val SEND_COMPLAINT = "send_complaint"
    const val EDIT_BIODATA = "edit_biodata"

    private val payBillDeepSteps = listOf(
        GuideStep(
            target = GuideTarget.HOME_QUICK_TAGIHAN,
            title = "1. Buka Tagihan",
            message = "Dari Beranda, ketuk Tagihan di Akses Cepat untuk membuka halaman Tagihan Berjalan.",
            route = Routes.HOME,
            mainTab = true,
        ),
        GuideStep(
            target = GuideTarget.OUTSTANDING_SUMMARY,
            title = "2. Cek Rincian Tagihan",
            message = "Periksa nominal, periode, kredit/prorata, dan denda (jika ada) sebelum membayar.",
            route = Routes.OUTSTANDING,
        ),
        GuideStep(
            target = GuideTarget.OUTSTANDING_PAY,
            title = "3. Mulai Pembayaran",
            message = "Tekan Bayar Tagihan (atau Bayar Aktivasi). Jika sudah ada pesanan terbuka, buka pesanan tersebut.",
            route = Routes.OUTSTANDING,
        ),
        GuideStep(
            target = GuideTarget.PAYMENT_METHODS_LIST,
            title = "4. Pilih Metode",
            message = "Pilih Tunai, Transfer Bank, atau QRIS sesuai yang diaktifkan admin. Ketuk salah satu metode hingga terpilih.",
            route = Routes.PAYMENT_METHOD,
        ),
        GuideStep(
            target = GuideTarget.PAYMENT_CONTINUE,
            title = "5. Buat Pesanan",
            message = "Tekan Lanjutkan Pembayaran, lalu konfirmasi. Sistem membuat pesanan dan menampilkan instruksi bayar.",
            route = Routes.PAYMENT_METHOD,
        ),
        GuideStep(
            target = GuideTarget.PAYMENT_INSTRUCTION_DETAIL,
            title = "6. Ikuti Instruksi Bayar",
            message = "Di halaman ini ada status pesanan dan detail pembayaran (rekening/QR/tunai). Bayar sesuai instruksi.",
            route = Routes.PAYMENT_INSTRUCTION,
        ),
        GuideStep(
            target = GuideTarget.PAYMENT_CONFIRM_BTN,
            title = "7. Konfirmasi Sudah Bayar",
            message = "Setelah transfer/bayar, tekan Saya Sudah Bayar. Status berubah menjadi Menunggu Konfirmasi admin.",
            route = Routes.PAYMENT_INSTRUCTION,
        ),
        GuideStep(
            target = GuideTarget.PAYMENT_AWAITING_STATUS,
            title = "8. Menunggu Verifikasi",
            message = "Admin memverifikasi pembayaran. Status Menunggu Konfirmasi akan berubah otomatis setelah disetujui.",
            route = Routes.PAYMENT_INSTRUCTION,
        ),
        GuideStep(
            target = GuideTarget.ORDERS_LIST,
            title = "9. Cek di Riwayat Pesanan",
            message = "Anda juga bisa memantau status di Profil → Riwayat Pesanan. Setelah berhasil, layanan diperpanjang otomatis.",
            route = Routes.ORDERS,
        ),
    )

    private val all: Map<String, UserGuide> = mapOf(
        PAY_BILL to UserGuide(id = PAY_BILL, steps = payBillDeepSteps),
        PAYMENT_METHODS to UserGuide(
            id = PAYMENT_METHODS,
            steps = listOf(
                GuideStep(
                    target = GuideTarget.HOME_QUICK_TAGIHAN,
                    title = "Metode Pembayaran",
                    message = "Metode dipilih saat proses bayar. Mulai dari Tagihan di Beranda.",
                    route = Routes.HOME,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.OUTSTANDING_PAY,
                    title = "Masuk ke Alur Bayar",
                    message = "Tekan Bayar Tagihan untuk membuka daftar metode pembayaran.",
                    route = Routes.OUTSTANDING,
                ),
                GuideStep(
                    target = GuideTarget.PAYMENT_METHODS_LIST,
                    title = "Pilihan Metode",
                    message = "Di sini muncul Tunai (bayar di kantor), Transfer Bank, dan/atau QRIS — sesuai pengaturan admin.",
                    route = Routes.PAYMENT_METHOD,
                ),
                GuideStep(
                    target = GuideTarget.PAYMENT_CONTINUE,
                    title = "Lanjut setelah memilih",
                    message = "Setelah memilih metode, tekan Lanjutkan Pembayaran. Instruksi lengkap muncul di langkah berikutnya.",
                    route = Routes.PAYMENT_METHOD,
                ),
                GuideStep(
                    target = GuideTarget.PAYMENT_INSTRUCTION_DETAIL,
                    title = "Instruksi per Metode",
                    message = "Halaman instruksi menampilkan rekening, QR, atau petunjuk tunai sesuai metode yang dipilih.",
                    route = Routes.PAYMENT_INSTRUCTION,
                ),
            ),
        ),
        AFTER_PAY_CONFIRM to UserGuide(
            id = AFTER_PAY_CONFIRM,
            steps = listOf(
                GuideStep(
                    target = GuideTarget.PAYMENT_CONFIRM_BTN,
                    title = "Setelah Bayar",
                    message = "Di Instruksi Pembayaran, tekan Saya Sudah Bayar agar admin menerima konfirmasi Anda.",
                    route = Routes.PAYMENT_INSTRUCTION,
                ),
                GuideStep(
                    target = GuideTarget.PAYMENT_AWAITING_STATUS,
                    title = "Status Menunggu",
                    message = "Status menjadi Menunggu Konfirmasi. Jangan buat pesanan baru selama menunggu verifikasi.",
                    route = Routes.PAYMENT_INSTRUCTION,
                ),
                GuideStep(
                    target = GuideTarget.PAYMENT_CHECK_STATUS,
                    title = "Cek Status",
                    message = "Anda bisa menekan Cek Status Pembayaran, atau menunggu pembaruan otomatis di halaman ini.",
                    route = Routes.PAYMENT_INSTRUCTION,
                ),
                GuideStep(
                    target = GuideTarget.BOTTOM_PROFILE,
                    title = "Lewat Profil",
                    message = "Alternatif: buka tab Profil untuk melihat Riwayat Pesanan.",
                    route = Routes.HOME,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.PROFILE_ORDERS,
                    title = "Menu Riwayat Pesanan",
                    message = "Ketuk Riwayat Pesanan.",
                    route = Routes.PROFILE,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.ORDERS_LIST,
                    title = "Daftar & Status Akhir",
                    message = "Di sini status berubah menjadi Pembayaran Berhasil setelah admin menyetujui. Layanan aktif otomatis.",
                    route = Routes.ORDERS,
                ),
            ),
        ),
        CHANGE_PACKAGE to UserGuide(
            id = CHANGE_PACKAGE,
            steps = listOf(
                GuideStep(
                    target = GuideTarget.HOME_QUICK_GANTI_PAKET,
                    title = "1. Buka Ganti Paket",
                    message = "Dari Beranda ketuk Ganti Paket di Akses Cepat.",
                    route = Routes.HOME,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.CHANGE_PACKAGE_LIST,
                    title = "2. Pilih Paket",
                    message = "Pilih paket tujuan. Akan muncul konfirmasi pengajuan perubahan.",
                    route = Routes.CHANGE_PACKAGE,
                ),
                GuideStep(
                    target = GuideTarget.HOME_QUICK_TAGIHAN,
                    title = "3. Bayar Tagihan Berikutnya",
                    message = "Setelah diajukan, nominal tagihan menyesuaikan paket baru. Selesaikan pembayaran lewat Tagihan seperti biasa.",
                    route = Routes.HOME,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.OUTSTANDING_PAY,
                    title = "4. Selesaikan di Tagihan",
                    message = "Buka Tagihan Berjalan dan bayar. Paket baru aktif setelah pembayaran diverifikasi admin.",
                    route = Routes.OUTSTANDING,
                ),
                GuideStep(
                    target = GuideTarget.PAYMENT_METHODS_LIST,
                    title = "5. Metode & Instruksi",
                    message = "Pilih metode, buat pesanan, lalu ikuti instruksi bayar hingga konfirmasi — sama seperti perpanjang tagihan.",
                    route = Routes.PAYMENT_METHOD,
                ),
            ),
        ),
        ORDER_STATUS to UserGuide(
            id = ORDER_STATUS,
            steps = listOf(
                GuideStep(
                    target = GuideTarget.BOTTOM_PROFILE,
                    title = "1. Buka Profil",
                    message = "Ketuk tab Profil di bilah bawah.",
                    route = Routes.HOME,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.PROFILE_ORDERS,
                    title = "2. Riwayat Pesanan",
                    message = "Ketuk Riwayat Pesanan.",
                    route = Routes.PROFILE,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.ORDERS_LIST,
                    title = "3. Lihat Daftar",
                    message = "Semua pesanan dan statusnya ada di sini. Tarik ke bawah untuk menyegarkan.",
                    route = Routes.ORDERS,
                ),
                GuideStep(
                    target = GuideTarget.ORDERS_LIST,
                    title = "4. Buka Detail",
                    message = "Ketuk salah satu pesanan untuk membuka Instruksi Pembayaran / detail status (menunggu, berhasil, ditolak).",
                    route = Routes.ORDERS,
                ),
            ),
        ),
        SLOW_INTERNET to UserGuide(
            id = SLOW_INTERNET,
            steps = listOf(
                GuideStep(
                    target = GuideTarget.HOME_NETWORK_RX,
                    title = "1. Cek RX Power",
                    message = "Di Beranda, cek RX Power. Jika Offline atau nilai buruk, jaringan mungkin bermasalah.",
                    route = Routes.HOME,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.HOME_NETWORK_SSID,
                    title = "2. Buka WiFi",
                    message = "Ketuk kartu SSID untuk membuka Pengaturan WiFi.",
                    route = Routes.HOME,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.WIFI_ACTIONS,
                    title = "3. Refresh / Reboot",
                    message = "Coba Refresh Data atau Reboot Perangkat terlebih dahulu.",
                    route = Routes.WIFI_SETTINGS,
                ),
                GuideStep(
                    target = GuideTarget.HOME_QUICK_PENGADUAN,
                    title = "4. Jika Masih Bermasalah",
                    message = "Kembali ke Beranda dan ketuk Pengaduan, atau buka tab Laporan.",
                    route = Routes.HOME,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.REPORT_FAB,
                    title = "5. Buat Laporan",
                    message = "Tekan tombol + untuk membuat laporan.",
                    route = Routes.REPORT,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.CREATE_REPORT_FORM,
                    title = "6. Isi Form",
                    message = "Pilih kategori, isi deskripsi, lampirkan foto bila perlu.",
                    route = Routes.CREATE_REPORT,
                ),
                GuideStep(
                    target = GuideTarget.CREATE_REPORT_SUBMIT,
                    title = "7. Kirim",
                    message = "Tekan Kirim Laporan. Pengaduan diteruskan ke admin via WhatsApp.",
                    route = Routes.CREATE_REPORT,
                ),
            ),
        ),
        WIFI_SETTINGS to UserGuide(
            id = WIFI_SETTINGS,
            steps = listOf(
                GuideStep(
                    target = GuideTarget.HOME_NETWORK_SSID,
                    title = "1. Buka dari Beranda",
                    message = "Ketuk kartu SSID di Beranda.",
                    route = Routes.HOME,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.WIFI_ACTIONS,
                    title = "2. Aksi Perangkat",
                    message = "Di sini tersedia Refresh Data dan Reboot Perangkat jika koneksi bermasalah.",
                    route = Routes.WIFI_SETTINGS,
                ),
                GuideStep(
                    target = GuideTarget.WIFI_FORM,
                    title = "3. Ubah SSID / Password",
                    message = "Isi nama WiFi baru dan/atau password (min. 8 karakter). Kosongkan password jika tidak diubah.",
                    route = Routes.WIFI_SETTINGS,
                ),
                GuideStep(
                    target = GuideTarget.WIFI_SAVE,
                    title = "4. Terapkan",
                    message = "Tekan Terapkan Perubahan. Perangkat akan menerapkan setting; klien WiFi mungkin perlu connect ulang.",
                    route = Routes.WIFI_SETTINGS,
                ),
            ),
        ),
        SEND_COMPLAINT to UserGuide(
            id = SEND_COMPLAINT,
            steps = listOf(
                GuideStep(
                    target = GuideTarget.HOME_QUICK_PENGADUAN,
                    title = "1. Dari Akses Cepat",
                    message = "Ketuk Pengaduan di Beranda, atau gunakan tab Laporan.",
                    route = Routes.HOME,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.REPORT_FAB,
                    title = "2. Tombol Buat",
                    message = "Di tab Laporan, tekan tombol +.",
                    route = Routes.REPORT,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.CREATE_REPORT_FORM,
                    title = "3. Isi Laporan",
                    message = "Pilih kategori & jenis masalah, tulis deskripsi, lampirkan foto bukti bila ada.",
                    route = Routes.CREATE_REPORT,
                ),
                GuideStep(
                    target = GuideTarget.CREATE_REPORT_SUBMIT,
                    title = "4. Kirim Laporan",
                    message = "Tekan Kirim Laporan. Tim akan meninjau dan menghubungi Anda.",
                    route = Routes.CREATE_REPORT,
                ),
            ),
        ),
        EDIT_BIODATA to UserGuide(
            id = EDIT_BIODATA,
            steps = listOf(
                GuideStep(
                    target = GuideTarget.BOTTOM_PROFILE,
                    title = "1. Buka Profil",
                    message = "Ketuk tab Profil.",
                    route = Routes.HOME,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.PROFILE_EDIT_BIODATA,
                    title = "2. Edit Biodata",
                    message = "Ketuk menu Edit Biodata.",
                    route = Routes.PROFILE,
                    mainTab = true,
                ),
                GuideStep(
                    target = GuideTarget.EDIT_BIODATA_FORM,
                    title = "3. Ubah Data",
                    message = "Ubah nama, email, alamat, atau foto profil. Nomor WhatsApp & data paket hanya bisa diubah admin.",
                    route = Routes.EDIT_BIODATA,
                ),
                GuideStep(
                    target = GuideTarget.EDIT_BIODATA_SAVE,
                    title = "4. Simpan",
                    message = "Tekan Simpan Perubahan untuk menyimpan biodata ke server.",
                    route = Routes.EDIT_BIODATA,
                ),
            ),
        ),
    )

    fun byId(id: String): UserGuide? = all[id]

    fun demoOrderFor(target: GuideTarget): OrderDto? = when (target) {
        GuideTarget.PAYMENT_INSTRUCTION_DETAIL,
        GuideTarget.PAYMENT_CONFIRM_BTN,
        -> GuideDemoData.pendingOrder
        GuideTarget.PAYMENT_AWAITING_STATUS,
        GuideTarget.PAYMENT_CHECK_STATUS,
        -> GuideDemoData.awaitingOrder
        else -> null
    }
}
