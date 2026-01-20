/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.jetstream.presentation.screens.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.jetstream.data.util.StringConstants
import com.google.jetstream.presentation.screens.dashboard.rememberChildPadding
import kotlinx.coroutines.launch

@Immutable
data class AccountsSectionData(
    val title: String,
    val value: String? = null,
    val onClick: () -> Unit = {}
)

@Composable
fun AccountsSection(
    viewModel: AccountsSectionViewModel = hiltViewModel()
) {
    val childPadding = rememberChildPadding()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Get current username from credentials
    val credentials by viewModel.credentials.collectAsState(initial = null)
    val userDisplayName = credentials?.username ?: "Not logged in"

    val accountsSectionListItems = remember(userDisplayName) {
        listOf(
            AccountsSectionData(
                title = "Current Account",
                value = userDisplayName
            ),
            AccountsSectionData(
                title = StringConstants.Composable.Placeholders.AccountsSelectionLogOut,
                value = userDisplayName,
                onClick = { showLogoutDialog = true }
            ),
            AccountsSectionData(
                title = StringConstants.Composable.Placeholders
                    .AccountsSelectionViewSubscriptionsTitle
            ),
            AccountsSectionData(
                title = StringConstants.Composable.Placeholders.AccountsSelectionDeleteAccountTitle,
                onClick = { showDeleteDialog = true }
            )
        )
    }

    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = childPadding.start),
        columns = GridCells.Fixed(2),
        content = {
            items(accountsSectionListItems.size) { index ->
                AccountsSelectionItem(
                    modifier = Modifier.focusRequester(focusRequester),
                    key = index,
                    accountsSectionData = accountsSectionListItems[index]
                )
            }
        }
    )

    // Logout confirmation dialog
    AccountsSectionDeleteDialog(
        showDialog = showLogoutDialog,
        onDismissRequest = { showLogoutDialog = false },
        title = "Log Out",
        text = "Are you sure you want to log out?",
        confirmText = "Log Out",
        onConfirm = {
            coroutineScope.launch {
                viewModel.logout()
            }
            showLogoutDialog = false
        },
        modifier = Modifier.width(428.dp)
    )

    // Delete account dialog
    AccountsSectionDeleteDialog(
        showDialog = showDeleteDialog,
        onDismissRequest = { showDeleteDialog = false },
        modifier = Modifier.width(428.dp)
    )
}
