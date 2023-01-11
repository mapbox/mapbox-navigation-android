package com.mapbox.navigation.ui.shield.internal.loader

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private typealias LoaderCallback<R> = (Expected<String, R>) -> Unit

internal abstract class ResourceDownloader<I, R> : ResourceLoader<I, R> {
    internal companion object {
        internal const val CANCELED_MESSAGE = "canceled"
    }

    private val ongoingRequest = mutableMapOf<I, MutableList<LoaderCallback<R>>>()

    override suspend fun load(argument: I): Expected<String, R> {
        return if (ongoingRequest.contains(argument)) {
            suspendCancellableCoroutine { continuation ->
                val callback: LoaderCallback<R> = { result ->
                    continuation.resume(result)
                }

                ongoingRequest[argument]?.add(callback)
                continuation.invokeOnCancellation {
                    ongoingRequest[argument]?.remove(callback)
                }
            }
        } else {
            ongoingRequest[argument] = mutableListOf()
            val result = try {
                download(argument)
            } catch (ex: CancellationException) {
                ExpectedFactory.createError<String, R>(CANCELED_MESSAGE)
            }
            ongoingRequest.remove(argument)?.onEach { it(result) }
            result
        }
    }

    protected abstract suspend fun download(argument: I): Expected<String, R>
}
