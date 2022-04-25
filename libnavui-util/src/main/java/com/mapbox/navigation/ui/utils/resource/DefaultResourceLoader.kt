package com.mapbox.navigation.ui.utils.resource

import com.mapbox.bindgen.Expected
import com.mapbox.common.Cancelable
import com.mapbox.common.ResourceDescription
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadOptions
import com.mapbox.common.ResourceLoadProgress
import com.mapbox.common.ResourceLoadProgressCallback
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadResultCallback
import com.mapbox.common.TileDataDomain
import com.mapbox.common.TileStore
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * Default [ResourceLoader] implementation.
 *
 * Uses [TileStore] to load resources.
 * Keeps track of all [Cancelable] instances returned by the [TileStore.loadResource] and
 * assigns each a unique <i>id</i> that can be passed to [ResourceLoader.cancel] to
 * abort load operation.
 */
internal class DefaultResourceLoader(
    private val tileStore: TileStore
) : ResourceLoader() {

    private val nextRequestId = AtomicLong(0L)
    private val cancelableMap = ConcurrentHashMap<Long, Cancelable>()
    private val observers: Queue<ResourceLoadObserver> = ConcurrentLinkedQueue()

    override fun load(request: ResourceLoadRequest, callback: ResourceLoadCallback): Long {
        val requestId = nextRequestId.incrementAndGet()
        val callbackAdapter = CallbackAdapter(request, callback, *observers.toTypedArray())

        cancelableMap[requestId] = tileStore.loadResource(
            /* description */ request.toResourceDescription(),
            /* options */ request.toResourceLoadOptions("DefaultResourceLoader-$requestId"),
            /* progressCallback */ callbackAdapter
        ) {
            cancelableMap.remove(requestId)
            callbackAdapter.run(it)
        }

        return requestId
    }

    override fun cancel(requestId: Long) {
        cancelableMap.remove(requestId)?.cancel()
    }

    override fun registerObserver(observer: ResourceLoadObserver) {
        observers.add(observer)
    }

    override fun unregisterObserver(observer: ResourceLoadObserver) {
        observers.remove(observer)
    }

    private fun ResourceLoadRequest.toResourceDescription() =
        ResourceDescription(url, TileDataDomain.NAVIGATION)

    private fun ResourceLoadRequest.toResourceLoadOptions(tag: String) =
        ResourceLoadOptions(tag, flags, networkRestriction, null)

    private class CallbackAdapter(
        private val request: ResourceLoadRequest,
        private vararg val observers: ResourceLoadObserver
    ) : ResourceLoadProgressCallback, ResourceLoadResultCallback {

        private var started = false

        override fun run(progress: ResourceLoadProgress) {
            if (!started) {
                started = true
                observers.forEach { it.onStart(request) }
            }
            observers.forEach { it.onProgress(request, progress) }
        }

        override fun run(result: Expected<ResourceLoadError, ResourceLoadResult>) {
            observers.forEach { it.onFinish(request, result) }
        }
    }
}
