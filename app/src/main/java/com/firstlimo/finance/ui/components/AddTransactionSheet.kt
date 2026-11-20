package com.firstlimo.finance.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.firstlimo.finance.data.model.FinancialTransaction
import com.firstlimo.finance.data.model.TransactionType
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.utils.DateUtils
import com.firstlimo.finance.utils.ReceiptImageHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    initialType: TransactionType = TransactionType.COLLECTION,
    editingTransaction: FinancialTransaction? = null,
    onDismiss: () -> Unit,
    onSave: (type: TransactionType, amount: Double, notes: String, date: String, time: String, receiptPath: String?) -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(editingTransaction?.type ?: initialType) }
    var amountText by remember { mutableStateOf(editingTransaction?.amount?.toString() ?: "") }
    var notesText by remember { mutableStateOf(editingTransaction?.notes ?: "") }
    var dateText by remember { mutableStateOf(editingTransaction?.dateString ?: DateUtils.getCurrentDate()) }
    var timeText by remember { mutableStateOf(editingTransaction?.timeString ?: DateUtils.getCurrentTime()) }
    var receiptImagePath by remember { mutableStateOf(editingTransaction?.receiptImagePath) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // لاقط الصور من المعرض
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ReceiptImageHelper.saveReceiptImage(context, uri)
            if (savedPath != null) {
                receiptImagePath = savedPath
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ترويسة النافذة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTransaction != null) "تعديل العملية" else "تسجيل عملية جديدة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // اختيار نوع العملية
            Text(
                text = "نوع العملية:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeChip(
                        title = "تحصيل ليموزين",
                        isSelected = selectedType == TransactionType.COLLECTION,
                        color = EmeraldCollection,
                        modifier = Modifier.weight(1f)
                    ) { selectedType = TransactionType.COLLECTION }

                    TypeChip(
                        title = "مصروف ليموزين",
                        isSelected = selectedType == TransactionType.EXPENSE,
                        color = CrimsonExpense,
                        modifier = Modifier.weight(1f)
                    ) { selectedType = TransactionType.EXPENSE }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeChip(
                        title = "كوميسيون (سائق)",
                        isSelected = selectedType == TransactionType.COMMISSION,
                        color = PurpleCommission,
                        modifier = Modifier.weight(1f)
                    ) { selectedType = TransactionType.COMMISSION }

                    TypeChip(
                        title = "إيداع نقدي شخصي",
                        isSelected = selectedType == TransactionType.PERSONAL_DEPOSIT,
                        color = AmberPersonalCash,
                        modifier = Modifier.weight(1f)
                    ) { selectedType = TransactionType.PERSONAL_DEPOSIT }
                }

                TypeChip(
                    title = "مصروف نقدي شخصي",
                    isSelected = selectedType == TransactionType.PERSONAL_EXPENSE,
                    color = CrimsonExpense,
                    modifier = Modifier.fillMaxWidth()
                ) { selectedType = TransactionType.PERSONAL_EXPENSE }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // إدخال المبلغ
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("المبلغ (ج.م)", color = TextSecondary) },
                placeholder = { Text("0.00", color = TextTertiary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkCardSurface,
                    unfocusedContainerColor = DarkCardSurface
                )
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = CrimsonExpense,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // التاريخ والوقت
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("التاريخ", color = TextSecondary) },
                    singleLine = true,
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

                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text("الوقت", color = TextSecondary) },
                    singleLine = true,
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

            Spacer(modifier = Modifier.height(12.dp))

            // الملاحظات والتفاصيل
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("التفاصيل أو الملاحظات (اختياري)", color = TextSecondary) },
                placeholder = { Text("مثال: بنزين، غسيل، رقم الرحلة...", color = TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkCardSurface,
                    unfocusedContainerColor = DarkCardSurface
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // قسم إرفاق الفاتورة / الإيصال
            if (receiptImagePath != null && File(receiptImagePath!!).exists()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkCardSurface,
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = File(receiptImagePath!!),
                                contentDescription = "معاينة الفاتورة",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "تم إرفاق صورة الإيصال بنجاح",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = EmeraldCollection
                            )
                        }

                        IconButton(onClick = { receiptImagePath = null }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف الصورة", tint = CrimsonExpense)
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تصوير أو إرفاق صورة الفاتورة / الإيصال",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // زر الحفظ الفخم
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = "يرجى إدخال مبلغ صحيح أكبر من صفر"
                        return@Button
                    }
                    onSave(selectedType, amount, notesText.trim(), dateText.trim(), timeText.trim(), receiptImagePath)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(
                    text = if (editingTransaction != null) "تحديث العملية" else "حفظ العملية",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TypeChip(
    title: String,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else DarkCardSurface,
        border = if (isSelected) BorderStroke(1.5.dp, color) else BorderStroke(1.dp, DarkCardBorder)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) color else TextSecondary
            )
        }
    }
}
