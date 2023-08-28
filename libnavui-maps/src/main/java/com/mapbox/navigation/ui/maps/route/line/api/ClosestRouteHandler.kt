package com.mapbox.navigation.ui.maps.route.line.api

import androidx.annotation.VisibleForTesting
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedFeature
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal object CompositeClosestRouteHandlerProvider {

    fun createHandler(
        handlers: List<ClosestRouteHandler>
    ): CompositeClosestRouteHandler {
        return CompositeClosestRouteHandler(handlers)
    }
}

internal interface ClosestRouteHandler {

    suspend fun handle(
        map: MapboxMap,
        clickPoint: ScreenCoordinate,
        features: List<FeatureCollection>
    ): Expected<Unit, Int>
}

internal class CompositeClosestRouteHandler(
    private val handlers: List<ClosestRouteHandler>
) : ClosestRouteHandler {

    override suspend fun handle(
        map: MapboxMap,
        clickPoint: ScreenCoordinate,
        features: List<FeatureCollection>
    ): Expected<Unit, Int> {
        for (handler in handlers) {
            val handlerResult = handler.handle(map, clickPoint, features)
            if (handlerResult.isValue) {
                return handlerResult
            }
        }
        return ExpectedFactory.createError(Unit)
    }
}

internal class SinglePointClosestRouteHandler(
    @get:VisibleForTesting
    internal val layerIds: List<String>
) : ClosestRouteHandler {

    override suspend fun handle(
        map: MapboxMap,
        clickPoint: ScreenCoordinate,
        features: List<FeatureCollection>
    ): Expected<Unit, Int> {
        return suspendCoroutine { continuation ->
            map.queryRenderedFeatures(
                clickPoint,
                RenderedQueryOptions(layerIds, null)
            ) {
                val result = ClosestRouteUtils.getIndexOfFirstFeature(it.value.orEmpty(), features)
                continuation.resume(result)
            }
        }
    }
}

internal class RectClosestRouteHandler(
    @get:VisibleForTesting
    internal val layerIds: List<String>,
    @get:VisibleForTesting
    internal val padding: Float,
) : ClosestRouteHandler {

    override suspend fun handle(
        map: MapboxMap,
        clickPoint: ScreenCoordinate,
        features: List<FeatureCollection>
    ): Expected<Unit, Int> {
        val leftFloat = (clickPoint.x - padding)
        val rightFloat = (clickPoint.x + padding)
        val topFloat = (clickPoint.y - padding)
        val bottomFloat = (clickPoint.y + padding)
        val clickRect = ScreenBox(
            ScreenCoordinate(leftFloat, topFloat),
            ScreenCoordinate(rightFloat, bottomFloat)
        )
        return suspendCoroutine { continuation ->
            map.queryRenderedFeatures(
                clickRect,
                RenderedQueryOptions(layerIds, null)
            ) {
                val result = ClosestRouteUtils.getIndexOfFirstFeature(it.value.orEmpty(), features)
                continuation.resume(result)
            }
        }
    }
}

internal object ClosestRouteUtils {

    fun getIndexOfFirstFeature(
        features: List<QueriedFeature>,
        routeFeatures: List<FeatureCollection>
    ): Expected<Unit, Int> {
        val firstFeatureId = features.firstOrNull()?.feature?.id()
        if (firstFeatureId == null) {
            return ExpectedFactory.createError(Unit)
        }
        val index = routeFeatures.indexOfFirst {
            it.features()?.firstOrNull()?.id() == firstFeatureId
        }
        return if (index >= 0) {
            ExpectedFactory.createValue(index)
        } else {
            ExpectedFactory.createError(Unit)
        }
    }
}
