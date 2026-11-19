package com.firstlimo.finance.data.remote

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit

sealed class AuthState {
    object Idle : AuthState()
    object CodeSent : AuthState()
    object Loading : AuthState()
    data class Success(val userId: String, val phoneNumber: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class FirebaseAuthService(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val currentPhoneNumber: String?
        get() = auth.currentUser?.phoneNumber

    val isAuthenticated: Boolean
        get() = auth.currentUser != null

    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun sendOtp(activity: Activity, phoneNumber: String): Flow<AuthState> = callbackFlow {
        trySend(AuthState.Loading)

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // التحقق التلقائي الفوري (Instant Verification)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { result ->
                        val user = result.user
                        if (user != null) {
                            trySend(AuthState.Success(user.uid, user.phoneNumber ?: phoneNumber))
                        }
                    }
                    .addOnFailureListener { e ->
                        trySend(AuthState.Error(e.localizedMessage ?: "فشل تسجيل الدخول التلقائي"))
                    }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                trySend(AuthState.Error(e.localizedMessage ?: "فشل إرسال كود التحقق. يرجى التأكد من الرقم."))
            }

            override fun onCodeSent(
                verificationIdSent: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = verificationIdSent
                resendToken = token
                trySend(AuthState.CodeSent)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)

        awaitClose { }
    }

    fun verifyOtp(code: String): Flow<AuthState> = callbackFlow {
        trySend(AuthState.Loading)
        val vId = verificationId

        if (vId == null) {
            trySend(AuthState.Error("لم يتم العثور على كود التحقق. يرجى إعادة الإرسال."))
            close()
            return@callbackFlow
        }

        val credential = PhoneAuthProvider.getCredential(vId, code)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    trySend(AuthState.Success(user.uid, user.phoneNumber ?: ""))
                }
            }
            .addOnFailureListener { e ->
                trySend(AuthState.Error(e.localizedMessage ?: "كود التحقق غير صحيح."))
            }

        awaitClose { }
    }

    fun signOut() {
        auth.signOut()
    }
}
