package com.ribminet.obill.data

object DummyData {

    val user = UserProfile(
        fullName = "BUDI HARTONO S",
        customerId = "budi",
        username = "budi",
        email = "",
        whatsapp = "",
        address = "",
        profileCompletion = 60,
        packageName = "PAKET EKONOMIS",
        packageSpeed = "5 Mbps",
        packagePrice = 50_000,
        serviceActive = true,
        signalDbm = -18.4,
        ssid = "RIBMI-BUDI",
        ssidPassword = "budi12345",
        activeUntil = "01 Jul 2026",
        nextDueDate = "01 Jul 2026",
    )

    val packages = listOf(
        InternetPackage(
            id = "eko",
            name = "PAKET EKONOMIS",
            speed = "5 Mbps",
            price = 50_000,
            features = listOf("Unlimited Kuota", "Cocok untuk 1-3 perangkat", "Browsing & Sosial Media"),
            current = true,
        ),
        InternetPackage(
            id = "std",
            name = "PAKET STANDAR",
            speed = "20 Mbps",
            price = 150_000,
            features = listOf("Unlimited Kuota", "Cocok untuk 5-8 perangkat", "Streaming HD & WFH"),
            popular = true,
        ),
        InternetPackage(
            id = "fam",
            name = "PAKET KELUARGA",
            speed = "50 Mbps",
            price = 250_000,
            features = listOf("Unlimited Kuota", "Cocok untuk 10+ perangkat", "Streaming 4K & Gaming"),
        ),
        InternetPackage(
            id = "pro",
            name = "PAKET PRO BISNIS",
            speed = "100 Mbps",
            price = 450_000,
            features = listOf("Unlimited Kuota", "IP Statis Opsional", "Prioritas Jaringan 24 Jam"),
        ),
    )

    val outstanding = listOf(
        OutstandingBill(2026, "Januari 2026", 50_000),
        OutstandingBill(2026, "Februari 2026", 50_000),
        OutstandingBill(2026, "Maret 2026", 50_000),
        OutstandingBill(2025, "Mei 2025", 50_000),
        OutstandingBill(2025, "Juni 2025", 50_000),
        OutstandingBill(2025, "Juli 2025", 50_000),
        OutstandingBill(2025, "Agustus 2025", 50_000),
        OutstandingBill(2025, "September 2025", 50_000),
        OutstandingBill(2025, "Oktober 2025", 50_000),
        OutstandingBill(2025, "November 2025", 50_000),
        OutstandingBill(2025, "Desember 2025", 50_000),
    )

    val payments = listOf(
        Bill(
            id = "PG-1782299224",
            periodLabel = "Tagihan Bulan Mei 2026",
            amount = 50_000,
            status = PaymentStatus.PAID,
            channel = PaymentChannel.INSTANT,
            dateTime = "24 Juni 2026 18:07:04",
            payCode = "PG-1782299224",
            gatewayId = "9abe4ab4-5a47-49aa",
            method = "BRI Virtual Account",
            targetBank = "BRI",
            targetAccount = "-",
            targetOwner = "-",
        ),
        Bill(
            id = "PG-1782298500",
            periodLabel = "Tagihan Bulan Juni 2026",
            amount = 50_000,
            status = PaymentStatus.PAID,
            channel = PaymentChannel.INSTANT,
            dateTime = "24 Juni 2026 18:05:34",
            payCode = "PG-1782298500",
            gatewayId = "7cd2a1b9-2f10-41bc",
            method = "GoPay QRIS",
            targetBank = "GoPay",
            targetAccount = "-",
            targetOwner = "-",
        ),
        Bill(
            id = "RIBMI-9S64nSs4",
            periodLabel = "Tagihan Bulan April 2026",
            amount = 50_000,
            status = PaymentStatus.CANCELLED,
            channel = PaymentChannel.MANUAL,
            dateTime = "17 Juni 2026 11:19:42",
            payCode = "RIBMI-9S64nSs4",
            gatewayId = "-",
            method = "Transfer Manual",
            targetBank = "BNI",
            targetAccount = "074556156",
            targetOwner = "Mukidi",
        ),
        Bill(
            id = "PG-1781002311",
            periodLabel = "Tagihan Bulan Maret 2026",
            amount = 50_000,
            status = PaymentStatus.PENDING,
            channel = PaymentChannel.INSTANT,
            dateTime = "27 Juni 2026 13:31:11",
            payCode = "570947040413",
            gatewayId = "11bd9f0a-7c43-42de",
            method = "Indomaret",
            targetBank = "Indomaret",
            targetAccount = "-",
            targetOwner = "-",
        ),
    )

