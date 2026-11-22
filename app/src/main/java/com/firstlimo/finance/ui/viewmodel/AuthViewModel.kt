package com.firstlimo.finance.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firstlimo.finance.data.remote.AuthState
import com.firstlimo.finance.data.remote.FirebaseAuthService
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val authService: FirebaseAuthService = FirebaseAuthService()
    private val prefs = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val currentUserId: String?
        get() = authService.currentUserId ?: prefs.getString("local_user_id", null)

    val currentUserEmail: String?
        get() = authService.currentUserEmail ?: prefs.getString("local_user_email", null)

    val currentUserName: String?
        get() = authService.currentDisplayName ?: prefs.getString("local_user_name", null)

    val isAuthenticated: Boolean
        get() = currentUserId != null

    init {
        val uid = currentUserId
        val email = currentUserEmail
        val name = currentUserName
        if (uid != null) {
            _authState.value = AuthState.Success(uid, email ?: "", name ?: "")
        }
    }

    /**
     * تسجيل الدخول المباشر بحساب Google عبر Firebase Credential
     */
    fun signInWithGoogleCredential(credential: AuthCredential) {
        viewModelScope.launch {
            authService.signInWithGoogleCredential(credential).collect { state ->
                _authState.value = state
                if (state is AuthState.Success) {
                    prefs.edit()
                        .putString("local_user_id", state.userId)
                        .putString("local_user_email", state.email)
                        .putString("local_user_name", state.displayName)
                        .apply()
                }
            }
        }
    }

    /**
     * حفظ جلسة حساب Google محلياً لدعم العمل حتى بدون اتصال
     */
    fun loginLocallyWithGoogle(email: String, displayName: String, uid: String) {
        prefs.edit()
            .putString("local_user_id", uid)
            .putString("local_user_email", email)
            .putString("local_user_name", displayName)
            .apply()
        _authState.value = AuthState.Success(uid, email, displayName)
    }

    /**
     * خيار الدخول كضيف محلياً دون مزامنة سحابية (إذا لم يرغب المستخدم بتسجيل إيميل)
     */
    fun loginAsGuest() {
        val guestId = "guest_user"
        prefs.edit()
            .putString("local_user_id", guestId)
            .putString("local_user_email", "حساب محلي")
            .putString("local_user_name", "مستخدم محلي")
            .apply()
        _authState.value = AuthState.Success(guestId, "حساب محلي", "مستخدم محلي")
    }

    fun signOut() {
        authService.signOut()
        prefs.edit().clear().apply()
        _authState.value = AuthState.Idle
    }
}
