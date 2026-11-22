package com.firstlimo.finance.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlimo.finance.ui.theme.*

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String?, address: String?, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { /* منع الإغلاق بالنقر بالخلفية */ },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        ),
        shape = RoundedCornerShape(22.dp),
        containerColor = DarkSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "إضافة عميل جديد",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // حقل الاسم (الحقل الوحيد الإجباري)
                Column {
                    Text(
                        text = "الاسم * (إجباري)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (nameError) CrimsonExpense else TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) nameError = false
                        },
                        placeholder = { Text("اسم العميل / الشخص / الشركة", color = TextTertiary, fontSize = 13.sp) },
                        singleLine = true,
                        isError = nameError,
                        trailingIcon = {
                            if (nameError) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = CrimsonExpense)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkCardSurface,
                            unfocusedContainerColor = DarkCardSurface
                        )
                    )
                    if (nameError) {
                        Text(
                            text = "يرجى إدخال اسم العميل للمتابعة",
                            color = CrimsonExpense,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                        )
                    }
                }

                // حقل رقم الهاتف (اختياري)
                Column {
                    Text(
                        text = "رقم الهاتف (اختياري)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // بادئة مصر +20
                        Surface(
                            modifier = Modifier
                                .height(54.dp)
                                .width(80.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = DarkCardSurface,
                            border = BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "🇪🇬 +20",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            placeholder = { Text("010XXXXXXXX", color = TextTertiary, fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkCardSurface,
                                unfocusedContainerColor = DarkCardSurface
                            )
                        )
                    }
                }

                // حقل العنوان (اختياري)
                Column {
                    Text(
                        text = "العنوان (اختياري)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = { Text("مثال: القاهرة، مدينة نصر...", color = TextTertiary, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkCardSurface,
                            unfocusedContainerColor = DarkCardSurface
                        )
                    )
                }

                // حقل الملاحظات / التصنيف (اختياري)
                Column {
                    Text(
                        text = "ملاحظات إضافية (اختياري)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("مثال: عميل VIP، كابتن رحلات...", color = TextTertiary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkCardSurface,
                            unfocusedContainerColor = DarkCardSurface
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    onConfirm(name, phone, address, notes)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("تأكيد", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBackground)
            }
        }
    )
}
