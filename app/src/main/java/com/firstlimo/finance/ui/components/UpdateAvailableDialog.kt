package com.firstlimo.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.firstlimo.finance.ui.theme.AccentBlue
import com.firstlimo.finance.ui.theme.TextPrimary
import com.firstlimo.finance.ui.theme.TextSecondary

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
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
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
                    text = "يتوفر إصدار جديد برقم (${release.tagName}).",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                if (release.body.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ما الجديد في هذا التحديث:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = release.body,
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
                        fontWeight = FontWeight.Medium,
                        color = AccentBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = AccentBlue,
                        trackColor = Color(0xFFE2E8F0),
                    )
                }
            }
        },
        confirmButton = {
            if (downloadProgress == null) {
                Button(
                    onClick = onUpdateClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("تحديث الآن", color = Color.White, fontWeight = FontWeight.Bold)
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
