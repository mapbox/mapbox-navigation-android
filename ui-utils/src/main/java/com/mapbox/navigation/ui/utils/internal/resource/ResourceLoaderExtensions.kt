package com.mapbox.navigation.ui.utils.internal.resource

import com.mapbox.bindgen.Expected
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadProgress
import com.mapbox.common.ResourceLoadResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Asynchronously load the resource.
 * @see [ResourceLoader.load]
 *
 * @param request Resource Load Request
 * @param onFinished Lambda to invoke when load operation finishes
 *
 * @return requestId Request ID that can be used to cancel load operation.
 */
fun ResourceLoader.load(
    request: ResourceLoadRequest,
    onFinished: (Expected<ResourceLoadError, ResourceLoadResult>) -> Unit,
): Long = load(
    request,
    object : ResourceLoadCallback {
        override fun onStart(request: ResourceLoadRequest) = Unit
        override fun onProgress(request: ResourceLoadRequest, progress: ResourceLoadProgress) = Unit
        override fun onFinish(
            request: ResourceLoadRequest,
            result: Expected<ResourceLoadError, ResourceLoadResult>,
        ) = onFinished(result)
    },
)

/**
 * Coroutine extension for [ResourceLoader.load] method.
 * Uses [suspendCancellableCoroutine] to invoke [ResourceLoader.cancel] when parent Job
 * is cancelled.
 *
 * @param request Resource Load Request
 *
 * @return Expected value with [ResourceLoadResult] on success or [ResourceLoadError] on error
 */
suspend fun ResourceLoader.load(
    request: ResourceLoadRequest,
): Expected<ResourceLoadError, ResourceLoadResult> =
    suspendCancellableCoroutine { cont ->
        val requestId = load(request, cont::resume)
        cont.invokeOnCancellation { cancel(requestId) }
    }
