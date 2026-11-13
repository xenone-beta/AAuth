package dev.aauth.app

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.aauth.app.core.BackupCrypto
import dev.aauth.app.core.InvalidBackupException
import dev.aauth.app.core.MigrationParser
import dev.aauth.app.core.OtpAccount
import dev.aauth.app.core.OtpAuthUri
import dev.aauth.app.core.PinHasher
import dev.aauth.app.core.PinLockout
import dev.aauth.app.core.WrongPasswordException
import dev.aauth.app.data.Account
import dev.aauth.app.data.BackupFormat
import dev.aauth.app.ui.widget.CodesWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A one-shot message for the snackbar: a string resource plus optional format arguments. */
class UiMessage(@StringRes val resId: Int, val args: List<Any> = emptyList())

/** Result of checking a PIN typed on the lock screen. */
sealed interface PinCheck {
    data object Ok : PinCheck

    /** Wrong PIN. [attemptsLeft] is how many tries remain before the next forced wait (0 = now waiting). */
    data class Wrong(val attemptsLeft: Int, val lockedUntil: Long, val wipeLeft: Int = -1) : PinCheck

    /** Too many wrong PINs: every account was erased and the PIN removed. */
    data object Wiped : PinCheck

    /** Entry is refused until [until] (wall-clock millis). */
    data class Locked(val until: Long) : PinCheck
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application as AAuthApplication
    private val repository = container.repository
    private val settings = container.settings

    /** `null` while the first database read is in flight. */
    val accounts: StateFlow<List<Account>?> =
        repository.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val appLockEnabled: StateFlow<Boolean> = settings.appLock
    val secureScreen: StateFlow<Boolean> = settings.secureScreen

    /** 0 when no PIN is set. */
    val pinLength: StateFlow<Int> = settings.pinLength

    /** Wrong PINs in a row after which the accounts are erased (0 = off). */
    val wipeAfter: StateFlow<Int> = settings.wipeAfter

    private val messageChannel = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = messageChannel.receiveAsFlow()

    private fun post(message: UiMessage) {
        messageChannel.trySend(message)
    }

    /** Re-renders home screen widgets after accounts or lock settings change. */
    private fun refreshWidget() {
        viewModelScope.launch {
            runCatching { CodesWidget().updateAll(getApplication<Application>()) }
        }
    }

    // ---- Adding accounts -------------------------------------------------------------------

    /** Called with the raw text of a scanned QR code (otpauth:// or Google Authenticator export). */
    fun onQrScanned(raw: String) {
        viewModelScope.launch {
            val parsed = runCatching {
                if (MigrationParser.isMigrationUri(raw)) MigrationParser.parse(raw) else listOf(OtpAuthUri.parse(raw))
            }.getOrNull()

            if (parsed.isNullOrEmpty()) {
                post(UiMessage(R.string.scan_invalid))
                return@launch
            }
            reportAdded(repository.addAll(parsed), parsed.size)
        }
    }

    fun addManual(account: OtpAccount) {
        viewModelScope.launch { reportAdded(repository.addAll(listOf(account)), 1) }
    }

    private fun reportAdded(added: Int, total: Int) {
        post(
            when {
                added == 0 -> UiMessage(R.string.already_added)
                total == 1 -> UiMessage(R.string.account_added)
                else -> UiMessage(R.string.accounts_added, listOf(added))
            },
        )
        if (added > 0) refreshWidget()
    }

    /** HOTP: moves to the next counter value, which shows a new code. */
    fun nextCounter(id: Long, current: Long) {
        viewModelScope.launch {
            repository.setCounter(id, current + 1)
            refreshWidget()
        }
    }

    fun rename(id: Long, issuer: String, label: String) {
        viewModelScope.launch {
            repository.rename(id, issuer, label)
            post(UiMessage(R.string.account_updated))
            refreshWidget()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            refreshWidget()
        }
    }

    // ---- Settings --------------------------------------------------------------------------

    fun setAppLock(enabled: Boolean) {
        settings.setAppLock(enabled)
        refreshWidget()
    }

