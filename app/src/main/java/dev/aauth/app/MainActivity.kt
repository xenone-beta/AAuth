package dev.aauth.app

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.aauth.app.ui.lock.LockScreen
import dev.aauth.app.ui.theme.AAuthTheme

// FragmentActivity (not just ComponentActivity) because BiometricPrompt needs it.
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var unlocked by mutableStateOf(false)
    private var promptShowing = false
    private var lastStopTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AAuthTheme {
                val lockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
                val secureScreen by viewModel.secureScreen.collectAsStateWithLifecycle()
                val pinLength by viewModel.pinLength.collectAsStateWithLifecycle()

                // Blocks screenshots and hides the content in the recent-apps switcher.
                LaunchedEffect(secureScreen) {
                    if (secureScreen) {
                        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                if (!lockEnabled || unlocked) {
                    AAuthApp(viewModel)
                } else {
                    LockScreen(
                        pinLength = pinLength,
                        biometricAvailable = canUseBiometrics(hasPin = pinLength > 0),
                        initialLockedUntil = viewModel.pinLockedUntil(),
                        onCheckPin = { pin -> viewModel.checkPin(pin) },
                        onBiometric = ::promptUnlock,
                        onUnlocked = { unlocked = true },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!viewModel.appLockEnabled.value) {
            // Lock is off: treat the session as unlocked, so switching it on in Settings
            // does not immediately lock the user out of the screen they are on.
            unlocked = true
            return
        }
        val awayFor = SystemClock.elapsedRealtime() - lastStopTime
        if (lastStopTime != 0L && awayFor > LOCK_GRACE_MS) unlocked = false
        if (!unlocked) promptUnlock()
    }

    override fun onStop() {
        super.onStop()
        lastStopTime = SystemClock.elapsedRealtime()
    }

    /**
     * With an app PIN, only biometrics are offered next to it: allowing the device screen lock
     * as well would let anyone who knows the phone's PIN skip the app PIN.
     */
    private fun authenticators(hasPin: Boolean): Int =
        if (hasPin) {
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }

    private fun canUseBiometrics(hasPin: Boolean): Boolean =
        BiometricManager.from(this).canAuthenticate(authenticators(hasPin)) == BiometricManager.BIOMETRIC_SUCCESS

    private fun promptUnlock() {
        if (promptShowing) return
        val hasPin = viewModel.pinLength.value > 0
        if (!canUseBiometrics(hasPin)) {
            // No PIN and no screen lock on the device: there is nothing to authenticate with.
            if (!hasPin) unlocked = true
            return
        }

        promptShowing = true
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    promptShowing = false
                    unlocked = true
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    promptShowing = false
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.unlock_title))
            .setAllowedAuthenticators(authenticators(hasPin))
            .apply { if (hasPin) setNegativeButtonText(getString(R.string.use_pin)) }
            .build()
        prompt.authenticate(info)
    }

    private companion object {
        /** Re-lock only if the app was in the background longer than this. */
        const val LOCK_GRACE_MS = 30_000L
    }
}
