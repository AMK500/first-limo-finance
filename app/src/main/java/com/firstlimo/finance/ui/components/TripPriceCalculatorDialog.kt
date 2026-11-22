package com.firstlimo.finance.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.firstlimo.finance.ui.theme.*
import java.text.DecimalFormat

@Composable
fun TripPriceCalculatorDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("calc_prefs", Context.MODE_PRIVATE) }

    var distanceKmText by remember { mutableStateOf("") }
    var fuelRateText by remember { mutableStateOf(prefs.getString("fuel_rate", "8.0") ?: "8.0") }
    var fuelPriceText by remember { mutableStateOf(prefs.getString("fuel_price", "15.0") ?: "15.0") }
    var tollGatesText by remember { mutableStateOf("0") }
    var profitText by remember { mutableStateOf(prefs.getString("desired_profit", "200") ?: "200") }

    val distanceKm = distanceKmText.toDoubleOrNull() ?: 0.0
    val fuelRate = fuelRateText.toDoubleOrNull() ?: 8.0 // لتر لكل 100 كم
    val fuelPrice = fuelPriceText.toDoubleOrNull() ?: 15.0 // ج.م لكل لتر
    val tollGates = tollGatesText.toDoubleOrNull() ?: 0.0
    val profit = profitText.toDoubleOrNull() ?: 0.0

    // الحسابات
    val fuelLiters = (distanceKm / 100.0) * fuelRate
    val fuelCost = fuelLiters * fuelPrice
    val totalTripCost = fuelCost + tollGates
    val suggestedCustomerPrice = totalTripCost + profit

    val df = DecimalFormat("#,##0.0")
    val dfInt = DecimalFormat("#,##0")

    // حفظ تلقائي للإعدادات
    LaunchedEffect(fuelRateText, fuelPriceText, profitText) {
        prefs.edit()
            .putString("fuel_rate", fuelRateText)
            .putString("fuel_price", fuelPriceText)
            .putString("desired_profit", profitText)
            .apply()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                border = BorderStroke(1.2.dp, DarkCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // الترويسة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("حاسبة تسعير المشاوير", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("حساب تكلفة البنزين والكارتات والربح", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // إدخال المسافة بالكيلومتر
                    OutlinedTextField(
                        value = distanceKmText,
                        onValueChange = { distanceKmText = it },
                        label = { Text("المسافة الإجمالية (كم)", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = AccentBlue) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // الكارتات والرسوم
                    OutlinedTextField(
                        value = tollGatesText,
                        onValueChange = { tollGatesText = it },
                        label = { Text("الكارتات وبوابات الرسوم (ج.م)", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Toll, contentDescription = null, tint = AccentGold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // هامش الربح المطلوب
                    OutlinedTextField(
                        value = profitText,
                        onValueChange = { profitText = it },
                        label = { Text("صافي الربح المطلوب للرحلة (ج.م)", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Paid, contentDescription = null, tint = EmeraldCollection) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldCollection,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // صف استهلاك البنزين وسعر اللتر
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = fuelRateText,
                            onValueChange = { fuelRateText = it },
                            label = { Text("لتر / 100كم", fontSize = 11.sp, color = TextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            )
                        )

                        OutlinedTextField(
                            value = fuelPriceText,
                            onValueChange = { fuelPriceText = it },
                            label = { Text("سعر اللتر (ج.م)", fontSize = 11.sp, color = TextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // بطاقة النتائج المباشرة
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ResultRow(
                                title = "استهلاك الوقود المتوقع:",
                                value = "${df.format(fuelLiters)} لتر (${dfInt.format(fuelCost)} ج.م)",
                                valueColor = TextPrimary
                            )
                            ResultRow(
                                title = "إجمالي تكلفة الطريق:",
                                value = "${dfInt.format(totalTripCost)} ج.م",
                                valueColor = CrimsonExpense
                            )
                            ResultRow(
                                title = "صافي الربح المستهدف:",
                                value = "+${dfInt.format(profit)} ج.م",
                                valueColor = EmeraldCollection
                            )

                            HorizontalDivider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("السعر المقترح للعميل:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                                Text("${dfInt.format(suggestedCustomerPrice)} ج.م", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AccentGold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // أزرار الحفظ والنسخ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val quote = """
                                    🚖 عرض سعر رحلة - فرست ليموزين
                                    • المسافة: ${dfInt.format(distanceKm)} كم
                                    • الكارتات والرسوم: ${dfInt.format(tollGates)} ج.م
                                    • إجمالي السعر المطلوب: ${dfInt.format(suggestedCustomerPrice)} ج.م
                                """.trimIndent()

                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("عرض سعر الرحلة", quote)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ تفاصيل العرض إلى الحافظة بنجاح 📋", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ العرض", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Text("إغلاق", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(
    title: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
