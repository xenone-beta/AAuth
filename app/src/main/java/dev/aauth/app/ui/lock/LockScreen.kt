package dev.aauth.app.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.aauth.app.PinCheck
import dev.aauth.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shown while the app is locked.
 *
 * With [pinLength] == 0 there is no app PIN, so the screen only offers the system prompt
 * (biometrics or device screen lock). With a PIN, a numeric keypad is shown and the
 * entry is submitted automatically once [pinLength] digits are typed.
 */
@Composable
fun LockScreen(
    pinLength: Int,
    biometricAvailable: Boolean,
    initialLockedUntil: Long,
    onCheckPin: suspend (String) -> PinCheck,
    onBiometric: () -> Unit,
    onUnlocked: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        if (pinLength <= 0) {
            SystemLock(onBiometric)
        } else {
            PinPad(pinLength, biometricAvailable, initialLockedUntil, onCheckPin, onBiometric, onUnlocked)
        }
    }
}

@Composable
private fun SystemLock(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LockBadge()
        Spacer(Modifier.height(24.dp))
        Text(text = stringResource(R.string.lock_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onUnlock) { Text(stringResource(R.string.lock_unlock)) }
    }
}

@Composable
private fun LockBadge() {
    Surface(
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun PinPad(
    pinLength: Int,
    biometricAvailable: Boolean,
    initialLockedUntil: Long,
    onCheckPin: suspend (String) -> PinCheck,
    onBiometric: () -> Unit,
    onUnlocked: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var entered by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var attemptsLeft by remember { mutableIntStateOf(-1) } // -1 = no error to show
    var wipeLeft by remember { mutableIntStateOf(-1) } // -1 = the erase option is off
    var lockedUntil by remember { mutableLongStateOf(initialLockedUntil) }
    var secondsLeft by remember { mutableLongStateOf(0L) }

    // Counts down while PIN entry is refused.
    LaunchedEffect(lockedUntil) {
        while (true) {
            val left = ((lockedUntil - System.currentTimeMillis() + 999L) / 1000L).coerceAtLeast(0L)
            secondsLeft = left
            if (left == 0L) break
            delay(250)
        }
    }
    val waiting = secondsLeft > 0

    fun submit(pin: String) {
        checking = true
        scope.launch {
            when (val result = onCheckPin(pin)) {
                PinCheck.Ok -> onUnlocked()
                PinCheck.Wiped -> onUnlocked() // nothing is left to protect; the home screen explains what happened
                is PinCheck.Wrong -> {
                    attemptsLeft = result.attemptsLeft
                    lockedUntil = result.lockedUntil
                    wipeLeft = result.wipeLeft
                }
                is PinCheck.Locked -> lockedUntil = result.until
            }
            entered = ""
            checking = false
        }
    }

    fun press(digit: Char) {
        if (checking || waiting || entered.length >= pinLength) return
        attemptsLeft = -1
        entered += digit
        if (entered.length == pinLength) submit(entered)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LockBadge()
        Spacer(Modifier.height(16.dp))
        Text(text = stringResource(R.string.pin_enter), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(pinLength) { index ->
                val filled = index < entered.length
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .then(
                            if (filled) {
                                Modifier.background(MaterialTheme.colorScheme.primary)
                            } else {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            },
                        ),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        // Reserve the line so the keypad does not jump when a message appears.
        Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
            when {
                wipeLeft in 0..3 -> Text(
                    text = stringResource(R.string.pin_wipe_warning, wipeLeft),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                waiting -> Text(
                    text = stringResource(R.string.pin_wait, formatSeconds(secondsLeft)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                attemptsLeft >= 0 -> Text(
                    text = stringResource(R.string.pin_wrong, attemptsLeft),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        val rows = listOf("123", "456", "789")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    for (digit in row) {
                        DigitKey(digit, enabled = !waiting && !checking) { press(digit) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                KeySlot {
                    if (biometricAvailable) {
                        IconButton(onClick = onBiometric, modifier = Modifier.size(72.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Fingerprint,
                                contentDescription = stringResource(R.string.use_biometrics),
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                }
                DigitKey('0', enabled = !waiting && !checking) { press('0') }
                KeySlot {
                    IconButton(
                        onClick = {
                            if (!checking && entered.isNotEmpty()) entered = entered.dropLast(1)
                        },
                        modifier = Modifier.size(72.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = stringResource(R.string.pin_backspace),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeySlot(content: @Composable () -> Unit) {
    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun DigitKey(digit: Char, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = digit.toString(), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

private fun formatSeconds(total: Long): String {
    val minutes = total / 60
    val seconds = total % 60
    return "%d:%02d".format(minutes, seconds)
}
