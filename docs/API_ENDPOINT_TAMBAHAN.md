# Kebutuhan Endpoint API Tambahan — Aplikasi Mobile Pelanggan PPPoE

Dokumen ini melengkapi dokumentasi utama **"API Aplikasi Mobile — PELANGGAN PPPoE"**.
Endpoint yang sudah ada (auth OTP, `/me`, `/payments`, `/bill`, `/bill_pay`, `/upgrade_*`,
`/payment_methods`, `/order_*`) **sudah diintegrasikan** ke aplikasi Android.

Namun ada beberapa fitur di aplikasi yang **belum punya endpoint** di dokumentasi.
Berikut detail kebutuhan + contoh request/response yang diharapkan agar tim developer API web
dapat menyiapkannya. Konvensi sama dengan dokumentasi utama:

- Base URL: `https://[domain-anda]/`
- Header wajib: `Authorization: Bearer <token>`, `X-Customer-App-Key: <kunci-jika-diisi>`, `Content-Type: application/json`
- Format sukses: `{ "success": true, ... }`; gagal: `{ "success": false, "message": "...", "code": "..." }`
- Token kedaluwarsa → `401` `code: UNAUTHORIZED`

> **Catatan integrasi saat ini:** field `signal_dbm`, `ssid`, dan jumlah perangkat di aplikasi
> masih memakai data sementara (placeholder) karena `/me` belum menyertakannya. Setelah endpoint
> di bawah tersedia, mapping di aplikasi (`data/remote/Mappers.kt`) tinggal disesuaikan.

---

## Ringkasan endpoint yang diminta

| # | Method | URL usulan | Dipakai di halaman | Prioritas |
|---|--------|-----------|--------------------|-----------|
| 1 | GET | `api/customer/langganan/monitoring/optical` | Dashboard, Monitoring Redaman Fiber | Tinggi |
| 2 | GET | `api/customer/langganan/monitoring/clients` | Dashboard, Monitoring Client WiFi | Tinggi |
| 3 | GET | `api/customer/langganan/wifi` | Pengaturan WiFi | Tinggi |
| 4 | POST | `api/customer/langganan/wifi_update` | Pengaturan WiFi (ubah SSID/password) | Tinggi |
| 5 | GET | `api/customer/langganan/complaints` | Laporan/Pengaduan | Tinggi |
| 6 | GET | `api/customer/langganan/complaint/{id}` | Detail Pengaduan | Sedang |
| 7 | POST | `api/customer/langganan/complaint_create` | Buat Pengaduan | Tinggi |
| 8 | GET | `api/customer/langganan/complaint_categories` | Form Buat Pengaduan | Sedang |
| 9 | GET | `api/customer/langganan/notifications` | Notifikasi | Sedang |
| 10 | POST | `api/customer/langganan/notifications_read` | Notifikasi (tandai dibaca) | Rendah |
| 11 | GET | `api/customer/langganan/news` | Dashboard (berita/promo) | Rendah |
| 12 | POST | `api/customer/langganan/profile_update` | Edit Biodata | Sedang |
| 13 | POST | `api/customer/langganan/upload_proof` | Konfirmasi bayar (unggah bukti) | Sedang |
| 14 | POST | `api/customer/langganan/device/reboot` | Aksi perangkat (opsional) | Rendah |
| 15 | POST | `api/customer/langganan/push_token` | Registrasi token FCM (opsional) | Rendah |

Selain itu ada **usulan penambahan field** pada `/me` (lihat bagian 16).

---

## 1. Monitoring redaman fiber optik (Genie ACS)

Aplikasi menampilkan kualitas sinyal optik dalam **dBm** (RX/TX power), tegangan, suhu, dan
grafik riwayat 7 hari. Sumber data: Genie ACS (TR-069) per perangkat ONU pelanggan.

### GET `api/customer/langganan/monitoring/optical`

```json
{
  "success": true,
  "device": {
    "online": true,
    "serial_number": "ZTEGC1A2B3C4",
    "model": "ZTE F609",
    "last_inform": "2026-06-28 17:05:11"
  },
  "optical": {
    "rx_power_dbm": -18.4,
    "tx_power_dbm": 2.1,
    "voltage_v": 3.28,
    "bias_current_ma": 12.5,
    "temperature_c": 41.2,
    "status": "good"
  },
  "history": [
    { "date": "2026-06-22", "rx_power_dbm": -18.1 },
    { "date": "2026-06-23", "rx_power_dbm": -18.3 },
    { "date": "2026-06-24", "rx_power_dbm": -18.6 },
    { "date": "2026-06-25", "rx_power_dbm": -18.2 },
    { "date": "2026-06-26", "rx_power_dbm": -18.5 },
    { "date": "2026-06-27", "rx_power_dbm": -18.4 },
    { "date": "2026-06-28", "rx_power_dbm": -18.4 }
  ]
}
```

