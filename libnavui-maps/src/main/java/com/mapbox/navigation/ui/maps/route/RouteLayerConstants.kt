package com.mapbox.navigation.ui.maps.route

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.mapbox.navigation.ui.base.R

/**
 * Constants related to route and arrow layer APIs
 */
object RouteLayerConstants {

    /**
     * Layer ID for the top most route line related layer. Use this to position any other layer on top of the route line.
     */
    const val TOP_LEVEL_ROUTE_LINE_LAYER_ID = "mapbox-top-level-route-layer"

    /**
     * Layer ID for the bottom most route line related layer. Use this to position any other layer below the route line.
     */
    const val BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID = "mapbox-bottom-level-route-layer"

    /**
     * Layer ID for the primary route line
     */
    internal const val PRIMARY_ROUTE_LAYER_ID = "mapbox-navigation-route-layer"

    /**
     * Layer ID for the traffic line
     */
    internal const val PRIMARY_ROUTE_TRAFFIC_LAYER_ID = "mapbox-navigation-route-traffic-layer"

    /**
     * Layer ID for the primary route casing line
     */
    internal const val PRIMARY_ROUTE_CASING_LAYER_ID = "mapbox-navigation-route-casing-layer"

    /**
     * Layer ID for the first alternative route line
     */
    internal const val ALTERNATIVE_ROUTE1_LAYER_ID = "mapbox-navigation-alt-route1-layer"

    /**
     * Layer ID for the first alternative route casing line
     */
    internal const val ALTERNATIVE_ROUTE1_CASING_LAYER_ID =
        "mapbox-navigation-alt-route1-casing-layer"

    /**
     * Layer ID for the second alternative route line
     */
    internal const val ALTERNATIVE_ROUTE2_LAYER_ID = "mapbox-navigation-alt-route2-layer"

    /**
     * Layer ID for the second alternative route casing line
     */
    internal const val ALTERNATIVE_ROUTE2_CASING_LAYER_ID =
        "mapbox-navigation-alt-route2-casing-layer"

    /**
     * Layer ID for the first alternative route traffic line
     */
    internal const val ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID =
        "mapbox-navigation-alt-route1-traffic-layer"

    /**
     * Layer ID for the second alternative route traffic line
     */
    internal const val ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID =
        "mapbox-navigation-alt-route2-traffic-layer"

    /**
     * Layer ID for the maneuver arrow shaft casing
     */
    internal const val ARROW_SHAFT_CASING_LINE_LAYER_ID =
        "mapbox-navigation-arrow-shaft-casing-layer"

    /**
     * Layer ID for the maneuver arrow shaft
     */
    internal const val ARROW_SHAFT_LINE_LAYER_ID = "mapbox-navigation-arrow-shaft-layer"

    /**
     * Layer ID for the maneuver arrow head casing
     */
    internal const val ARROW_HEAD_CASING_LAYER_ID = "mapbox-navigation-arrow-head-casing-layer"

    /**
     * Layer ID for the maneuver arrow head
     */
    internal const val ARROW_HEAD_LAYER_ID = "mapbox-navigation-arrow-head-layer"

    /**
     * Layer ID for the waypoint icons layer
     */
    internal const val WAYPOINT_LAYER_ID = "mapbox-navigation-waypoint-layer"

    /**
     * Layer ID for the restricted area layer
     */
    internal const val RESTRICTED_ROAD_LAYER_ID = "mapbox-restricted-road-layer"

