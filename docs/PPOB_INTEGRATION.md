# Integrasi PPOB — Obill Mobile

Transaksi PPOB mandiri pelanggan via **QRIS dinamis** (bayar dulu, baru Digiflazz).

Dokumen API server:

- Katalog & riwayat: `API_CUSTOMER_PPOB.md`
- Quote / checkout / payment_status: [`API_CUSTOMER_PPOB_QRIS.md`](API_CUSTOMER_PPOB_QRIS.md)

## Endpoint yang dipakai

| Method | Path | Keterangan |
|--------|------|------------|
| GET | `api/customer/ppob/catalog` | Katalog (+ `needs_name_check`) |
| POST | `api/customer/ppob/quote` | Preview harga (DANA/PLN) |
| POST | `api/customer/ppob/checkout` | Buat order + QRIS |
| GET | `api/customer/ppob/payment_status` | Poll status bayar |
| POST | `api/customer/ppob/cancel` | Batalkan order unpaid |
| GET | `api/customer/ppob/transaction` | Detail |
| GET | `api/customer/ppob/transactions` | Riwayat |
| POST | `api/customer/ppob/inquiry` | Cek tagihan pascabayar |

> `POST topup` / `pay_pasca` **tidak** dipakai di app PPPoE untuk bayar; gunakan `checkout`.

## Aturan transaksi aktif

Jika pelanggan punya order dengan `payment_status=unpaid` / `WaitingPayment`:

1. Masuk PPOB → otomatis diarahkan ke layar **Bayar QRIS** order tersebut
2. Memilih jenis / checkout baru → diblokir, resume ke order unpaid
3. **Batalkan Transaksi** → `POST cancel`, baru boleh buat order baru

QRIS image di-cache lokal (`PpobPaymentPrefs`) karena `payment_status` sering tidak mengembalikan ulang `qris_image_url`.

## Wizard UI

```
PPOB → Prabayar/Pascabayar → Kategori → Produk → Input nomor
  → (DANA/PLN/pasca: Cek Nomor) → Checkout → QRIS (auto-poll)
  → Halaman Sukses
```

| Produk | Tombol di input | Endpoint sebelum QRIS |
|--------|-----------------|------------------------|
| Pulsa / data / umum | **Checkout** | langsung `checkout` |
| DANA / PLN | **Cek Nomor** | `quote` → review → `checkout` |
| Pascabayar | **Cek Nomor** | `inquiry` → review → `checkout` |

Layar QRIS: poll `payment_status` tiap ~4 detik.

- `is_waiting_payment` → tetap tampilkan QR
- `is_paid` && !`is_success` → “Memproses…”
- `is_success` → halaman sukses (+ token PLN bila ada)

## File implementasi

| Komponen | Lokasi |
|----------|--------|
| DTO | `PpobDto.kt` |
| API | `CustomerApi.kt` |
| Repository | `CustomerRepository.kt` |
| ViewModel | `PpobViewModel.kt` |
| UI wizard | `PpobScreen.kt` |
| Push `ppob_success` | `AppViewModel.routeForNotificationType` → detail |

## Prasyarat server

1. Digiflazz + harga jual produk
2. QRIS dinamis + payhook aktif
3. Cron sync katalog
