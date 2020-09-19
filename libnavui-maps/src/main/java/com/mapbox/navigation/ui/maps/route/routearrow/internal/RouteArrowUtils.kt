package com.mapbox.navigation.ui.maps.route.routearrow.internal

import androidx.core.graphics.drawable.DrawableCompat
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
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
import com.mapbox.navigation.ui.internal.route.RouteConstants
import com.mapbox.navigation.ui.internal.route.RouteConstants.*
import com.mapbox.navigation.ui.internal.utils.MapImageUtils
import com.mapbox.navigation.ui.maps.route.routearrow.api.RouteArrowResourceProvider
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMisc

object RouteArrowUtils {
    fun obtainArrowPointsFrom(routeProgress: RouteProgress): List<Point> {
        val reversedCurrent: List<Point> = routeProgress.currentLegProgress?.currentStepProgress?.stepPoints?.reversed() ?: listOf()

        val arrowLineCurrent = LineString.fromLngLats(reversedCurrent)
        val arrowLineUpcoming = LineString.fromLngLats(routeProgress.upcomingStepPoints!!)

        val arrowCurrentSliced = TurfMisc.lineSliceAlong(arrowLineCurrent, 0.0, RouteConstants.THIRTY.toDouble(), TurfConstants.UNIT_METERS)
        val arrowUpcomingSliced = TurfMisc.lineSliceAlong(arrowLineUpcoming, 0.0, RouteConstants.THIRTY.toDouble(), TurfConstants.UNIT_METERS)

        return arrowCurrentSliced.coordinates().reversed().plus(arrowUpcomingSliced.coordinates())
    }

