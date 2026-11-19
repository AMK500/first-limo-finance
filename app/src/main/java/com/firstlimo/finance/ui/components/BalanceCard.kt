package com.firstlimo.finance.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlimo.finance.ui.theme.DarkCardSurface
import com.firstlimo.finance.ui.theme.TextPrimary
import com.firstlimo.finance.ui.theme.TextSecondary
import java.text.DecimalFormat

@Composable
fun BalanceCard(
    title: String,
    amount: Double,
    cardColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isLarge: Boolean = false
) {
    val formatter = DecimalFormat("#,##0.00")
    val formattedAmount = "${formatter.format(amount)} ج.م"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCardSurface
        ),
        border = BorderStroke(
            width = if (isLarge) 1.5.dp else 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(cardColor.copy(alpha = 0.6f), cardColor.copy(alpha = 0.15f))
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isLarge) 20.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(if (isLarge) 44.dp else 36.dp)
                            .background(cardColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = cardColor,
                            modifier = Modifier.size(if (isLarge) 26.dp else 22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontSize = if (isLarge) 17.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isLarge) 16.dp else 12.dp))

            Text(
                text = formattedAmount,
                fontSize = if (isLarge) 28.sp else 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = cardColor
            )
        }
    }
}