    fun setSecureScreen(enabled: Boolean) = settings.setSecureScreen(enabled)

    // ---- PIN -------------------------------------------------------------------------------

    /** Stores a new PIN and turns the app lock on. */
    fun savePin(pin: String) {
        if (!PinHasher.isValidPin(pin)) return
        viewModelScope.launch {
            withContext(Dispatchers.Default) { settings.savePin(pin) }
            settings.setAppLock(true)
            post(UiMessage(R.string.pin_saved))
            refreshWidget()
        }
    }

    fun removePin() {
        settings.clearPin()
        post(UiMessage(R.string.pin_removed))
    }

    /** Verifies a PIN from the lock screen, applying escalating delays after repeated mistakes. */
    suspend fun checkPin(pin: String): PinCheck {
        val now = System.currentTimeMillis()
        val lockedUntil = settings.lockedUntil
        if (lockedUntil > now) return PinCheck.Locked(lockedUntil)

        val ok = withContext(Dispatchers.Default) { settings.verifyPin(pin) }
        if (ok) {
            settings.failedAttempts = 0
            settings.lockedUntil = 0L
            return PinCheck.Ok
        }

        val failed = settings.failedAttempts + 1
        settings.failedAttempts = failed

        val wipeAfter = settings.wipeAfter.value
        if (wipeAfter > 0 && failed >= wipeAfter) {
            wipeEverything()
            return PinCheck.Wiped
        }

        val delay = PinLockout.delayMillis(failed)
        val until = if (delay > 0) System.currentTimeMillis() + delay else 0L
        settings.lockedUntil = until
        val wipeLeft = if (wipeAfter > 0) wipeAfter - failed else -1
        return PinCheck.Wrong(PinLockout.attemptsLeftBeforeDelay(failed), until, wipeLeft)
    }

    fun setWipeAfter(attempts: Int) = settings.setWipeAfter(attempts)

    /** Erases all accounts and removes the PIN and the app lock, as promised by the "erase after N attempts" option. */
    private suspend fun wipeEverything() {
        repository.deleteAll()
        settings.clearPin()
        settings.setAppLock(false)
        refreshWidget()
        post(UiMessage(R.string.wiped_message))
    }

    /** Wall-clock time until which PIN entry is refused, or 0 when entry is allowed. */
    fun pinLockedUntil(): Long = settings.lockedUntil.takeIf { it > System.currentTimeMillis() } ?: 0L

    // ---- Backup ----------------------------------------------------------------------------

    fun exportBackup(uri: Uri, password: CharArray) {
        viewModelScope.launch {
            try {
                val accounts = repository.exportAll()
                if (accounts.isEmpty()) {
                    post(UiMessage(R.string.export_empty))
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    val encrypted = BackupCrypto.encrypt(BackupFormat.toBytes(accounts), password)
                    val stream = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                        ?: error("Cannot open output stream")
                    stream.use { it.write(encrypted) }
                }
                post(UiMessage(R.string.export_done))
            } catch (e: Exception) {
                post(UiMessage(R.string.export_failed))
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun importBackup(uri: Uri, password: CharArray) {
        viewModelScope.launch {
            try {
                val plain = withContext(Dispatchers.IO) {
                    val stream = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?: error("Cannot open input stream")
                    val bytes = stream.use { it.readBytes() }
                    BackupCrypto.decrypt(bytes, password)
                }
                val accounts = BackupFormat.fromBytes(plain)
                val added = repository.addAll(accounts)
                post(UiMessage(R.string.import_done, listOf(added, accounts.size - added)))
                if (added > 0) refreshWidget()
            } catch (e: WrongPasswordException) {
                post(UiMessage(R.string.import_wrong_password))
            } catch (e: InvalidBackupException) {
                post(UiMessage(R.string.import_invalid_file))
            } catch (e: Exception) {
                post(UiMessage(R.string.import_invalid_file))
            } finally {
                password.fill('\u0000')
            }
        }
    }
}
