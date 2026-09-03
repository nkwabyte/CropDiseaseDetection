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

    private fun formatAuthError(throwable: Throwable, defaultMessage: String): String {
        val raw = (throwable.message ?: throwable.toString()).trim()
        val lower = raw.lowercase()

        return when {
            // Invalid credentials / wrong password / user not found
            raw.contains("17004") ||
            raw.contains("ERROR_INVALID_CREDENTIAL") ||
            raw.contains("INVALID_LOGIN_CREDENTIALS") ||
            raw.contains("ERROR_WRONG_PASSWORD") ||
            raw.contains("ERROR_USER_NOT_FOUND") ||
            lower.contains("wrong-password") ||
            lower.contains("user-not-found") ||
            lower.contains("invalid-credential") ||
            lower.contains("malformed or has expired") ||
            lower.contains("no user record") ||
            lower.contains("password is invalid") ||
            lower.contains("invalid credential") -> {
                "Invalid email or password. Please check your credentials and try again."
            }

            // Invalid email
            raw.contains("17008") ||
            raw.contains("ERROR_INVALID_EMAIL") ||
            lower.contains("invalid-email") ||
            lower.contains("badly formatted") -> {
                "Please enter a valid email address."
            }

            // Account disabled
            raw.contains("17005") ||
            raw.contains("ERROR_USER_DISABLED") ||
            lower.contains("user-disabled") ||
            lower.contains("has been disabled") -> {
                "This account has been disabled. Please contact support."
            }

            // Email already in use
            raw.contains("17007") ||
            raw.contains("ERROR_EMAIL_ALREADY_IN_USE") ||
            lower.contains("email-already-in-use") ||
            lower.contains("already in use") -> {
                "An account with this email already exists. Try signing in instead."
            }

            // Weak password
            raw.contains("17026") ||
            raw.contains("ERROR_WEAK_PASSWORD") ||
            lower.contains("weak-password") ||
            lower.contains("password should be at least") ||
            lower.contains("at least 6 characters") -> {
                "Password is too weak. Please use at least 6 characters."
            }

            // Too many requests
            raw.contains("17010") ||
            raw.contains("ERROR_TOO_MANY_REQUESTS") ||
            lower.contains("too-many-requests") ||
            lower.contains("blocked all requests") -> {
                "Too many attempts. Please wait a moment and try again."
            }

            // Network error
            raw.contains("17020") ||
            raw.contains("ERROR_NETWORK_ERROR") ||
            lower.contains("network error") ||
            lower.contains("network-request-failed") ||
            lower.contains("timeout") ||
            lower.contains("unreachable") -> {
                "Network connection error. Please check your internet connection."
            }

            // Operation not allowed
            raw.contains("17006") ||
            raw.contains("ERROR_OPERATION_NOT_ALLOWED") ||
            lower.contains("operation-not-allowed") -> {
                "Sign-in method is not enabled. Please contact support."
            }

            // Requires recent login
            raw.contains("17014") ||
            raw.contains("ERROR_REQUIRES_RECENT_LOGIN") ||
            lower.contains("requires-recent-login") ||
            lower.contains("recent login") -> {
                "Please sign out and sign in again before performing this action."
            }

            // Raw FIRAuthErrorDomain or other cryptic exception strings fallback
            raw.contains("FIRAuthErrorDomain") || raw.contains("FirebaseAuthException") -> {
                defaultMessage
            }

            raw.isNotBlank() && raw.length < 80 && !raw.contains("Exception") && !raw.contains("{") -> {
                raw
            }

            else -> defaultMessage
        }
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
                _authState.value = AuthState.Error(
                    formatAuthError(e, "Invalid email or password. Please check your credentials and try again.")
                )
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
                _authState.value = AuthState.Error(
                    formatAuthError(e, "Registration failed. Please try again.")
                )
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
                _authState.value = AuthState.Error(
                    formatAuthError(e, "Failed to send reset email. Please try again.")
                )
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
                _authState.value = AuthState.Error(
                    formatAuthError(e, "Failed to delete account. You may need to sign in again.")
                )
            }
        }
    }
}
