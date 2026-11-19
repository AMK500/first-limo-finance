package com.firstlimo.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlimo.finance.ui.theme.DarkCardBorder
import com.firstlimo.finance.ui.theme.DarkCardSurface
import com.firstlimo.finance.ui.theme.EmeraldCollection
import com.firstlimo.finance.ui.theme.TextSecondary

@Composable
fun SyncStatusBadge(
    isOnline: Boolean,
    isSyncing: Boolean,
    onManualSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        isSyncing -> Color(0xFF38BDF8) // Cyan
        isOnline -> EmeraldCollection    // Green
        else -> Color(0xFFF59E0B)        // Amber
    }

    val statusText = when {
        isSyncing -> "جارِ المزامنة..."
        isOnline -> "سحابي متصل"
        else -> "حفظ محلي آمن"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkCardSurface)
            .clickable { onManualSync() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "مزامنة",
            modifier = Modifier.size(13.dp),
            tint = TextSecondary
        )
    }
}
