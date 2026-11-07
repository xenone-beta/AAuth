@file:OptIn(ExperimentalMaterial3Api::class)

package dev.aauth.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import dev.aauth.app.R
import dev.aauth.app.core.PinHasher
import java.time.LocalDate

private const val AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

@Composable
fun SettingsScreen(
    appLock: Boolean,
    secureScreen: Boolean,
    pinLength: Int,
    snackbarHostState: SnackbarHostState,
    onAppLockChange: (Boolean) -> Unit,
    onSecureScreenChange: (Boolean) -> Unit,
    onSavePin: (String) -> Unit,
    onRemovePin: () -> Unit,
    onExport: (Uri, CharArray) -> Unit,
    onImport: (Uri, CharArray) -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val systemLockAvailable = remember {
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }
    val hasPin = pinLength > 0
    // The lock can be switched on if the device has a screen lock or the user set an app PIN.
    val lockAvailable = systemLockAvailable || hasPin

    var showPinDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    // "" = follow the system, otherwise a language tag such as "en" or "ru".
    val currentLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
    var showExportDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf<CharArray?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val password = exportPassword
        exportPassword = null
        if (uri != null && password != null) onExport(uri, password)
    }
    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) importUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.settings_general))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_language)) },
                supportingContent = { Text(stringResource(languageLabel(currentLanguage))) },
                leadingContent = { Icon(Icons.Outlined.Language, contentDescription = null) },
                trailingContent = { ChevronIcon() },
                modifier = Modifier.clickable { showLanguageDialog = true },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(stringResource(R.string.settings_security))
            SwitchRow(
                title = stringResource(R.string.settings_app_lock),
                description = stringResource(
                    if (lockAvailable) R.string.settings_app_lock_desc else R.string.settings_app_lock_unavailable,
                ),
                checked = appLock && lockAvailable,
                enabled = lockAvailable,
                onCheckedChange = onAppLockChange,
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_pin)) },
                supportingContent = {
                    Text(stringResource(if (hasPin) R.string.settings_pin_set else R.string.settings_pin_desc))
                },
                trailingContent = { ChevronIcon() },
                modifier = Modifier.clickable { showPinDialog = true },
            )
            SwitchRow(
                title = stringResource(R.string.settings_secure_screen),
                description = stringResource(R.string.settings_secure_screen_desc),
                checked = secureScreen,
                enabled = true,
                onCheckedChange = onSecureScreenChange,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader(stringResource(R.string.settings_backup))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_export)) },
                supportingContent = { Text(stringResource(R.string.settings_export_desc)) },
                trailingContent = { ChevronIcon() },
                modifier = Modifier.clickable { showExportDialog = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_import)) },
                supportingContent = { Text(stringResource(R.string.settings_import_desc)) },
                trailingContent = { ChevronIcon() },
                modifier = Modifier.clickable { openLauncher.launch(arrayOf("*/*")) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about_row)) },
                supportingContent = { Text(stringResource(R.string.settings_about_row_desc)) },
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                trailingContent = { ChevronIcon() },
                modifier = Modifier.clickable(onClick = onAbout),
            )
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(
            current = currentLanguage,
            onDismiss = { showLanguageDialog = false },
            onSelect = { tag ->
                showLanguageDialog = false
                AppCompatDelegate.setApplicationLocales(
                    if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
                )
            },
        )
    }

    if (showPinDialog) {
        PinDialog(
            hasPin = hasPin,
            onDismiss = { showPinDialog = false },
            onSave = { pin ->
                showPinDialog = false
                onSavePin(pin)
            },
            onRemove = {
                showPinDialog = false
                onRemovePin()
            },
        )
    }

    if (showExportDialog) {
        PasswordDialog(
            title = stringResource(R.string.password_title_export),
            message = stringResource(R.string.export_warning),
            requireConfirmation = true,
            onDismiss = { showExportDialog = false },
            onConfirm = { password ->
                showExportDialog = false
                exportPassword = password.toCharArray()
                createLauncher.launch("aauth-backup-${LocalDate.now()}.aauth")
            },
        )
    }

    importUri?.let { uri ->
        PasswordDialog(
            title = stringResource(R.string.password_title_import),
            message = null,
            requireConfirmation = false,
            onDismiss = { importUri = null },
            onConfirm = { password ->
                importUri = null
                onImport(uri, password.toCharArray())
            },
        )
    }
}

private val LANGUAGES = listOf("", "en", "ru")

private fun languageLabel(tag: String): Int = when (tag) {
    "en" -> R.string.language_en
    "ru" -> R.string.language_ru
    else -> R.string.language_system
}

@Composable
private fun LanguageDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                for (tag in LANGUAGES) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = current == tag,
                                role = Role.RadioButton,
                                onClick = { onSelect(tag) },
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = current == tag, onClick = null)
                        Text(
                            text = stringResource(languageLabel(tag)),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun ChevronIcon() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    )
}

@Composable
private fun PinDialog(
    hasPin: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }

    val mismatch = repeat.isNotEmpty() && repeat != pin
    val valid = PinHasher.isValidPin(pin) && pin == repeat

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (hasPin) R.string.pin_change_title else R.string.pin_set_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.pin_hint, PinHasher.MIN_LENGTH, PinHasher.MAX_LENGTH),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { v -> if (v.length <= PinHasher.MAX_LENGTH && v.all { it in '0'..'9' }) pin = v },
                    label = { Text(stringResource(R.string.pin_new)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                OutlinedTextField(
                    value = repeat,
                    onValueChange = { v -> if (v.length <= PinHasher.MAX_LENGTH && v.all { it in '0'..'9' }) repeat = v },
                    label = { Text(stringResource(R.string.pin_repeat)) },
                    singleLine = true,
                    isError = mismatch,
                    supportingText = if (mismatch) {
                        { Text(stringResource(R.string.pin_mismatch)) }
                    } else {
                        null
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                if (hasPin) {
                    TextButton(onClick = onRemove) {
                        Text(
                            text = stringResource(R.string.pin_remove),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(pin) }, enabled = valid) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun PasswordDialog(
    title: String,
    message: String?,
    requireConfirmation: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }

    val tooShort = requireConfirmation && password.isNotEmpty() && password.length < 8
    val mismatch = requireConfirmation && repeat.isNotEmpty() && repeat != password
    val valid = if (requireConfirmation) password.length >= 8 && password == repeat else password.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (message != null) Text(message, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    singleLine = true,
                    isError = tooShort,
                    supportingText = if (tooShort) {
                        { Text(stringResource(R.string.password_short)) }
                    } else {
                        null
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                if (requireConfirmation) {
                    OutlinedTextField(
                        value = repeat,
                        onValueChange = { repeat = it },
                        label = { Text(stringResource(R.string.password_confirm)) },
                        singleLine = true,
                        isError = mismatch,
                        supportingText = if (mismatch) {
                            { Text(stringResource(R.string.password_mismatch)) }
                        } else {
                            null
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }, enabled = valid) {
                Text(stringResource(R.string.continue_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