| Field | Keterangan |
|-------|------------|
| `optical.rx_power_dbm` | Daya terima (paling penting; ditampilkan besar di dashboard). |
| `optical.status` | `good` \| `warning` \| `bad` (mis. good ≥ -25, warning -25..-28, bad < -28). |
| `history[]` | Maksimal 7–30 titik untuk grafik. |

| HTTP | Kondisi |
|------|---------|
| 200 | OK. |
| 404 | `DEVICE_NOT_FOUND` — perangkat belum termapping di ACS. |
| 503 | `ACS_UNAVAILABLE` — Genie ACS tidak merespons. |

---

## 2. Monitoring perangkat (client) WiFi (Genie ACS)

Dashboard menampilkan jumlah perangkat terhubung; halaman monitoring menampilkan daftarnya.

### GET `api/customer/langganan/monitoring/clients`

```json
{
  "success": true,
  "summary": { "total": 5, "online": 4, "offline": 1, "band_24g": 2, "band_5g": 3 },
  "clients": [
    {
      "name": "Redmi Note 12",
      "ip": "192.168.1.10",
      "mac": "A4:50:46:1F:2C:88",
      "band": "5G",
      "signal_dbm": -42,
      "online": true,
      "connected_since": "2026-06-28 08:11:00"
    },
    {
      "name": "CCTV Depan",
      "ip": "192.168.1.20",
      "mac": "AC:84:C6:12:9B:30",
      "band": "2.4G",
      "signal_dbm": -70,
      "online": false,
      "connected_since": null
    }
  ]
}
```

| Field | Keterangan |
|-------|------------|
| `band` | `2.4G` \| `5G`. |
| `signal_dbm` | RSSI perangkat (negatif). |
| `name` | Hostname/alias; jika kosong, app menampilkan MAC. |

---

## 3 & 4. Pengaturan WiFi (lihat & ubah SSID/password)

Pelanggan dapat melihat dan mengganti **nama (SSID)** dan **password** WiFi. Disarankan
mendukung 2.4GHz & 5GHz terpisah. Perubahan diterapkan ke ONU via Genie ACS (SetParameterValues).

### GET `api/customer/langganan/wifi`

```json
{
  "success": true,
  "wifi": [
    { "band": "2.4G", "ssid": "RIBMI-BUDI", "security": "WPA2-PSK", "enabled": true },
    { "band": "5G",   "ssid": "RIBMI-BUDI-5G", "security": "WPA2-PSK", "enabled": true }
  ]
}
```

> Password **tidak** dikembalikan demi keamanan (hanya status). App menampilkan field password kosong.

### POST `api/customer/langganan/wifi_update`

Body:

```json
{
  "band": "2.4G",
  "ssid": "RIBMI-BUDI",
  "password": "passwordbaru123",
  "apply_to_all_bands": false
}
```

| Field | Wajib | Keterangan |
|-------|-------|------------|
| `band` | Ya | `2.4G` \| `5G` \| `all`. |
| `ssid` | Tidak | SSID baru (3–32 karakter). Kosongkan jika tidak diubah. |
| `password` | Tidak | Password baru (min. 8 karakter). Kosongkan jika tidak diubah. |
| `apply_to_all_bands` | Tidak | Terapkan SSID & password ke semua band. |

Respons sukses (proses ACS bisa **asinkron**):

```json
{
  "success": true,
  "message": "Perubahan WiFi sedang diterapkan ke perangkat (1-2 menit).",
  "applied": false,
  "task_id": "acs-task-7781"
}
```

| HTTP | Kondisi |
|------|---------|
| 400 | `INVALID_SSID` / `WEAK_PASSWORD`. |
| 409 | `DEVICE_OFFLINE` — ONU sedang offline, perubahan ditunda. |
| 503 | `ACS_UNAVAILABLE`. |

> Karena penerapan ACS bisa lambat, sertakan `task_id` agar app bisa polling status (opsional:
> `GET api/customer/langganan/wifi/task/{task_id}` → `{ "status": "pending|done|failed" }`).

---

## 5–8. Pengaduan / Komplain (Ticketing)

Aplikasi punya halaman **Laporan/Pengaduan**: daftar tiket, statistik status, buat tiket baru,
dan lihat detail. Saat ini data masih lokal (dummy).

### Status tiket
`menunggu` | `diproses` | `selesai` (boleh tambah `ditolak`).

### GET `api/customer/langganan/complaints`

Query opsional: `status`, `limit` (default 50).

