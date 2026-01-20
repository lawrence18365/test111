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

import com.google.jetstream.R
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
const val SeriesIdBundleKey = "seriesId"
const val VodIdBundleKey = "vodId"
const val SearchQueryBundleKey = "searchQuery"

enum class Screens(
    private val args: List<String>? = null,
    val isTabItem: Boolean = false,
    val tabIcon: Int? = null
) {
    Profile,
    LiveChannels(isTabItem = true, tabIcon = R.drawable.ic_fa_tv),
    TvGuide(isTabItem = true, tabIcon = R.drawable.ic_fa_list),
    Movies(isTabItem = true, tabIcon = R.drawable.ic_fa_film),
    Shows(isTabItem = true, tabIcon = R.drawable.ic_fa_video),
    Favourites(isTabItem = true, tabIcon = R.drawable.ic_fa_heart),
    Search(isTabItem = true, tabIcon = R.drawable.ic_fa_search),
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
    ),
    SeriesDetail(listOf(SeriesIdBundleKey)),
    VodDetail(listOf(VodIdBundleKey));

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
