package com.mapbox.navigation.ui.base.internal.route;

import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import com.mapbox.navigation.ui.base.R;

import java.util.Collections;
import java.util.List;

public class RouteConstants {
  public static final String PRIMARY_ROUTE_SOURCE_ID = "mapbox-navigation-route-source";
  public static final String PRIMARY_ROUTE_LAYER_ID = "mapbox-navigation-route-layer";
  public static final String PRIMARY_ROUTE_TRAFFIC_LAYER_ID = "mapbox-navigation-route-traffic-layer";
  public static final String PRIMARY_ROUTE_CASING_LAYER_ID = "mapbox-navigation-route-casing-layer";
  public static final String ALTERNATIVE_ROUTE1_SOURCE_ID = "mapbox-navigation-alt-route1-source";
  public static final String ALTERNATIVE_ROUTE1_LAYER_ID = "mapbox-navigation-alt-route1-layer";
  public static final String ALTERNATIVE_ROUTE1_CASING_LAYER_ID = "mapbox-navigation-alt-route1-casing-layer";
  public static final String ALTERNATIVE_ROUTE2_SOURCE_ID = "mapbox-navigation-alt-route2-source";
  public static final String ALTERNATIVE_ROUTE2_LAYER_ID = "mapbox-navigation-alt-route2-layer";
  public static final String ALTERNATIVE_ROUTE2_CASING_LAYER_ID = "mapbox-navigation-alt-route2-casing-layer";
  public static final String ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID = "mapbox-navigation-alt-route1-traffic-layer";
  public static final String ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID = "mapbox-navigation-alt-route2-traffic-layer";
  public static final String WAYPOINT_SOURCE_ID = "mapbox-navigation-waypoint-source";
  public static final String WAYPOINT_LAYER_ID = "mapbox-navigation-waypoint-layer";
  public static final int TWO_POINTS = 2;
  public static final int THIRTY = 30;
  public static final String ARROW_BEARING = "mapbox-navigation-arrow-bearing";
  public static final String ARROW_SHAFT_SOURCE_ID = "mapbox-navigation-arrow-shaft-source";
  public static final String ARROW_HEAD_SOURCE_ID = "mapbox-navigation-arrow-head-source";
  public static final String ARROW_SHAFT_CASING_LINE_LAYER_ID = "mapbox-navigation-arrow-shaft-casing-layer";
  public static final String ARROW_SHAFT_LINE_LAYER_ID = "mapbox-navigation-arrow-shaft-layer";
  public static final String ARROW_HEAD_ICON = "mapbox-navigation-arrow-head-icon";
  public static final String ARROW_HEAD_ICON_CASING = "mapbox-navigation-arrow-head-icon-casing";
  public static final double MAX_DEGREES = 360;
  public static final String ARROW_HEAD_CASING_LAYER_ID = "mapbox-navigation-arrow-head-casing-layer";
  public static final Double[] ARROW_HEAD_CASING_OFFSET = {0.0, -7.0};
  public static final String ARROW_HEAD_LAYER_ID = "mapbox-navigation-arrow-head-layer";
  public static final Double[] ARROW_HEAD_OFFSET = {0.0, -7.0};
  public static final Double MIN_ARROW_ZOOM = 10.0;
  public static final Double MAX_ARROW_ZOOM = 22.0;
  public static final Double MIN_ZOOM_ARROW_SHAFT_SCALE = 2.6;
  public static final Double MAX_ZOOM_ARROW_SHAFT_SCALE = 13.0;
  public static final Double MIN_ZOOM_ARROW_SHAFT_CASING_SCALE = 3.4;
  public static final Double MAX_ZOOM_ARROW_SHAFT_CASING_SCALE = 17.0;
  public static final Double MIN_ZOOM_ARROW_HEAD_SCALE = 0.2;
  public static final Double MAX_ZOOM_ARROW_HEAD_SCALE = 0.8;
  public static final Double MIN_ZOOM_ARROW_HEAD_CASING_SCALE = 0.2;
  public static final Double MAX_ZOOM_ARROW_HEAD_CASING_SCALE = 0.8;
  public static final Double OPAQUE = 0.0;
  public static final Double ARROW_HIDDEN_ZOOM_LEVEL = 14.0;
  public static final Double TRANSPARENT = 1.0;
  public static final String WAYPOINT_PROPERTY_KEY = "wayPoint";
  public static final String WAYPOINT_ORIGIN_VALUE = "origin";
  public static final String WAYPOINT_DESTINATION_VALUE = "destination";
  public static final String LOW_CONGESTION_VALUE = "low";
  public static final String MODERATE_CONGESTION_VALUE = "moderate";
  public static final String HEAVY_CONGESTION_VALUE = "heavy";
  public static final String SEVERE_CONGESTION_VALUE = "severe";
  public static final String UNKNOWN_CONGESTION_VALUE = "unknown";
  public static final String ORIGIN_MARKER_NAME = "originMarker";
  public static final String DESTINATION_MARKER_NAME = "destinationMarker";
  public static final String MAPBOX_LOCATION_ID = "mapbox-location";
  public static final double ROUTE_LINE_UPDATE_MAX_DISTANCE_THRESHOLD_IN_METERS = 1.0;
  public static final String DEFAULT_ROUTE_DESCRIPTOR_PLACEHOLDER = "mapboxDescriptorPlaceHolderUnused";
  public static final double MAX_ELAPSED_SINCE_INDEX_UPDATE_NANO = 1_500_000_000; // 1.5s
  @ColorInt
  public static final int ROUTE_LINE_TRAVELED_COLOR = Color.parseColor("#00000000");
  @ColorInt
  public static final int ROUTE_LINE_TRAVELED_CASING_COLOR = Color.parseColor("#00000000");
  @ColorInt
  public static final int ROUTE_DEFAULT_COLOR = Color.parseColor("#56A8FB");
  @ColorInt
  public static final int ROUTE_UNKNOWN_TRAFFIC_COLOR = Color.parseColor("#56A8FB");
  @ColorInt
  public static final int ROUTE_LOW_TRAFFIC_COLOR = Color.parseColor("#56A8FB");
  @ColorInt
  public static final int ROUTE_MODERATE_TRAFFIC_COLOR = Color.parseColor("#ff9500");
  @ColorInt
  public static final int ROUTE_HEAVY_TRAFFIC_COLOR = Color.parseColor("#ff4d4d");
  @ColorInt
  public static final int ROUTE_SEVERE_TRAFFIC_COLOR = Color.parseColor("#8f2447");
  @ColorInt
  public static final int ROUTE_CASING_COLOR = Color.parseColor("#2F7AC6");
  @ColorInt
  public static final int ALTERNATE_ROUTE_DEFAULT_COLOR = Color.parseColor("#8694A5");
  @ColorInt
  public static final int ALTERNATE_ROUTE_CASING_COLOR = Color.parseColor("#727E8D");
  @ColorInt
  public static final int ALTERNATE_ROUTE_UNKNOWN_TRAFFIC_COLOR = Color.parseColor("#8694A5");
  @ColorInt
  public static final int ALTERNATE_ROUTE_LOW_TRAFFIC_COLOR = Color.parseColor("#8694A5");
  @ColorInt
  public static final int ALTERNATE_ROUTE_MODERATE_TRAFFIC_COLOR = Color.parseColor("#BEA087");
  @ColorInt
  public static final int ALTERNATE_ROUTE_HEAVY_TRAFFIC_COLOR = Color.parseColor("#B58281");
  @ColorInt
  public static final int ALTERNATE_ROUTE_SEVERE_TRAFFIC_COLOR = Color.parseColor("#B58281");
  public static final boolean ROUNDED_LINE_CAP = true;
  @DrawableRes
  public static final int ORIGIN_WAYPOINT_ICON = R.drawable.mapbox_ic_route_origin;
  @DrawableRes
  public static final int DESTINATION_WAYPOINT_ICON = R.drawable.mapbox_ic_route_destination;
  public static final List<String> TRAFFIC_BACKFILL_ROAD_CLASSES = Collections.emptyList();
  @ColorInt
  public static final int MANEUVER_ARROW_COLOR = Color.parseColor("#FFFFFF");
  @ColorInt
  public static final int MANEUVER_ARROW_BORDER_COLOR = Color.parseColor("#2D3F53");
  @DrawableRes
  public static final int MANEUVER_ARROWHEAD_ICON_DRAWABLE = R.drawable.mapbox_ic_arrow_head;
  @DrawableRes
  public static final int MANEUVER_ARROWHEAD_ICON_CASING_DRAWABLE = R.drawable.mapbox_ic_arrow_head_casing;
  public static final double DEFAULT_ROUTE_SOURCES_TOLERANCE = 0.375;
}
