package dev.aauth.app

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.aauth.app.core.BackupCrypto
import dev.aauth.app.core.InvalidBackupException
import dev.aauth.app.core.MigrationParser
import dev.aauth.app.core.OtpAccount
import dev.aauth.app.core.OtpAuthUri
import dev.aauth.app.core.WrongPasswordException
import dev.aauth.app.data.Account
import dev.aauth.app.data.BackupFormat
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

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application as AAuthApplication
    private val repository = container.repository
    private val settings = container.settings

    /** `null` while the first database read is in flight. */
    val accounts: StateFlow<List<Account>?> =
        repository.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val appLockEnabled: StateFlow<Boolean> = settings.appLock
    val secureScreen: StateFlow<Boolean> = settings.secureScreen

    private val messageChannel = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = messageChannel.receiveAsFlow()

    private fun post(message: UiMessage) {
        messageChannel.trySend(message)
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
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    // ---- Settings --------------------------------------------------------------------------

    fun setAppLock(enabled: Boolean) = settings.setAppLock(enabled)

    fun setSecureScreen(enabled: Boolean) = settings.setSecureScreen(enabled)

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
