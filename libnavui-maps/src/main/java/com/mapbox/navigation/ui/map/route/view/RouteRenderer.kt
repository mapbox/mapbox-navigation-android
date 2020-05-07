package com.mapbox.navigation.ui.map.route.view

import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.style.expressions.Expression
import com.mapbox.maps.plugin.style.layers.LineLayer
import com.mapbox.maps.plugin.style.layers.lineLayer
import com.mapbox.maps.plugin.style.sources.geojsonSource
import com.mapbox.maps.plugin.style.sources.updateGeoJSON

internal class RouteRenderer<D : LineLayerRenderData>(
    style: Style,
    sourceId: String,
    routeLayerId: String,
    shieldLayerId: String
) {
    private val source = geojsonSource(sourceId) {
        featureCollection(FeatureCollection.fromFeatures(emptyArray()))
        lineMetrics(true)
    }
    private val routeLayer = lineLayer(routeLayerId, sourceId) {}
    private val shieldLayer = lineLayer(shieldLayerId, sourceId) {}

    init {
        source.bindTo(style)
        shieldLayer.bindTo(style)
        routeLayer.bindTo(style)
    }

    fun update(data: D) {
        source.updateGeoJSON(data.featureCollection)
        fun LineLayer.update(color: Int, width: Expression) {
            visibility(data.visibility)
            lineColor(color)
            lineWidth(width)
            lineCap(data.lineCap)
            lineJoin(data.lineJoin)
        }
        routeLayer.update(data.routeColor, data.routeWidthExpression)
        shieldLayer.update(data.shieldColor, data.shieldWidthExpression)
    }
}
