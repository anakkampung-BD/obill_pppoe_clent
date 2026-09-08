package com.ribminet.obill.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.ribminet.obill.data.remote.AnnouncementDto
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Modal pengumuman — multi-item sebagai slider auto 5 detik.
 * Tekan-tahan untuk menahan auto-slide.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnnouncementDialog(
    announcements: List<AnnouncementDto>,
    visible: Boolean,
    initialId: String? = null,
    markingRead: Boolean = false,
    onMarkRead: (AnnouncementDto) -> Unit,
    onClose: () -> Unit,
) {
    if (!visible || announcements.isEmpty()) return

    val startIndex = remember(announcements, initialId) {
        val idx = initialId?.let { id -> announcements.indexOfFirst { it.resolvedId() == id } } ?: -1
        if (idx >= 0) idx else 0
    }
    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { announcements.size },
    )
    var paused by remember { mutableStateOf(false) }
    val scale = remember { Animatable(0.92f) }
    val currentItem = announcements.getOrNull(pagerState.currentPage)
    val showMarkRead = currentItem != null && !currentItem.markedRead()

    LaunchedEffect(visible) {
        if (visible) {
            scale.snapTo(0.92f)
            scale.animateTo(1f)
        }
    }

    LaunchedEffect(initialId, announcements) {
        if (!visible) return@LaunchedEffect
        val idx = initialId?.let { id -> announcements.indexOfFirst { it.resolvedId() == id } } ?: -1
        if (idx >= 0 && pagerState.currentPage != idx) {
            pagerState.animateScrollToPage(idx)
        }
    }

    LaunchedEffect(announcements.size, paused, visible) {
        if (!visible || paused || announcements.size <= 1) return@LaunchedEffect
        while (true) {
            delay(5_000)
            if (paused) continue
            val next = (pagerState.currentPage + 1) % announcements.size
            pagerState.animateScrollToPage(next)
        }
    }

    Dialog(
        onDismissRequest = { if (!markingRead) onClose() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !markingRead,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .scale(scale.value)
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardWhite)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            paused = true
                            waitForUpOrCancellation()
                            paused = false
                        }
                    },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 4.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BrandBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Campaign,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (announcements.size > 1) {
                                "Pengumuman ${pagerState.currentPage + 1}/${announcements.size}"
                            } else {
                                "Pengumuman"
                            },
                            color = BrandBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                    IconButton(onClick = onClose, enabled = !markingRead) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !markingRead,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                ) { page ->
                    val item = announcements[page]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 8.dp),
                    ) {
                        val imageUrl = item.resolvedImageUrl()
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                            )
                            Spacer(Modifier.height(14.dp))
                        }
                        Text(
                            item.title?.takeIf { it.isNotBlank() } ?: "Pengumuman",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            item.body?.takeIf { it.isNotBlank() } ?: "",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        item.displayDate()?.let { date ->
                            Spacer(Modifier.height(12.dp))
                            Text(date, color = TextSecondary.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                }

                if (announcements.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        repeat(announcements.size) { index ->
                            val selected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (selected) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) BrandBlue else TextSecondary.copy(alpha = 0.35f),
                                    ),
                            )
                        }
                    }
                }

                if (showMarkRead) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        PrimaryButton(
                            text = if (markingRead) "Menyimpan..." else "Sudah Baca",
                            enabled = !markingRead,
                            onClick = {
                                val current = announcements.getOrNull(pagerState.currentPage) ?: return@PrimaryButton
                                if (current.markedRead()) return@PrimaryButton
                                onMarkRead(current)
                            },
                        )
                    }
                } else {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
