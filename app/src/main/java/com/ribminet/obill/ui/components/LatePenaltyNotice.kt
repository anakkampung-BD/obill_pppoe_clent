package com.ribminet.obill.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ribminet.obill.data.remote.LatePenaltyDto
import com.ribminet.obill.data.remote.formatDateId
import com.ribminet.obill.ui.theme.DangerRed
import com.ribminet.obill.ui.theme.DangerSurface
import com.ribminet.obill.ui.theme.TextSecondary
import com.ribminet.obill.util.rupiah

@Composable
fun LatePenaltyNotice(
    penalty: LatePenaltyDto,
    modifier: Modifier = Modifier,
) {
    if (penalty.applies != true && (penalty.totalPenalty ?: 0L) <= 0L) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DangerSurface)
            .padding(14.dp),
    ) {
        Text("Denda Keterlambatan", fontWeight = FontWeight.Bold, color = DangerRed, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        penalty.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
        }
        penalty.formulaLabel?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = DangerRed, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
        }
        penalty.dueDate?.let {
            Text("Jatuh tempo: ${formatDateId(it)}", color = TextSecondary, fontSize = 12.sp)
        }
        penalty.lateDays?.takeIf { it > 0 }?.let { days ->
            Text("$days hari keterlambatan", color = TextSecondary, fontSize = 12.sp)
        }
        penalty.dailyPenaltyRate?.takeIf { it > 0 }?.let { rate ->
            Text("Denda per hari: ${rupiah(rate)}", color = TextSecondary, fontSize = 12.sp)
        }
        penalty.totalPenalty?.takeIf { it > 0 }?.let { total ->
            Spacer(Modifier.height(4.dp))
            Text("Total denda: ${rupiah(total)}", fontWeight = FontWeight.Bold, color = DangerRed, fontSize = 13.sp)
        }
    }
}
