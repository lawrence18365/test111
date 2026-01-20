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

package com.google.jetstream.data.repositories

import com.google.jetstream.data.entities.Movie
import com.google.jetstream.data.entities.MovieCategoryDetails
import com.google.jetstream.data.entities.MovieDetails
import com.google.jetstream.data.entities.MovieList
import com.google.jetstream.data.entities.ThumbnailType
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import com.google.jetstream.data.repositories.xtream.XtreamResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val xtreamRepository: XtreamRepository
) : MovieRepository {

    override fun getFeaturedMovies() = flow {
        // For featured, we can just return a subset of movies or specific logic
        val list = fetchMovies()
        emit(list.shuffled().take(10))
    }

    override fun getTrendingMovies(): Flow<MovieList> = flow {
        val list = fetchMovies()
        emit(list.take(15))
    }

    override fun getTop10Movies(): Flow<MovieList> = flow {
        val list = fetchMovies()
        emit(list.sortedByDescending { it.id }.take(10))
    }

    override fun getNowPlayingMovies(): Flow<MovieList> = flow {
        val list = fetchMovies()
        emit(list.shuffled().take(10))
    }

    override fun getMovieCategories() = flow {
        // This expects MovieCategoryList which is List<MovieCategory>
        // We need to map Xtream categories
        val result = xtreamRepository.getVodCategories()
        val list =
            if (result is XtreamResult.Success) {
                result.data.map {
                    com.google.jetstream.data.entities.MovieCategory(
                        id = it.categoryId,
                        name = it.categoryName
                    )
                }
            } else {
                emptyList()
            }
        emit(list)
    }

    override suspend fun getMovieCategoryDetails(categoryId: String): MovieCategoryDetails {
        val categoriesResult = xtreamRepository.getVodCategories()
        val categoryName = if (categoriesResult is XtreamResult.Success) {
            categoriesResult.data.find { it.categoryId == categoryId }?.categoryName ?: "Category"
        } else "Category"

        val moviesResult = xtreamRepository.getVodStreamsByCategory(categoryId)
        val movieList = if (moviesResult is XtreamResult.Success) {
            moviesResult.data.map { item ->
                Movie(
                    id = item.streamId.toString(),
                    videoUri = "", 
                    subtitleUri = null,
                    posterUri = item.streamIcon ?: "",
                    name = item.name,
                    description = "",
                    category = "VOD"
                )
            }
        } else {
            emptyList()
        }

        return MovieCategoryDetails(
            id = categoryId,
            name = categoryName,
            movies = movieList
        )
    }

    override suspend fun getMovieDetails(movieId: String): MovieDetails {
        // Check if it is a Series or Movie. IDs might overlap, but for now assuming VOD first.
        // Or try both.
        val id = movieId.toIntOrNull() ?: return emptyMovieDetails(movieId)
        
        // Try VOD Info first
        val vodResult = xtreamRepository.getVodInfo(id)
        if (vodResult is XtreamResult.Success) {
            val info = vodResult.data.info
            val movieData = vodResult.data.movieData
            val extension = movieData?.containerExtension ?: "mp4"
            val streamUrl = xtreamRepository.buildVodStreamUrl(id, extension) ?: ""
            
            return MovieDetails(
                id = movieId,
                videoUri = streamUrl,
                subtitleUri = null,
                posterUri = info?.movieImage ?: "",
                name = movieData?.name ?: info?.name ?: "Unknown",
                description = info?.plot ?: "",
                pgRating = info?.rating ?: "",
                releaseDate = info?.releaseDate ?: "",
                categories = listOf(info?.genre ?: "VOD"),
                duration = info?.duration ?: "",
                director = info?.director ?: "",
                screenplay = "",
                music = "",
                castAndCrew = emptyList(),
                status = "Released",
                originalLanguage = "",
                budget = "",
                revenue = "",
                similarMovies = emptyList(),
                reviewsAndRatings = emptyList()
            )
        }
        
        // Fallback to Series Info if VOD fails (hacky but handles unified ID system if any)
        // Ideally we should know type. But standard interface doesn't pass type.
        
        return emptyMovieDetails(movieId)
    }

    private fun emptyMovieDetails(movieId: String) = MovieDetails(
        id = movieId,
        videoUri = "",
        subtitleUri = null,
        posterUri = "",
        name = "Error loading details",
        description = "Could not fetch details for this content.",
        pgRating = "",
        releaseDate = "",
        categories = emptyList(),
        duration = "",
        director = "",
        screenplay = "",
        music = "",
        castAndCrew = emptyList(),
        status = "",
        originalLanguage = "",
        budget = "",
        revenue = "",
        similarMovies = emptyList(),
        reviewsAndRatings = emptyList()
    )

    override suspend fun searchMovies(query: String): MovieList {
        val movies = fetchMovies()
        return movies.filter { it.name.contains(query, ignoreCase = true) }
    }

    override fun getMoviesWithLongThumbnail() = getMovies()

    override fun getMovies(): Flow<MovieList> = flow {
        emit(fetchMovies())
    }

    override fun getPopularFilmsThisWeek(): Flow<MovieList> = flow {
        val list = fetchMovies()
        emit(list.sortedByDescending { it.name }.take(15)) // Mock sorting
    }

    override fun getTVShows(): Flow<MovieList> = flow {
        val result = xtreamRepository.getSeries()
        val list = if (result is XtreamResult.Success) {
            result.data.map { item ->
                Movie(
                    id = item.seriesId.toString(),
                    videoUri = "",
                    subtitleUri = null,
                    posterUri = item.cover ?: "",
                    name = item.name,
                    description = item.plot ?: "",
                    category = "Series"
                )
            }
        } else {
            emptyList()
        }
        emit(list)
    }

    override fun getBingeWatchDramas(): Flow<MovieList> = flow {
        val list = fetchMovies() // Placeholder, using movies
        emit(list.take(5))
    }

    // This method is deprecated by FavoritesRepository but kept for interface compat
    override fun getFavouriteMovies(): Flow<MovieList> =
        flow {
            emit(emptyList())
        }

    private suspend fun fetchMovies(): List<Movie> {
        val result = xtreamRepository.getVodStreams()
        return if (result is XtreamResult.Success) {
            result.data.map { item ->
                Movie(
                    id = item.streamId.toString(),
                    videoUri = "", 
                    subtitleUri = null,
                    posterUri = item.streamIcon ?: "",
                    name = item.name,
                    description = "", // Description often not in list
                    category = "VOD"
                )
            }
        } else {
            emptyList()
        }
    }
}
