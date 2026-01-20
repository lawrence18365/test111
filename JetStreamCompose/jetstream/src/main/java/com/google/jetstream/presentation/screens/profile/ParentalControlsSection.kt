package com.google.jetstream.presentation.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.TextField
import androidx.tv.material3.surfaceColorAtElevation
import com.google.jetstream.presentation.theme.JetStreamCardShape

@Composable
fun ParentalControlsSection(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val parentalEnabled by viewModel.parentalEnabled.collectAsState()
    val isPinSet by viewModel.isPinSet.collectAsState()

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(horizontal = 72.dp)) {
        Text(
            text = "Parental Controls",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        ListItem(
            selected = false,
            onClick = { viewModel.setParentalEnabled(!parentalEnabled) },
            trailingContent = {
                Switch(
                    checked = parentalEnabled,
                    onCheckedChange = { viewModel.setParentalEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            headlineContent = {
                Text(
                    text = "Enable parental controls",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
            ),
            shape = ListItemDefaults.shape(shape = JetStreamCardShape)
        )

        if (parentalEnabled) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isPinSet) "Change PIN" else "Set PIN",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(4) },
                label = { Text("PIN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = confirmPin,
                onValueChange = { confirmPin = it.filter(Char::isDigit).take(4) },
                label = { Text("Confirm PIN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (pin.length < 4 || pin != confirmPin) {
                            statusMessage = "PINs must match and be 4 digits"
                        } else {
                            viewModel.setParentalPin(pin)
                            pin = ""
                            confirmPin = ""
                            statusMessage = "PIN saved"
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (pin.length < 4 || pin != confirmPin) {
                            statusMessage = "PINs must match and be 4 digits"
                        } else {
                            viewModel.setParentalPin(pin)
                            pin = ""
                            confirmPin = ""
                            statusMessage = "PIN saved"
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("Save PIN")
                }

                if (isPinSet) {
                    OutlinedButton(
                        onClick = {
                            viewModel.clearParentalPin()
                            pin = ""
                            confirmPin = ""
                            statusMessage = "PIN cleared"
                        },
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text("Clear PIN")
                    }
                }
            }

            statusMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
