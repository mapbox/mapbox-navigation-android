package com.mapbox.navigation.ui.utils.internal.resource

import androidx.annotation.RestrictTo
import com.mapbox.bindgen.Expected
import com.mapbox.common.Cancelable
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.ReachabilityInterface
import com.mapbox.common.ResourceDescription
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadFlags
import com.mapbox.common.ResourceLoadOptions
import com.mapbox.common.ResourceLoadProgress
import com.mapbox.common.ResourceLoadProgressCallback
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadResultCallback
import com.mapbox.common.TileDataDomain
import com.mapbox.common.TileStore
import com.mapbox.navigation.ui.base.util.resource.ResourceLoadCallback
import com.mapbox.navigation.ui.base.util.resource.ResourceLoadObserver
import com.mapbox.navigation.ui.base.util.resource.ResourceLoader
import com.mapbox.navigation.utils.internal.NavigationTileStore
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * Default [ResourceLoader] implementation.
 *
 * Uses [navigationTileStore]'s [TileStore] to load resources.
 * Keeps track of all [Cancelable] instances returned by the [TileStore.loadResource] and
 * assigns each a unique <i>id</i> that can be passed to [ResourceLoader.cancel] to
 * abort load operation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class DefaultResourceLoader(
    private val navigationTileStore: NavigationTileStore,
    private val reachability: ReachabilityInterface,
) : ResourceLoader() {

    private val nextRequestId = AtomicLong(0L)
    private val cancelableMap = ConcurrentHashMap<Long, Cancelable>()
    private val observers: Queue<ResourceLoadObserver> = ConcurrentLinkedQueue()

    override fun load(request: ResourceLoadRequest, callback: ResourceLoadCallback): Long {
        return load(navigationTileStore(), request, callback)
    }

    override fun load(
        tileStore: TileStore,
        request: ResourceLoadRequest,
        callback: ResourceLoadCallback,
    ): Long {
        val requestId = nextRequestId.incrementAndGet()
        val callbackAdapter = CallbackAdapter(request, callback, observers)

        callbackAdapter.notifyOnStart(request)
        cancelableMap[requestId] = tileStore.loadResource(
            /* description */ request.toResourceDescription(),
            /* options */ loadOptions(request, requestId),
            /* progressCallback */ callbackAdapter,
        ) {
            cancelableMap.remove(requestId)
            callbackAdapter.run(it)
        }

        return requestId
    }

    override fun cancel(requestId: Long) {
        cancelableMap.remove(requestId)?.cancel()
    }

    private fun loadOptions(request: ResourceLoadRequest, requestId: Long): ResourceLoadOptions {
        val requiresNetwork = request.networkRestriction != NetworkRestriction.DISALLOW_ALL
        val tag = "DefaultResourceLoader-$requestId"
        return if (requiresNetwork && !reachability.isReachable) {
            ResourceLoadOptions(
                tag,
                ResourceLoadFlags.ACCEPT_EXPIRED,
                NetworkRestriction.DISALLOW_ALL,
                null,
            )
        } else {
            ResourceLoadOptions(
                tag,
                request.flags,
                request.networkRestriction,
                null,
            )
        }
    }

    override fun registerObserver(observer: ResourceLoadObserver) {
        observers.add(observer)
    }

    override fun unregisterObserver(observer: ResourceLoadObserver) {
        observers.remove(observer)
    }

    private fun ResourceLoadRequest.toResourceDescription() =
        ResourceDescription(TileDataDomain.NAVIGATION, url)

    private class CallbackAdapter(
        private val request: ResourceLoadRequest,
        private val callback: ResourceLoadCallback,
        private val observers: Queue<ResourceLoadObserver>,
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
