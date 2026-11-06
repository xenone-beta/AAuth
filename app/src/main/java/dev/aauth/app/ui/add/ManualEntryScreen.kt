@file:OptIn(ExperimentalMaterial3Api::class)

package dev.aauth.app.ui.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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

            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(stringResource(R.string.advanced))
            }

            if (showAdvanced) {
                Text(stringResource(R.string.algorithm), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    algorithms.forEachIndexed { index, algorithm ->
                        SegmentedButton(
                            selected = algorithmIndex == index,
                            onClick = { algorithmIndex = index },
                            shape = SegmentedButtonDefaults.itemShape(index, algorithms.size),
                        ) { Text(algorithm.uriName) }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.digits), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DIGIT_OPTIONS.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = digits == option,
                            onClick = { digits = option },
                            shape = SegmentedButtonDefaults.itemShape(index, DIGIT_OPTIONS.size),
                        ) { Text(option.toString()) }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.period), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PERIOD_OPTIONS.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = period == option,
                            onClick = { period = option },
                            shape = SegmentedButtonDefaults.itemShape(index, PERIOD_OPTIONS.size),
                        ) { Text(option.toString()) }
                    }
                }
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