```json
{
  "success": true,
  "summary": { "total": 2, "menunggu": 2, "diproses": 0, "selesai": 0 },
  "count": 2,
  "complaints": [
    {
      "id": "ADU-002",
      "category": "Gangguan Internet",
      "problem": "Tidak Bisa Akses Web",
      "description": "tolong internet saya tidak bisa akses halaman web",
      "status": "menunggu",
      "created_at": "2026-06-22 15:22:00",
      "updated_at": "2026-06-22 15:22:00",
      "admin_response": null
    }
  ]
}
```

### GET `api/customer/langganan/complaint/{id}`

```json
{
  "success": true,
  "complaint": {
    "id": "ADU-002",
    "category": "Gangguan Internet",
    "problem": "Tidak Bisa Akses Web",
    "description": "tolong internet saya tidak bisa akses halaman web",
    "status": "diproses",
    "created_at": "2026-06-22 15:22:00",
    "updated_at": "2026-06-22 16:00:00",
    "admin_response": "Tim teknis sedang memeriksa jaringan area Anda.",
    "attachments": ["https://.../bukti1.jpg"],
    "timeline": [
      { "at": "2026-06-22 15:22:00", "status": "menunggu", "note": "Tiket dibuat" },
      { "at": "2026-06-22 16:00:00", "status": "diproses", "note": "Diteruskan ke teknisi" }
    ]
  }
}
```

### POST `api/customer/langganan/complaint_create`

Body:

```json
{
  "category": "Gangguan Internet",
  "problem": "Tidak Bisa Akses Web",
  "description": "internet saya tidak bisa akses halaman web sejak pagi",
  "attachments": ["https://.../upload-bukti.jpg"]
}
```

| Field | Wajib | Keterangan |
|-------|-------|------------|
| `category` | Ya | Salah satu dari `complaint_categories`. |
| `problem` | Ya | Salah satu problem pada kategori tsb. |
| `description` | Tidak | Penjelasan bebas. |
| `attachments` | Tidak | URL hasil `upload_proof`/`upload`. |

Respons:

```json
{ "success": true, "message": "Pengaduan dibuat.", "complaint": { "id": "ADU-003", "status": "menunggu" } }
```

### GET `api/customer/langganan/complaint_categories`

```json
{
  "success": true,
  "categories": [
    { "category": "Gangguan Internet", "problems": ["Tidak Bisa Akses Web", "Koneksi Lambat", "Internet Putus-Putus", "Tidak Ada Koneksi"] },
    { "category": "Tagihan & Pembayaran", "problems": ["Pembayaran Belum Masuk", "Tagihan Ganda", "Salah Nominal"] },
    { "category": "Perangkat (Modem/Router)", "problems": ["Lampu Modem Merah", "Modem Mati Total", "WiFi Tidak Muncul"] },
    { "category": "Permintaan Layanan", "problems": ["Pindah Lokasi", "Upgrade Paket", "Pemasangan Baru"] },
    { "category": "Lainnya", "problems": ["Pertanyaan Umum", "Saran & Masukan"] }
  ]
}
```

---

## 9 & 10. Notifikasi

Halaman Notifikasi & badge di dashboard.

### GET `api/customer/langganan/notifications`

```json
{
  "success": true,
  "unread_count": 1,
  "count": 2,
  "notifications": [
    {
      "id": 101,
      "title": "Pembayaran Terverifikasi",
      "body": "Pembayaran tagihan Juli 2026 telah diverifikasi.",
      "type": "payment",
      "is_read": false,
      "created_at": "2026-06-28 09:10:00"
    },
    {
      "id": 100,
      "title": "Masa Aktif Akan Berakhir",
      "body": "Langganan Anda berakhir dalam 3 hari.",
      "type": "billing",
      "is_read": true,
      "created_at": "2026-06-27 08:00:00"
    }
  ]
}
```

`type`: `payment` | `billing` | `info` | `promo` | `complaint`.

### POST `api/customer/langganan/notifications_read`

Body: `{ "ids": [101] }` atau `{ "all": true }`.

```json
{ "success": true, "message": "Notifikasi ditandai dibaca.", "unread_count": 0 }
```

---

## 11. Berita / Promo

Dashboard menampilkan banner promo & daftar berita.

### GET `api/customer/langganan/news`

```json
{
  "success": true,
  "promo_banner": {
    "title": "PROMO INTERNET TERCEPAT DI KOTA!",
    "subtitle": "Paket Rumah & Keluarga",
    "highlight": "800 Mbps",
    "price_label": "Mulai Rp 350.000/bln",
    "image_url": "https://.../promo.png",
    "action_url": null
  },
  "news": [
    {
      "id": 9,
      "title": "Pemeliharaan Jaringan 30 Juni",
      "summary": "Akan ada pemeliharaan pukul 01.00-03.00 WIB.",
      "date": "2026-06-25",
      "image_url": "https://.../news.png"
    }
  ]
}
```

