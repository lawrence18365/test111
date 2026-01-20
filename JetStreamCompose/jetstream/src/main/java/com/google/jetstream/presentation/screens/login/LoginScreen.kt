/*
 * Login Screen for Xtream Codes Authentication
 */
package com.google.jetstream.presentation.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val serverFocusRequester = remember { FocusRequester() }

    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Reset state when screen is first displayed (e.g., after logout)
    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    // Navigate on successful login
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    // Request focus on server field when screen loads
    LaunchedEffect(Unit) {
        serverFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f3460)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val textFieldColors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1b263b),
                unfocusedContainerColor = Color(0xFF1b263b),
                disabledContainerColor = Color(0xFF1b263b),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF00b4d8),
                focusedIndicatorColor = Color(0xFF00b4d8),
                unfocusedIndicatorColor = Color.White.copy(alpha = 0.3f),
                placeholderColor = Color.White.copy(alpha = 0.6f)
            )

            // App Title
            Text(
                text = "IPTV Player",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Connect to your Xtream Codes server",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Server URL Field
            Text(
                text = "Server URL",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            TextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                placeholder = { Text("http://example.com:8080") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(serverFocusRequester),
                colors = textFieldColors,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Username Field
            Text(
                text = "Username",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            TextField(
                value = username,
                onValueChange = { username = it },
                placeholder = { Text("Your username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            Text(
                text = "Password",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Your password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                visualTransformation = if (showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                            viewModel.login(serverUrl, username, password)
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error message
            if (uiState is LoginUiState.Error) {
                Text(
                    text = (uiState as LoginUiState.Error).message,
                    color = Color(0xFFef5350),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Login Button
            Button(
                onClick = {
                    viewModel.login(serverUrl, username, password)
                },
                enabled = serverUrl.isNotBlank() &&
                        username.isNotBlank() &&
                        password.isNotBlank() &&
                        uiState !is LoginUiState.Loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.colors(
                    containerColor = Color(0xFF00b4d8),
                    contentColor = Color.White
                )
            ) {
                if (uiState is LoginUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp).width(24.dp),
                        color = Color.White
                    )
                } else {
                    Text("Connect")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show/Hide Password Toggle
            OutlinedButton(
                onClick = { showPassword = !showPassword },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showPassword) "Hide Password" else "Show Password")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Help text
            Text(
                text = "Enter your IPTV provider credentials.\nContact your provider if you don't have login details.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}
