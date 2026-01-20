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

package com.google.jetstream.presentation.screens.dashboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.jetstream.data.entities.Movie
import com.google.jetstream.presentation.screens.Screens
import com.google.jetstream.presentation.screens.SearchQueryBundleKey
import com.google.jetstream.presentation.screens.categories.CategoriesScreen
import com.google.jetstream.presentation.screens.epg.EpgScreen
import com.google.jetstream.presentation.screens.favourites.FavouritesScreen
import com.google.jetstream.presentation.screens.home.HomeScreen
import com.google.jetstream.presentation.screens.livechannels.LiveChannelsScreen
import com.google.jetstream.presentation.screens.profile.ProfileScreen
import com.google.jetstream.presentation.screens.seriesdetail.SeriesDetailScreen
import com.google.jetstream.presentation.screens.streamPlayer.StreamPlayerArgs
import com.google.jetstream.presentation.screens.xtreamsearch.XtreamSearchScreen
import com.google.jetstream.presentation.screens.xtreamseries.XtreamSeriesScreen
import com.google.jetstream.presentation.screens.xtreamvod.XtreamVodDetailScreen
import com.google.jetstream.presentation.screens.xtreamvod.XtreamVodScreen
import com.google.jetstream.presentation.utils.Padding

val ParentPadding = PaddingValues(vertical = 16.dp, horizontal = 58.dp)

@Composable
fun rememberChildPadding(direction: LayoutDirection = LocalLayoutDirection.current): Padding {
    return remember {
        Padding(
            start = ParentPadding.calculateStartPadding(direction) + 8.dp,
            top = ParentPadding.calculateTopPadding(),
            end = ParentPadding.calculateEndPadding(direction) + 8.dp,
            bottom = ParentPadding.calculateBottomPadding()
        )
    }
}

@Composable
fun DashboardScreen(
    openCategoryMovieList: (categoryId: String) -> Unit,
    openMovieDetailsScreen: (movieId: String) -> Unit,
    openVideoPlayer: (Movie) -> Unit,
    openStreamPlayer: (StreamPlayerArgs) -> Unit,
    isComingBackFromDifferentScreen: Boolean,
    resetIsComingBackFromDifferentScreen: () -> Unit,
    onBackPressed: () -> Unit
) {
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val navController = rememberNavController()

    var isTopBarVisible by remember { mutableStateOf(true) }
    var isTopBarFocused by remember { mutableStateOf(false) }

    var currentDestination: String? by remember { mutableStateOf(null) }
    val currentTopBarSelectedTabIndex by remember(currentDestination) {
        derivedStateOf {
            val route = currentDestination?.substringBefore("/") ?: return@derivedStateOf 0
            val screen = Screens.values().firstOrNull { it.name == route }
                ?: return@derivedStateOf 0
            TopBarTabs.indexOf(screen).takeIf { it >= 0 } ?: 0
        }
    }

    DisposableEffect(Unit) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            currentDestination = destination.route
        }

        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    BackPressHandledArea(
        // 1. On user's first back press, bring focus to the current selected tab, if TopBar is not
        //    visible, first make it visible, then focus the selected tab
        // 2. On second back press, bring focus back to the first displayed tab
        // 3. On third back press, exit the app
        onBackPressed = {
            if (!isTopBarVisible) {
                isTopBarVisible = true
                TopBarFocusRequesters[currentTopBarSelectedTabIndex + 1].requestFocus()
            } else if (currentTopBarSelectedTabIndex == 0) onBackPressed()
            else if (!isTopBarFocused) {
                TopBarFocusRequesters[currentTopBarSelectedTabIndex + 1].requestFocus()
            } else TopBarFocusRequesters[1].requestFocus()
        }
    ) {
        // We do not want to focus the TopBar everytime we come back from another screen e.g.
        // MovieDetails, CategoryMovieList or VideoPlayer screen
        var wasTopBarFocusRequestedBefore by rememberSaveable { mutableStateOf(false) }

        var topBarHeightPx: Int by rememberSaveable { mutableIntStateOf(0) }

        // Used to show/hide DashboardTopBar
        val topBarYOffsetPx by animateIntAsState(
            targetValue = if (isTopBarVisible) 0 else -topBarHeightPx,
            animationSpec = tween(),
            label = "",
            finishedListener = {
                if (it == -topBarHeightPx && isComingBackFromDifferentScreen) {
                    focusManager.moveFocus(FocusDirection.Down)
                    resetIsComingBackFromDifferentScreen()
                }
            }
        )

        // Used to push down/pull up NavHost when DashboardTopBar is shown/hidden
        val navHostTopPaddingDp by animateDpAsState(
            targetValue = if (isTopBarVisible) with(density) { topBarHeightPx.toDp() } else 0.dp,
            animationSpec = tween(),
            label = "",
        )

        LaunchedEffect(Unit) {
            if (!wasTopBarFocusRequestedBefore) {
                TopBarFocusRequesters[currentTopBarSelectedTabIndex + 1].requestFocus()
                wasTopBarFocusRequestedBefore = true
            }
        }

        DashboardTopBar(
            modifier = Modifier
                .offset { IntOffset(x = 0, y = topBarYOffsetPx) }
                .onSizeChanged { topBarHeightPx = it.height }
                .onFocusChanged { isTopBarFocused = it.hasFocus }
                .padding(
                    horizontal = ParentPadding.calculateStartPadding(
                        LocalLayoutDirection.current
                    ) + 8.dp
                )
                .padding(
                    top = ParentPadding.calculateTopPadding(),
                    bottom = ParentPadding.calculateBottomPadding()
                ),
            selectedTabIndex = currentTopBarSelectedTabIndex,
        ) { screen ->
            val targetRoute = screen()
            if (currentDestination != targetRoute) {
                navController.navigate(targetRoute) {
                    if (screen == TopBarTabs[0]) popUpTo(TopBarTabs[0].invoke())
                    launchSingleTop = true
                }
            }
        }

        Body(
            openCategoryMovieList = openCategoryMovieList,
            openMovieDetailsScreen = openMovieDetailsScreen,
            openVideoPlayer = openVideoPlayer,
            openStreamPlayer = openStreamPlayer,
            updateTopBarVisibility = { isTopBarVisible = it },
            isTopBarVisible = isTopBarVisible,
            navController = navController,
            modifier = Modifier.offset(y = navHostTopPaddingDp),
        )
    }
}

