package com.firstlimo.finance.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstlimo.finance.data.remote.AuthState
import com.firstlimo.finance.data.remote.FirebaseAuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authService: FirebaseAuthService = FirebaseAuthService()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val currentUserId: String?
        get() = authService.currentUserId

    val currentPhoneNumber: String?
        get() = authService.currentPhoneNumber

    val isAuthenticated: Boolean
        get() = authService.isAuthenticated

    init {
        // التحقق من الجلسة السابقة
        val uid = authService.currentUserId
        val phone = authService.currentPhoneNumber
        if (uid != null) {
            _authState.value = AuthState.Success(uid, phone ?: "")
        }
    }

    fun sendVerificationCode(activity: Activity, phoneNumber: String) {
        viewModelScope.launch {
            authService.sendOtp(activity, phoneNumber).collect { state ->
                _authState.value = state
            }
        }
    }

    fun verifyCode(code: String) {
        viewModelScope.launch {
            authService.verifyOtp(code).collect { state ->
                _authState.value = state
            }
        }
    }

    fun signOut() {
        authService.signOut()
        _authState.value = AuthState.Idle
    }
}