    val bankAccounts = listOf(
        BankAccount("BCA", "05458465456", "Mukidi"),
        BankAccount("BNI", "074556156", "Mukidi"),
    )

    val paymentMethods = listOf(
        PaymentMethodOption("bca_va", "BCA Virtual Account", "VIRTUAL ACCOUNT", "BCA"),
        PaymentMethodOption("mandiri_va", "Mandiri Virtual Account", "VIRTUAL ACCOUNT", "MANDIRI"),
        PaymentMethodOption("bni_va", "BNI Virtual Account", "VIRTUAL ACCOUNT", "BNI"),
        PaymentMethodOption("bri_va", "BRI Virtual Account", "VIRTUAL ACCOUNT", "BRI"),
        PaymentMethodOption("gopay", "GoPay QRIS", "E-MONEY", "GOPAY"),
        PaymentMethodOption("qris", "QRIS", "E-MONEY", "QRIS"),
        PaymentMethodOption("dana", "Dana", "E-MONEY", "DANA"),
        PaymentMethodOption("alfamart", "Alfamart", "INDOMARET/ALFAMART", "ALFA"),
        PaymentMethodOption("indomaret", "Indomaret", "INDOMARET/ALFAMART", "INDO"),
    )

    val complaints = listOf(
        Complaint(
            id = "ADU-002",
            category = "GANGGUAN INTERNET",
            problem = "TIDAK BISA AKSES WEB",
            description = "tolong internet saya tidak bisa akses halaman web",
            status = ComplaintStatus.MENUNGGU,
            dateTime = "22 Jun 2026, 15:22",
        ),
        Complaint(
            id = "ADU-001",
            category = "GANGGUAN INTERNET",
            problem = "KONEKSI LAMBAT",
            description = "gkxjgxkgx",
            status = ComplaintStatus.MENUNGGU,
            dateTime = "11 Jun 2026, 16:51",
        ),
    )

    val complaintCategories = listOf(
        "Gangguan Internet", "Tagihan & Pembayaran", "Perangkat (Modem/Router)", "Permintaan Layanan", "Lainnya"
    )

    val complaintProblems = mapOf(
        "Gangguan Internet" to listOf("Tidak Bisa Akses Web", "Koneksi Lambat", "Internet Putus-Putus", "Tidak Ada Koneksi"),
        "Tagihan & Pembayaran" to listOf("Pembayaran Belum Masuk", "Tagihan Ganda", "Salah Nominal"),
        "Perangkat (Modem/Router)" to listOf("Lampu Modem Merah", "Modem Mati Total", "WiFi Tidak Muncul"),
        "Permintaan Layanan" to listOf("Pindah Lokasi", "Upgrade Paket", "Pemasangan Baru"),
        "Lainnya" to listOf("Pertanyaan Umum", "Saran & Masukan"),
    )

    val connectedClients = listOf(
        ConnectedClient("Redmi Note 12", "192.168.1.10", "A4:50:46:1F:2C:88", "5 GHz", -42, true),
        ConnectedClient("Laptop-Budi", "192.168.1.11", "3C:91:80:AA:01:5D", "5 GHz", -55, true),
        ConnectedClient("Smart TV LG", "192.168.1.12", "B0:7E:11:43:9F:21", "2.4 GHz", -63, true),
        ConnectedClient("iPhone Istri", "192.168.1.13", "F8:1E:DF:77:80:14", "5 GHz", -48, true),
        ConnectedClient("CCTV Depan", "192.168.1.20", "AC:84:C6:12:9B:30", "2.4 GHz", -70, false),
    )

    val news = listOf<NewsItem>()

    val promoBannerText = "PROMO INTERNET TERCEPAT DI KOTA"
}
