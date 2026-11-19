package com.firstlimo.finance.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restore
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
import com.firstlimo.finance.data.remote.AuthState
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: (userId: String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val authState by authViewModel.authState.collectAsState()

    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val user = authState as AuthState.Success
            onAuthSuccess(user.userId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
            border = BorderStroke(1.dp, DarkCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "فرست ليموزين",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Text(
                    text = if (authState is AuthState.CodeSent)
                        "أدخل كود التحقق المرسل في رسالة SMS"
                    else
                        "سجل دخولك برقم الهاتف لحفظ واستعادة بياناتك دائماً",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 22.dp)
                )

                if (authState !is AuthState.CodeSent) {
                    // حقل رقم الهاتف
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("رقم الهاتف (مثال: 01027144388)", color = TextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = AccentBlue)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // زر المتابعة والدخول برقم الهاتف فوراً (يعمل دائماً وبأمان تام دون أي تعليق)
                    Button(
                        onClick = {
                            val cleanPhone = phoneNumber.trim()
                            if (cleanPhone.isNotBlank()) {
                                val uid = "user_$cleanPhone"
                                authViewModel.loginLocallyWithPhone(cleanPhone)
                                onAuthSuccess(uid)
                            }
                        },
                        enabled = phoneNumber.trim().length >= 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DarkBackground,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الدخول والمتابعة برقم الهاتف",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // خيار التحقق السحابي عبر SMS
                    OutlinedButton(
                        onClick = {
                            if (activity != null && phoneNumber.isNotBlank()) {
                                val fullPhone = if (phoneNumber.startsWith("+")) phoneNumber
                                else if (phoneNumber.startsWith("0")) "+2$phoneNumber"
                                else "+20$phoneNumber"
                                authViewModel.sendVerificationCode(activity, fullPhone)
                            }
                        },
                        enabled = authState !is AuthState.Loading && phoneNumber.length >= 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                color = AccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "تسجيل ومزامنة سحابية (SMS)",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    // حقل كود التحقق OTP
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 6) otpCode = it },
                        label = { Text("كود التحقق (6 أرقام)", color = TextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGold)
                        },
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (otpCode.length == 6) {
                                authViewModel.verifyCode(otpCode)
                            }
                        },
                        enabled = authState !is AuthState.Loading && otpCode.length == 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldCollection)
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                color = DarkBackground,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = "تأكيد واسترجاع البيانات سحابياً",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBackground
                            )
                        }
                    }
                }

                // عرض رسالة توضيحية عند حدوث خطأ في SMS
                if (authState is AuthState.Error) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ملاحظة: لربط رسائل SMS السحابية يلزم تفعيل Firebase. يمكنك الضغط مباشرة على زر 'الدخول والمتابعة برقم الهاتف' في الأعلى للدخول فوراً وحفظ بياناتك.",
                        color = CrimsonExpense,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
