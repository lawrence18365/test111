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

package com.google.jetstream.presentation.theme // ktlint-disable filename

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.google.jetstream.R

private val baseColorScheme @Composable get() = darkColorScheme(
    primary = colorResource(R.color.primary),
    onPrimary = colorResource(R.color.onPrimary),
    primaryContainer = colorResource(R.color.primaryContainer),
    onPrimaryContainer = colorResource(R.color.onPrimaryContainer),
    secondary = colorResource(R.color.secondary),
    onSecondary = colorResource(R.color.onSecondary),
    secondaryContainer = colorResource(R.color.secondaryContainer),
    onSecondaryContainer = colorResource(R.color.onSecondaryContainer),
    tertiary = colorResource(R.color.tertiary),
    onTertiary = colorResource(R.color.onTertiary),
    tertiaryContainer = colorResource(R.color.tertiaryContainer),
    onTertiaryContainer = colorResource(R.color.onTertiaryContainer),
    background = colorResource(R.color.background),
    onBackground = colorResource(R.color.onBackground),
    surface = colorResource(R.color.surface),
    onSurface = colorResource(R.color.onSurface),
    surfaceVariant = colorResource(R.color.surfaceVariant),
    onSurfaceVariant = colorResource(R.color.onSurfaceVariant),
    error = colorResource(R.color.error),
    onError = colorResource(R.color.onError),
    errorContainer = colorResource(R.color.errorContainer),
    onErrorContainer = colorResource(R.color.onErrorContainer),
    border = colorResource(R.color.border),
)

@Composable
fun JetStreamTheme(
    themeOption: ThemeOption = ThemeOption.Ocean,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeOption) {
        ThemeOption.Ocean -> baseColorScheme
        ThemeOption.Sunset -> baseColorScheme.copy(
            primary = Color(0xFFFF8A5B),
            onPrimary = Color(0xFF1B0D07),
            primaryContainer = Color(0xFF5A2B14),
            onPrimaryContainer = Color(0xFFFFDCCB),
            secondary = Color(0xFFFFC857),
            onSecondary = Color(0xFF2A1B00),
            secondaryContainer = Color(0xFF5C3A0A),
            onSecondaryContainer = Color(0xFFFFE5B3),
            tertiary = Color(0xFFFF6F61),
            onTertiary = Color(0xFF2A0A07),
            tertiaryContainer = Color(0xFF5A1E16),
            onTertiaryContainer = Color(0xFFFFD0C8)
        )
        ThemeOption.Forest -> baseColorScheme.copy(
            primary = Color(0xFF4CAF50),
            onPrimary = Color(0xFF08230B),
            primaryContainer = Color(0xFF1E4D23),
            onPrimaryContainer = Color(0xFFCFF5D1),
            secondary = Color(0xFF8BC34A),
            onSecondary = Color(0xFF132100),
            secondaryContainer = Color(0xFF2E4D15),
            onSecondaryContainer = Color(0xFFD8F5B5),
            tertiary = Color(0xFF26A69A),
            onTertiary = Color(0xFF00211F),
            tertiaryContainer = Color(0xFF0F4D46),
            onTertiaryContainer = Color(0xFFB9F2EA)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = MaterialTheme.shapes,
        typography = Typography,
        content = content
    )
}
