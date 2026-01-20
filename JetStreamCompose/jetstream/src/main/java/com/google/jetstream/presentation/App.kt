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

package com.google.jetstream.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import com.google.jetstream.presentation.screens.Screens
import com.google.jetstream.presentation.screens.StreamCategoryBundleKey
import com.google.jetstream.presentation.screens.StreamIdBundleKey
import com.google.jetstream.presentation.screens.StreamNameBundleKey
import com.google.jetstream.presentation.screens.StreamNumberBundleKey
import com.google.jetstream.presentation.screens.StreamProgramEndBundleKey
import com.google.jetstream.presentation.screens.StreamProgramStartBundleKey
import com.google.jetstream.presentation.screens.StreamProgramTitleBundleKey
import com.google.jetstream.presentation.screens.StreamTypeBundleKey
import com.google.jetstream.presentation.screens.StreamIconBundleKey
import com.google.jetstream.presentation.screens.StreamUrlBundleKey
import com.google.jetstream.presentation.screens.categories.CategoryMovieListScreen
import com.google.jetstream.presentation.screens.dashboard.DashboardScreen
import com.google.jetstream.presentation.screens.login.LoginScreen
import com.google.jetstream.presentation.screens.movies.MovieDetailsScreen
import com.google.jetstream.presentation.screens.streamPlayer.StreamPlayerArgs
import com.google.jetstream.presentation.screens.streamPlayer.StreamPlayerScreen
import com.google.jetstream.presentation.screens.streamPlayer.toRoute
import com.google.jetstream.presentation.screens.videoPlayer.VideoPlayerScreen
import java.net.URLDecoder

