package com.ribminet.obill.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream

object ImageUtil {
    data class Photo(val bytes: ByteArray, val mime: String, val name: String)

    /**
     * Membaca gambar dari [uri], menurunkan resolusi, dan mengompres ke JPEG
     * agar ukuran tetap di bawah [maxBytes]. Mengembalikan null bila gagal.
     */
    fun compress(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1600,
        maxBytes: Int = 5 * 1024 * 1024,
    ): Photo? {
        return try {
            val bitmap = decodeBitmap(context, uri) ?: return null
            val scaled = scaleDown(bitmap, maxDimension)
            if (scaled !== bitmap) bitmap.recycle()

            val out = ByteArrayOutputStream()
            var quality = 90
            var bytes: ByteArray
            do {
                out.reset()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                bytes = out.toByteArray()
                quality -= 10
            } while (bytes.size > maxBytes && quality >= 30)

            if (scaled !== bitmap) scaled.recycle()
            if (bytes.size > maxBytes) return null
            Photo(bytes, "image/jpeg", "pengaduan_${System.currentTimeMillis()}.jpg")
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                return ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } catch (_: Exception) {
                // fallback ke BitmapFactory
            }
        }
        return context.contentResolver.openInputStream(uri)?.use { input ->
            val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            BitmapFactory.decodeStream(input, null, options)
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / largest
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }
}
