package com.firstlimo.finance.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.firstlimo.finance.data.model.FinancialTransaction
import com.firstlimo.finance.data.model.TransactionType
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.utils.DateUtils
import com.firstlimo.finance.utils.ReceiptImageHelper
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    initialType: TransactionType = TransactionType.COLLECTION,
    editingTransaction: FinancialTransaction? = null,
    onDismiss: () -> Unit,
    onSave: (type: TransactionType, amount: Double, notes: String, date: String, time: String, receiptPath: String?) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    var selectedType by remember { mutableStateOf(editingTransaction?.type ?: initialType) }
    
    // حالة الآلة الحاسبة والمبلغ
    val initialAmountStr = editingTransaction?.let { formatCalcResult(it.amount) } ?: ""
    var amountText by remember { mutableStateOf(initialAmountStr) }
    var calcExpression by remember { mutableStateOf(initialAmountStr) }
    var justCalculated by remember { mutableStateOf(false) }

    var notesText by remember { mutableStateOf(editingTransaction?.notes ?: "") }
    var dateText by remember { mutableStateOf(editingTransaction?.dateString ?: DateUtils.getCurrentDate()) }
    var timeText by remember { mutableStateOf(editingTransaction?.timeString ?: DateUtils.getCurrentTime()) }
    var receiptImagePath by remember { mutableStateOf(editingTransaction?.receiptImagePath) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // لاقط الصور من المعرض للفواتير
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        com.firstlimo.finance.MainActivity.isInternalActionActive = false
        if (uri != null) {
            val savedPath = ReceiptImageHelper.saveReceiptImage(context, uri)
            if (savedPath != null) {
                receiptImagePath = savedPath
            }
        }
    }

    // منطق الآلة الحاسبة
    fun onDigitClick(digit: String) {
        errorMessage = null
        if (justCalculated) {
            calcExpression = digit
            amountText = digit
            justCalculated = false
        } else {
            if (calcExpression == "0" && digit != ".") {
                calcExpression = digit
            } else {
                calcExpression += digit
            }
            // حساب فوري ومباشر للمبلغ
            val live = evaluateLiveExpression(calcExpression)
            if (live != null && live > 0) {
                amountText = formatCalcResult(live)
            } else if (!calcExpression.contains(Regex("[+\\-×÷]"))) {
                amountText = calcExpression
            }
        }
    }

    fun onOperatorClick(op: String) {
        errorMessage = null
        justCalculated = false
        if (calcExpression.isBlank()) {
            if (amountText.isNotBlank()) {
                calcExpression = "$amountText $op "
            } else {
                calcExpression = "0 $op "
            }
            return
        }
        val trimmed = calcExpression.trimEnd()
        if (trimmed.endsWith("+") || trimmed.endsWith("-") || trimmed.endsWith("×") || trimmed.endsWith("÷")) {
            calcExpression = trimmed.dropLast(1).trimEnd() + " $op "
        } else {
            calcExpression = "$calcExpression $op "
        }
    }

    fun onClearClick() {
        calcExpression = ""
        amountText = ""
        justCalculated = false
        errorMessage = null
    }

    fun onBackspaceClick() {
        errorMessage = null
        justCalculated = false
        if (calcExpression.isNotEmpty()) {
            calcExpression = if (calcExpression.endsWith(" ")) {
                calcExpression.dropLast(3).trimEnd()
            } else {
                calcExpression.dropLast(1)
            }
            val live = evaluateLiveExpression(calcExpression)
            if (live != null && live > 0) {
                amountText = formatCalcResult(live)
            } else if (!calcExpression.contains(Regex("[+\\-×÷]"))) {
                amountText = calcExpression
            }
        }
    }

    fun onEqualsClick() {
        if (calcExpression.isBlank()) return
        val evaluated = evaluateLiveExpression(calcExpression) ?: evaluateMathExpression(calcExpression)
        if (evaluated != null && evaluated > 0) {
            val formatted = formatCalcResult(evaluated)
            amountText = formatted
            calcExpression = formatted
            justCalculated = true
            errorMessage = null
        } else if (evaluated != null && evaluated <= 0) {
            val formatted = formatCalcResult(evaluated)
            amountText = formatted
            calcExpression = formatted
            justCalculated = true
            errorMessage = "الناتج يجب أن يكون أكبر من صفر"
        } else {
            errorMessage = "عملية حسابية غير مكتملة أو غير صالحة"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { false } // منع الإغلاق بالسحب لأسفل نهائياً
        ),
        dragHandle = null,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // مسافة أمان لتفادي أزرار الهاتف
                .imePadding()
                .verticalScroll(scrollState)
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

            Spacer(modifier = Modifier.height(10.dp))

            // اختيار نوع العملية
            Text(
                text = "نوع العملية:",
                fontSize = 13.sp,
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

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // شاشة عرض الآلة الحاسبة والمبلغ مع سكرول مخفي وحساب فوري
            // ==========================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkCardSurface,
                border = BorderStroke(1.5.dp, if (errorMessage != null) CrimsonExpense else AccentGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // السطر العلوي: أيقونة وعنوان الآلة الحاسبة وشارة الحساب الفوري
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "الآلة الحاسبة للمبلغ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                        }

                        // شارة الحساب الفوري التلقائي
                        if (calcExpression.contains(Regex("[+\\-×÷]"))) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccentGold.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "حساب فوري",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // سطر العملية الحسابية كاملة مع تمرير أفقي مخفي بسلاسة
                    val expressionScrollState = rememberScrollState()
                    LaunchedEffect(calcExpression) {
                        if (calcExpression.isNotEmpty()) {
                            expressionScrollState.animateScrollTo(expressionScrollState.maxValue)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(expressionScrollState),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (calcExpression.isNotBlank()) calcExpression else "0",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (calcExpression.contains(Regex("[+\\-×÷]"))) TextSecondary else TextTertiary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    // سطر الناتج الفوري والعملة (ج.م)
                    val amountScrollState = rememberScrollState()
                    val displayLiveAmount = remember(calcExpression, amountText) {
                        val live = evaluateLiveExpression(calcExpression)
                        if (live != null && live > 0) {
                            formatCalcResult(live)
                        } else if (amountText.isNotBlank()) {
                            amountText
                        } else {
                            "0"
                        }
                    }

                    LaunchedEffect(displayLiveAmount) {
                        amountScrollState.animateScrollTo(amountScrollState.maxValue)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // خانة ج.م
                        Text(
                            text = "ج.م",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentGold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        // الناتج الحسابي الفوري باللون الأصفر الذهبي الفخم مع تمرير أفقي مخفي للأرقام الكبيرة
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(amountScrollState),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = displayLiveAmount,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AccentGold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = CrimsonExpense,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // لوحة مفاتيح الآلة الحاسبة المدمجة (أزرار العمليات على اليمين)
            // ==========================================
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkCardSurface.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // الصف الأول: ÷ على اليمين، ثم ⌫، ثم C
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CalcButton(
                                text = "÷",
                                color = AccentGold,
                                textColor = AccentGold,
                                containerColor = DarkSurface,
                                modifier = Modifier.weight(1f),
                                onClick = { onOperatorClick("÷") }
                            )
                            CalcIconButton(
                                icon = Icons.AutoMirrored.Filled.Backspace,
                                tint = TextSecondary,
                                containerColor = DarkSurface,
                                modifier = Modifier.weight(1.5f),
                                onClick = { onBackspaceClick() }
                            )
                            CalcButton(
                                text = "C",
                                color = CrimsonExpense,
                                textColor = CrimsonExpense,
                                containerColor = CrimsonExpense.copy(alpha = 0.15f),
                                modifier = Modifier.weight(1.5f),
                                onClick = { onClearClick() }
                            )
                        }

                        // الصف الثاني: × على اليمين، ثم 9، 8، 7
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CalcButton(
                                text = "×",
                                color = AccentGold,
                                textColor = AccentGold,
                                containerColor = DarkSurface,
                                modifier = Modifier.weight(1f),
                                onClick = { onOperatorClick("×") }
                            )
                            CalcButton(text = "9", modifier = Modifier.weight(1f), onClick = { onDigitClick("9") })
                            CalcButton(text = "8", modifier = Modifier.weight(1f), onClick = { onDigitClick("8") })
                            CalcButton(text = "7", modifier = Modifier.weight(1f), onClick = { onDigitClick("7") })
                        }

                        // الصف الثالث: - على اليمين، ثم 6، 5، 4
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CalcButton(
                                text = "-",
                                color = AccentGold,
                                textColor = AccentGold,
                                containerColor = DarkSurface,
                                modifier = Modifier.weight(1f),
                                onClick = { onOperatorClick("-") }
                            )
                            CalcButton(text = "6", modifier = Modifier.weight(1f), onClick = { onDigitClick("6") })
                            CalcButton(text = "5", modifier = Modifier.weight(1f), onClick = { onDigitClick("5") })
                            CalcButton(text = "4", modifier = Modifier.weight(1f), onClick = { onDigitClick("4") })
                        }

                        // الصف الرابع: + على اليمين، ثم 3، 2، 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CalcButton(
                                text = "+",
                                color = AccentGold,
                                textColor = AccentGold,
                                containerColor = DarkSurface,
                                modifier = Modifier.weight(1f),
                                onClick = { onOperatorClick("+") }
                            )
                            CalcButton(text = "3", modifier = Modifier.weight(1f), onClick = { onDigitClick("3") })
                            CalcButton(text = "2", modifier = Modifier.weight(1f), onClick = { onDigitClick("2") })
                            CalcButton(text = "1", modifier = Modifier.weight(1f), onClick = { onDigitClick("1") })
                        }

                        // الصف الخامس: = (احسب) على اليمين، ثم .، 00، 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // زر اليساوي الفخم على اليمين
                            CalcButton(
                                text = "= (احسب)",
                                color = AccentGold,
                                textColor = DarkBackground,
                                containerColor = AccentGold,
                                modifier = Modifier.weight(1.3f),
                                isBold = true,
                                onClick = { onEqualsClick() }
                            )
                            CalcButton(text = ".", modifier = Modifier.weight(0.9f), onClick = { onDigitClick(".") })
                            CalcButton(text = "00", modifier = Modifier.weight(0.9f), onClick = { onDigitClick("00") })
                            CalcButton(text = "0", modifier = Modifier.weight(0.9f), onClick = { onDigitClick("0") })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // خانة التفاصيل أو الملاحظات (تظهر كيبورد الجهاز العادية)
            // ==========================================
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("التفاصيل أو الملاحظات (اختياري)", color = TextSecondary) },
                placeholder = { Text("مثال: بنزين، كارتة، غسيل، رقم الرحلة...", color = TextTertiary) },
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
                    onClick = {
                        com.firstlimo.finance.MainActivity.isInternalActionActive = true
                        imagePickerLauncher.launch("image/*")
                    },
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

            Spacer(modifier = Modifier.height(18.dp))

            // زر الحفظ الفخم
            Button(
                onClick = {
                    keyboardController?.hide()
                    var finalAmount: Double? = null

                    // إذا نسي المستخدم الضغط على = أو كانت هناك عملية حسابية جارية، نحسبها تلقائياً كأنه ضغط على =
                    if (calcExpression.contains(Regex("[+\\-×÷]"))) {
                        val evaluated = evaluateMathExpression(calcExpression)
                        if (evaluated != null && evaluated > 0) {
                            finalAmount = evaluated
                            val formatted = formatCalcResult(evaluated)
                            amountText = formatted
                            calcExpression = formatted
                            justCalculated = true
                        }
                    }

                    if (finalAmount == null || finalAmount <= 0) {
                        finalAmount = amountText.toDoubleOrNull()
                    }
                    if (finalAmount == null || finalAmount <= 0) {
                        finalAmount = evaluateMathExpression(calcExpression)
                    }

                    if (finalAmount == null || finalAmount <= 0) {
                        errorMessage = "يرجى إدخال مبلغ صحيح أكبر من صفر"
                        return@Button
                    }
                    onSave(selectedType, finalAmount, notesText.trim(), dateText.trim(), timeText.trim(), receiptImagePath)
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

// زر الأرقام والعمليات في الآلة الحاسبة
@Composable
private fun CalcButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = DarkCardBorder,
    textColor: Color = TextPrimary,
    containerColor: Color = DarkSurface,
    isBold: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = if (text.length > 2) 13.sp else 16.sp,
                fontWeight = if (isBold || text.length <= 2) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// زر الأيقونة (مثل الحذف ⌫) في الآلة الحاسبة
@Composable
private fun CalcIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = TextSecondary,
    containerColor: Color = DarkSurface,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
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

// دالة لتقييم التعبيرات الرياضية (+, -, ×, ÷)
fun evaluateMathExpression(expr: String): Double? {
    return try {
        val sanitized = expr.replace("×", "*").replace("÷", "/").replace(" ", "").trim()
        if (sanitized.isEmpty()) return null

        var pos = 0
        fun peek(): Char = if (pos < sanitized.length) sanitized[pos] else '\u0000'
        fun get(): Char = if (pos < sanitized.length) sanitized[pos++] else '\u0000'

        fun parseFactor(): Double {
            if (peek() == '+') { get(); return parseFactor() }
            if (peek() == '-') { get(); return -parseFactor() }
            if (peek() == '(') {
                get()
                val v = parseFactor()
                if (peek() == ')') get()
                return v
            }
            val start = pos
            while (peek().isDigit() || peek() == '.') {
                get()
            }
            if (start == pos) return 0.0
            return sanitized.substring(start, pos).toDoubleOrNull() ?: 0.0
        }

        fun parseTerm(): Double {
            var v = parseFactor()
            while (true) {
                when (peek()) {
                    '*' -> { get(); v *= parseFactor() }
                    '/' -> {
                        get()
                        val denom = parseFactor()
                        v = if (denom != 0.0) v / denom else 0.0
                    }
                    else -> return v
                }
            }
        }

        fun parseExpr(): Double {
            var v = parseTerm()
            while (true) {
                when (peek()) {
                    '+' -> { get(); v += parseTerm() }
                    '-' -> { get(); v -= parseTerm() }
                    else -> return v
                }
            }
        }

        val result = parseExpr()
        if (result.isInfinite() || result.isNaN()) null else result
    } catch (e: Exception) {
        null
    }
}

// تنسيق ناتج الآلة الحاسبة دون أصفار عشرية زائدة
fun formatCalcResult(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}

// حساب فوري للتعبير الرياضي حتى لو كان في نهايته معامل قيد الإدخال (+, -, ×, ÷)
fun evaluateLiveExpression(expr: String): Double? {
    val trimmed = expr.trim()
    if (trimmed.isEmpty()) return null
    var clean = trimmed
    while (clean.endsWith("+") || clean.endsWith("-") || clean.endsWith("×") || clean.endsWith("÷") || clean.endsWith(" ") || clean.endsWith(".")) {
        clean = clean.dropLast(1).trim()
    }
    if (clean.isEmpty()) return null
    return evaluateMathExpression(clean)
}
