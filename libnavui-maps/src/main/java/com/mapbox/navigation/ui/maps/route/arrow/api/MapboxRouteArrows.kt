package com.mapbox.navigation.ui.maps.route.arrow.api


import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.math.MathUtils
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconRotationAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.base.internal.route.RouteConstants
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.MAX_DEGREES
import com.mapbox.navigation.ui.base.internal.utils.MapImageUtils
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A more featured implementation for adding multiple route arrows on the map. While this class
 * will add a maneuver arrow based on [RouteProgress] you can also add additional arrows based
 * on a collection of two or more points. It is suggested that you use either this class
 * for managing route arrows or the embedded default implementation that is created and managed by
 * [NavigationMapRoute]. To use this class exclusively you should call
 * [NavigationMapRoute.updateRouteArrowVisibilityTo(false)] in order to disable the default
 * implementation. You should also register your own [RouteProgressObserver] and pass the
 * [RouteProgress] objects emitted to this class if you want the same maneuver arrow behavior as
 * the default implementation. For more fine grained control you can add and remove arrows
 * at any time using the methods in this class.
 */
class MapboxRouteArrows(
    private val options: RouteArrowOptions
) {

    companion object {
        const val ARROW_BEARING_ADVANCED = "mapbox-navigation-arrow-bearing-advanced"
        const val ARROW_SHAFT_SOURCE_ID_ADVANCED = "mapbox-navigation-arrow-shaft-source-advanced"
        const val ARROW_HEAD_SOURCE_ID_ADVANCED = "mapbox-navigation-arrow-head-source-advanced"
        const val ARROW_SHAFT_CASING_LINE_LAYER_ID_ADVANCED =
            "mapbox-navigation-arrow-shaft-casing-layer-advanced"
        const val ARROW_SHAFT_LINE_LAYER_ID_ADVANCED = "mapbox-navigation-arrow-shaft-layer-advanced"
        const val ARROW_HEAD_ICON_ADVANCED = "mapbox-navigation-arrow-head-icon-advanced"
        const val ARROW_HEAD_ICON_CASING_ADVANCED = "mapbox-navigation-arrow-head-icon-casing-advanced"
        const val ARROW_HEAD_CASING_LAYER_ID_ADVANCED = "mapbox-navigation-arrow-head-casing-layer-advanced"
        const val ARROW_HEAD_LAYER_ID_ADVANCED = "mapbox-navigation-arrow-head-layer-advanced"
    }

    private val arrows: CopyOnWriteArrayList<List<Point>> = CopyOnWriteArrayList()
    private var maneuverArrow: List<Point> = listOf()

    /**
     * Returns all of the collections of points making up arrows that have been added. Each
     * collection of points represents a single arrow.
     */
    fun getArrows(): List<List<Point>> {
        return arrows.toList()
    }

    /**
     * Adds an arrow representing the next maneuver. There is at most one maneuver arrow on the
     * map at any given time. For each [RouteProgress] submitted the next maneuver is calculated.
     * If the newly calculated maneuver arrow is different from the current maneuver arrow, the
     * existing maneuver arrow is replaced with the newly calculated arrow. Other arrows added
     * via the [addArrow] call are not affected when calling this method.
     *
     * @param style a valid map style
     * @param routeProgress a route progress object used for the maneuver arrow calculation.
     */
    fun addUpcomingManeuverArrow(style: Style, routeProgress: RouteProgress) {
        val invalidUpcomingStepPoints = (routeProgress.upcomingStepPoints == null
            || routeProgress.upcomingStepPoints!!.size < RouteConstants.TWO_POINTS)
        val invalidCurrentStepPoints = routeProgress.currentLegProgress == null ||
            routeProgress.currentLegProgress!!.currentStepProgress == null ||
            routeProgress.currentLegProgress!!.currentStepProgress!!.stepPoints == null ||
            routeProgress.currentLegProgress!!.currentStepProgress!!.stepPoints!!.size <
            RouteConstants.TWO_POINTS

        if (invalidUpcomingStepPoints || invalidCurrentStepPoints) {
            return
        }

        removeArrow(style, maneuverArrow)
        maneuverArrow = obtainArrowPointsFrom(routeProgress)
        addArrow(style, maneuverArrow)
    }

    /**
     * Adds an arrow to the map made up of the points submitted. An arrow is made up of at least
     * two points. The direction of the arrow head is determined by calculating the bearing
     * between the last two points submitted. Each call will add a new arrow.
     *
     * @param style a valid map style
     * @param points the points that make up the arrow to be drawn
     */
    fun addArrow(style: Style, points: List<Point>) {
        if (points.size < RouteConstants.TWO_POINTS || !style.fullyLoaded) {
            return
        }

        initializeLayers(style)

        if (arrows.flatten().intersect(points).isEmpty()) {
            arrows.add(points)
            redrawArrows(style)
        }
    }

    /**
     * Will remove any arrow having one or more points contained in the points submitted.
     * To remove a previously added arrow it isn't necessary to submit all of the points of the
     * previously submitted arrow(s). Instead it is necessary only to submit at least one point
     * for each previously added arrow that should be removed.
     *
     * @param style a valid map style
     * @param points one or more points used as criteria for removing arrows from the map
     */
    fun removeArrow(style: Style, points: List<Point>) {
        initializeLayers(style)

        val arrowsToRemove = arrows.filter { it.intersect(points).isNotEmpty() }
        if (maneuverArrow.intersect(points).isNotEmpty()) {
            maneuverArrow = listOf()
        }
        arrows.removeAll(arrowsToRemove)
        redrawArrows(style)
    }

    /**
     * Clears all arrows from the map.
     *
     * @param style a valid map style
     */
    fun clearArrows(style: Style) {
        initializeLayers(style)
        arrows.clear()
        maneuverArrow = listOf()
        redrawArrows(style)
    }

    /**
     * Returns a value indicating whether or not the map layers that host the arrows are visible.
     *
     * @param style a valid map style
     */
    fun arrowsAreVisible(style: Style): Boolean {
        return style.getLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID_ADVANCED)?.visibility  ==
            Visibility.VISIBLE &&
            style.getLayer(ARROW_SHAFT_LINE_LAYER_ID_ADVANCED)?.visibility == Visibility.VISIBLE &&
            style.getLayer(ARROW_HEAD_CASING_LAYER_ID_ADVANCED)?.visibility == Visibility.VISIBLE &&
            style.getLayer(ARROW_HEAD_LAYER_ID_ADVANCED)?.visibility == Visibility.VISIBLE
    }

    /**
     * Sets the layers hosting the arrows to visible.
     *
     * @param style a valid map style
     */
    fun show(style: Style) {
        setVisibility(style, Visibility.VISIBLE)
    }

    /**
     * Hides the layers hosting the arrows.
     *
     * @param style a valid map style
     */
    fun hide(style: Style) {
        setVisibility(style, Visibility.NONE)
    }

    // This came from MathUtils in the Maps SDK which may have been removed.
    private fun wrap(value: Double, min: Double, max: Double): Double {
        val delta = max - min
        val firstMod = (value - min) % delta
        val secondMod = (firstMod + delta) % delta
        return secondMod + min
    }

    private fun redrawArrows(style: Style) {
        val shaftFeatures = arrows.map {
            LineString.fromLngLats(it)
        }.map {
            Feature.fromGeometry(it)
        }
        val shaftFeatureCollection = FeatureCollection.fromFeatures(shaftFeatures)

        val arrowHeadFeatures = arrows.map {
            val azimuth = TurfMeasurement.bearing(it[it.size - 2], it[it.size - 1])
            Feature.fromGeometry(it[it.size - 1]).also {  feature ->
                feature.addNumberProperty(
                    ARROW_BEARING_ADVANCED,
                    wrap(azimuth, 0.0, MAX_DEGREES.toDouble())
                )
            }
        }
        val arrowHeadFeatureCollection = FeatureCollection.fromFeatures(arrowHeadFeatures)

        style.getSourceAs<GeoJsonSource>(ARROW_SHAFT_SOURCE_ID_ADVANCED).featureCollection(shaftFeatureCollection)
        style.getSourceAs<GeoJsonSource>(ARROW_HEAD_SOURCE_ID_ADVANCED).featureCollection(arrowHeadFeatureCollection)
    }

    private fun initializeLayers(style: Style) {
        if (!style.isFullyLoaded() || layersAreInitialized(style)) {
            return
        }

        initializeArrowShaft(style, 16, options.tolerance.toFloat())
        initializeArrowHead(style, 16, options.tolerance.toFloat())
        addArrowHeadIcon(style, options.arrowHeadIcon, options.arrowColor)
        addArrowHeadIconCasing(style, options.arrowHeadIconBorder, options.arrowBorderColor)

        val shaftLayer = createArrowShaftLayer(style, options.arrowColor)
        val shaftCasingLayer = createArrowShaftCasingLayer(style, options.arrowBorderColor)
        val headLayer = createArrowHeadLayer(style)
        val headCasingLayer = createArrowHeadCasingLayer(style)

        style.addLayerAbove(shaftCasingLayer, options.aboveLayerId)
        style.addLayerAbove(headCasingLayer, shaftCasingLayer.layerId)
        style.addLayerAbove(shaftLayer, headCasingLayer.layerId)
        style.addLayerAbove(headLayer, shaftLayer.layerId)


    }

    private fun layersAreInitialized(style: Style): Boolean {
        return style.fullyLoaded &&
            style.styleSourceExists(ARROW_SHAFT_SOURCE_ID_ADVANCED) &&
            style.styleSourceExists(ARROW_HEAD_SOURCE_ID_ADVANCED) &&
            style.styleLayerExists(ARROW_SHAFT_LINE_LAYER_ID_ADVANCED) &&
            style.styleLayerExists(ARROW_SHAFT_CASING_LINE_LAYER_ID_ADVANCED) &&
            style.styleLayerExists(ARROW_HEAD_LAYER_ID_ADVANCED) &&
            style.styleLayerExists(ARROW_HEAD_CASING_LAYER_ID_ADVANCED)
    }

    private fun initializeArrowShaft(style: Style, sourceMaxZoom: Int, sourceTolerance: Float) {
        if (style.styleSourceExists(ARROW_SHAFT_SOURCE_ID_ADVANCED)) {
            return
        }

        geoJsonSource(ARROW_SHAFT_SOURCE_ID_ADVANCED) {
            //maxzoom(16)
            featureCollection(FeatureCollection.fromFeatures(listOf()))
            tolerance(options.tolerance)
        }.bindTo(style)
    }

    private fun initializeArrowHead(style: Style, sourceMaxZoom: Int, sourceTolerance: Float) {
        if (style.styleSourceExists(ARROW_HEAD_SOURCE_ID_ADVANCED)) {
            return
        }

        geoJsonSource(ARROW_HEAD_SOURCE_ID_ADVANCED) {
            //maxzoom(16)
            featureCollection(FeatureCollection.fromFeatures(listOf()))
            tolerance(options.tolerance)
        }.bindTo(style)

    }

    private fun addArrowHeadIcon(
        style: Style,
        arrowHeadIcon: Drawable,
        arrowColor: Int
    ) {
        if (style.getStyleImage(ARROW_HEAD_ICON_ADVANCED) != null) {
            return
        }

        val head = DrawableCompat.wrap(arrowHeadIcon)
        DrawableCompat.setTint(head.mutate(), arrowColor)
        val icon = MapImageUtils.getBitmapFromDrawable(head)
        style.addImage(ARROW_HEAD_ICON_ADVANCED, icon)
    }

    private fun addArrowHeadIconCasing(
        style: Style,
        arrowHeadCasing: Drawable,
        arrowBorderColor: Int
    ) {
        if (style.getStyleImage(ARROW_HEAD_ICON_CASING_ADVANCED) != null) {
            return
        }

        val headCasing = DrawableCompat.wrap(arrowHeadCasing)
        DrawableCompat.setTint(headCasing.mutate(), arrowBorderColor)
        val icon = MapImageUtils.getBitmapFromDrawable(headCasing)
        style.addImage(ARROW_HEAD_ICON_CASING_ADVANCED, icon)
    }

    private fun createArrowShaftLayer(style: Style, arrowColor: Int): LineLayer {
        val shaftLayer = style.getLayer(ARROW_SHAFT_LINE_LAYER_ID_ADVANCED)?.run {
            this as LineLayer
        }
        if (shaftLayer != null) {
            style.removeStyleLayer(ARROW_SHAFT_LINE_LAYER_ID_ADVANCED)
        }

        return LineLayer(
            ARROW_SHAFT_LINE_LAYER_ID_ADVANCED,
            ARROW_SHAFT_SOURCE_ID_ADVANCED
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
            .visibility(Visibility.VISIBLE)
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
    }

    private fun createArrowShaftCasingLayer(
        style: Style,
        arrowBorderColor: Int
    ): LineLayer {
        val shaftCasingLayer = style.getLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID_ADVANCED)?.run {
            this as LineLayer
        }
        if (shaftCasingLayer != null) {
            style.removeStyleLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID_ADVANCED)
        }

        return LineLayer(
            ARROW_SHAFT_CASING_LINE_LAYER_ID_ADVANCED,
            ARROW_SHAFT_SOURCE_ID_ADVANCED
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
            .visibility(Visibility.VISIBLE)
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
    }

    private fun createArrowHeadLayer(style: Style): SymbolLayer {
        val headLayer = style.getLayer(ARROW_HEAD_LAYER_ID_ADVANCED)?.run {
            this as SymbolLayer
        }
        if (headLayer != null) {
            style.removeStyleLayer(ARROW_HEAD_LAYER_ID_ADVANCED)
        }

        return SymbolLayer(
            ARROW_HEAD_LAYER_ID_ADVANCED,
            ARROW_HEAD_SOURCE_ID_ADVANCED
        )
            .iconImage(ARROW_HEAD_ICON_ADVANCED)
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
                        ARROW_BEARING_ADVANCED
                    )
                }
            )
            .visibility(Visibility.VISIBLE)
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
    }

    private fun createArrowHeadCasingLayer(style: Style): SymbolLayer {
        val headCasingLayer = style.getLayer(ARROW_HEAD_CASING_LAYER_ID_ADVANCED)?.run {
            this as SymbolLayer
        }
        if (headCasingLayer != null) {
            style.removeStyleLayer(ARROW_HEAD_CASING_LAYER_ID_ADVANCED)
        }

        return SymbolLayer(
            ARROW_HEAD_CASING_LAYER_ID_ADVANCED,
            ARROW_HEAD_SOURCE_ID_ADVANCED
        )
            .iconImage(ARROW_HEAD_ICON_CASING_ADVANCED)
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
                        ARROW_BEARING_ADVANCED
                    )
                }
            )
            .visibility(Visibility.VISIBLE)
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
    }

    private fun setVisibility(style: Style, visibilityValue: Visibility) {
        style.getLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID_ADVANCED)?.visibility(visibilityValue)

        style.getLayer(ARROW_SHAFT_LINE_LAYER_ID_ADVANCED)?.visibility(visibilityValue)

        style.getLayer(ARROW_HEAD_CASING_LAYER_ID_ADVANCED)?.visibility(visibilityValue)

        style.getLayer(ARROW_HEAD_LAYER_ID_ADVANCED)?.visibility(visibilityValue)
    }

    internal fun obtainArrowPointsFrom(routeProgress: RouteProgress): List<Point> {
        val reversedCurrent = routeProgress.currentLegProgress
            ?.currentStepProgress
            ?.stepPoints
            ?.reversed() ?: listOf()

        val arrowLineCurrent = LineString.fromLngLats(reversedCurrent)
        val arrowLineUpcoming = LineString.fromLngLats(routeProgress.upcomingStepPoints!!)
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
}

