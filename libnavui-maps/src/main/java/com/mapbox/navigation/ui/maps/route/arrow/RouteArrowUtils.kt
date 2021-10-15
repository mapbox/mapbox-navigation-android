package com.mapbox.navigation.ui.maps.route.arrow

import androidx.core.graphics.drawable.DrawableCompat
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addPersistentLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconRotationAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.utils.internal.extensions.getBitmap
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMisc

internal object RouteArrowUtils {

    fun obtainArrowPointsFrom(routeProgress: RouteProgress): List<Point> {
        val reversedCurrent: List<Point> = routeProgress.currentLegProgress
            ?.currentStepProgress?.stepPoints?.reversed()
            ?: listOf()
        val arrowLineCurrent = LineString.fromLngLats(reversedCurrent)
        if (reversedCurrent.size < 2) {
            return listOf()
        }

        val upcomingStepPoints = routeProgress.upcomingStepPoints ?: emptyList()
        val arrowLineUpcoming = LineString.fromLngLats(upcomingStepPoints)
        if (upcomingStepPoints.size < 2) {
            return listOf()
        }

        val arrowCurrentSliced = TurfMisc.lineSliceAlong(
            arrowLineCurrent,
            0.0,
            RouteLayerConstants.THIRTY.toDouble(),
            TurfConstants.UNIT_METERS
        )
        val arrowUpcomingSliced = TurfMisc.lineSliceAlong(
            arrowLineUpcoming,
            0.0,
            RouteLayerConstants.THIRTY.toDouble(),
            TurfConstants.UNIT_METERS
        )

        return arrowCurrentSliced.coordinates().reversed().plus(arrowUpcomingSliced.coordinates())
    }

