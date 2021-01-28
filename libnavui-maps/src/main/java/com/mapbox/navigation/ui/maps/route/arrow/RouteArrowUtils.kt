package com.mapbox.navigation.ui.maps.route.arrow

import androidx.core.graphics.drawable.DrawableCompat
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconRotationAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.internal.route.RouteConstants
import com.mapbox.navigation.ui.base.internal.utils.MapImageUtils
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMisc

internal object RouteArrowUtils {
    fun obtainArrowPointsFrom(routeProgress: RouteProgress): List<Point> {
        val reversedCurrent: List<Point> =
            routeProgress.currentLegProgress?.currentStepProgress?.stepPoints?.reversed()
                ?: listOf()

        val arrowLineCurrent = LineString.fromLngLats(reversedCurrent)
        val arrowLineUpcoming = LineString.fromLngLats(routeProgress.upcomingStepPoints!!)

        if (arrowLineCurrent.coordinates().size < 2) {
            return listOf()
        }

        if (arrowLineUpcoming.coordinates().size < 2) {
            return listOf()
        }

        val arrowCurrentSliced = TurfMisc.lineSliceAlong(
            arrowLineCurrent,
            0.0,
            RouteConstants.THIRTY.toDouble(),
            TurfConstants.UNIT_METERS
        )
        val arrowUpcomingSliced = TurfMisc.lineSliceAlong(
            arrowLineUpcoming,
            0.0,
            RouteConstants.THIRTY.toDouble(),
            TurfConstants.UNIT_METERS
        )

        return arrowCurrentSliced.coordinates().reversed().plus(arrowUpcomingSliced.coordinates())
    }

