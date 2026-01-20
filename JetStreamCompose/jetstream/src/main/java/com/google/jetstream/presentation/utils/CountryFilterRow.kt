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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CountryFilterRow(
    selectedCountry: CountryFilter,
    onCountrySelected: (CountryFilter) -> Unit,
    countries: List<CountryFilter> = DefaultCountryFilters
) {
    TvLazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(countries, key = { it.name }) { country ->
            FilterChip(
                selected = selectedCountry == country,
                onClick = { onCountrySelected(country) },
                border = FilterChipDefaults.border(
                    border = androidx.tv.material3.Border(
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.border.copy(alpha = 0.4f)
                        )
                    ),
                    focusedBorder = androidx.tv.material3.Border(
                        border = focusBorderStroke()
                    )
                ),
                scale = FilterChipDefaults.scale(focusedScale = 1f),
                colors = FilterChipDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.5f
                    ),
                    focusedSelectedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.7f
                    ),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.6f
                    )
                )
            ) {
                Text(country.displayName)
            }
        }
    }
}