@Composable
private fun BackPressHandledArea(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) =
    Box(
        modifier = Modifier
            .onPreviewKeyEvent {
                if (it.key == Key.Back && it.type == KeyEventType.KeyUp) {
                    onBackPressed()
                    true
                } else {
                    false
                }
            }
            .then(modifier),
        content = content
    )

@Composable
private fun Body(
    openCategoryMovieList: (categoryId: String) -> Unit,
    openMovieDetailsScreen: (movieId: String) -> Unit,
    openVideoPlayer: (Movie) -> Unit,
    openStreamPlayer: (StreamPlayerArgs) -> Unit,
    updateTopBarVisibility: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    isTopBarVisible: Boolean = true,
) =
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Screens.LiveChannels(),
    ) {
        composable(Screens.Profile()) {
            ProfileScreen()
        }
        composable(Screens.LiveChannels()) {
            LiveChannelsScreen(
                onChannelSelected = openStreamPlayer,
                onScroll = updateTopBarVisibility
            )
        }
        composable(Screens.TvGuide()) {
            EpgScreen(
                onChannelSelected = openStreamPlayer,
                onScroll = updateTopBarVisibility
            )
        }
        composable(Screens.Home()) {
            HomeScreen(
                onMovieClick = { selectedMovie ->
                    openMovieDetailsScreen(selectedMovie.id)
                },
                goToVideoPlayer = openVideoPlayer,
                onScroll = updateTopBarVisibility,
                isTopBarVisible = isTopBarVisible
            )
        }
        composable(Screens.Categories()) {
            CategoriesScreen(
                onCategoryClick = openCategoryMovieList,
                onScroll = updateTopBarVisibility
            )
        }

        composable(Screens.Movies()) {
            XtreamVodScreen(
                onVodSelected = { vod ->
                    navController.navigate(Screens.VodDetail.withArgs(vod.streamId))
                },
                onScroll = updateTopBarVisibility
            )
        }
        composable(Screens.Shows()) {
            XtreamSeriesScreen(
                onSeriesSelected = { series ->
                    navController.navigate(Screens.SeriesDetail.withArgs(series.seriesId))
                },
                onScroll = updateTopBarVisibility
            )
        }
        composable(Screens.Favourites()) {
            FavouritesScreen(
                onMovieClick = openMovieDetailsScreen,
                onScroll = updateTopBarVisibility,
                isTopBarVisible = isTopBarVisible
            )
        }
        composable(Screens.Search()) { backStackEntry ->
            val initialQuery = backStackEntry.savedStateHandle.get<String>(SearchQueryBundleKey)
            LaunchedEffect(initialQuery) {
                if (!initialQuery.isNullOrBlank()) {
                    backStackEntry.savedStateHandle.remove<String>(SearchQueryBundleKey)
                }
            }
            XtreamSearchScreen(
                onChannelSelected = openStreamPlayer,
                onVodSelected = { vod ->
                    navController.navigate(Screens.VodDetail.withArgs(vod.streamId))
                },
                onSeriesSelected = { series ->
                    navController.navigate(Screens.SeriesDetail.withArgs(series.seriesId))
                },
                initialQuery = initialQuery
            )
        }
        composable(Screens.VodDetail()) {
            XtreamVodDetailScreen(
                onPlaySelected = openStreamPlayer,
                onCastSelected = { query ->
                    navController.navigate(Screens.Search()) {
                        launchSingleTop = true
                    }
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(SearchQueryBundleKey, query)
                }
            )
        }
        composable(Screens.SeriesDetail()) {
            SeriesDetailScreen(
                onEpisodeSelected = openStreamPlayer,
                onBackPressed = { navController.navigateUp() },
                onCastSelected = { query ->
                    navController.navigate(Screens.Search()) {
                        launchSingleTop = true
                    }
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(SearchQueryBundleKey, query)
                }
            )
        }
    }
