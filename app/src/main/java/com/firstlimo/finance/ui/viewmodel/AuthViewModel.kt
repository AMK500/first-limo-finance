package com.firstlimo.finance.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firstlimo.finance.data.remote.AuthState
import com.firstlimo.finance.data.remote.FirebaseAuthService
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

    val currentPhoneNumber: String?
        get() = authService.currentPhoneNumber ?: prefs.getString("local_phone_number", null)

    val isAuthenticated: Boolean
        get() = currentUserId != null

    init {
        val uid = currentUserId
        val phone = currentPhoneNumber
        if (uid != null) {
            _authState.value = AuthState.Success(uid, phone ?: "")
        }
    }

    fun loginLocallyWithPhone(phoneNumber: String) {
        val cleanPhone = phoneNumber.trim()
        val uid = "user_$cleanPhone"
        prefs.edit()
            .putString("local_user_id", uid)
            .putString("local_phone_number", cleanPhone)
            .apply()
        _authState.value = AuthState.Success(uid, cleanPhone)
    }

    fun sendVerificationCode(activity: Activity, phoneNumber: String) {
        viewModelScope.launch {
            authService.sendOtp(activity, phoneNumber).collect { state ->
                _authState.value = state
                if (state is AuthState.Success) {
                    prefs.edit()
                        .putString("local_user_id", state.userId)
                        .putString("local_phone_number", state.phoneNumber)
                        .apply()
                }
            }
        }
    }

    fun verifyCode(code: String) {
        viewModelScope.launch {
            authService.verifyOtp(code).collect { state ->
                _authState.value = state
                if (state is AuthState.Success) {
                    prefs.edit()
                        .putString("local_user_id", state.userId)
                        .putString("local_phone_number", state.phoneNumber)
                        .apply()
                }
            }
        }
    }

    fun signOut() {
        authService.signOut()
        prefs.edit().clear().apply()
        _authState.value = AuthState.Idle
    }
}
