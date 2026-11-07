@file:OptIn(ExperimentalMaterial3Api::class)

package dev.aauth.app.ui.add

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.aauth.app.R
import dev.aauth.app.core.Base32
import dev.aauth.app.core.OtpAccount
import dev.aauth.app.core.OtpAlgorithm

private val DIGIT_OPTIONS = listOf(6, 7, 8)
private val PERIOD_OPTIONS = listOf(15, 30, 60)

/** Label on the left, segmented buttons on the right, as in the design mockup. */
@Composable
private fun SegmentRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(96.dp),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(option, maxLines = 1) }
            }
        }
    }
}

@Composable
fun ManualEntryScreen(
    onSave: (OtpAccount) -> Unit,
    onBack: () -> Unit,
) {
    var issuer by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var algorithmIndex by rememberSaveable { mutableIntStateOf(0) }
    var digits by rememberSaveable { mutableIntStateOf(6) }
    var period by rememberSaveable { mutableIntStateOf(30) }

    val algorithms = OtpAlgorithm.entries
    val secretValid = secret.isNotBlank() &&
        runCatching { Base32.decode(secret).isNotEmpty() }.getOrDefault(false)
    val secretError = secret.isNotBlank() && !secretValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manual_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = issuer,
                onValueChange = { issuer = it },
                label = { Text(stringResource(R.string.field_issuer)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.field_account)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it },
                label = { Text(stringResource(R.string.field_secret)) },
                singleLine = true,
                isError = secretError,
                supportingText = if (secretError) {
                    { Text(stringResource(R.string.error_secret)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(stringResource(R.string.advanced))
                }
            }

            if (showAdvanced) {
                SegmentRow(
                    label = stringResource(R.string.algorithm),
                    options = algorithms.map { it.uriName },
                    selectedIndex = algorithmIndex,
                    onSelect = { algorithmIndex = it },
                )
                Spacer(Modifier.height(12.dp))
                SegmentRow(
                    label = stringResource(R.string.digits),
                    options = DIGIT_OPTIONS.map { it.toString() },
                    selectedIndex = DIGIT_OPTIONS.indexOf(digits),
                    onSelect = { digits = DIGIT_OPTIONS[it] },
                )
                Spacer(Modifier.height(12.dp))
                SegmentRow(
                    label = stringResource(R.string.period),
                    options = PERIOD_OPTIONS.map { it.toString() },
                    selectedIndex = PERIOD_OPTIONS.indexOf(period),
                    onSelect = { period = PERIOD_OPTIONS[it] },
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    onSave(
                        OtpAccount(
                            issuer = issuer.trim(),
                            label = label.trim(),
                            secret = secret.filter { it.isLetterOrDigit() }.uppercase(),
                            algorithm = algorithms[algorithmIndex],
                            digits = digits,
                            period = period,
                        ),
                    )
                },
                enabled = secretValid && (issuer.isNotBlank() || label.isNotBlank()),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
