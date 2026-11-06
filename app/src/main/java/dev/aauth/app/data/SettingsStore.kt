package dev.aauth.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tiny SharedPreferences-backed settings, exposed as StateFlows for Compose. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("aauth_settings", Context.MODE_PRIVATE)

    private val _appLock = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK, false))
    val appLock: StateFlow<Boolean> = _appLock.asStateFlow()

    private val _secureScreen = MutableStateFlow(prefs.getBoolean(KEY_SECURE_SCREEN, true))
    val secureScreen: StateFlow<Boolean> = _secureScreen.asStateFlow()

    fun setAppLock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
        _appLock.value = enabled
    }

    fun setSecureScreen(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECURE_SCREEN, enabled).apply()
        _secureScreen.value = enabled
    }

    private companion object {
        const val KEY_APP_LOCK = "app_lock"
        const val KEY_SECURE_SCREEN = "secure_screen"
    }
}
