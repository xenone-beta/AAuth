@file:OptIn(ExperimentalMaterial3Api::class)

package dev.aauth.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aauth.app.R
import dev.aauth.app.copySensitive
import dev.aauth.app.core.OtpType
import dev.aauth.app.core.Totp
import dev.aauth.app.data.Account
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    accounts: List<Account>?,
    snackbarHostState: SnackbarHostState,
    onScan: () -> Unit,
    onManual: () -> Unit,
    onSettings: () -> Unit,
    onDelete: (Long) -> Unit,
    onEdit: (Long, String, String) -> Unit,
    onNext: (Long, Long) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val copiedText = stringResource(R.string.copied)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Account?>(null) }
    var editing by remember { mutableStateOf<Account?>(null) }

    // Ticks a few times per second so the countdown rings animate smoothly.
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(200)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (!accounts.isNullOrEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_account)) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            accounts == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            accounts.isEmpty() -> EmptyState(
                modifier = Modifier.padding(padding),
                onScan = onScan,
                onManual = onManual,
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(accounts, key = { it.id }) { account ->
                    AccountCard(
                        account = account,
                        nowMillis = now,
                        modifier = Modifier.animateItem(),
                        onNext = { onNext(account.id, account.counter) },
                        onCopy = { code ->
                            copySensitive(context, code)
                            scope.launch { snackbarHostState.showSnackbar(copiedText) }
                        },
                        onEdit = { editing = account },
                        onDelete = { pendingDelete = account },
                    )
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.scan_qr)) },
                supportingContent = { Text(stringResource(R.string.scan_qr_desc)) },
                leadingContent = { SheetIcon(Icons.Outlined.QrCodeScanner) },
                modifier = Modifier.clickable {
                    showSheet = false
                    onScan()
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.enter_key)) },
                supportingContent = { Text(stringResource(R.string.enter_key_desc)) },
                leadingContent = { SheetIcon(Icons.Outlined.Keyboard) },
                modifier = Modifier.clickable {
                    showSheet = false
                    onManual()
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    editing?.let { account ->
        EditDialog(
            account = account,
            onDismiss = { editing = null },
            onSave = { issuer, label ->
                onEdit(account.id, issuer, label)
                editing = null
            },
        )
    }

    pendingDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_body, account.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(account.id)
                        pendingDelete = null
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun AccountCard(
    account: Account,
    nowMillis: Long,
    onNext: () -> Unit,
    onCopy: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val secret = account.secret
    val window = account.windowSeconds
    val step = nowMillis / 1000L / window
    // Recomputed only when the window (or HOTP counter) changes, not on every animation tick.
    val code = remember(account.id, step, secret, account.counter, account.type) {
        account.codeAt(nowMillis)
    }
    val remaining = Totp.secondsRemaining(nowMillis, window)
    val fraction = Totp.remainingFraction(nowMillis, window)
    val expiring = account.isTimeBased && remaining <= 5
    val accent by animateColorAsState(
        targetValue = if (expiring) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        animationSpec = tween(400),
        label = "accent",
    )

    var menuOpen by remember { mutableStateOf(false) }

    Card(
        onClick = { code?.let { onCopy(it) } },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ServiceAvatar(title = account.title)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = account.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                if (account.subtitle.isNotEmpty()) {
                    Text(
                        text = account.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(4.dp))
                // The code slides up and fades when it changes.
                AnimatedContent(
                    targetState = code,
                    transitionSpec = {
                        (slideInVertically(tween(300)) { it / 2 } + fadeIn(tween(300))) togetherWith
                            (slideOutVertically(tween(300)) { -it / 2 } + fadeOut(tween(150)))
                    },
                    label = "code",
                ) { shown ->
                    Text(
                        text = shown?.let { formatCode(it, account.type) } ?: stringResource(R.string.decrypt_error),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp,
                        ),
                        color = if (shown == null) MaterialTheme.colorScheme.error else accent,
                    )
                }
            }

            if (account.isTimeBased) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.size(40.dp),
                        color = accent,
                        strokeWidth = 4.dp,
                        trackColor = if (expiring) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                    )
                    Text(
                        text = remaining.toString(),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            } else {
                // HOTP codes do not expire: the button moves the counter and shows the next one.
                IconButton(onClick = onNext) {
                    Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.next_code))
                }
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.copy_code)) },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                        enabled = code != null,
                        onClick = {
                            menuOpen = false
                            code?.let { onCopy(it) }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    onScan: () -> Unit,
    onManual: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_shield_keyhole),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onScan) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.scan_qr))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onManual) {
            Text(stringResource(R.string.enter_key))
        }
    }
}

/** Leading icon for the add-account sheet: a tonal circle like in the design mockup. */
@Composable
private fun SheetIcon(icon: ImageVector) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun EditDialog(
    account: Account,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var issuer by remember(account.id) { mutableStateOf(account.issuer) }
    var label by remember(account.id) { mutableStateOf(account.label) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = issuer,
                    onValueChange = { issuer = it },
                    label = { Text(stringResource(R.string.field_issuer)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.field_account)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(issuer, label) },
                enabled = issuer.isNotBlank() || label.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** "123456" -> "123 456", "12345678" -> "1234 5678"; Steam codes stay in one piece. */
private fun formatCode(code: String, type: OtpType): String {
    if (type == OtpType.STEAM) return code
    val mid = (code.length + 1) / 2
    return code.substring(0, mid) + " " + code.substring(mid)
}
