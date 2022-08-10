package com.mapbox.androidauto.car.search

import com.mapbox.androidauto.car.feedback.core.CarFeedbackSearchOptions
import com.mapbox.androidauto.car.feedback.core.CarFeedbackSearchOptionsProvider
import com.mapbox.androidauto.car.placeslistonmap.PlacesListOnMapProvider
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.search.AsyncOperationTask
import com.mapbox.search.CompletionCallback
import com.mapbox.search.record.FavoriteRecord
import com.mapbox.search.record.FavoritesDataProvider
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FavoritesApi(
    private val favoritesProvider: FavoritesDataProvider
) : PlacesListOnMapProvider, CarFeedbackSearchOptionsProvider {

    private var getAllTask: AsyncOperationTask? = null
    private var addFavoriteTask: AsyncOperationTask? = null
    private var removeFavoriteTask: AsyncOperationTask? = null
    private var favoriteRecords: List<FavoriteRecord> = emptyList()

    override suspend fun getPlaces(): Expected<GetPlacesError, List<PlaceRecord>> {
        val favorites = getFavorites()
        favoriteRecords = favorites.value ?: emptyList()
        return favorites.mapValue { favoriteRecord ->
            favoriteRecord.map {
                PlaceRecordMapper.fromFavoriteRecord(it)
            }
        }
    }

    override fun cancel() {
        cancelRequests()
    }

    override fun searchOptions(): CarFeedbackSearchOptions = CarFeedbackSearchOptions(
        favoriteRecords = favoriteRecords,
    )

    suspend fun getFavorites(): Expected<GetPlacesError, List<FavoriteRecord>> {
        getAllTask?.cancel()
        return suspendCoroutine { continuation ->
            getAllTask = favoritesProvider.getAll(
                object : CompletionCallback<List<FavoriteRecord>> {

                    override fun onComplete(result: List<FavoriteRecord>) {
                        continuation.resume(ExpectedFactory.createValue(result))
                    }

                    override fun onError(e: Exception) {
                        continuation.resume(
                            ExpectedFactory.createError(
                                GetPlacesError(e.message ?: "Error getting favorites.", e)
                            )
                        )
                    }
                },
            )
        }
    }

    suspend fun addFavorite(
        favoriteRecord: FavoriteRecord
    ): Expected<GetPlacesError, FavoriteRecord> {
        addFavoriteTask?.cancel()
        return suspendCoroutine { continuation ->
            addFavoriteTask = favoritesProvider.upsert(
                favoriteRecord,
                object : CompletionCallback<Unit> {
                    override fun onComplete(result: Unit) {
                        continuation.resume(ExpectedFactory.createValue(favoriteRecord))
                    }

                    override fun onError(e: Exception) {
                        continuation.resume(
                            ExpectedFactory.createError(
                                GetPlacesError(e.message ?: "Error adding favorite.", e)
                            )
                        )
                    }
                },
            )
        }
    }

    suspend fun removeFavorite(favoriteId: String): Expected<GetPlacesError, Boolean> {
        removeFavoriteTask?.cancel()
        return suspendCoroutine { continuation ->
            removeFavoriteTask = favoritesProvider.remove(
                favoriteId,
                object : CompletionCallback<Boolean> {
                    override fun onComplete(result: Boolean) {
                        continuation.resume(ExpectedFactory.createValue(result))
                    }

                    override fun onError(e: Exception) {
                        continuation.resume(
                            ExpectedFactory.createError(
                                GetPlacesError(e.message ?: "Error adding favorite.", e)
                            )
                        )
                    }
                },
            )
        }
    }

    private fun cancelRequests() {
        getAllTask?.cancel()
        addFavoriteTask?.cancel()
        removeFavoriteTask?.cancel()
    }
}
