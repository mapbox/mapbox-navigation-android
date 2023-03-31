package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.maps.extension.observable.model.SourceDataType
import com.mapbox.maps.plugin.delegates.listeners.OnSourceDataLoadedListener
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
                    emptySet()
                )
            )
        } else {
            val map = callbackWrapper.map
            val listener = OnSourceDataLoadedListener { eventData ->
                val dataId = eventData.dataId?.toIntOrNull()
                if (eventData.type == SourceDataType.METADATA && dataId != null) {
                    routeRendered(SourceIdAndDataId(eventData.id, dataId))
                }
            }.also { map.addOnSourceDataLoadedListener(it) }
            val routeRenderCallbackHolder = RouteRenderCallbackHolder(
                callbackWrapper,
                renderedRouteIdsToNotify,
                clearedRouteIdsToNotify,
                expectedRoutesToRender,
                listener
            )
            expectedRoutesToRender.getSourceAndDataIds().forEach {
                val key = SourceIdAndDataId(it.first, it.second)
                routeRenderCallbackDatas[key] = routeRenderCallbackHolder
            }
        }
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
    val callbackWrapper: RoutesRenderedCallbackWrapper,
    val renderedRouteIdsToNotify: Set<String>,
    val clearedRouteIdsToNotify: Set<String>,
    val expectedRoutes: ExpectedRoutesToRenderData,
    val listener: OnSourceDataLoadedListener,
) {

    private val allRouteIds = expectedRoutes.allRenderedRouteIds + expectedRoutes.allClearedRouteIds
    private val successfulRouteIds = mutableSetOf<String>()
    private val renderingCancelledRouteIds = mutableSetOf<String>()
    private val clearingCancelledRouteIds = mutableSetOf<String>()

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

    fun hasAllRoutes(): Boolean {
        return allRouteIds ==
            successfulRouteIds + renderingCancelledRouteIds + clearingCancelledRouteIds
    }

    fun notifyAndCleanUp() {
        callbackWrapper.map.removeOnSourceDataLoadedListener(listener)
        val result = RoutesRenderedResult(
            successfullyRenderedRouteIds = renderedRouteIdsToNotify - renderingCancelledRouteIds,
            renderingCancelledRouteIds = renderingCancelledRouteIds,
            successfullyClearedRouteIds = clearedRouteIdsToNotify - clearingCancelledRouteIds,
            clearingCancelledRouteIds = clearingCancelledRouteIds
        )
        callbackWrapper.callback.onRoutesRendered(result)
    }
}

private data class SourceIdAndDataId(
    val sourceId: String,
    val dataId: Int,
) {

    fun isOutdatedBy(other: SourceIdAndDataId): Boolean =
        this.sourceId == other.sourceId && this.dataId < other.dataId
}
