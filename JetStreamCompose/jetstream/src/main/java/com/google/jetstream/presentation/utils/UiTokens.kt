/*
 * Copyright 2024 Google LLC
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

package com.google.jetstream.presentation.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme

@Composable
fun headerBackdropBrush(): Brush {
    val surface = MaterialTheme.colorScheme.surface
    val tint = lerp(surface, MaterialTheme.colorScheme.primary, 0.2f)
    return Brush.verticalGradient(
        colors = listOf(
            tint.copy(alpha = 0.9f),
            surface.copy(alpha = 0.0f)
        )
    )
}

@Composable
fun focusBorderStroke(color: Color = MaterialTheme.colorScheme.primary): BorderStroke {
    return BorderStroke(1.5.dp, color.copy(alpha = 0.75f))
}
