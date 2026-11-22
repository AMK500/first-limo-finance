package com.firstlimo.finance.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firstlimo.finance.data.remote.AuthState
import com.firstlimo.finance.ui.theme.*
import com.firstlimo.finance.ui.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: (userId: String) -> Unit
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val user = authState as AuthState.Success
            isSigningIn = false
            onAuthSuccess(user.userId)
        } else if (authState is AuthState.Error) {
            isSigningIn = false
            errorMessage = (authState as AuthState.Error).message
        }
    }

    // تجهيز عميل Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val idToken = account.idToken
                if (!idToken.isNullOrBlank()) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    authViewModel.signInWithGoogleCredential(credential)
                } else {
                    // في حال عدم وجود idToken محلياً (قبل اكتمال الربط الكامل بـ Google Services)، يتم الدخول المباشر بمعرف الإيميل
                    val email = account.email ?: "user@gmail.com"
                    val name = account.displayName ?: "مستخدم فرست ليموزين"
                    val uid = "google_${account.id ?: email.replace("@", "_").replace(".", "_")}"
                    authViewModel.loginLocallyWithGoogle(email, name, uid)
                }
            } else {
                isSigningIn = false
                errorMessage = "لم يتم اختيار حساب Google"
            }
        } catch (e: Exception) {
            isSigningIn = false
            // محاولة التحقق إذا تم الحصول على حساب أساسي بالرغم من استثناء التوكن
            try {
                val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
                if (lastAccount?.email != null) {
                    val email = lastAccount.email!!
                    val name = lastAccount.displayName ?: "مستخدم فرست ليموزين"
                    val uid = "google_${lastAccount.id ?: email.replace("@", "_").replace(".", "_")}"
                    authViewModel.loginLocallyWithGoogle(email, name, uid)
                    return@rememberLauncherForActivityResult
                }
            } catch (_: Exception) {}

            errorMessage = "تعذر تسجيل الدخول بـ Google (${e.localizedMessage ?: "تأكد من خدمات Google Play"})"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
            border = BorderStroke(1.2.dp, DarkCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // أيقونة وشعار فرست ليموزين
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.linearGradient(listOf(AccentBlue.copy(alpha = 0.25f), AccentGold.copy(alpha = 0.2f))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "فرست ليموزين 🚖",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Text(
                    text = "النظام المالي وإدارة الديون السحابية",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // مميزات السحابة المشفرة
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CloudFeatureRow(
                        icon = Icons.Default.CloudDone,
                        title = "حفظ سحابي فوري ومدى الحياة",
                        subtitle = "حفظ ديونك ومصاريفك تلقائياً على Google Cloud"
                    )
                    CloudFeatureRow(
                        icon = Icons.Default.PhoneAndroid,
                        title = "استعادة فورية عند تغيير الهاتف",
                        subtitle = "سجل بنفس الإيميل وتعود بياناتك كاملة بضغطة زر"
                    )
                    CloudFeatureRow(
                        icon = Icons.Default.Security,
                        title = "عزل وخصوصية تامة 100%",
                        subtitle = "كل مستخدم له خزنته المستقلة ولا يمكن لأحد رؤية بياناتك"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage!!,
                        fontSize = 12.sp,
                        color = CrimsonExpense,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // زر تسجيل الدخول الرسمي بحساب Google
                Button(
                    onClick = {
                        isSigningIn = true
                        errorMessage = null

                        try {
                            val defaultWebClientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                            val webClientId = if (defaultWebClientIdRes != 0) {
                                context.getString(defaultWebClientIdRes)
                            } else {
                                "596107741265-l10mgrfmllvph1v75n70jbk4i82msnpo.apps.googleusercontent.com"
                            }

                            val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(webClientId)
                                .requestEmail()

                            val googleSignInClient = GoogleSignIn.getClient(context, gsoBuilder.build())
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        } catch (e: Exception) {
                            isSigningIn = false
                            errorMessage = "تعذر فتح نافذة تسجيل Google: ${e.localizedMessage}"
                        }
                    },
                    enabled = !isSigningIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            color = AccentBlue,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.firstlimo.finance.R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "تسجيل الدخول بحساب Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F1F1F)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // خيار الحساب المحلي للأجهزة التي لا تدعم خدمات جوجل
                TextButton(
                    onClick = {
                        authViewModel.loginAsGuest()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "المتابعة كحساب محلي (دون مزامنة سحابية)",
                        fontSize = 12.sp,
                        color = TextTertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
    }
}