    @JvmStatic
    fun initRouteArrowLayers(
        style: Style,
        routeArrowDrawableProvider: RouteArrowDrawableProvider,
        routeArrowResourceProvider: RouteArrowResourceProvider,
        aboveLayerId: String) {

        if (!style.isFullyLoaded()) {
            return
        }

        if (!style.sourceExists(ARROW_SHAFT_SOURCE_ID)) {
            geoJsonSource(ARROW_SHAFT_SOURCE_ID) {
                maxzoom(16)
                featureCollection(FeatureCollection.fromFeatures(listOf()))
            }.bindTo(style)
        }

        if (!style.sourceExists(ARROW_HEAD_SOURCE_ID)) {
            geoJsonSource(ARROW_HEAD_SOURCE_ID) {
                maxzoom(16)
                featureCollection(FeatureCollection.fromFeatures(listOf()))
            }.bindTo(style)
        }

        if (style.getImage(ARROW_HEAD_ICON_CASING) != null) {
            style.removeImage(ARROW_HEAD_ICON_CASING)
        }
        val arrowHeadCasingDrawable = DrawableCompat.wrap(routeArrowDrawableProvider.getArrowHeadIconCasingDrawable())
        DrawableCompat.setTint(arrowHeadCasingDrawable.mutate(), routeArrowResourceProvider.getArrowBorderColor())
        val arrowHeadCasingBitmap =  MapImageUtils.getBitmapFromDrawable(arrowHeadCasingDrawable)
        style.addImage(ARROW_HEAD_ICON_CASING, arrowHeadCasingBitmap)

        if (style.getImage(ARROW_HEAD_ICON) != null) {
            style.removeImage(ARROW_HEAD_ICON)
        }
        val arrowHeadDrawable = DrawableCompat.wrap(routeArrowDrawableProvider.getArrowHeadIconDrawable())
        DrawableCompat.setTint(arrowHeadDrawable.mutate(), routeArrowResourceProvider.getArrowColor())
        val arrowHeadBitmap = MapImageUtils.getBitmapFromDrawable(arrowHeadDrawable)
        style.addImage(ARROW_HEAD_ICON, arrowHeadBitmap)

        // arrow shaft casing
        if (style.layerExists(ARROW_SHAFT_CASING_LINE_LAYER_ID)) {
            style.removeLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID)
        }
        val arrowShaftCasingLayer = LineLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID, ARROW_SHAFT_SOURCE_ID)
            .lineColor(Expression.color(routeArrowResourceProvider.getArrowBorderColor()))
            .lineWidth(Expression.interpolate {
                linear()
                zoom()
                stop {
                    literal(MIN_ARROW_ZOOM)
                    literal(MIN_ZOOM_ARROW_SHAFT_CASING_SCALE)
                }
                stop {
                    literal(MAX_ARROW_ZOOM)
                    literal(MAX_ZOOM_ARROW_SHAFT_CASING_SCALE)
                }
            })
            .lineCap(LineCap.ROUND)
            .lineJoin(LineJoin.ROUND)
            .visibility(Visibility.NONE)
            .lineOpacity(Expression.step {
                zoom()
                literal(OPAQUE)
                stop {
                    literal(ARROW_HIDDEN_ZOOM_LEVEL)
                    literal(TRANSPARENT)
                }
            })

        // arrow head casing
        if (style.layerExists(ARROW_HEAD_CASING_LAYER_ID)) {
            style.removeLayer(ARROW_HEAD_CASING_LAYER_ID)
        }
        val arrowHeadCasingLayer = SymbolLayer(ARROW_HEAD_CASING_LAYER_ID, ARROW_HEAD_SOURCE_ID)
            .iconImage(ARROW_HEAD_ICON_CASING)
            .iconAllowOverlap(true)
            .iconIgnorePlacement(true)
            .iconSize(Expression.interpolate {
                linear()
                zoom()
                stop {
                    literal(MIN_ARROW_ZOOM)
                    literal(MIN_ZOOM_ARROW_HEAD_CASING_SCALE)
                }
                stop {
                    literal(MAX_ARROW_ZOOM)
                    literal(MAX_ZOOM_ARROW_HEAD_CASING_SCALE)
                }
            })
            .iconOffset(ARROW_HEAD_OFFSET.toList())
            .iconRotationAlignment(IconRotationAlignment.MAP)
            .iconRotate(get { literal(ARROW_BEARING) })
            .visibility(Visibility.NONE)
            .iconOpacity(Expression.step {
                zoom()
                literal(OPAQUE)
                stop {
                    literal(ARROW_HIDDEN_ZOOM_LEVEL)
                    literal(TRANSPARENT)
                }
            }
            )

        // arrow shaft
        if (style.layerExists(ARROW_SHAFT_LINE_LAYER_ID)) {
            style.removeLayer(ARROW_SHAFT_LINE_LAYER_ID)
        }
        val arrowShaftLayer = LineLayer(ARROW_SHAFT_LINE_LAYER_ID, ARROW_SHAFT_SOURCE_ID)
            .lineColor(Expression.color(routeArrowResourceProvider.getArrowColor()))
            .lineWidth(Expression.interpolate {
                linear()
                zoom()
                stop {
                    literal(MIN_ARROW_ZOOM)
                    literal(MIN_ZOOM_ARROW_SHAFT_SCALE)
                }
                stop {
                    literal(MAX_ARROW_ZOOM)
                    literal(MAX_ZOOM_ARROW_SHAFT_SCALE)
                }
            })
            .lineCap(LineCap.ROUND)
            .lineJoin(LineJoin.ROUND)
            .visibility(Visibility.NONE)
            .lineOpacity(Expression.step {
                zoom()
                literal(OPAQUE)
                stop {
                    literal(ARROW_HIDDEN_ZOOM_LEVEL)
                    literal(TRANSPARENT)
                }
            })

        // arrow head
        if (style.layerExists(ARROW_HEAD_LAYER_ID)) {
            style.removeLayer(ARROW_HEAD_LAYER_ID)
        }
        val arrowHeadLayer = SymbolLayer(ARROW_HEAD_LAYER_ID, ARROW_HEAD_SOURCE_ID)
            .iconImage(ARROW_HEAD_ICON)
            .iconAllowOverlap(true)
            .iconIgnorePlacement(true)
            .iconSize(Expression.interpolate {
                linear()
                zoom()
                stop {
                    literal(MIN_ARROW_ZOOM)
                    literal(MIN_ZOOM_ARROW_HEAD_SCALE)
                }
                stop {
                    literal(MAX_ARROW_ZOOM)
                    literal(MAX_ZOOM_ARROW_HEAD_SCALE)
                }
            })
            .iconOffset(ARROW_HEAD_CASING_OFFSET.toList())
            .iconRotationAlignment(IconRotationAlignment.MAP)
            .iconRotate(get { literal(ARROW_BEARING) })
            .visibility(Visibility.NONE)
            .iconOpacity(Expression.step {
                zoom()
                literal(OPAQUE)
                stop {
                    literal(ARROW_HIDDEN_ZOOM_LEVEL)
                    literal(TRANSPARENT)
                }
            })

        style.addLayerAbove(arrowShaftCasingLayer, aboveLayerId)
        style.addLayerAbove(arrowHeadCasingLayer, arrowShaftCasingLayer.layerId)
        style.addLayerAbove(arrowShaftLayer, arrowHeadCasingLayer.layerId)
        style.addLayerAbove(arrowHeadLayer, arrowShaftLayer.layerId)
    }
}
