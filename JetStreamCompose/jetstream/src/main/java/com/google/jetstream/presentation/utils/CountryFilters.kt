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

import com.google.jetstream.data.models.xtream.XtreamCategory

enum class CountryFilter(
    val displayName: String,
    private val matchers: List<Regex>
) {
    IRELAND(
        displayName = "Ireland",
        matchers = listOf(
            Regex("\\bireland\\b"),
            Regex("\\birish\\b"),
            Regex("\\beire\\b"),
            Regex("\\birl\\b")
        )
    ),
    USA(
        displayName = "USA",
        matchers = listOf(
            Regex("\\busa\\b"),
            Regex("\\bunited\\s+states\\b"),
            Regex("\\bu\\s*s\\s*a\\b"),
            Regex("\\bu\\s*s\\b"),
            Regex("\\bamerica\\b")
        )
    ),
    UK(
        displayName = "UK",
        matchers = listOf(
            Regex("\\buk\\b"),
            Regex("\\bunited\\s+kingdom\\b"),
            Regex("\\bu\\s*k\\b"),
            Regex("\\bbritain\\b"),
            Regex("\\bbritish\\b"),
            Regex("\\bengland\\b"),
            Regex("\\bscotland\\b"),
            Regex("\\bwales\\b"),
            Regex("\\bgb\\b"),
            Regex("\\bgbr\\b")
        )
    );

    fun matches(categoryName: String): Boolean {
        val normalized = normalize(categoryName)
        return matchers.any { it.containsMatchIn(normalized) }
    }
}

val DefaultCountryFilters: List<CountryFilter> = listOf(
    CountryFilter.IRELAND,
    CountryFilter.USA,
    CountryFilter.UK
)

fun categoryIdsForCountry(
    categories: List<XtreamCategory>,
    country: CountryFilter
): Set<String> {
    return categories
        .filter { country.matches(it.categoryName) }
        .map { it.categoryId }
        .toSet()
}

fun categoriesForCountry(
    categories: List<XtreamCategory>,
    country: CountryFilter
): List<XtreamCategory> {
    return categories.filter { country.matches(it.categoryName) }
}

private fun normalize(value: String): String {
    return value
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}