    @OptIn(MapboxExperimental::class)
    fun initializeLayers(style: Style, options: RouteArrowOptions) {
        if (layersAreInitialized(style)) {
            return
        }

        val aboveLayerIdToUse = if (style.styleLayerExists(options.aboveLayerId)) {
            options.aboveLayerId
        } else {
            null
        }

        if (!style.styleSourceExists(RouteLayerConstants.ARROW_SHAFT_SOURCE_ID)) {
            geoJsonSource(RouteLayerConstants.ARROW_SHAFT_SOURCE_ID) {
                maxzoom(16)
                tolerance(options.tolerance)
            }.featureCollection(FeatureCollection.fromFeatures(listOf())).bindTo(style)
        }

        if (!style.styleSourceExists(RouteLayerConstants.ARROW_HEAD_SOURCE_ID)) {
            geoJsonSource(RouteLayerConstants.ARROW_HEAD_SOURCE_ID) {
                maxzoom(16)
                tolerance(options.tolerance)
            }.featureCollection(FeatureCollection.fromFeatures(listOf())).bindTo(style)
        }

        if (style.getStyleImage(RouteLayerConstants.ARROW_HEAD_ICON_CASING) != null) {
            style.removeStyleImage(RouteLayerConstants.ARROW_HEAD_ICON_CASING)
        }

        if (
            options.arrowHeadIconCasing.intrinsicHeight > 0 &&
            options.arrowHeadIconCasing.intrinsicWidth > 0
        ) {
            val arrowHeadCasingDrawable = DrawableCompat.wrap(
                options.arrowHeadIconCasing
            )
            DrawableCompat.setTint(
                arrowHeadCasingDrawable.mutate(),
                options.arrowCasingColor
            )
            val arrowHeadCasingBitmap = arrowHeadCasingDrawable.getBitmap()
            style.addImage(RouteLayerConstants.ARROW_HEAD_ICON_CASING, arrowHeadCasingBitmap)
        }

        if (style.getStyleImage(RouteLayerConstants.ARROW_HEAD_ICON) != null) {
            style.removeStyleImage(RouteLayerConstants.ARROW_HEAD_ICON)
        }

        if (options.arrowHeadIcon.intrinsicHeight > 0 && options.arrowHeadIcon.intrinsicWidth > 0) {
            val arrowHeadDrawable = DrawableCompat.wrap(
                options.arrowHeadIcon
            )
            DrawableCompat.setTint(
                arrowHeadDrawable.mutate(),
                options.arrowColor
            )
            val arrowHeadBitmap = arrowHeadDrawable.getBitmap()
            style.addImage(RouteLayerConstants.ARROW_HEAD_ICON, arrowHeadBitmap)
        }

        // arrow shaft casing
        if (style.styleLayerExists(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)) {
            style.removeStyleLayer(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)
        }
        val arrowShaftCasingLayer = LineLayer(
            RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID,
            RouteLayerConstants.ARROW_SHAFT_SOURCE_ID
        )
            .lineColor(
                Expression.color(
                    options.arrowCasingColor
                )
            )
            .lineWidth(options.arrowShaftCasingScaleExpression)
            .lineCap(LineCap.ROUND)
            .lineJoin(LineJoin.ROUND)
            .visibility(Visibility.VISIBLE)
            .lineOpacity(
                Expression.step {
                    zoom()
                    literal(RouteLayerConstants.OPAQUE)
                    stop {
                        literal(RouteLayerConstants.ARROW_HIDDEN_ZOOM_LEVEL)
                        literal(RouteLayerConstants.TRANSPARENT)
                    }
                }
            )

        // arrow head casing
        if (style.styleLayerExists(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID)) {
            style.removeStyleLayer(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID)
        }
        val arrowHeadCasingLayer = SymbolLayer(
            RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID,
            RouteLayerConstants.ARROW_HEAD_SOURCE_ID
        )
            .iconImage(RouteLayerConstants.ARROW_HEAD_ICON_CASING)
            .iconAllowOverlap(true)
            .iconIgnorePlacement(true)
            .iconSize(options.arrowHeadCasingScaleExpression)
            .iconOffset(RouteLayerConstants.ARROW_HEAD_OFFSET.toList())
            .iconRotationAlignment(IconRotationAlignment.MAP)
            .iconRotate(
                com.mapbox.maps.extension.style.expressions.dsl.generated.get {
                    literal(
                        RouteLayerConstants.ARROW_BEARING
                    )
                }
            )
            .visibility(Visibility.VISIBLE)
            .iconOpacity(
                Expression.step {
                    zoom()
                    literal(RouteLayerConstants.OPAQUE)
                    stop {
                        literal(RouteLayerConstants.ARROW_HIDDEN_ZOOM_LEVEL)
                        literal(RouteLayerConstants.TRANSPARENT)
                    }
                }
            )

        // arrow shaft
        if (style.styleLayerExists(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID)) {
            style.removeStyleLayer(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID)
        }
        val arrowShaftLayer = LineLayer(
            RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID,
            RouteLayerConstants.ARROW_SHAFT_SOURCE_ID
        )
            .lineColor(
                Expression.color(options.arrowColor)
            )
            .lineWidth(options.arrowShaftScaleExpression)
            .lineCap(LineCap.ROUND)
            .lineJoin(LineJoin.ROUND)
            .visibility(Visibility.VISIBLE)
            .lineOpacity(
                Expression.step {
                    zoom()
                    literal(RouteLayerConstants.OPAQUE)
                    stop {
                        literal(RouteLayerConstants.ARROW_HIDDEN_ZOOM_LEVEL)
                        literal(RouteLayerConstants.TRANSPARENT)
                    }
                }
            )

        // arrow head
        if (style.styleLayerExists(RouteLayerConstants.ARROW_HEAD_LAYER_ID)) {
            style.removeStyleLayer(RouteLayerConstants.ARROW_HEAD_LAYER_ID)
        }
        val arrowHeadLayer = SymbolLayer(
            RouteLayerConstants.ARROW_HEAD_LAYER_ID,
            RouteLayerConstants.ARROW_HEAD_SOURCE_ID
        )
            .iconImage(RouteLayerConstants.ARROW_HEAD_ICON)
            .iconAllowOverlap(true)
            .iconIgnorePlacement(true)
            .iconSize(options.arrowHeadScaleExpression)
            .iconOffset(RouteLayerConstants.ARROW_HEAD_CASING_OFFSET.toList())
            .iconRotationAlignment(IconRotationAlignment.MAP)
            .iconRotate(
                com.mapbox.maps.extension.style.expressions.dsl.generated.get {
                    literal(
                        RouteLayerConstants.ARROW_BEARING
                    )
                }
            )
            .visibility(Visibility.VISIBLE)
            .iconOpacity(
                Expression.step {
                    zoom()
                    literal(RouteLayerConstants.OPAQUE)
                    stop {
                        literal(RouteLayerConstants.ARROW_HIDDEN_ZOOM_LEVEL)
                        literal(RouteLayerConstants.TRANSPARENT)
                    }
                }
            )

        style.addPersistentLayer(
            arrowShaftCasingLayer,
            LayerPosition(aboveLayerIdToUse, null, null)
        )
        style.addPersistentLayer(
            arrowHeadCasingLayer,
            LayerPosition(arrowShaftCasingLayer.layerId, null, null)
        )
        style.addPersistentLayer(
            arrowShaftLayer,
            LayerPosition(arrowHeadCasingLayer.layerId, null, null)
        )
        style.addPersistentLayer(
            arrowHeadLayer,
            LayerPosition(arrowShaftLayer.layerId, null, null)
        )
    }

    internal fun layersAreInitialized(style: Style): Boolean {
        return style.styleSourceExists(RouteLayerConstants.ARROW_SHAFT_SOURCE_ID) &&
            style.styleSourceExists(RouteLayerConstants.ARROW_HEAD_SOURCE_ID) &&
            style.styleLayerExists(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ARROW_HEAD_LAYER_ID)
    }
}
