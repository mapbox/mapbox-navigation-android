package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.common.Cancelable
import com.mapbox.maps.SourceDataLoadedCallback
import com.mapbox.maps.SourceDataLoadedType
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RoutesExpector {

    private val routeRenderCallbackDatas =
        mutableMapOf<SourceIdAndDataId, RouteRenderCallbackHolder>()

    /**
     * `renderedRouteIdsToNotify` - the ids which have to be passed to callback,
     *  corresponding to routes that are rendered (as opposed to cleared).
     * `clearedRouteIdsToNotify` = the ids which have to be passed to callback,
     *  corresponding to routes that are cleared (as opposed to rendered).
     * `expectedRoutesToRender` - contains info about ids we expect to receive in Maps SDK listener
     *  (some routes may not be rerendered as a result of optimization, so generally route ids
     *  passed to callback may differ from ids we expect to receive in our listener).
     *
     *  The algorithm is the following:
     *  1. If we don't expect anything from the map, we just invoke the callback right away
     *     with the corresponding ids.
     *  2. If we expect anything from the map, we add a listener and store all related data
     *     as an entry in `routeRenderCallbackDatas`.
     *  3. When the listener is invoked, we look for sourceId (eventData.id) and
     *     dataId (eventData.dataId). DataId was passed by the SDK in [MapboxRouteLineView] to
     *     identify the operation (see [MapboxRouteLineView#renderRouteDrawDataInternal] docs).
     *     It's a monotonic increasing integer associated with a particular sourceId.
     *     When we receive dataId in our listener here, by it and sourceId we understand
     *     which operation this listener invocation corresponds to.
     *     If the listener is triggered with sourceId="id#1" and dataId=2, it means that
     *     the operations for (sourceId="id#1", dataId=0) and (sourceId="id#1", dataId=1) either
     *     finished or are cancelled. We use this knowledge to understand which routes will never
     *     be rendered or cleared (because a newer operation on the same source cancelled
     *     this operation).
     *  4. For (sourceId, dataId) received in listener we cancel the corresponding route ids for
     *     all keys (sourceId, oldDataId), where oldDataId < dataId.
     *     If for some callback it was the last route we were waiting for, we remove the
     *     corresponding entry and notify user callback.
     *  5. For (sourceId, dataId) received in listener we find the corresponding entry and say that
     *     the operation finished. We understand whether it was rendering or clearing,
     *     remember this fact.
     *     If it was the last route we were waiting for, we remove the corresponding entry and
     *     notify user callback.
     */
    fun expectRoutes(
        renderedRouteIdsToNotify: Set<String>,
        clearedRouteIdsToNotify: Set<String>,
        expectedRoutesToRender: ExpectedRoutesToRenderData,
        callbackWrapper: RoutesRenderedCallbackWrapper,
    ) {
        if (expectedRoutesToRender.isEmpty()) {
            callbackWrapper.callback.onRoutesRendered(
                RoutesRenderedResult(
                    renderedRouteIdsToNotify,
                    emptySet(),
                    clearedRouteIdsToNotify,
                    emptySet(),
                ),
            )
        } else {
            val map = callbackWrapper.map
            val callback = SourceDataLoadedCallback { eventData ->
                val dataId = eventData.dataId?.toIntOrNull()
                if (eventData.type == SourceDataLoadedType.METADATA && dataId != null) {
                    routeRendered(SourceIdAndDataId(eventData.sourceId, dataId))
                }
            }
            val subscription = map.subscribeSourceDataLoaded(callback)
            val routeRenderCallbackHolder = RouteRenderCallbackHolder(
                callbackWrapper.callback,
                renderedRouteIdsToNotify,
                clearedRouteIdsToNotify,
                expectedRoutesToRender,
                subscription,
            )
            expectedRoutesToRender.getSourceAndDataIds().forEach {
                val key = SourceIdAndDataId(it.first, it.second)
                routeRenderCallbackDatas[key] = routeRenderCallbackHolder
            }
        }
    }

    /**
     * Stops waiting for the map: cancels every source-data subscription and notifies each pending
     * callback, reporting the routes that have not been rendered or cleared yet as cancelled.
     *
     * All subscriptions are cancelled before any callback is invoked, and a callback that throws
     * does not prevent the remaining ones from being notified. The first exception is rethrown
     * once every holder has been handled.
     */
    fun cancelAll() {
        val holders = routeRenderCallbackDatas.values.distinct()
        routeRenderCallbackDatas.clear()
        holders.forEach { it.cancelSubscription() }
        var failure: Throwable? = null
        holders.forEach {
            try {
                it.cancelPending()
                it.notifyAndCleanUp()
            } catch (t: Throwable) {
                if (failure == null) failure = t else failure?.addSuppressed(t)
            }
        }
        failure?.let { throw it }
    }

    private fun maybeFinish(callbackHolder: RouteRenderCallbackHolder) {
        if (callbackHolder.hasAllRoutes()) {
            val keysToRemove = routeRenderCallbackDatas.filter { it.value == callbackHolder }.keys
            keysToRemove.forEach { routeRenderCallbackDatas.remove(it) }
            callbackHolder.notifyAndCleanUp()
        }
    }

    private fun routeRendered(key: SourceIdAndDataId) {
        cancelOutdatedCallbacks(key)
        routeRenderCallbackDatas[key]?.let { callbackHolder ->
            callbackHolder.onRouteRendered(key.sourceId)
            maybeFinish(callbackHolder)
        }
    }

    private fun cancelOutdatedCallbacks(key: SourceIdAndDataId) {
        val finishJobs = mutableListOf<() -> Unit>()
        routeRenderCallbackDatas.forEach {
            if (it.key.isOutdatedBy(key)) {
                it.value.onRouteRenderingCancelled(it.key.sourceId)
                finishJobs.add { maybeFinish(it.value) }
            }
        }
        finishJobs.forEach { it() }
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private class RouteRenderCallbackHolder(
    val callback: RoutesRenderedCallback,
    val renderedRouteIdsToNotify: Set<String>,
    val clearedRouteIdsToNotify: Set<String>,
    val expectedRoutes: ExpectedRoutesToRenderData,
    val subscription: Cancelable,
) {

    private val allRouteIds = expectedRoutes.allRenderedRouteIds + expectedRoutes.allClearedRouteIds
    private val successfulRouteIds = mutableSetOf<String>()
    private val renderingCancelledRouteIds = mutableSetOf<String>()
    private val clearingCancelledRouteIds = mutableSetOf<String>()
    private var notified = false
    private var subscriptionCancelled = false

    fun cancelSubscription() {
        if (subscriptionCancelled) return
        subscriptionCancelled = true
        subscription.cancel()
    }

    fun onRouteRendered(sourceId: String) {
        val renderedRouteId = expectedRoutes.getRenderedRouteId(sourceId)
        val clearedRouteId = expectedRoutes.getClearedRouteId(sourceId)
        if (
            renderedRouteId != null &&
            renderedRouteId in allRouteIds &&
            renderedRouteId in renderedRouteIdsToNotify
        ) {
            successfulRouteIds.add(renderedRouteId)
            renderingCancelledRouteIds.remove(renderedRouteId)
        }
        if (
            clearedRouteId != null &&
            clearedRouteId in allRouteIds &&
            clearedRouteId in clearedRouteIdsToNotify
        ) {
            successfulRouteIds.add(clearedRouteId)
            clearingCancelledRouteIds.remove(clearedRouteId)
        }
    }

    fun onRouteRenderingCancelled(sourceId: String) {
        val renderedRouteId = expectedRoutes.getRenderedRouteId(sourceId)
        val clearedRouteId = expectedRoutes.getClearedRouteId(sourceId)
        if (
            renderedRouteId != null &&
            renderedRouteId in allRouteIds &&
            renderedRouteId in renderedRouteIdsToNotify
        ) {
            renderingCancelledRouteIds.add(renderedRouteId)
            successfulRouteIds.remove(renderedRouteId)
        }
        if (
            clearedRouteId != null &&
            clearedRouteId in allRouteIds &&
            clearedRouteId in clearedRouteIdsToNotify
        ) {
            clearingCancelledRouteIds.add(clearedRouteId)
            successfulRouteIds.remove(clearedRouteId)
        }
    }

    fun cancelPending() {
        expectedRoutes.getSourceAndDataIds().forEach { (sourceId, _) ->
            expectedRoutes.getRenderedRouteId(sourceId)?.let { routeId ->
                if (isPending(routeId, renderedRouteIdsToNotify)) {
                    renderingCancelledRouteIds.add(routeId)
                }
            }
            expectedRoutes.getClearedRouteId(sourceId)?.let { routeId ->
                if (isPending(routeId, clearedRouteIdsToNotify)) {
                    clearingCancelledRouteIds.add(routeId)
                }
            }
        }
    }

    private fun isPending(routeId: String, routeIdsToNotify: Set<String>): Boolean {
        return routeId in allRouteIds &&
            routeId in routeIdsToNotify &&
            routeId !in successfulRouteIds
    }

    fun hasAllRoutes(): Boolean {
        return allRouteIds ==
            successfulRouteIds + renderingCancelledRouteIds + clearingCancelledRouteIds
    }

    /**
     * Notifies the callback at most once. A callback may call [RoutesExpector.cancelAll]
     * reentrantly while a map event is still finishing other holders, so a holder that
     * `cancelAll` already notified can reach this method again.
     */
    fun notifyAndCleanUp() {
        if (notified) return
        notified = true
        cancelSubscription()
        val result = RoutesRenderedResult(
            successfullyRenderedRouteIds = renderedRouteIdsToNotify - renderingCancelledRouteIds,
            renderingCancelledRouteIds = renderingCancelledRouteIds,
            successfullyClearedRouteIds = clearedRouteIdsToNotify - clearingCancelledRouteIds,
            clearingCancelledRouteIds = clearingCancelledRouteIds,
        )
        callback.onRoutesRendered(result)
    }
}

private data class SourceIdAndDataId(
    val sourceId: String,
    val dataId: Int,
) {

    fun isOutdatedBy(other: SourceIdAndDataId): Boolean =
        this.sourceId == other.sourceId && this.dataId < other.dataId
}
