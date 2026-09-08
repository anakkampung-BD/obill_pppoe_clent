package com.ribminet.obill.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.ObillApp
import com.ribminet.obill.data.remote.ApiResult
import com.ribminet.obill.data.remote.LegalBlockDto
import com.ribminet.obill.data.remote.LegalDocumentDto
import com.ribminet.obill.data.remote.LegalSectionDto
import com.ribminet.obill.ui.components.AppCard
import com.ribminet.obill.ui.components.AppTopBar
import com.ribminet.obill.ui.components.PrimaryButton
import com.ribminet.obill.ui.components.SecondaryButton
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.IconChipBlue
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary

enum class LegalDocType(val fallbackTitle: String) {
    TERMS("Syarat & Ketentuan"),
    PRIVACY("Kebijakan Privasi"),
}

@Composable
fun LegalDocScreen(
    type: LegalDocType,
    onBack: () -> Unit,
) {
    val repo = remember { ObillApp.instance.repository }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var doc by remember { mutableStateOf<LegalDocumentDto?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(type, reloadKey) {
        loading = true
        error = null
        val result = when (type) {
            LegalDocType.TERMS -> repo.legalTerms()
            LegalDocType.PRIVACY -> repo.legalPrivacy()
        }
        when (result) {
            is ApiResult.Ok -> {
                val payload = result.data.document
                if (result.data.success && payload != null) {
                    doc = payload
                } else {
                    error = result.data.message ?: "Dokumen tidak tersedia."
                }
            }
            is ApiResult.Err -> error = result.message
        }
        loading = false
    }

    val title = doc?.title?.takeIf { it.isNotBlank() } ?: type.fallbackTitle
    val current = doc

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(title = title, onBack = onBack)

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BrandBlue)
            }
            return@Column
        }

        if (error != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(40.dp))
                Text(error.orEmpty(), color = TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                PrimaryButton(text = "Coba Lagi", onClick = { reloadKey++ })
            }
            return@Column
        }

        if (current == null) {
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (current.intro.isNotEmpty()) {
                AppCard {
                    current.intro.forEachIndexed { index, line ->
                        if (index > 0) Spacer(Modifier.height(8.dp))
                        JustifiedBody(line, color = TextPrimary)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Section "Kontak" sudah ada di API; jangan render ulang dari field contact.
            current.sections.forEach { section ->
                LegalSectionCard(section)
                Spacer(Modifier.height(12.dp))
            }

            val hasContactSection = current.sections.any {
                it.title.orEmpty().contains("kontak", ignoreCase = true)
            }
            val contact = current.contact
            if (!hasContactSection &&
                contact != null &&
                (!contact.email.isNullOrBlank() || !contact.whatsapp.isNullOrBlank())
            ) {
                AppCard {
                    Text("Kontak", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    contact.email?.takeIf { it.isNotBlank() }?.let {
                        Text("Email: $it", color = TextSecondary, fontSize = 13.sp)
                    }
                    contact.whatsapp?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("WhatsApp: $it", color = TextSecondary, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            val meta = listOfNotNull(
                current.version?.takeIf { it.isNotBlank() }?.let { "Versi $it" },
                current.updatedAt?.takeIf { it.isNotBlank() }?.let { "Diperbarui $it" },
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
            }

            val htmlUrl = current.htmlUrl?.takeIf { it.isNotBlank() }
            if (htmlUrl != null) {
                SecondaryButton(
                    text = "Buka di Browser",
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl)))
                        }
                    },
                )
                Spacer(Modifier.height(24.dp))
            } else {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun LegalSectionCard(section: LegalSectionDto) {
    AppCard {
        val sectionTitle = section.title?.takeIf { it.isNotBlank() }
        if (sectionTitle != null) {
            Text(sectionTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
        }
        section.paragraphs.forEachIndexed { index, paragraph ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            JustifiedBody(paragraph, color = TextSecondary)
        }
        if (section.items.isNotEmpty()) {
            if (section.paragraphs.isNotEmpty()) Spacer(Modifier.height(8.dp))
            section.items.forEach { item ->
                BulletLine(item)
            }
        }
        section.blocks.forEach { block ->
            Spacer(Modifier.height(10.dp))
            LegalBlockContent(block)
        }
    }
}

@Composable
private fun LegalBlockContent(block: LegalBlockDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IconChipBlue)
            .padding(12.dp),
    ) {
        val subtitle = block.subtitle?.takeIf { it.isNotBlank() }
        if (subtitle != null) {
            Text(subtitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrandBlue)
            Spacer(Modifier.height(6.dp))
        }
        block.paragraphs.forEachIndexed { index, paragraph ->
            if (index > 0) Spacer(Modifier.height(6.dp))
            JustifiedBody(paragraph, color = TextSecondary)
        }
        block.items.forEach { item ->
            BulletLine(item)
        }
    }
}

@Composable
private fun JustifiedBody(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        color = color,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Justify,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BulletLine(text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("•", color = BrandBlue, fontSize = 13.sp, modifier = Modifier.width(14.dp))
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Justify,
            modifier = Modifier.weight(1f),
        )
    }
}
