package dev.aauth.app.data

import android.content.Context
import android.util.Base64
import dev.aauth.app.core.PinHasher
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

    /** Length of the stored PIN, or 0 when no PIN is set. Used for auto-submit on the lock screen. */
    private val _pinLength = MutableStateFlow(if (prefs.contains(KEY_PIN_HASH)) prefs.getInt(KEY_PIN_LENGTH, 0) else 0)
    val pinLength: StateFlow<Int> = _pinLength.asStateFlow()

    val hasPin: Boolean get() = _pinLength.value > 0

    fun setAppLock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
        _appLock.value = enabled
    }

    fun setSecureScreen(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECURE_SCREEN, enabled).apply()
        _secureScreen.value = enabled
    }

    // ---- PIN -------------------------------------------------------------------------------

    /** Hashes and stores [pin]. Slow (PBKDF2): call off the main thread. */
    fun savePin(pin: String) {
        require(PinHasher.isValidPin(pin)) { "Invalid PIN" }
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt(KEY_PIN_LENGTH, pin.length)
            .putInt(KEY_FAILED, 0)
            .putLong(KEY_LOCKED_UNTIL, 0L)
            .apply()
        _pinLength.value = pin.length
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_LENGTH)
            .putInt(KEY_FAILED, 0)
            .putLong(KEY_LOCKED_UNTIL, 0L)
            .apply()
        _pinLength.value = 0
    }

    /** Checks [pin] against the stored hash. Slow (PBKDF2): call off the main thread. */
    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val hash = prefs.getString(KEY_PIN_HASH, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        return PinHasher.verify(pin, salt, hash)
    }

    var failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED, value).apply()

    /** Wall-clock time (ms) until which PIN entry is refused. */
    var lockedUntil: Long
        get() = prefs.getLong(KEY_LOCKED_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_LOCKED_UNTIL, value).apply()

    private companion object {
        const val KEY_APP_LOCK = "app_lock"
        const val KEY_SECURE_SCREEN = "secure_screen"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_LENGTH = "pin_length"
        const val KEY_FAILED = "pin_failed"
        const val KEY_LOCKED_UNTIL = "pin_locked_until"
    }
}
