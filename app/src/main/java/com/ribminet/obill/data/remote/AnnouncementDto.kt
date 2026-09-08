package com.ribminet.obill.data.remote

import com.google.gson.annotations.SerializedName

/** Pengumuman aktif dari admin (filter segmen di server). Jadwal dalam WIB. */
data class AnnouncementDto(
    val id: Int? = null,
    val title: String? = null,
    val body: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    val segment: String? = null,
    @SerializedName("starts_at") val startsAt: String? = null,
    @SerializedName("ends_at") val endsAt: String? = null,
    val timezone: String? = null,
    @SerializedName("sort_order") val sortOrder: Int? = null,
    @SerializedName("is_read") val isRead: Boolean? = null,
    @SerializedName("read_count") val readCount: Int? = null,
) {
    fun resolvedImageUrl(): String? = imageUrl?.takeIf { it.isNotBlank() }

    fun resolvedId(): String =
        id?.toString()
            ?: "${title.orEmpty()}|${startsAt.orEmpty()}".hashCode().toString()

    /** Sudah ditandai baca (server atau lokal). */
    fun markedRead(): Boolean = isRead == true

    /** Tanggal mulai tayang untuk UI (WIB, tanpa asumsikan UTC). */
    fun displayDate(): String? {
        val raw = startsAt?.takeIf { it.isNotBlank() } ?: return null
        val formatted = formatDateId(raw)
        return formatted.takeIf { it != "-" }
    }
}

data class AnnouncementListResp(
    val success: Boolean = false,
    val message: String? = null,
    val segment: String? = null,
    val timezone: String? = null,
    @SerializedName("server_time") val serverTime: String? = null,
    val count: Int? = null,
    val announcements: List<AnnouncementDto> = emptyList(),
) {
    fun items(): List<AnnouncementDto> = announcements
}

/** Body POST announcement_read (PPPoE: id + Bearer; identitas pelanggan ikut dikirim). */
data class AnnouncementReadReq(
    @SerializedName("announcement_id") val announcementId: Int,
    /** Unique pembaca; PPPoE = customer_id. */
    @SerializedName("reader_key") val readerKey: String? = null,
    /** Nama tampilan pelanggan. */
    @SerializedName("reader_name") val readerName: String? = null,
)

data class AnnouncementReaderDto(
    val name: String? = null,
    @SerializedName("reader_type") val readerType: String? = null,
    @SerializedName("read_at") val readAt: String? = null,
)

data class AnnouncementReadResp(
    val success: Boolean = false,
    val message: String? = null,
    @SerializedName("already_read") val alreadyRead: Boolean? = null,
    @SerializedName("announcement_id") val announcementId: Int? = null,
    @SerializedName("read_count") val readCount: Int? = null,
    @SerializedName("is_read") val isRead: Boolean? = null,
    @SerializedName("reader_name") val readerName: String? = null,
    @SerializedName("reader_names") val readerNames: List<String>? = null,
    val readers: List<AnnouncementReaderDto>? = null,
)

/** Urutkan sesuai spesifikasi API: sort_order naik, lalu starts_at. */
fun List<AnnouncementDto>.sortedForDisplay(): List<AnnouncementDto> =
    sortedWith(
        compareBy<AnnouncementDto> { it.sortOrder ?: Int.MAX_VALUE }
            .thenBy { it.startsAt.orEmpty() },
    )
