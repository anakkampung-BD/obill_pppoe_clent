package com.ribminet.obill.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ribminet.obill.data.remote.AnnouncementDto
import com.ribminet.obill.ui.theme.BrandBlue
import com.ribminet.obill.ui.theme.CardWhite
import com.ribminet.obill.ui.theme.IconChipBlue
import com.ribminet.obill.ui.theme.TextPrimary
import com.ribminet.obill.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Segmen pengumuman beranda. Multi-item → slider auto tiap 5 detik.
 * Tekan-tahan menahan auto-slide.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeAnnouncementSection(
    announcements: List<AnnouncementDto>,
    onOpen: (AnnouncementDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (announcements.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Pengumuman",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TextPrimary,
        )
        Spacer(Modifier.height(12.dp))

        if (announcements.size == 1) {
            AnnouncementHomeCard(
                item = announcements.first(),
                onClick = { onOpen(announcements.first()) },
            )
        } else {
            val pagerState = rememberPagerState(
                pageCount = { announcements.size },
            )
            var paused by remember { mutableStateOf(false) }

            LaunchedEffect(announcements.size, paused) {
                if (paused) return@LaunchedEffect
                while (true) {
                    delay(5_000)
                    if (paused) continue
                    val next = (pagerState.currentPage + 1) % announcements.size
                    pagerState.animateScrollToPage(next)
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    key = { page -> announcements[page].resolvedId() },
                ) { page ->
                    AnnouncementHomeCard(
                        item = announcements[page],
                        onClick = { onOpen(announcements[page]) },
                        onPressChange = { pressing -> paused = pressing },
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
        }
    }
}

@Composable
private fun AnnouncementHomeCard(
    item: AnnouncementDto,
    onClick: () -> Unit,
    onPressChange: ((Boolean) -> Unit)? = null,
) {
    val imageUrl = item.resolvedImageUrl()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .then(
                if (onPressChange != null) {
                    Modifier.pointerInput(item.resolvedId()) {
                        detectTapGestures(
                            onPress = {
                                onPressChange(true)
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    onPressChange(false)
                                }
                            },
                            onTap = { onClick() },
                        )
                    }
                } else {
                    Modifier.clickableNoRipple(onClick)
                },
            )
            .padding(14.dp),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Spacer(Modifier.height(12.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(IconChipBlue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title?.takeIf { it.isNotBlank() } ?: "Pengumuman",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    item.body?.takeIf { it.isNotBlank() } ?: "",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp,
                )
                item.displayDate()?.let { date ->
                    Spacer(Modifier.height(4.dp))
                    Text(date, color = TextSecondary.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
