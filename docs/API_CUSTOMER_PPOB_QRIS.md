# API PPOB QRIS — Aplikasi Customer PPPoE

Endpoint transaksi PPOB mandiri untuk pelanggan internet rumahan (aplikasi mobile PPPoE).

Pelengkap [`API_CUSTOMER_PPOB.md`](API_CUSTOMER_PPOB.md) (katalog & riwayat).  
Autentikasi sama: app key + Bearer dari login OTP langganan.

**Base URL:** `https://[domain]/`

---

## Alur

```
1. GET catalog          → pilih jenis / kategori / produk (wizard di app)
2. POST quote           → (opsional) preview komponen harga
3. POST checkout        → server buat order + QRIS (harga jual + fee cek nama + kode unik)
4. Tampilkan QRIS       → poll GET payment_status
5. Customer bayar QRIS
6. Payhook match nominal → server tandai lunas → Digiflazz (cek nama bila DANA/PLN + topup)
7. payment_status / push → Sukses (SN / token PLN)
```

Digiflazz **tidak** dipanggil sebelum pembayaran lunas.

---

## Komponen harga QRIS

| Produk | Total bayar |
|--------|-------------|
| Pulsa / data / umum | Harga jual + kode unik (0–999) |
| DANA | Harga jual + biaya cek nama DANA + kode unik |
| PLN | Harga jual + biaya cek nama PLN + kode unik |

- **Harga jual** dari `tb_ppob_sell_price` (wajib sudah diatur admin).
- **Biaya cek nama** = harga jual produk cek nama Digiflazz (atau modal jika jual belum diset).
- **Kode unik** dialokasi saat `checkout`, bentrok dicegah dengan antrian hotspot + order PPOB unpaid.

---

## Endpoint

| Method | URL | Auth | Keterangan |
|--------|-----|------|------------|
| GET | `api/customer/ppob/catalog` | App key* | Katalog (+ `needs_name_check`, `name_check_fee`; tanpa produk cek nama) |
| POST | `api/customer/ppob/quote` | Bearer | Preview harga (tanpa QRIS) |
| POST | `api/customer/ppob/checkout` | Bearer | Buat order + QRIS |
| POST | `api/customer/ppob/cancel` | Bearer | Batalkan order belum bayar |
| GET | `api/customer/ppob/payment_status?ref_id=` | Bearer | Poll status bayar + Digiflazz |
| GET | `api/customer/ppob/transaction?ref_id=` | Bearer | Detail transaksi |
| GET | `api/customer/ppob/transactions` | Bearer | Riwayat |

> `POST topup` (langsung Digiflazz) tetap ada untuk kompatibilitas — **jangan dipakai** di app PPPoE; gunakan `checkout`.

---

## 1. Quote — `POST /api/customer/ppob/quote`

```json
{
  "buyer_sku_code": "xl5",
  "customer_no": "081234567890"
}
```

Respons sukses:

```json
{
  "success": true,
  "product": {
    "buyer_sku_code": "xl5",
    "product_name": "XL 5.000",
    "category": "Pulsa",
    "brand": "XL",
    "needs_name_check": false,
    "name_check_type": null
  },
  "pricing": {
    "sell_price": 6500,
    "name_check_fee": 0,
    "name_check_label": "",
    "name_check_sku": null,
    "base_before_unique": 6500,
    "unique_code_note": "Kode unik 0–999 dialokasi saat checkout."
  }
}
```

Contoh DANA: `name_check_fee` > 0, `needs_name_check: true`, `name_check_type: "dana"`.

---

## 2. Checkout — `POST /api/customer/ppob/checkout`

```json
{
  "buyer_sku_code": "danax",
  "customer_no": "082178277876",
  "product_name": "DANA 10.000"
}
```

Respons sukses (ringkas):

```json
{
  "success": true,
  "message": "Order dibuat. Scan QRIS untuk membayar.",
  "ref_id": "PPOB260906…",
  "payment": {
    "pay_channel": "qris_dinamis",
    "payment_status": "unpaid",
    "pay_amount": 10247,
    "breakdown": {
      "sell_price": 10000,
      "name_check_fee": 200,
      "name_check_label": "Cek nama DANA",
      "base_before_unique": 10200,
      "unique_code": 47,
      "unique_code_str": "047",
      "pay_amount": 10247,
      "breakdown_line": "Rp 10.000 + Cek nama DANA Rp 200 + kode unik 047 = Rp 10.247"
    },
    "qris_string": "00020101…",
    "qris_image_url": "https://…/uploads/qris_dinamis/ppob_….png",
    "expire_minutes": 30
  },
  "transaction": { }
}
```

Tampilkan `qris_image_url` / encode `qris_string`. Nominal yang harus dibayar = **`pay_amount`** (persis).