@Composable
fun App(
    onBackPressed: () -> Unit,
    xtreamRepository: XtreamRepository
) {
    val navController = rememberNavController()
    var isComingBackFromDifferentScreen by remember { mutableStateOf(false) }

    // Observe login state
    val isLoggedIn by xtreamRepository.isLoggedIn.collectAsState(initial = false)

    // Determine start destination based on login state
    val startDestination = if (isLoggedIn) Screens.Dashboard() else Screens.Login()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        builder = {
            composable(
                route = Screens.CategoryMovieList(),
                arguments = listOf(
                    navArgument(CategoryMovieListScreen.CategoryIdBundleKey) {
                        type = NavType.StringType
                    }
                )
            ) {
                CategoryMovieListScreen(
                    onBackPressed = {
                        if (navController.navigateUp()) {
                            isComingBackFromDifferentScreen = true
                        }
                    },
                    onMovieSelected = { movie ->
                        navController.navigate(
                            Screens.MovieDetails.withArgs(movie.id)
                        )
                    }
                )
            }
            composable(
                route = Screens.MovieDetails(),
                arguments = listOf(
                    navArgument(MovieDetailsScreen.MovieIdBundleKey) {
                        type = NavType.StringType
                    }
                )
            ) {
                MovieDetailsScreen(
                    goToMoviePlayer = { movieId ->
                        navController.navigate(Screens.VideoPlayer.withArgs(movieId))
                    },
                    refreshScreenWithNewMovie = { movie ->
                        navController.navigate(
                            Screens.MovieDetails.withArgs(movie.id)
                        ) {
                            popUpTo(Screens.MovieDetails()) {
                                inclusive = true
                            }
                        }
                    },
                    onBackPressed = {
                        if (navController.navigateUp()) {
                            isComingBackFromDifferentScreen = true
                        }
                    }
                )
            }
            composable(route = Screens.Login()) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screens.Dashboard()) {
                            popUpTo(Screens.Login()) { inclusive = true }
                        }
                    }
                )
            }
            composable(route = Screens.Dashboard()) {
                DashboardScreen(
                    openCategoryMovieList = { categoryId ->
                        navController.navigate(
                            Screens.CategoryMovieList.withArgs(categoryId)
                        )
                    },
                    openMovieDetailsScreen = { movieId ->
                        navController.navigate(
                            Screens.MovieDetails.withArgs(movieId)
                        )
                    },
                    openVideoPlayer = { movie ->
                        navController.navigate(Screens.VideoPlayer.withArgs(movie.id))
                    },
                    openStreamPlayer = { streamArgs ->
                        navController.navigate(streamArgs.toRoute())
                    },
                    onBackPressed = onBackPressed,
                    isComingBackFromDifferentScreen = isComingBackFromDifferentScreen,
                    resetIsComingBackFromDifferentScreen = {
                        isComingBackFromDifferentScreen = false
                    }
                )
            }
            composable(
                route = Screens.VideoPlayer(),
                arguments = listOf(
                    navArgument(VideoPlayerScreen.MovieIdBundleKey) {
                        type = NavType.StringType
                    }
                )
            ) {
                VideoPlayerScreen(
                    onBackPressed = {
                        if (navController.navigateUp()) {
                            isComingBackFromDifferentScreen = true
                        }
                    }
                )
            }
            composable(
                route = Screens.StreamPlayer(),
                arguments = listOf(
                    navArgument(StreamUrlBundleKey) {
                        type = NavType.StringType
                    },
                    navArgument(StreamNameBundleKey) { type = NavType.StringType },
                    navArgument(StreamIdBundleKey) { type = NavType.IntType },
                    navArgument(StreamTypeBundleKey) { type = NavType.StringType },
                    navArgument(StreamIconBundleKey) { type = NavType.StringType },
                    navArgument(StreamCategoryBundleKey) { type = NavType.StringType },
                    navArgument(StreamNumberBundleKey) { type = NavType.IntType },
                    navArgument(StreamProgramTitleBundleKey) { type = NavType.StringType },
                    navArgument(StreamProgramStartBundleKey) { type = NavType.LongType },
                    navArgument(StreamProgramEndBundleKey) { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val streamUrl = URLDecoder.decode(
                    backStackEntry.arguments?.getString(StreamUrlBundleKey) ?: "",
                    "UTF-8"
                )
                val streamName = URLDecoder.decode(
                    backStackEntry.arguments?.getString(StreamNameBundleKey) ?: "Channel",
                    "UTF-8"
                )
                val streamId = backStackEntry.arguments?.getInt(StreamIdBundleKey) ?: -1
                val streamType = URLDecoder.decode(
                    backStackEntry.arguments?.getString(StreamTypeBundleKey) ?: "",
                    "UTF-8"
                )
                val streamIcon = URLDecoder.decode(
                    backStackEntry.arguments?.getString(StreamIconBundleKey).orEmpty(),
                    "UTF-8"
                ).ifBlank { null }
                val streamCategory = URLDecoder.decode(
                    backStackEntry.arguments?.getString(StreamCategoryBundleKey).orEmpty(),
                    "UTF-8"
                ).ifBlank { null }
                val streamNumber = backStackEntry.arguments?.getInt(StreamNumberBundleKey) ?: -1
                val programTitle = URLDecoder.decode(
                    backStackEntry.arguments?.getString(StreamProgramTitleBundleKey).orEmpty(),
                    "UTF-8"
                ).ifBlank { null }
                val programStart = backStackEntry.arguments?.getLong(StreamProgramStartBundleKey) ?: 0L
                val programEnd = backStackEntry.arguments?.getLong(StreamProgramEndBundleKey) ?: 0L
                StreamPlayerScreen(
                    streamArgs = StreamPlayerArgs(
                        streamUrl = streamUrl,
                        streamName = streamName,
                        streamId = streamId,
                        streamType = streamType,
                        streamIcon = streamIcon,
                        categoryName = streamCategory,
                        channelNumber = streamNumber.takeIf { it > 0 },
                        programTitle = programTitle,
                        programStart = programStart.takeIf { it > 0L },
                        programEnd = programEnd.takeIf { it > 0L }
                    ),
                    onBackPressed = {
                        if (navController.navigateUp()) {
                            isComingBackFromDifferentScreen = true
                        }
                    }
                )
            }
        }
    )
}
