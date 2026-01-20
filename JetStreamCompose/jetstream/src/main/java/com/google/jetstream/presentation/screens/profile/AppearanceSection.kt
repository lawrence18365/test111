package com.google.jetstream.presentation.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.FilterChip
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.google.jetstream.presentation.theme.ThemeOption

@Composable
fun AppearanceSection(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeOption by viewModel.themeOption.collectAsState()

    Column(modifier = Modifier.padding(horizontal = 72.dp)) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(ThemeOption.values().toList()) { option ->
                FilterChip(
                    selected = themeOption == option,
                    onClick = { viewModel.setTheme(option) }
                ) {
                    Text(option.label)
                }
            }
        }
    }
}
