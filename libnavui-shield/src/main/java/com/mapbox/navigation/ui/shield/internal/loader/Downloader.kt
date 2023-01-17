package com.mapbox.navigation.ui.shield.internal.loader

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private typealias Callback<R> = (Expected<Error, R>) -> Unit

internal abstract class Downloader<I, O> : Loader<I, O> {
    internal companion object {
        internal const val CANCELED_MESSAGE = "canceled"
    }

    private val ongoingRequest = mutableMapOf<I, MutableList<Callback<O>>>()

    override suspend fun load(input: I): Expected<Error, O> {
        return if (ongoingRequest.contains(input)) {
            suspendCancellableCoroutine { continuation ->
                val callback: Callback<O> = { result ->
                    continuation.resume(result)
                }

                ongoingRequest[input]?.add(callback)
                continuation.invokeOnCancellation {
                    ongoingRequest[input]?.remove(callback)
                }
            }
        } else {
            ongoingRequest[input] = mutableListOf()
            val result = try {
                download(input)
            } catch (ex: CancellationException) {
                ExpectedFactory.createError<Error, O>(Error(CANCELED_MESSAGE, ex))
            }
            ongoingRequest.remove(input)?.onEach { it(result) }
            result
        }
    }

    protected abstract suspend fun download(input: I): Expected<Error, O>
}
