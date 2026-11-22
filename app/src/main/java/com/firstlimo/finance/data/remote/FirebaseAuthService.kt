package com.firstlimo.finance.data.remote

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val userId: String, val email: String, val displayName: String = "") : AuthState()
    data class Error(val message: String) : AuthState()
}

class FirebaseAuthService(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val currentUserEmail: String?
        get() = auth.currentUser?.email

    val currentDisplayName: String?
        get() = auth.currentUser?.displayName

    val isAuthenticated: Boolean
        get() = auth.currentUser != null

    /**
     * تسجيل الدخول بواسطة بيانات اعتماد Google الرسمية
     */
    fun signInWithGoogleCredential(credential: AuthCredential): Flow<AuthState> = callbackFlow {
        trySend(AuthState.Loading)

        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    trySend(
                        AuthState.Success(
                            userId = user.uid,
                            email = user.email ?: "",
                            displayName = user.displayName ?: ""
                        )
                    )
                } else {
                    trySend(AuthState.Error("تعذر الحصول على بيانات المستخدم من Google"))
                }
            }
            .addOnFailureListener { e ->
                trySend(AuthState.Error(e.localizedMessage ?: "فشل تسجيل الدخول بحساب Google. تأكد من الاتصال بالإنترنت."))
            }

        awaitClose { }
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (_: Exception) {}
    }
}