    internal const val PRIMARY_ROUTE_SOURCE_ID = "mapbox-navigation-route-source"
    internal const val ALTERNATIVE_ROUTE1_SOURCE_ID = "mapbox-navigation-alt-route1-source"
    internal const val ALTERNATIVE_ROUTE2_SOURCE_ID = "mapbox-navigation-alt-route2-source"
    internal const val WAYPOINT_SOURCE_ID = "mapbox-navigation-waypoint-source"
    internal const val TWO_POINTS = 2
    internal const val THIRTY = 30
    internal const val ARROW_BEARING = "mapbox-navigation-arrow-bearing"
    internal const val ARROW_SHAFT_SOURCE_ID = "mapbox-navigation-arrow-shaft-source"
    internal const val ARROW_HEAD_SOURCE_ID = "mapbox-navigation-arrow-head-source"
    internal const val ARROW_HEAD_ICON = "mapbox-navigation-arrow-head-icon"
    internal const val ARROW_HEAD_ICON_CASING = "mapbox-navigation-arrow-head-icon-casing"
    internal const val MAX_DEGREES = 360.0
    internal val ARROW_HEAD_CASING_OFFSET = arrayOf(0.0, -7.0)
    internal val ARROW_HEAD_OFFSET = arrayOf(0.0, -7.0)
    internal const val MIN_ARROW_ZOOM = 10.0
    internal const val MAX_ARROW_ZOOM = 22.0
    internal const val MIN_ZOOM_ARROW_SHAFT_SCALE = 2.6
    internal const val MAX_ZOOM_ARROW_SHAFT_SCALE = 13.0
    internal const val MIN_ZOOM_ARROW_SHAFT_CASING_SCALE = 3.4
    internal const val MAX_ZOOM_ARROW_SHAFT_CASING_SCALE = 17.0
    internal const val MIN_ZOOM_ARROW_HEAD_SCALE = 0.2
    internal const val MAX_ZOOM_ARROW_HEAD_SCALE = 0.8
    internal const val MIN_ZOOM_ARROW_HEAD_CASING_SCALE = 0.2
    internal const val MAX_ZOOM_ARROW_HEAD_CASING_SCALE = 0.8
    internal const val OPAQUE = 0.0
    internal const val ARROW_HIDDEN_ZOOM_LEVEL = 14.0
    internal const val TRANSPARENT = 1.0
    internal const val WAYPOINT_PROPERTY_KEY = "wayPoint"
    internal const val WAYPOINT_ORIGIN_VALUE = "origin"
    internal const val WAYPOINT_DESTINATION_VALUE = "destination"
    internal const val LOW_CONGESTION_VALUE = "low"
    internal const val MODERATE_CONGESTION_VALUE = "moderate"
    internal const val HEAVY_CONGESTION_VALUE = "heavy"
    internal const val SEVERE_CONGESTION_VALUE = "severe"
    internal const val UNKNOWN_CONGESTION_VALUE = "unknown"
    internal const val CLOSURE_CONGESTION_VALUE = "closed"
    internal const val RESTRICTED_CONGESTION_VALUE = "restricted"
    internal const val ORIGIN_MARKER_NAME = "originMarker"
    internal const val DESTINATION_MARKER_NAME = "destinationMarker"
    internal const val ROUTE_LINE_UPDATE_MAX_DISTANCE_THRESHOLD_IN_METERS = 3.0
    internal const val DEFAULT_ROUTE_DESCRIPTOR_PLACEHOLDER = "mapboxDescriptorPlaceHolderUnused"
    internal const val MAX_ELAPSED_SINCE_INDEX_UPDATE_NANO = 1500000000.0 // 1.5s
    internal const val DEFAULT_ROUTE_SOURCES_TOLERANCE = 0.375
    internal const val ROUNDED_LINE_CAP = true
    internal const val RESTRICTED_ROAD_SECTION_SCALE = 10.0
    internal const val SOFT_GRADIENT_STOP_GAP_METERS: Double = 30.0
    internal val TRAFFIC_BACKFILL_ROAD_CLASSES = emptyList<String>()
    internal const val RESTRICTED_ROAD_LINE_OPACITY = 1.0
    internal const val RESTRICTED_ROAD_LINE_WIDTH = 7.0
    internal val RESTRICTED_ROAD_DASH_ARRAY = listOf(.5, 2.0)

