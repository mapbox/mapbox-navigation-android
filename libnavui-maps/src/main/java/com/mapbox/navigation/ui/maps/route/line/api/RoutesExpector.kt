package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.maps.extension.observable.model.SourceDataType
import com.mapbox.maps.plugin.delegates.listeners.OnSourceDataLoadedListener
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RoutesExpector {

    private val routeRenderCallbackDatas =
        mutableMapOf<SourceIdAndDataId, RouteRenderCallbackHolder>()

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
