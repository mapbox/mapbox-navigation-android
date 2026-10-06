package com.mapbox.navigation.ui.androidauto.internal.search

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.androidauto.placeslistonmap.PlacesListOnMapProvider
import com.mapbox.navigation.ui.androidauto.search.GetPlacesError
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord
import com.mapbox.navigation.ui.androidauto.search.PlaceRecordMapper
import com.mapbox.search.common.AsyncOperationTask
import com.mapbox.search.common.CompletionCallback
import com.mapbox.search.record.FavoriteRecord
import com.mapbox.search.record.FavoritesDataProvider
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Collections
import kotlin.coroutines.resume

class FavoritesApi(
    private val favoritesProvider: FavoritesDataProvider,
) : PlacesListOnMapProvider {

    // Callers that are still waiting for a result, so the deprecated cancel() can reach them.
    // Synchronized because callers may start a call on a background dispatcher while Search SDK
    // callbacks and cancellation arrive on the main thread.
    private val pendingContinuations: MutableSet<CancellableContinuation<*>> =
        Collections.synchronizedSet(mutableSetOf())

    override suspend fun getPlaces(): Expected<GetPlacesError, List<PlaceRecord>> {
        val favorites = getFavorites()
        return favorites.mapValue { favoriteRecord ->
            favoriteRecord.map {
                PlaceRecordMapper.fromFavoriteRecord(it)
            }
        }
    }

    @Deprecated("Use coroutine scope cancellation instead.")
    override fun cancel() {
        val pending = synchronized(pendingContinuations) { pendingContinuations.toList() }
        pending.forEach { it.cancel() }
    }

    suspend fun getFavorites(): Expected<GetPlacesError, List<FavoriteRecord>> =
        awaitTask("Error getting favorites.") { callback ->
            favoritesProvider.getAll(callback)
        }

    suspend fun addFavorite(
        favoriteRecord: FavoriteRecord,
    ): Expected<GetPlacesError, FavoriteRecord> {
        val result: Expected<GetPlacesError, Unit> = awaitTask("Error adding favorite.") {
            favoritesProvider.upsert(favoriteRecord, it)
        }
        return result.mapValue { favoriteRecord }
    }

    suspend fun removeFavorite(favoriteId: String): Expected<GetPlacesError, Boolean> =
        awaitTask("Error removing favorite.") { callback ->
            favoritesProvider.remove(favoriteId, callback)
        }

    /**
     * Runs the task started by [start] and suspends until it completes. Cancelling the caller
     * cancels the task. Every caller gets its own task, so concurrent callers each get a result.
     */
    private suspend fun <T : Any> awaitTask(
        defaultErrorMessage: String,
        start: (CompletionCallback<T>) -> AsyncOperationTask,
    ): Expected<GetPlacesError, T> = suspendCancellableCoroutine { continuation ->
        val task = start(
            object : CompletionCallback<T> {
                override fun onComplete(result: T) {
                    pendingContinuations.remove(continuation)
                    continuation.resume(ExpectedFactory.createValue(result))
                }

                override fun onError(e: Exception) {
                    pendingContinuations.remove(continuation)
                    continuation.resume(
                        ExpectedFactory.createError(
                            GetPlacesError(e.message ?: defaultErrorMessage, e),
                        ),
                    )
                }
            },
        )
        // Registered only once the task started, so a provider that throws leaves nothing behind.
        pendingContinuations.add(continuation)
        // The callback may already have run, synchronously or on another thread, before the add.
        if (!continuation.isActive) {
            pendingContinuations.remove(continuation)
        }
        continuation.invokeOnCancellation {
            pendingContinuations.remove(continuation)
            task.cancel()
        }
    }
}