    internal val LOW_CONGESTION_RANGE = 0..39

    internal val MODERATE_CONGESTION_RANGE = 40..59

    internal val HEAVY_CONGESTION_RANGE = 60..79

    internal val SEVERE_CONGESTION_RANGE = 80..100

    @ColorInt
    internal val ROUTE_LINE_TRAVELED_COLOR = Color.TRANSPARENT

    @ColorInt
    internal val ROUTE_LINE_TRAVELED_CASING_COLOR = Color.TRANSPARENT

    @ColorInt
    internal val ROUTE_DEFAULT_COLOR = Color.parseColor("#56A8FB")

    @ColorInt
    internal val ROUTE_UNKNOWN_TRAFFIC_COLOR = Color.parseColor("#56A8FB")

    @ColorInt
    internal val ROUTE_LOW_TRAFFIC_COLOR = Color.parseColor("#56A8FB")

    @ColorInt
    internal val ROUTE_MODERATE_TRAFFIC_COLOR = Color.parseColor("#ff9500")

    @ColorInt
    internal val ROUTE_HEAVY_TRAFFIC_COLOR = Color.parseColor("#ff4d4d")

    @ColorInt
    internal val ROUTE_SEVERE_TRAFFIC_COLOR = Color.parseColor("#8f2447")

    @ColorInt
    internal val ROUTE_CASING_COLOR = Color.parseColor("#2F7AC6")

    @ColorInt
    internal val ALTERNATE_ROUTE_DEFAULT_COLOR = Color.parseColor("#8694A5")

    @ColorInt
    internal val ALTERNATE_ROUTE_CASING_COLOR = Color.parseColor("#727E8D")

    @ColorInt
    internal val ALTERNATE_ROUTE_UNKNOWN_TRAFFIC_COLOR = Color.parseColor("#8694A5")

    @ColorInt
    internal val ALTERNATE_ROUTE_LOW_TRAFFIC_COLOR = Color.parseColor("#8694A5")

    @ColorInt
    internal val ALTERNATE_ROUTE_MODERATE_TRAFFIC_COLOR = Color.parseColor("#BEA087")

    @ColorInt
    internal val ALTERNATE_ROUTE_HEAVY_TRAFFIC_COLOR = Color.parseColor("#B58281")

    @ColorInt
    internal val ALTERNATE_ROUTE_SEVERE_TRAFFIC_COLOR = Color.parseColor("#B58281")

    @ColorInt
    internal val RESTRICTED_ROAD_COLOR = Color.parseColor("#000000")

    @ColorInt
    internal val ALTERNATE_RESTRICTED_ROAD_COLOR = Color.parseColor("#333333")

    @DrawableRes
    internal val ORIGIN_WAYPOINT_ICON: Int = R.drawable.mapbox_ic_route_origin

    @DrawableRes
    internal val DESTINATION_WAYPOINT_ICON: Int = R.drawable.mapbox_ic_route_destination

    @ColorInt
    internal val MANEUVER_ARROW_COLOR = Color.parseColor("#FFFFFF")

    @ColorInt
    internal val MANEUVER_ARROW_CASING_COLOR = Color.parseColor("#2D3F53")

    @ColorInt
    internal val ROUTE_CLOSURE_COLOR = Color.parseColor("#000000")

    @ColorInt
    internal val ALTERNATIVE_ROUTE_CLOSURE_COLOR = Color.parseColor("#333333")

    @ColorInt
    internal val IN_ACTIVE_ROUTE_LEG_COLOR = Color.TRANSPARENT

    @DrawableRes
    internal val MANEUVER_ARROWHEAD_ICON_DRAWABLE: Int = R.drawable.mapbox_ic_arrow_head

    @DrawableRes
    internal val MANEUVER_ARROWHEAD_ICON_CASING_DRAWABLE: Int =
        R.drawable.mapbox_ic_arrow_head_casing
}
