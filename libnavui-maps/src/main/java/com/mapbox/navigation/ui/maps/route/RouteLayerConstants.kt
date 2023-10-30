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
    internal const val LAYER_GROUP_1_SOURCE_ID = "mapbox-layerGroup:1:Source"
    internal const val LAYER_GROUP_2_SOURCE_ID = "mapbox-layerGroup:2:Source"
    internal const val LAYER_GROUP_3_SOURCE_ID = "mapbox-layerGroup:3:Source"
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
    internal const val ROUTE_LINE_UPDATE_MAX_DISTANCE_THRESHOLD_IN_METERS = 10.0
    internal const val DEFAULT_ROUTE_DESCRIPTOR_PLACEHOLDER = "mapboxDescriptorPlaceHolderUnused"
    internal const val MAX_ELAPSED_SINCE_INDEX_UPDATE_NANO = 1500000000.0 // 1.5s
    internal const val DEFAULT_ROUTE_SOURCES_TOLERANCE = 0.375
    internal const val ROUNDED_LINE_CAP = true
    internal const val SOFT_GRADIENT_STOP_GAP_METERS: Double = 30.0
    internal val TRAFFIC_BACKFILL_ROAD_CLASSES = emptyList<String>()
    internal const val RESTRICTED_ROAD_LINE_OPACITY = 1.0
    internal const val RESTRICTED_ROAD_LINE_WIDTH = 7.0
    internal val RESTRICTED_ROAD_DASH_ARRAY = listOf(.5, 2.0)
    internal const val DEFAULT_VANISHING_POINT_MIN_UPDATE_INTERVAL_NANO = 62_500_000L
    internal const val ROUTE_LINE_BLUR = 5.0
    internal const val ROUTE_LINE_BLUR_OPACITY = .3

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
    internal val ROUTE_LEG_INACTIVE_UNKNOWN_TRAFFIC_COLOR = Color.TRANSPARENT

    @ColorInt
    internal val ROUTE_LEG_INACTIVE_LOW_TRAFFIC_COLOR = Color.TRANSPARENT

    @ColorInt
    internal val ROUTE_LEG_INACTIVE_MODERATE_TRAFFIC_COLOR = Color.TRANSPARENT

    @ColorInt
    internal val ROUTE_LEG_INACTIVE_HEAVY_TRAFFIC_COLOR = Color.TRANSPARENT

    @ColorInt
    internal val ROUTE_LEG_INACTIVE_SEVERE_TRAFFIC_COLOR = Color.TRANSPARENT

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
    internal val ROUTE_LEG_INACTIVE_RESTRICTED_ROAD_COLOR = Color.TRANSPARENT

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
    internal val ROUTE_LEG_INACTIVE_CLOSURE_COLOR = Color.TRANSPARENT

    @ColorInt
    internal val ALTERNATIVE_ROUTE_CLOSURE_COLOR = Color.parseColor("#333333")

    @ColorInt
    internal val INACTIVE_ROUTE_LEG_CASING_COLOR = Color.TRANSPARENT

    @ColorInt
    internal val IN_ACTIVE_ROUTE_LEG_COLOR = Color.TRANSPARENT

    @DrawableRes
    internal val MANEUVER_ARROWHEAD_ICON_DRAWABLE: Int = R.drawable.mapbox_ic_arrow_head

    @DrawableRes
    internal val MANEUVER_ARROWHEAD_ICON_CASING_DRAWABLE: Int =
        R.drawable.mapbox_ic_arrow_head_casing

    internal const val LAYER_GROUP_1_BLURRED_BACKGROUND = "mapbox-layerGroup-1-blur"
    internal const val LAYER_GROUP_1_TRAIL_CASING = "mapbox-layerGroup-1-trailCasing"
    internal const val LAYER_GROUP_1_TRAIL = "mapbox-layerGroup-1-trail"
    internal const val LAYER_GROUP_1_CASING = "mapbox-layerGroup-1-casing"
    internal const val LAYER_GROUP_1_MAIN = "mapbox-layerGroup-1-main"
    internal const val LAYER_GROUP_1_TRAFFIC = "mapbox-layerGroup-1-traffic"
    internal const val LAYER_GROUP_1_RESTRICTED = "mapbox-layerGroup-1-restricted"

    internal const val LAYER_GROUP_2_TRAIL_CASING = "mapbox-layerGroup-2-trailCasing"
    internal const val LAYER_GROUP_2_TRAIL = "mapbox-layerGroup-2-trail"
    internal const val LAYER_GROUP_2_CASING = "mapbox-layerGroup-2-casing"
    internal const val LAYER_GROUP_2_MAIN = "mapbox-layerGroup-2-main"
    internal const val LAYER_GROUP_2_TRAFFIC = "mapbox-layerGroup-2-traffic"
    internal const val LAYER_GROUP_2_RESTRICTED = "mapbox-layerGroup-2-restricted"

    internal const val LAYER_GROUP_3_TRAIL_CASING = "mapbox-layerGroup-3-trailCasing"
    internal const val LAYER_GROUP_3_TRAIL = "mapbox-layerGroup-3-trail"
    internal const val LAYER_GROUP_3_CASING = "mapbox-layerGroup-3-casing"
    internal const val LAYER_GROUP_3_MAIN = "mapbox-layerGroup-3-main"
    internal const val LAYER_GROUP_3_TRAFFIC = "mapbox-layerGroup-3-traffic"
    internal const val LAYER_GROUP_3_RESTRICTED = "mapbox-layerGroup-3-restricted"

    /*
    When a line on the map overlaps itself it appears as though the later part of the line
    is on top of the earlier part of the line. For multi-leg routes this gives the appearance
    the inactive leg(s) appearing on top of the active leg.  Customer feedback indicated this
    was unsatisfactory. The masking layers were created to address this issue and give the
    opposite of the default appearance. The end result is the active route leg appears
    above inactive leg(s) when a route overlaps itself.

    The masking layers share the same source as the primary route line. They are placed above
    the primary route line layers. The implementation will set the inactive route legs to transparent
    so the primary route line layers are visible.  The active leg section of the masking layers
    are opaque thus masking the the primary route line layers beneath.
     */
    internal const val MASKING_LAYER_TRAIL_CASING = "mapbox-masking-layer-trailCasing"
    internal const val MASKING_LAYER_TRAIL = "mapbox-masking-layer-trail"
    internal const val MASKING_LAYER_CASING = "mapbox-masking-layer-casing"
    internal const val MASKING_LAYER_MAIN = "mapbox-masking-layer-main"
    internal const val MASKING_LAYER_TRAFFIC = "mapbox-masking-layer-traffic"
    internal const val MASKING_LAYER_RESTRICTED = "mapbox-masking-layer-restricted"
}