---

## 12. Update biodata pelanggan

Halaman Edit Biodata. Field yang boleh diubah pelanggan dibatasi (mis. email & alamat;
`customer_name`/`phone` mungkin perlu verifikasi admin).

### POST `api/customer/langganan/profile_update`

Body:

```json
{ "email": "budi.baru@mail.com", "address": "Jl. Melati No. 5" }
```

Respons (kembalikan objek customer terbaru seperti `/me`):

```json
{ "success": true, "message": "Profil diperbarui.", "customer": { "...": "objek customer" } }
```

| HTTP | Kondisi |
|------|---------|
| 400 | `INVALID_EMAIL` dll. |
| 422 | `FIELD_NOT_EDITABLE` — field tidak diizinkan diubah dari app. |

---

## 13. Unggah bukti transfer / lampiran

Endpoint `order_confirm` & `complaint_create` menerima `proof_url`/`attachments` berupa URL.
Aplikasi butuh endpoint untuk mengunggah file gambar lalu mendapatkan URL-nya.

### POST `api/customer/langganan/upload_proof`

Request: `multipart/form-data` dengan field `file` (image/jpeg|png, maks. 5 MB).
(Header `Authorization` tetap wajib; `Content-Type` diatur otomatis oleh multipart.)

```json
{
  "success": true,
  "url": "https://.../uploads/cust12/bukti-260628-9f3a.jpg",
  "expires_at": null
}
```

| HTTP | Kondisi |
|------|---------|
| 400 | `FILE_TOO_LARGE` / `UNSUPPORTED_TYPE`. |

---

## 14. Aksi perangkat — reboot (opsional)

Memungkinkan pelanggan me-restart ONU/router via ACS.

### POST `api/customer/langganan/device/reboot`

```json
{ "success": true, "message": "Perintah reboot dikirim ke perangkat.", "task_id": "acs-reboot-551" }
```

| HTTP | Kondisi |
|------|---------|
| 409 | `DEVICE_OFFLINE`. |
| 429 | `REBOOT_RATE_LIMITED` — terlalu sering. |

---

## 15. Registrasi token push notification (opsional)

Untuk notifikasi real-time (FCM). Dipanggil setelah login.

### POST `api/customer/langganan/push_token`

```json
{ "token": "fcm-token-abc...", "platform": "android", "app_version": "1.0.0" }
```

```json
{ "success": true, "message": "Token terdaftar." }
```

---

## 16. Usulan penambahan field pada `/me`

Agar dashboard tidak perlu beberapa kali request, idealnya `/me` menambahkan ringkasan
(boleh juga tetap dipisah ke endpoint monitoring di atas):

```jsonc
{
  "success": true,
  "customer": {
    // ... field eksisting ...
    "optical_summary": { "rx_power_dbm": -18.4, "status": "good" },   // opsional
    "wifi_summary":    { "primary_ssid": "RIBMI-BUDI", "band_count": 2 }, // opsional
    "clients_summary": { "online": 4, "total": 5 }                    // opsional
  }
}
```

Jika ditambahkan, mapping di aplikasi (`CustomerDto.toUserProfile()`) tinggal mengisi
`signalDbm`, `ssid`, dan jumlah perangkat dari field-field ini menggantikan placeholder.

---

## Catatan flow yang sudah berubah di aplikasi (mengikuti API yang ada)

1. **Login**: dari username/password menjadi **nomor WhatsApp + OTP 6 digit** (`request_otp` → `verify_otp`).
   Token disimpan lokal; auto-login selama token belum kedaluwarsa.
2. **Bayar tagihan**: halaman "Tunggakan" diganti menjadi **"Tagihan Berjalan"** mengikuti `/bill`
   (satu tagihan perpanjangan + preview tanggal aktif baru), lalu pilih metode (`/payment_methods`) →
   `bill_pay` → instruksi pembayaran manual → `order_confirm`.
3. **Upgrade paket**: daftar paket dari `/upgrade_options` → pilih metode → `upgrade_request` →
   instruksi pembayaran → `order_confirm`.
4. **Pembayaran instan/gateway otomatis dihapus** dari flow karena API hanya mendukung pembayaran
   **manual** (transfer bank / QR DANA / QR GoPay) dengan verifikasi admin.

> Jika nantinya disediakan payment gateway otomatis (VA/QRIS dinamis berstatus realtime), mohon
> sertakan endpoint pembuatan transaksi + callback/polling status agar flow instan bisa diaktifkan kembali.
