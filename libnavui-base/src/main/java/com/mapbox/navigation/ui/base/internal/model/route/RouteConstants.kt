package com.mapbox.navigation.ui.base.internal.model.route

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.mapbox.navigation.ui.base.R

object RouteConstants {
    const val PRIMARY_ROUTE_SOURCE_ID = "mapbox-navigation-route-source"
    const val ALTERNATIVE_ROUTE1_SOURCE_ID = "mapbox-navigation-alt-route1-source"
    const val ALTERNATIVE_ROUTE2_SOURCE_ID = "mapbox-navigation-alt-route2-source"
    const val WAYPOINT_SOURCE_ID = "mapbox-navigation-waypoint-source"
    const val TWO_POINTS = 2
    const val THIRTY = 30
    const val ARROW_BEARING = "mapbox-navigation-arrow-bearing"
    const val ARROW_SHAFT_SOURCE_ID = "mapbox-navigation-arrow-shaft-source"
    const val ARROW_HEAD_SOURCE_ID = "mapbox-navigation-arrow-head-source"
    const val ARROW_HEAD_ICON = "mapbox-navigation-arrow-head-icon"
    const val ARROW_HEAD_ICON_CASING = "mapbox-navigation-arrow-head-icon-casing"
    const val MAX_DEGREES = 360.0
    val ARROW_HEAD_CASING_OFFSET = arrayOf(0.0, -7.0)
    val ARROW_HEAD_OFFSET = arrayOf(0.0, -7.0)
    const val MIN_ARROW_ZOOM = 10.0
    const val MAX_ARROW_ZOOM = 22.0
    const val MIN_ZOOM_ARROW_SHAFT_SCALE = 2.6
    const val MAX_ZOOM_ARROW_SHAFT_SCALE = 13.0
    const val MIN_ZOOM_ARROW_SHAFT_CASING_SCALE = 3.4
    const val MAX_ZOOM_ARROW_SHAFT_CASING_SCALE = 17.0
    const val MIN_ZOOM_ARROW_HEAD_SCALE = 0.2
    const val MAX_ZOOM_ARROW_HEAD_SCALE = 0.8
    const val MIN_ZOOM_ARROW_HEAD_CASING_SCALE = 0.2
    const val MAX_ZOOM_ARROW_HEAD_CASING_SCALE = 0.8
    const val OPAQUE = 0.0
    const val ARROW_HIDDEN_ZOOM_LEVEL = 14.0
    const val TRANSPARENT = 1.0
    const val WAYPOINT_PROPERTY_KEY = "wayPoint"
    const val WAYPOINT_ORIGIN_VALUE = "origin"
    const val WAYPOINT_DESTINATION_VALUE = "destination"
    const val LOW_CONGESTION_VALUE = "low"
    const val MODERATE_CONGESTION_VALUE = "moderate"
    const val HEAVY_CONGESTION_VALUE = "heavy"
    const val SEVERE_CONGESTION_VALUE = "severe"
    const val UNKNOWN_CONGESTION_VALUE = "unknown"
    const val CLOSURE_CONGESTION_VALUE = "closed"
    const val RESTRICTED_CONGESTION_VALUE = "restricted"
    const val ORIGIN_MARKER_NAME = "originMarker"
    const val DESTINATION_MARKER_NAME = "destinationMarker"
    const val ROUTE_LINE_UPDATE_MAX_DISTANCE_THRESHOLD_IN_METERS = 1.0
    const val DEFAULT_ROUTE_DESCRIPTOR_PLACEHOLDER = "mapboxDescriptorPlaceHolderUnused"
    const val MAX_ELAPSED_SINCE_INDEX_UPDATE_NANO = 1500000000.0 // 1.5s
    const val DEFAULT_ROUTE_SOURCES_TOLERANCE = 0.375
    const val ROUNDED_LINE_CAP = true
    const val RESTRICTED_ROAD_SECTION_SCALE = 10.0
    val TRAFFIC_BACKFILL_ROAD_CLASSES = emptyList<String>()

    @ColorInt
    val ROUTE_LINE_TRAVELED_COLOR = Color.TRANSPARENT

    @ColorInt
    val ROUTE_LINE_TRAVELED_CASING_COLOR = Color.TRANSPARENT

    @ColorInt
    val ROUTE_DEFAULT_COLOR = Color.parseColor("#56A8FB")

    @ColorInt
    val ROUTE_UNKNOWN_TRAFFIC_COLOR = Color.parseColor("#56A8FB")

    @ColorInt
    val ROUTE_LOW_TRAFFIC_COLOR = Color.parseColor("#56A8FB")

    @ColorInt
    val ROUTE_MODERATE_TRAFFIC_COLOR = Color.parseColor("#ff9500")

    @ColorInt
    val ROUTE_HEAVY_TRAFFIC_COLOR = Color.parseColor("#ff4d4d")

    @ColorInt
    val ROUTE_SEVERE_TRAFFIC_COLOR = Color.parseColor("#8f2447")

    @ColorInt
    val ROUTE_CASING_COLOR = Color.parseColor("#2F7AC6")

    @ColorInt
    val ALTERNATE_ROUTE_DEFAULT_COLOR = Color.parseColor("#8694A5")

    @ColorInt
    val ALTERNATE_ROUTE_CASING_COLOR = Color.parseColor("#727E8D")

    @ColorInt
    val ALTERNATE_ROUTE_UNKNOWN_TRAFFIC_COLOR = Color.parseColor("#8694A5")

    @ColorInt
    val ALTERNATE_ROUTE_LOW_TRAFFIC_COLOR = Color.parseColor("#8694A5")

    @ColorInt
    val ALTERNATE_ROUTE_MODERATE_TRAFFIC_COLOR = Color.parseColor("#BEA087")

    @ColorInt
    val ALTERNATE_ROUTE_HEAVY_TRAFFIC_COLOR = Color.parseColor("#B58281")

    @ColorInt
    val ALTERNATE_ROUTE_SEVERE_TRAFFIC_COLOR = Color.parseColor("#B58281")

    @ColorInt
    val RESTRICTED_ROAD_COLOR = Color.parseColor("#000000")

    @ColorInt
    val ALTERNATE_RESTRICTED_ROAD_COLOR = Color.parseColor("#333333")

    @DrawableRes
    val ORIGIN_WAYPOINT_ICON: Int = R.drawable.mapbox_ic_route_origin

    @DrawableRes
    val DESTINATION_WAYPOINT_ICON: Int = R.drawable.mapbox_ic_route_destination

    @ColorInt
    val MANEUVER_ARROW_COLOR = Color.parseColor("#FFFFFF")

    @ColorInt
    val MANEUVER_ARROW_CASING_COLOR = Color.parseColor("#2D3F53")

    @ColorInt
    val ROUTE_CLOSURE_COLOR = Color.parseColor("#000000")

    @ColorInt
    val ALTERNATIVE_ROUTE_CLOSURE_COLOR = Color.parseColor("#333333")

    @ColorInt
    val IN_ACTIVE_ROUTE_LEG_COLOR = Color.TRANSPARENT

    @DrawableRes
    val MANEUVER_ARROWHEAD_ICON_DRAWABLE: Int = R.drawable.mapbox_ic_arrow_head

    @DrawableRes
    val MANEUVER_ARROWHEAD_ICON_CASING_DRAWABLE: Int = R.drawable.mapbox_ic_arrow_head_casing
}
