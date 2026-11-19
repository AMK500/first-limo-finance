package com.firstlimo.finance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlimo.finance.ui.components.UpdateAvailableDialog
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.ui.viewmodel.AuthViewModel
import com.firstlimo.finance.ui.viewmodel.FinancialViewModel
import com.firstlimo.finance.utils.ExportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    financialViewModel: FinancialViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val transactions by financialViewModel.transactions.collectAsState()
    val summary by financialViewModel.financialSummary.collectAsState()
    val availableUpdate by financialViewModel.availableUpdate.collectAsState()
    val downloadProgress by financialViewModel.downloadProgress.collectAsState()

    var showSignOutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الإعدادات والحساب",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDark)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // كارت الحساب
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(AccentBlue.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "الحساب المتصل",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = authViewModel.currentPhoneNumber ?: "المستخدم الحالي",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // عناصر الإعدادات
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    SettingsRow(
                        title = "التحقق من وجود تحديث جديد",
                        subtitle = "فحص التحديثات المباشرة عبر GitHub (الإصدار الحالي: 1.0.0)",
                        icon = Icons.Default.SystemUpdate,
                        iconColor = AccentBlue
                    ) {
                        financialViewModel.checkForAppUpdates("1.0.0")
                    }

                    HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))

                    SettingsRow(
                        title = "تصدير تقرير الحسابات (Excel / CSV)",
                        subtitle = "حفظ كشف الحسابات ومشاركته عبر واتساب",
                        icon = Icons.Default.FileDownload,
                        iconColor = EmeraldCollection
                    ) {
                        val file = ExportUtils.exportToCsv(context, transactions, summary)
                        if (file != null) {
                            ExportUtils.shareFile(context, file)
                        }
                    }

                    HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))

                    SettingsRow(
                        title = "مزامنة البيانات سحابياً الآن",
                        subtitle = "مطابقة العمليات فوراً مع السيرفر",
                        icon = Icons.Default.Sync,
                        iconColor = BlueLimoNet
                    ) {
                        financialViewModel.syncUnsynced()
                    }

                    HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))

                    SettingsRow(
                        title = "تسجيل الخروج",
                        subtitle = "بياناتك محفوظة سحابياً ويمكن استعادتها برقم هاتفك",
                        icon = Icons.Default.Logout,
                        iconColor = CrimsonExpense,
                        isDestructive = true
                    ) {
                        showSignOutConfirm = true
                    }
                }
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("تسجيل الخروج") },
            text = { Text("هل ترغب في تسجيل الخروج؟ لن تفقد أي بيانات؛ حيث أنها محفوظة سحابياً وستعود تلقائياً عند تسجيل الدخول مجدداً بنفس رقم هاتفك.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirm = false
                        authViewModel.signOut()
                        onLoggedOut()
                    }
                ) {
                    Text("خروج", color = CrimsonExpense, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (availableUpdate != null) {
        UpdateAvailableDialog(
            release = availableUpdate!!,
            downloadProgress = downloadProgress,
            onUpdateClick = { financialViewModel.startInAppUpdate() },
            onDismiss = { financialViewModel.dismissUpdateDialog() }
        )
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) CrimsonExpense else TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronLeft,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.5f)
        )
    }
}
