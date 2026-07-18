package com.gamearena.booster.auth

import android.content.Context
import android.content.SharedPreferences
import com.gamearena.booster.network.ApiUser
import com.gamearena.booster.network.AuthResponse
import com.gamearena.booster.network.GameArenaApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: GameArenaApi
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences = context.getSharedPreferences("GameArena_auth", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<ApiUser?>(null)
    val currentUser: StateFlow<ApiUser?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        val token = prefs.getString(KEY_TOKEN, null)
        if (!token.isNullOrEmpty()) {
            _isLoggedIn.value = true
            loadProfile()
        }
    }

    fun getTokenBlocking(): String? = prefs.getString(KEY_TOKEN, null)

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        scope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = api.login(mapOf("username" to username, "password" to password))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    saveToken(body.token)
                    _currentUser.value = body.user
                    _isLoggedIn.value = true
                    onSuccess()
                } else {
                    val err = response.errorBody()?.string() ?: "Login failed"
                    _error.value = try {
                        org.json.JSONObject(err).optString("error", err)
                    } catch (e: Exception) { err }
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(username: String, password: String, email: String?, onSuccess: () -> Unit) {
        scope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val body = mutableMapOf("username" to username, "password" to password)
                if (!email.isNullOrBlank()) body["email"] = email
                val response = api.register(body)
                if (response.isSuccessful) {
                    val res = response.body()!!
                    saveToken(res.token)
                    _currentUser.value = res.user
                    _isLoggedIn.value = true
                    onSuccess()
                } else {
                    val err = response.errorBody()?.string() ?: "Registration failed"
                    _error.value = try {
                        org.json.JSONObject(err).optString("error", err)
                    } catch (e: Exception) { err }
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadProfile() {
        scope.launch {
            try {
                val response = api.getMe()
                if (response.isSuccessful) {
                    _currentUser.value = response.body()
                } else if (response.code() == 401) {
                    logout()
                }
            } catch (e: Exception) {
                // Keep cached user
            }
        }
    }

    fun logout() {
        prefs.edit().remove(KEY_TOKEN).apply()
        _currentUser.value = null
        _isLoggedIn.value = false
    }

    fun clearError() {
        _error.value = null
    }

    private fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
    }
}
