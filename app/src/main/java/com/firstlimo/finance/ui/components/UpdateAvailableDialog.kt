package com.firstlimo.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlimo.finance.data.model.GitHubRelease
import com.firstlimo.finance.ui.theme.*

@Composable
fun UpdateAvailableDialog(
    release: GitHubRelease,
    downloadProgress: Int?,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (downloadProgress == null) onDismiss()
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "تحديث جديد متوفر!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "يتوفر إصدار جديد برقم (${release.getCleanVersion()}).",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                )

                val notes = release.getDisplayNotes()
                if (notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "ما الجديد في هذا الإصدار:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notes,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 5
                    )
                }

                if (downloadProgress != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "جارِ تحميل التحديث... $downloadProgress%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = AccentGold,
                        trackColor = DarkCardBorder,
                    )
                }
            }
        },
        confirmButton = {
            if (downloadProgress == null) {
                Button(
                    onClick = onUpdateClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("تحميل وتحديث الآن", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (downloadProgress == null) {
                TextButton(onClick = onDismiss) {
                    Text("لاحقاً", color = TextSecondary)
                }
            }
        }
    )
}

@Composable
fun UpToDateDialog(
    currentVersion: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(EmeraldCollection.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldCollection,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "أنت على أحدث إصدار! ✅",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Text(
                text = "تطبيقك محدّث بالكامل إلى الإصدار ($currentVersion). لا توجد تحديثات جديدة متاحة حالياً.",
                fontSize = 14.sp,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldCollection)
            ) {
                Text("حسناً", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        }
    )
}
