# Integrasi OneSignal & Notifikasi — Obill Mobile

Ringkasan implementasi di aplikasi Android dan **yang masih perlu disiapkan** di server / OneSignal Dashboard.

## Yang sudah diimplementasi di aplikasi

| Fitur | Status |
|-------|--------|
| `GET onesignal_config` → init SDK | ✅ (opsional; App ID sudah di-embed di app) |
| `OneSignal.login(customer.id)` setelah OTP | ✅ |
| `POST register_onesignal` (subscription_id) | ✅ |
| `POST unregister_onesignal` + `OneSignal.logout()` saat logout | ✅ |
| Long-poll `notifications_poll` saat login/foreground | ✅ |
| `GET notifications` + daftar layar Notifikasi | ✅ |
| `GET unread_count` + badge ikon bell di Dashboard | ✅ |
| `POST notifications_read` (per item & tandai semua) | ✅ |
| Tap push → navigasi berdasarkan `data.type` | ✅ |
| Popup in-app (SweetAlert) + banner sistem | ✅ |
| Background sync feed (WorkManager ~15 menit) | ✅ |

### Navigasi saat tap push

| `type` | Layar tujuan |
|--------|----------------|
| `billing_reminder`, `expiry_reminder_24h` | Tagihan (`OUTSTANDING`) |
| `payment_verified` | Riwayat pembayaran (`HISTORY`) |
| `payment_rejected` | Instruksi pembayaran / pesanan (`PAYMENT_INSTRUCTION` atau `ORDERS`) |
| lainnya | Daftar notifikasi |

---

## Yang WAJIB disiapkan di server

1. **OneSignal app terpisah** untuk APK pelanggan (bukan app admin).
2. Simpan di server:
   - `customer_app_onesignal_app_id`
   - `customer_app_onesignal_api_key` (`os_v2_app_…`) — **hanya di server**, jangan di APK.
3. Pastikan endpoint berikut aktif:
   - `GET /api/customer/langganan/onesignal_config`
   - `POST /api/customer/langganan/register_onesignal`
   - `POST /api/customer/langganan/unregister_onesignal`
   - `GET /api/customer/langganan/notifications_poll`
   - `GET /api/customer/langganan/notifications`
   - `GET /api/customer/langganan/unread_count`
   - `POST /api/customer/langganan/notifications_read`
4. **Cron `check_expired`** harus berjalan (pengingat H-24 & grace).
5. Saat admin verifikasi/tolak pembayaran, server kirim feed + OneSignal push ke `customer_id` yang benar.

---

## Yang WAJIB disiapkan di OneSignal Dashboard

1. Buat aplikasi Android baru (package: `com.obill.aks`).
2. Hubungkan **Firebase Cloud Messaging (FCM)**:
   - Buat project Firebase.
   - Unduh `google-services.json` → letakkan di `app/google-services.json`.
   - Tambahkan plugin Google Services di Gradle (lihat [dokumentasi OneSignal](https://documentation.onesignal.com/docs/android-sdk-setup)).
3. Salin **App ID** ke konfigurasi server (`onesignal_config` API).
4. Salin **REST API Key** ke server saja.

> **Tanpa FCM + `google-services.json`, push background tidak akan sampai ke perangkat.** Feed API & long-poll tetap berfungsi saat aplikasi dibuka.

---

## Checklist tes OneSignal Dashboard

1. Build & install APK ke perangkat/emulator (bukan hanya buka dashboard)
2. Buka app → izin notifikasi **Allow** saat diminta
3. Di logcat filter `ObillOneSignal` — harus muncul:
   - `init appId=1fc0c26f-5f2f-4ff1-bc6e-1cdbfb501ff4`
   - `requestPermission accepted=true`
   - `optedIn=true` + `subscriptionId=...`
4. Klik **Check subscribed users** di dashboard OneSignal

> FCM credential di OneSignal Dashboard sudah cukup untuk emulator dengan Google Play Services — SDK OneSignal mengambil `android_sender_id` dari server OneSignal secara otomatis.

---

```
1fc0c26f-5f2f-4ff1-bc6e-1cdbfb501ff4
```

Disimpan di `push/OneSignalConfig.kt`. SDK di-init saat app start tanpa menunggu API server.

---

## Alur aplikasi

```
App start
  → OneSignal.initWithContext(APP_ID dari OneSignalConfig)

Login OTP sukses
  → OneSignal.login(customer.id)
  → POST register_onesignal(subscription_id)
  → start notifications_poll loop

Foreground
  → notifications_poll(since_id) berulang
  → unread_count → badge bell

Background
  → OneSignal push (jika FCM aktif)

Logout
  → POST unregister_onesignal
  → POST logout { subscription_id }
  → OneSignal.logout()
```

---

## Troubleshooting

| Gejala | Penyebab umum |
|--------|----------------|
| Push tidak sampai | FCM belum dikonfigurasi di OneSignal; `register_onesignal` belum terpanggil |
| Push ke user lain | `OneSignal.login` memakai `customer.id` yang salah |
| Feed kosong, push ada | Normal — refresh layar Notifikasi atau tunggu poll |
| Badge tidak update | Cek `unread_count` API & token Bearer valid |
| `onesignal_config` `configured: false` | App ID / API key belum diset di server |

---

## File kode terkait

- `push/OneSignalManager.kt` — wrapper SDK
- `data/remote/Dto.kt` — model notifikasi & OneSignal
- `data/remote/CustomerApi.kt` — endpoint API
- `AppViewModel.kt` — poll, register, badge, navigasi push
- `ui/screens/MiscScreens.kt` — `NotificationsScreen`
- `ui/screens/DashboardScreen.kt` — badge unread