    fun initializeLayers(style: Style, options: RouteArrowOptions) {
        if (
            !style.fullyLoaded ||
            layersAreInitialized(style) ||
            !style.styleLayerExists(options.aboveLayerId)
        ) {
            return
        }

        if (!style.styleSourceExists(RouteConstants.ARROW_SHAFT_SOURCE_ID)) {
            geoJsonSource(RouteConstants.ARROW_SHAFT_SOURCE_ID) {
                maxzoom(16)
                featureCollection(FeatureCollection.fromFeatures(listOf()))
                tolerance(options.tolerance)
            }.bindTo(style)
        }

        if (!style.styleSourceExists(RouteConstants.ARROW_HEAD_SOURCE_ID)) {
            geoJsonSource(RouteConstants.ARROW_HEAD_SOURCE_ID) {
                maxzoom(16)
                featureCollection(FeatureCollection.fromFeatures(listOf()))
                tolerance(options.tolerance)
            }.bindTo(style)
        }

        if (style.getStyleImage(RouteConstants.ARROW_HEAD_ICON_CASING) != null) {
            style.removeStyleImage(RouteConstants.ARROW_HEAD_ICON_CASING)
        }
        val arrowHeadCasingDrawable = DrawableCompat.wrap(
            options.arrowHeadIconBorder
        )
        DrawableCompat.setTint(
            arrowHeadCasingDrawable.mutate(),
            options.arrowBorderColor
        )
        val arrowHeadCasingBitmap = MapImageUtils.getBitmapFromDrawable(arrowHeadCasingDrawable)
        style.addImage(RouteConstants.ARROW_HEAD_ICON_CASING, arrowHeadCasingBitmap)

        if (style.getStyleImage(RouteConstants.ARROW_HEAD_ICON) != null) {
            style.removeStyleImage(RouteConstants.ARROW_HEAD_ICON)
        }
        val arrowHeadDrawable = DrawableCompat.wrap(
            options.arrowHeadIcon
        )
        DrawableCompat.setTint(
            arrowHeadDrawable.mutate(),
            options.arrowColor
        )
        val arrowHeadBitmap = MapImageUtils.getBitmapFromDrawable(arrowHeadDrawable)
        style.addImage(RouteConstants.ARROW_HEAD_ICON, arrowHeadBitmap)

        // arrow shaft casing
        if (style.styleLayerExists(RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)) {
            style.removeStyleLayer(RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)
        }
        val arrowShaftCasingLayer = LineLayer(
            RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID,
            RouteConstants.ARROW_SHAFT_SOURCE_ID
        )
            .lineColor(
                Expression.color(
                    options.arrowBorderColor
                )
            )
            .lineWidth(
                Expression.interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(RouteConstants.MIN_ARROW_ZOOM)
                        literal(RouteConstants.MIN_ZOOM_ARROW_SHAFT_CASING_SCALE)
                    }
                    stop {
                        literal(RouteConstants.MAX_ARROW_ZOOM)
                        literal(RouteConstants.MAX_ZOOM_ARROW_SHAFT_CASING_SCALE)
                    }
                }
            )
            .lineCap(LineCap.ROUND)
            .lineJoin(LineJoin.ROUND)
            .visibility(Visibility.NONE)
            .lineOpacity(
                Expression.step {
                    zoom()
                    literal(RouteConstants.OPAQUE)
                    stop {
                        literal(RouteConstants.ARROW_HIDDEN_ZOOM_LEVEL)
                        literal(RouteConstants.TRANSPARENT)
                    }
                }
            )

        // arrow head casing
        if (style.styleLayerExists(RouteConstants.ARROW_HEAD_CASING_LAYER_ID)) {
            style.removeStyleLayer(RouteConstants.ARROW_HEAD_CASING_LAYER_ID)
        }
        val arrowHeadCasingLayer = SymbolLayer(
            RouteConstants.ARROW_HEAD_CASING_LAYER_ID,
            RouteConstants.ARROW_HEAD_SOURCE_ID
        )
            .iconImage(RouteConstants.ARROW_HEAD_ICON_CASING)
            .iconAllowOverlap(true)
            .iconIgnorePlacement(true)
            .iconSize(
                Expression.interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(RouteConstants.MIN_ARROW_ZOOM)
                        literal(RouteConstants.MIN_ZOOM_ARROW_HEAD_CASING_SCALE)
                    }
                    stop {
                        literal(RouteConstants.MAX_ARROW_ZOOM)
                        literal(RouteConstants.MAX_ZOOM_ARROW_HEAD_CASING_SCALE)
                    }
                }
            )
            .iconOffset(RouteConstants.ARROW_HEAD_OFFSET.toList())
            .iconRotationAlignment(IconRotationAlignment.MAP)
            .iconRotate(
                com.mapbox.maps.extension.style.expressions.dsl.generated.get {
                    literal(
                        RouteConstants.ARROW_BEARING
                    )
                }
            )
            .visibility(Visibility.NONE)
            .iconOpacity(
                Expression.step {
                    zoom()
                    literal(RouteConstants.OPAQUE)
                    stop {
                        literal(RouteConstants.ARROW_HIDDEN_ZOOM_LEVEL)
                        literal(RouteConstants.TRANSPARENT)
                    }
                }
            )

        // arrow shaft
        if (style.styleLayerExists(RouteConstants.ARROW_SHAFT_LINE_LAYER_ID)) {
            style.removeStyleLayer(RouteConstants.ARROW_SHAFT_LINE_LAYER_ID)
        }
        val arrowShaftLayer = LineLayer(
            RouteConstants.ARROW_SHAFT_LINE_LAYER_ID,
            RouteConstants.ARROW_SHAFT_SOURCE_ID
        )
            .lineColor(
                Expression.color(options.arrowColor)
            )
            .lineWidth(
                Expression.interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(RouteConstants.MIN_ARROW_ZOOM)
                        literal(RouteConstants.MIN_ZOOM_ARROW_SHAFT_SCALE)
                    }
                    stop {
                        literal(RouteConstants.MAX_ARROW_ZOOM)
                        literal(RouteConstants.MAX_ZOOM_ARROW_SHAFT_SCALE)
                    }
                }
            )
            .lineCap(LineCap.ROUND)
            .lineJoin(LineJoin.ROUND)
            .visibility(Visibility.NONE)
            .lineOpacity(
                Expression.step {
                    zoom()
                    literal(RouteConstants.OPAQUE)
                    stop {
                        literal(RouteConstants.ARROW_HIDDEN_ZOOM_LEVEL)
                        literal(RouteConstants.TRANSPARENT)
                    }
                }
            )

        // arrow head
        if (style.styleLayerExists(RouteConstants.ARROW_HEAD_LAYER_ID)) {
            style.removeStyleLayer(RouteConstants.ARROW_HEAD_LAYER_ID)
        }
        val arrowHeadLayer = SymbolLayer(
            RouteConstants.ARROW_HEAD_LAYER_ID,
            RouteConstants.ARROW_HEAD_SOURCE_ID
        )
            .iconImage(RouteConstants.ARROW_HEAD_ICON)
            .iconAllowOverlap(true)
            .iconIgnorePlacement(true)
            .iconSize(
                Expression.interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(RouteConstants.MIN_ARROW_ZOOM)
                        literal(RouteConstants.MIN_ZOOM_ARROW_HEAD_SCALE)
                    }
                    stop {
                        literal(RouteConstants.MAX_ARROW_ZOOM)
                        literal(RouteConstants.MAX_ZOOM_ARROW_HEAD_SCALE)
                    }
                }
            )
            .iconOffset(RouteConstants.ARROW_HEAD_CASING_OFFSET.toList())
            .iconRotationAlignment(IconRotationAlignment.MAP)
            .iconRotate(
                com.mapbox.maps.extension.style.expressions.dsl.generated.get {
                    literal(
                        RouteConstants.ARROW_BEARING
                    )
                }
            )
            .visibility(Visibility.NONE)
            .iconOpacity(
                Expression.step {
                    zoom()
                    literal(RouteConstants.OPAQUE)
                    stop {
                        literal(RouteConstants.ARROW_HIDDEN_ZOOM_LEVEL)
                        literal(RouteConstants.TRANSPARENT)
                    }
                }
            )

        style.addLayerAbove(arrowShaftCasingLayer, options.aboveLayerId)
        style.addLayerAbove(arrowHeadCasingLayer, arrowShaftCasingLayer.layerId)
        style.addLayerAbove(arrowShaftLayer, arrowHeadCasingLayer.layerId)
        style.addLayerAbove(arrowHeadLayer, arrowShaftLayer.layerId)
    }

    internal fun layersAreInitialized(style: Style): Boolean {
        return style.fullyLoaded &&
            style.styleSourceExists(RouteConstants.ARROW_SHAFT_SOURCE_ID) &&
            style.styleSourceExists(RouteConstants.ARROW_HEAD_SOURCE_ID) &&
            style.styleLayerExists(RouteConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID) &&
            style.styleLayerExists(RouteConstants.ARROW_HEAD_CASING_LAYER_ID) &&
            style.styleLayerExists(RouteConstants.ARROW_SHAFT_LINE_LAYER_ID) &&
            style.styleLayerExists(RouteConstants.ARROW_HEAD_LAYER_ID)
    }
}