Error umum: `SELL_PRICE_MISSING`, `QRIS_NOT_READY`, `PRODUCT_NOT_FOUND`, `UNIQUE_CODE_EXHAUSTED`.

---

## 3. Poll status — `GET /api/customer/ppob/payment_status?ref_id=…`

Polling disarankan tiap 3–5 detik di layar pembayaran.

Status Digiflazz **Pending** juga di-update oleh cron server (`cron_ppob sync_pending`, tiap menit) meski app ditutup — push `ppob_success` tetap dikirim saat sukses.

| Field | Arti |
|-------|------|
| `is_waiting_payment` | Masih menunggu QRIS |
| `is_paid` | QRIS lunas; Digiflazz mungkin masih jalan |
| `is_success` | Digiflazz sukses (produk terkirim) |
| `status` | `WaitingPayment` / `Pending` / `Sukses` / `Gagal` |
| `transaction.sn` / `token` | SN atau token PLN |
| `transaction.amount` / `display_amount` / `pay_amount` | Total bayar QRIS (bukan `sell_price`) |
| `transaction.payment_breakdown` | Harga jual + fee cek nama + kode unik |

> Jangan tampilkan `sell_price` sebagai nominal bayar di riwayat. Contoh DANA: jual 8000 + cek nama + kode unik = **8616** → pakai `pay_amount` / `amount`.

UI:

1. `is_waiting_payment` → tetap tampilkan QRIS  
2. `is_paid` && !`is_success` → “Memproses…”  
3. `is_success` → sukses + tampilkan SN/token  
4. `status` mengandung `Gagal` / `Dibatalkan` → gagal / batal  

---

## 3b. Batalkan — `POST /api/customer/ppob/cancel`

Batalkan order QRIS yang **belum dibayar** (`payment_status=unpaid` / `WaitingPayment`).

```json
{ "ref_id": "PPOB260906…" }
```

Respons sukses:

```json
{
  "success": true,
  "message": "Transaksi dibatalkan.",
  "ref_id": "PPOB260906…",
  "transaction": {
    "payment_status": "cancelled",
    "status": "Dibatalkan",
    "is_cancelled": true,
    "is_waiting_payment": false
  }
}
```

| HTTP | `code` | Arti |
|------|--------|------|
| 200 | — / `ALREADY_CANCELLED` / `ALREADY_EXPIRED` | Dibatalkan / sudah batal / sudah expired (idempotent) |
| 400 | `FIELDS_REQUIRED` | `ref_id` kosong |
| 404 | `NOT_FOUND` | Bukan milik pelanggan / tidak ada |
| 409 | `ALREADY_PAID` | Sudah lunas — tidak bisa batal |
| 409 | `NOT_CANCELLABLE` | Status tidak mengizinkan batal |

Setelah batal, `pay_amount` tidak lagi dipegang untuk matching payhook (kode unik bebas dipakai order lain).

---

## 4. Payhook (server)

Setelah customer bayar, listener QRIS mengirim nominal ke `POST /api/payhook/notify` (lihat [`API_PAYHOOK.md`](API_PAYHOOK.md)).

Server:

1. Coba match antrian hotspot  
2. Jika tidak ada → match order PPOB `payment_status=unpaid` dengan `pay_amount` sama  
3. Tandai `paid` → Digiflazz cek nama (jika fee > 0) → topup produk → push `ppob_success` + WA  

Tidak perlu endpoint khusus di app untuk konfirmasi bayar.

---

## Status transaksi

| `payment_status` | `status` | Makna |
|------------------|----------|--------|
| `unpaid` | `WaitingPayment` | Belum bayar QRIS |
| `cancelled` | `Dibatalkan` | Dibatalkan pelanggan |
| `expired` | `Gagal` | QRIS kedaluwarsa |
| `paid` | `Pending` | Lunas, Digiflazz proses |
| `paid` | `Sukses` | Selesai |
| `paid` | `Gagal` | Bayar OK, Digiflazz gagal |

---

## Migrasi DB

```bash
mysql … < sql/tb_ppob_transaction_qris_payment.sql
```

Kolom juga bisa dibuat otomatis oleh `ppob_payment_ensure_schema()` saat checkout pertama.

Prasyarat: Digiflazz siap, harga jual produk diset, QRIS dinamis + payhook aktif (sama seperti hotspot).

---

## File terkait

| File | Peran |
|------|--------|
| `application/controllers/Api_customer_ppob.php` | Endpoint |
| `application/helpers/ppob_payment_helper.php` | Quote, checkout, finalize, payhook match |
| `application/helpers/payhook_helper.php` | Match PPOB setelah hotspot |
| `sql/tb_ppob_transaction_qris_payment.sql` | Kolom pembayaran |
