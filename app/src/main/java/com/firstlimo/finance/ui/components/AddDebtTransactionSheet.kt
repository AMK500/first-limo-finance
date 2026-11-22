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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.firstlimo.finance.MainActivity
import com.firstlimo.finance.data.local.DebtTransactionEntity
import com.firstlimo.finance.data.local.DebtType
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.utils.DateUtils
import com.firstlimo.finance.utils.ReceiptImageHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtTransactionSheet(
    customerName: String,
    initialType: DebtType,
    editingTransaction: DebtTransactionEntity? = null,
    onDismiss: () -> Unit,
    onSave: (type: DebtType, amount: Double, notes: String, date: String, time: String, receiptPath: String?, dueDate: String?) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    var selectedType by remember {
        mutableStateOf(
            if (editingTransaction != null) {
                if (editingTransaction.type == DebtType.TOOK.name) DebtType.TOOK else DebtType.GAVE
            } else initialType
        )
    }

    val initialAmountStr = editingTransaction?.let { formatCalcResult(it.amount) } ?: ""
    var amountText by remember { mutableStateOf(initialAmountStr) }
    var calcExpression by remember { mutableStateOf(initialAmountStr) }
    var justCalculated by remember { mutableStateOf(false) }

    var notesText by remember { mutableStateOf(editingTransaction?.notes ?: "") }
    var dateText by remember { mutableStateOf(editingTransaction?.dateString ?: DateUtils.getCurrentDate()) }
    var timeText by remember { mutableStateOf(editingTransaction?.timeString ?: DateUtils.getCurrentTime()) }
    var dueDateText by remember { mutableStateOf(editingTransaction?.dueDateString ?: "") }
    var receiptImagePath by remember { mutableStateOf(editingTransaction?.receiptImagePath) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        MainActivity.isInternalActionActive = false
        if (uri != null) {
            val savedPath = ReceiptImageHelper.saveReceiptImage(context, uri)
            if (savedPath != null) {
                receiptImagePath = savedPath
            }
        }
    }

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
            calcExpression = if (amountText.isNotBlank()) "$amountText $op " else "0 $op "
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
        } else {
            errorMessage = "عملية حسابية غير صحيحة"
        }
    }

    val typeColor = if (selectedType == DebtType.TOOK) EmeraldCollection else CrimsonExpense

    val blockNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return available // استهلاك أي حركة سحب زائدة للأعلى أو للأسفل لمنع وصولها للشيت وإغلاقه
            }
        }
    }

    BackHandler {
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = { /* منع الإغلاق التلقائي بالسحب أو النقر بالخلفية */ },
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { false } // منع الخروج بالسحب لأسفل نهائياً
        ),
        dragHandle = null, // إزالة المقبض الرمادي
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(blockNestedScrollConnection)
                .navigationBarsPadding() // مسافة أمان لتفادي أزرار الهاتف السفلية
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // الترويسة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (editingTransaction != null) "تعديل المعاملة" else "تسجيل معاملة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "العميل: $customerName",
                        fontSize = 13.sp,
                        color = AccentGold,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // اختيار نوع المعاملة (أخذت أخضر / أعطيت أحمر)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TypeChip(
                    title = "أخذت (قبضت / استلمت)",
                    isSelected = selectedType == DebtType.TOOK,
                    color = EmeraldCollection,
                    modifier = Modifier.weight(1f)
                ) { selectedType = DebtType.TOOK }

                TypeChip(
                    title = "أعطيت (دفعت / سلفة)",
                    isSelected = selectedType == DebtType.GAVE,
                    color = CrimsonExpense,
                    modifier = Modifier.weight(1f)
                ) { selectedType = DebtType.GAVE }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // شاشة عرض الآلة الحاسبة والمبلغ
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkCardSurface,
                border = BorderStroke(1.5.dp, if (errorMessage != null) CrimsonExpense else typeColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("الآلة الحاسبة للمبلغ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                        }

                        if (calcExpression.contains(Regex("[+\\-×÷]"))) {
                            Surface(shape = RoundedCornerShape(6.dp), color = AccentGold.copy(alpha = 0.15f)) {
                                Text("حساب فوري", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentGold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                        horizontalArrangement = Arrangement.End
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

                    val amountScrollState = rememberScrollState()
                    val displayLiveAmount = remember(calcExpression, amountText) {
                        val live = evaluateLiveExpression(calcExpression)
                        if (live != null && live > 0) formatCalcResult(live)
                        else if (amountText.isNotBlank()) amountText
                        else "0"
                    }

                    LaunchedEffect(displayLiveAmount) {
                        amountScrollState.animateScrollTo(amountScrollState.maxValue)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ج.م", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AccentGold)
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
                Text(errorMessage!!, color = CrimsonExpense, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // لوحة مفاتيح الآلة الحاسبة (العمليات على اليمين)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkCardSurface.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CalcButton(text = "÷", color = AccentGold, textColor = AccentGold, modifier = Modifier.weight(1f), onClick = { onOperatorClick("÷") })
                            CalcIconButton(icon = Icons.AutoMirrored.Filled.Backspace, tint = TextSecondary, modifier = Modifier.weight(1.5f), onClick = { onBackspaceClick() })
                            CalcButton(text = "C", color = CrimsonExpense, textColor = CrimsonExpense, containerColor = CrimsonExpense.copy(alpha = 0.15f), modifier = Modifier.weight(1.5f), onClick = { onClearClick() })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CalcButton(text = "×", color = AccentGold, textColor = AccentGold, modifier = Modifier.weight(1f), onClick = { onOperatorClick("×") })
                            CalcButton(text = "9", modifier = Modifier.weight(1f), onClick = { onDigitClick("9") })
                            CalcButton(text = "8", modifier = Modifier.weight(1f), onClick = { onDigitClick("8") })
                            CalcButton(text = "7", modifier = Modifier.weight(1f), onClick = { onDigitClick("7") })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CalcButton(text = "-", color = AccentGold, textColor = AccentGold, modifier = Modifier.weight(1f), onClick = { onOperatorClick("-") })
                            CalcButton(text = "6", modifier = Modifier.weight(1f), onClick = { onDigitClick("6") })
                            CalcButton(text = "5", modifier = Modifier.weight(1f), onClick = { onDigitClick("5") })
                            CalcButton(text = "4", modifier = Modifier.weight(1f), onClick = { onDigitClick("4") })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CalcButton(text = "+", color = AccentGold, textColor = AccentGold, modifier = Modifier.weight(1f), onClick = { onOperatorClick("+") })
                            CalcButton(text = "3", modifier = Modifier.weight(1f), onClick = { onDigitClick("3") })
                            CalcButton(text = "2", modifier = Modifier.weight(1f), onClick = { onDigitClick("2") })
                            CalcButton(text = "1", modifier = Modifier.weight(1f), onClick = { onDigitClick("1") })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CalcButton(text = "= (احسب)", color = AccentGold, textColor = DarkBackground, containerColor = AccentGold, modifier = Modifier.weight(1.3f), isBold = true, onClick = { onEqualsClick() })
                            CalcButton(text = ".", modifier = Modifier.weight(0.9f), onClick = { onDigitClick(".") })
                            CalcButton(text = "00", modifier = Modifier.weight(0.9f), onClick = { onDigitClick("00") })
                            CalcButton(text = "0", modifier = Modifier.weight(0.9f), onClick = { onDigitClick("0") })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // خانة الملاحظات
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("البيان أو الملاحظات (اختياري)", color = TextSecondary) },
                placeholder = { Text("مثال: دفعة حساب، سلفة، بنزين، كارتة...", color = TextTertiary) },
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            Spacer(modifier = Modifier.height(10.dp))

            // موعد الاستحقاق (اختياري للجمعيات وسلفيات الديون)
            OutlinedTextField(
                value = dueDateText,
                onValueChange = { dueDateText = it },
                label = { Text("موعد استحقاق السداد (اختياري، مثال: 2026-12-01)", fontSize = 12.sp, color = TextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkCardSurface,
                    unfocusedContainerColor = DarkCardSurface
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // إرفاق فاتورة
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
                                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("تم إرفاق صورة الإيصال بنجاح", fontSize = 13.sp, color = EmeraldCollection)
                        }
                        IconButton(onClick = { receiptImagePath = null }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف الصورة", tint = CrimsonExpense)
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        MainActivity.isInternalActionActive = true
                        imagePickerLauncher.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرفاق صورة الإيصال / الفاتورة", fontSize = 13.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // زر الحفظ الفخم
            Button(
                onClick = {
                    keyboardController?.hide()
                    var finalAmount: Double? = null

                    if (calcExpression.contains(Regex("[+\\-×÷]"))) {
                        val evaluated = evaluateLiveExpression(calcExpression)
                        if (evaluated != null && evaluated > 0) {
                            finalAmount = evaluated
                        }
                    }

                    if (finalAmount == null || finalAmount <= 0) {
                        finalAmount = amountText.toDoubleOrNull()
                    }
                    if (finalAmount == null || finalAmount <= 0) {
                        finalAmount = evaluateLiveExpression(calcExpression)
                    }

                    if (finalAmount == null || finalAmount <= 0) {
                        errorMessage = "يرجى إدخال مبلغ صحيح أكبر من صفر"
                        return@Button
                    }
                    onSave(selectedType, finalAmount, notesText.trim(), dateText.trim(), timeText.trim(), receiptImagePath, dueDateText.trim().ifBlank { null })
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = typeColor)
            ) {
                Text(
                    text = if (editingTransaction != null) "تحديث المعاملة" else "تسجيل المعاملة (${selectedType.titleArabic})",
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
        modifier = modifier.height(44.dp).clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

@Composable
private fun CalcIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = TextSecondary,
    containerColor: Color = DarkSurface,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(44.dp).clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.dp, DarkCardBorder.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}
