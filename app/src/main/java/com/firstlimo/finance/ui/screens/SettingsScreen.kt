package com.firstlimo.finance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.firstlimo.finance.ui.components.UpToDateDialog
import com.firstlimo.finance.ui.components.UpdateAvailableDialog
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.ui.viewmodel.AuthViewModel
import com.firstlimo.finance.ui.viewmodel.FinancialViewModel
import com.firstlimo.finance.utils.BiometricHelper
import com.firstlimo.finance.utils.ExportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    financialViewModel: FinancialViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val transactions by financialViewModel.transactions.collectAsState()
    val summary by financialViewModel.financialSummary.collectAsState()
    val availableUpdate by financialViewModel.availableUpdate.collectAsState()
    val showUpToDate by financialViewModel.showUpToDateDialog.collectAsState()
    val downloadProgress by financialViewModel.downloadProgress.collectAsState()
    val backupMessage by financialViewModel.backupStatusMessage.collectAsState()

    var showSignOutConfirm by remember { mutableStateOf(false) }
    var isBiometricActive by remember { mutableStateOf(BiometricHelper.isBiometricEnabled(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الإعدادات والحساب",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // كارت الحساب
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(30.dp)
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
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // قائمة الإعدادات
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
                    border = BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column {
                        // الرسوم البيانية والإحصائيات
                        SettingsRow(
                            title = "الرسوم البيانية والإحصائيات",
                            subtitle = "تحليل مرئي للإيرادات، أوجه الصرف، والكوميسيون",
                            icon = Icons.Default.Analytics,
                            iconColor = AccentGold
                        ) {
                            onNavigateToAnalytics()
                        }

                        HorizontalDivider(color = DarkCardBorder)

                        // قفل التطبيق بالبصمة
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = EmeraldCollection,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "قفل التطبيق بالبصمة / PIN",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "طلب البصمة لحماية أموالك عند فتح التطبيق",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            Switch(
                                checked = isBiometricActive,
                                onCheckedChange = { checked ->
                                    isBiometricActive = checked
                                    BiometricHelper.setBiometricEnabled(context, checked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DarkBackground,
                                    checkedTrackColor = EmeraldCollection
                                )
                            )
                        }

                        HorizontalDivider(color = DarkCardBorder)

                        // فحص التحديثات
                        SettingsRow(
                            title = "التحقق من وجود تحديث جديد",
                            subtitle = "الإصدار الحالي (${financialViewModel.currentAppVersion}) • فحص عبر GitHub",
                            icon = Icons.Default.SystemUpdate,
                            iconColor = AccentBlue
                        ) {
                            financialViewModel.checkForAppUpdates(isManualCheck = true)
                        }

                        HorizontalDivider(color = DarkCardBorder)

                        // حفظ نسخة احتياطية على ذاكرة الهاتف
                        SettingsRow(
                            title = "حفظ نسخة احتياطية على الهاتف",
                            subtitle = "حفظ دائم في مجلد المستندات يمنع فقدان البيانات نهائياً",
                            icon = Icons.Default.Save,
                            iconColor = EmeraldCollection
                        ) {
                            financialViewModel.createBackup()
                        }

                        HorizontalDivider(color = DarkCardBorder)

                        // استرجاع النسخة الاحتياطية
                        SettingsRow(
                            title = "استرجاع النسخة الاحتياطية",
                            subtitle = "استعادة كافة العمليات بعد إعادة تثبيت التطبيق",
                            icon = Icons.Default.Restore,
                            iconColor = AccentGold
                        ) {
                            financialViewModel.restoreFromBackup()
                        }

                        HorizontalDivider(color = DarkCardBorder)

                        // تصدير إكسيل
                        SettingsRow(
                            title = "تصدير تقرير الحسابات (Excel / CSV)",
                            subtitle = "مشاركة كشف الحساب عبر واتساب أو البريد",
                            icon = Icons.Default.FileDownload,
                            iconColor = PurpleCommission
                        ) {
                            val file = ExportUtils.exportToCsv(context, transactions, summary)
                            if (file != null) {
                                ExportUtils.shareFile(context, file)
                            }
                        }

                        HorizontalDivider(color = DarkCardBorder)

                        // مزامنة سحابية
                        SettingsRow(
                            title = "مزامنة البيانات الآن",
                            subtitle = "تحديث فوري للبيانات ومطابقتها",
                            icon = Icons.Default.Sync,
                            iconColor = BlueLimoNet
                        ) {
                            financialViewModel.syncUnsynced()
                        }

                        HorizontalDivider(color = DarkCardBorder)

                        // تسجيل الخروج
                        SettingsRow(
                            title = "تسجيل الخروج",
                            subtitle = "بياناتك محفوظة محلياً وسحابياً ويمكنك العودة في أي وقت",
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
    }

    // تنبيه النسخة الاحتياطية
    if (backupMessage != null) {
        AlertDialog(
            onDismissRequest = { financialViewModel.clearBackupMessage() },
            containerColor = DarkSurface,
            title = { Text("النسخ الاحتياطي", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(backupMessage!!, color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { financialViewModel.clearBackupMessage() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("حسناً", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // تنبيه: التطبيق على أحدث إصدار
    if (showUpToDate) {
        UpToDateDialog(
            currentVersion = financialViewModel.currentAppVersion,
            onDismiss = { financialViewModel.dismissUpToDateDialog() }
        )
    }

    // تنبيه: يوجد تحديث جديد
    if (availableUpdate != null) {
        UpdateAvailableDialog(
            release = availableUpdate!!,
            downloadProgress = downloadProgress,
            onUpdateClick = { financialViewModel.startInAppUpdate() },
            onDismiss = { financialViewModel.dismissUpdateDialog() }
        )
    }

    // تأكيد تسجيل الخروج
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            containerColor = DarkSurface,
            title = { Text("تسجيل الخروج", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("هل ترغب في تسجيل الخروج؟ بياناتك محفوظة بأمان ولن تفقد أي عملية.", color = TextSecondary) },
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
                    Text("إلغاء", color = TextSecondary)
                }
            }
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
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
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
            tint = TextSecondary.copy(alpha = 0.4f)
        )
    }
}
