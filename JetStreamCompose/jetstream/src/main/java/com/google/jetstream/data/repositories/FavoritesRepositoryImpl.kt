package com.google.jetstream.data.repositories

import com.google.jetstream.data.local.FavoriteChannel
import com.google.jetstream.data.local.FavoriteChannelDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteChannelDao: FavoriteChannelDao
) : FavoritesRepository {

    override fun getAllFavorites(): Flow<List<FavoriteChannel>> {
        return favoriteChannelDao.getAllFavorites()
    }

    override fun getFavoritesByType(streamType: String): Flow<List<FavoriteChannel>> {
        return favoriteChannelDao.getFavoritesByType(streamType)
    }

    override fun isFavorite(streamId: Int): Flow<Boolean> {
        return favoriteChannelDao.isFavorite(streamId)
    }

    override suspend fun toggleFavorite(favorite: FavoriteChannel) {
        val isFav = favoriteChannelDao.isFavoriteSync(favorite.streamId)
        if (isFav) {
            favoriteChannelDao.removeFavoriteById(favorite.streamId)
        } else {
            favoriteChannelDao.addFavorite(favorite)
        }
    }
}
