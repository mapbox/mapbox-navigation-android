package com.mapbox.navigation.ui.androidauto.internal.search

import com.mapbox.search.ForwardSearchOptions
import com.mapbox.search.ResponseInfo
import com.mapbox.search.ReverseGeoOptions
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchEngine
import com.mapbox.search.result.SearchResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal suspend fun SearchEngine.forward(
    query: String,
    options: ForwardSearchOptions,
): Result<List<SearchResult>> {
    return suspendCancellableCoroutine { continuation ->
        val task = forward(
            query,
            options,
            object : SearchCallback {
                override fun onResults(results: List<SearchResult>, responseInfo: ResponseInfo) {
                    if (continuation.isActive) {
                        continuation.resume(Result.success(results))
                    }
                }

                override fun onError(e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(e))
                    }
                }
            },
        )
        continuation.invokeOnCancellation { task.cancel() }
    }
}

internal suspend fun SearchEngine.search(options: ReverseGeoOptions): Result<List<SearchResult>> {
    return suspendCancellableCoroutine { continuation ->
        val task = search(
            options,
            object : SearchCallback {
                override fun onResults(results: List<SearchResult>, responseInfo: ResponseInfo) {
                    if (continuation.isActive) {
                        continuation.resume(Result.success(results))
                    }
                }

                override fun onError(e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(e))
                    }
                }
            },
        )
        continuation.invokeOnCancellation { task.cancel() }
    }
}
