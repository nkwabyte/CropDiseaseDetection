package com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkwabyte.cropdiseasedetection.common.model.UserRole
import com.nkwabyte.cropdiseasedetection.data.repository.SyncRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val syncRepository: SyncRepository) : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun loginWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email.trim(), pass)
                _authState.value = AuthState.Success("Logged in successfully")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun registerWithEmail(email: String, pass: String, userName: String, role: UserRole = UserRole.FARMER) {
        if (email.isBlank() || pass.isBlank() || userName.isBlank()) {
            _authState.value = AuthState.Error("Username, email, and password cannot be empty")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), pass)
                try {
                    result.user?.updateProfile(displayName = userName)
                } catch (pe: Exception) {
                    println("Failed to update display name: ${pe.message}")
                }
                syncRepository.saveUserProfile(userName, email.trim(), role)
                _authState.value = AuthState.Success("Registered successfully")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Email cannot be empty")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email.trim())
                _authState.value = AuthState.Success("Password reset email sent to $email")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to send reset email")
            }
        }
    }

    fun deleteAccount() {
        val user = auth.currentUser
        if (user == null) {
            _authState.value = AuthState.Error("No logged in user to delete")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                syncRepository.anonymizeUserData()
                user.delete()
                _authState.value = AuthState.Success("Account deleted successfully")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to delete account")
            }
        }
    }
}
