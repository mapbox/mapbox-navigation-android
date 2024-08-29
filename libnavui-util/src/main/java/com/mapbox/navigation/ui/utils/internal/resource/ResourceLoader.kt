package com.mapbox.navigation.ui.utils.internal.resource

import com.mapbox.bindgen.Expected
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadProgress
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.TileStore

/**
 * ResourceLoader is responsible for downloading and storage of any Nav SDK BLOB assets.
 */
abstract class ResourceLoader {

    /**
     * Asynchronously load the resource.
     *
     * If the requested resource is not available in device storage a download will be queued.
     * The [ResourceLoadRequest.flags] and [ResourceLoadRequest.networkRestriction] can be used to
     * control loading behaviour.
     *
     * If the downloaded data causes the disk quota to be exceeded, a quota enforcement operation is
     * scheduled.
     *
     * @param request Resource Load Request
     * @param callback Called to notify of load operation progress
     *
     * @return requestId Request ID that can be used to cancel load operation.
     */
    abstract fun load(request: ResourceLoadRequest, callback: ResourceLoadCallback): Long

    /**
     * Asynchronously load the resource.
     *
     * If the requested resource is not available in device storage a download will be queued.
     * The [ResourceLoadRequest.flags] and [ResourceLoadRequest.networkRestriction] can be used to
     * control loading behaviour.
     *
     * If the downloaded data causes the disk quota to be exceeded, a quota enforcement operation is
     * scheduled.
     *
     * @param tileStore TileStore instance to use when loading resource
     * @param request Resource Load Request
     * @param callback Called to notify of load operation progress
     *
     * @return requestId Request ID that can be used to cancel load operation.
     */
    abstract fun load(
        tileStore: TileStore,
        request: ResourceLoadRequest,
        callback: ResourceLoadCallback,
    ): Long

    /**
     * Cancel load operation.
     *
     * @param requestId Request ID returned by [ResourceLoader.load] call.
     */
    abstract fun cancel(requestId: Long)

    /**
     * Register a [ResourceLoadObserver] that can be used to monitor every resource load operations.
     */
    abstract fun registerObserver(observer: ResourceLoadObserver)

    /**
     * Unregister a [ResourceLoadObserver]
     */
    abstract fun unregisterObserver(observer: ResourceLoadObserver)
}

/**
 * Observer that gets notified when resource load operation progress changes.
 */
interface ResourceLoadObserver {
    /**
     * Invoked once when resource load operation starts.
     *
     * @param request Resource Load Request
     */
    fun onStart(request: ResourceLoadRequest)

    /**
     * Periodically called to notify of download progress.
     *
     * @param request Resource Load Request
     * @param progress Resource Load Progress
     */
    fun onProgress(request: ResourceLoadRequest, progress: ResourceLoadProgress)

    /**
     * Invoked once when resource load operation finishes.
     *
     * @param request Resource Load Request
     * @param result Expected value with [ResourceLoadResult] on success or [ResourceLoadError] on error
     */
    fun onFinish(
        request: ResourceLoadRequest,
        result: Expected<ResourceLoadError, ResourceLoadResult>,
    )
}

/**
 * Called to notify of [ResourceLoader.load] operation progress.
 */
interface ResourceLoadCallback : ResourceLoadObserver
