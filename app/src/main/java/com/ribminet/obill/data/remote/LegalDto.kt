package com.ribminet.obill.data.remote

import com.google.gson.annotations.SerializedName

data class LegalIndexResp(
    val success: Boolean = false,
    val version: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    val contact: LegalContactDto? = null,
    val documents: List<LegalDocumentSummaryDto> = emptyList(),
)

data class LegalDocumentSummaryDto(
    val id: String? = null,
    val title: String? = null,
    val slug: String? = null,
    val endpoint: String? = null,
    @SerializedName("html_url") val htmlUrl: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
)

data class LegalDocumentResp(
    val success: Boolean = false,
    val document: LegalDocumentDto? = null,
    val message: String? = null,
)

data class LegalDocumentDto(
    val id: String? = null,
    val slug: String? = null,
    val title: String? = null,
    val intro: List<String> = emptyList(),
    val sections: List<LegalSectionDto> = emptyList(),
    val version: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    val contact: LegalContactDto? = null,
    @SerializedName("html_url") val htmlUrl: String? = null,
    @SerializedName("api_url") val apiUrl: String? = null,
)

data class LegalSectionDto(
    val title: String? = null,
    val paragraphs: List<String> = emptyList(),
    val items: List<String> = emptyList(),
    val blocks: List<LegalBlockDto> = emptyList(),
)

data class LegalBlockDto(
    val subtitle: String? = null,
    val paragraphs: List<String> = emptyList(),
    val items: List<String> = emptyList(),
)

data class LegalContactDto(
    val email: String? = null,
    val whatsapp: String? = null,
)
