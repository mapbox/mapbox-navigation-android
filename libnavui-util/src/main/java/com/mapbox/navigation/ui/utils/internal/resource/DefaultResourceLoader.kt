package com.mapbox.navigation.ui.utils.internal.resource

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory.createError
import com.mapbox.common.Cancelable
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.ReachabilityInterface
import com.mapbox.common.ResourceDescription
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadErrorType
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
    private val tileStore: TileStore,
    private val reachability: ReachabilityInterface
) : ResourceLoader() {

    private val nextRequestId = AtomicLong(0L)
    private val cancelableMap = ConcurrentHashMap<Long, Cancelable>()
    private val observers: Queue<ResourceLoadObserver> = ConcurrentLinkedQueue()

    override fun load(request: ResourceLoadRequest, callback: ResourceLoadCallback): Long {
        return load(tileStore, request, callback)
    }

    override fun load(
        tileStore: TileStore,
        request: ResourceLoadRequest,
        callback: ResourceLoadCallback
    ): Long {
        val requestId = nextRequestId.incrementAndGet()
        val callbackAdapter = CallbackAdapter(request, callback, observers)
        val requiresNetwork = request.networkRestriction != NetworkRestriction.DISALLOW_ALL

        callbackAdapter.notifyOnStart(request)
        // Since the TileStore (commonSDK 21.3.1) will defer any requests that require internet connection,
        // we must verify network requirement here and fail fast if the network is not available.
        if (requiresNetwork && !reachability.isReachable) {
            callbackAdapter.run(createError(connectionError()))
        } else {
            cancelableMap[requestId] = tileStore.loadResource(
                /* description */ request.toResourceDescription(),
                /* options */ request.toResourceLoadOptions("DefaultResourceLoader-$requestId"),
                /* progressCallback */ callbackAdapter
            ) {
                cancelableMap.remove(requestId)
                callbackAdapter.run(it)
            }
        }

        return requestId
    }

    private fun connectionError() = ResourceLoadError(
        ResourceLoadErrorType.UNSATISFIED,
        "No internet connection",
        0L
    )

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
        private val callback: ResourceLoadCallback,
        private val observers: Queue<ResourceLoadObserver>
    ) : ResourceLoadProgressCallback, ResourceLoadResultCallback {

        fun notifyOnStart(request: ResourceLoadRequest) {
            callback.onStart(request)
            observers.forEach { it.onStart(request) }
        }

        override fun run(progress: ResourceLoadProgress) {
            callback.onProgress(request, progress)
            observers.forEach { it.onProgress(request, progress) }
        }

        override fun run(result: Expected<ResourceLoadError, ResourceLoadResult>) {
            callback.onFinish(request, result)
            observers.forEach { it.onFinish(request, result) }
        }
    }
}
