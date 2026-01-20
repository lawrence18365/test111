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

package com.google.jetstream.presentation.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.jetstream.presentation.screens.categories.CategoryMovieListScreen
import com.google.jetstream.presentation.screens.movies.MovieDetailsScreen
import com.google.jetstream.presentation.screens.videoPlayer.VideoPlayerScreen

const val StreamUrlBundleKey = "streamUrl"
const val StreamNameBundleKey = "streamName"
const val StreamIdBundleKey = "streamId"
const val StreamTypeBundleKey = "streamType"
const val StreamIconBundleKey = "streamIcon"
const val StreamCategoryBundleKey = "streamCategory"
const val StreamNumberBundleKey = "streamNumber"
const val StreamProgramTitleBundleKey = "programTitle"
const val StreamProgramStartBundleKey = "programStart"
const val StreamProgramEndBundleKey = "programEnd"

enum class Screens(
    private val args: List<String>? = null,
    val isTabItem: Boolean = false,
    val tabIcon: ImageVector? = null
) {
    Profile,
    LiveChannels(isTabItem = true, tabIcon = Icons.Default.LiveTv),
    TvGuide(isTabItem = true, tabIcon = Icons.Default.GridView),
    Movies(isTabItem = true),
    Shows(isTabItem = true),
    Favourites(isTabItem = true, tabIcon = Icons.Default.Favorite),
    Search(isTabItem = true, tabIcon = Icons.Default.Search),
    Home(isTabItem = false), // Keep Home but not as tab
    Categories(isTabItem = false), // Keep Categories but not as tab
    CategoryMovieList(listOf(CategoryMovieListScreen.CategoryIdBundleKey)),
    MovieDetails(listOf(MovieDetailsScreen.MovieIdBundleKey)),
    Login,
    Dashboard,
    VideoPlayer(listOf(VideoPlayerScreen.MovieIdBundleKey)),
    StreamPlayer(
        listOf(
            StreamUrlBundleKey,
            StreamNameBundleKey,
            StreamIdBundleKey,
            StreamTypeBundleKey,
            StreamIconBundleKey,
            StreamCategoryBundleKey,
            StreamNumberBundleKey,
            StreamProgramTitleBundleKey,
            StreamProgramStartBundleKey,
            StreamProgramEndBundleKey
        )
    );

    operator fun invoke(): String {
        val argList = StringBuilder()
        args?.let { nnArgs ->
            nnArgs.forEach { arg -> argList.append("/{$arg}") }
        }
        return name + argList
    }

    fun withArgs(vararg args: Any): String {
        val destination = StringBuilder()
        args.forEach { arg -> destination.append("/$arg") }
        return name + destination
    }
}
