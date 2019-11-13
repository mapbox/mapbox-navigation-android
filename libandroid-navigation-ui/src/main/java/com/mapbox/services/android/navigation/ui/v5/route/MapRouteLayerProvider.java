package com.mapbox.services.android.navigation.ui.v5.route;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.services.android.navigation.ui.v5.utils.MapImageUtils;

import static com.mapbox.mapboxsdk.style.expressions.Expression.color;
import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.match;
import static com.mapbox.mapboxsdk.style.expressions.Expression.product;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.switchCase;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconPitchAlignment;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.DESTINATION_MARKER_NAME;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.HEAVY_CONGESTION_VALUE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.MODERATE_CONGESTION_VALUE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ORIGIN_MARKER_NAME;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.PRIMARY_ROUTE_PROPERTY_KEY;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ROUTE_LAYER_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ROUTE_SHIELD_LAYER_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ROUTE_SOURCE_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.SEVERE_CONGESTION_VALUE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.WAYPOINT_DESTINATION_VALUE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.WAYPOINT_LAYER_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.WAYPOINT_ORIGIN_VALUE;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.WAYPOINT_PROPERTY_KEY;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.WAYPOINT_SOURCE_ID;

class MapRouteLayerProvider {

  LineLayer initializeRouteShieldLayer(Style style, float routeScale, float alternativeRouteScale,
                                       int routeShieldColor, int alternativeRouteShieldColor) {
    LineLayer shieldLayer = style.getLayerAs(ROUTE_SHIELD_LAYER_ID);
    if (shieldLayer != null) {
      style.removeLayer(shieldLayer);
    }

    shieldLayer = new LineLayer(ROUTE_SHIELD_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
      lineCap(Property.LINE_CAP_ROUND),
      lineJoin(Property.LINE_JOIN_ROUND),
      lineWidth(
        interpolate(
          exponential(1.5f), zoom(),
          stop(10f, 7f),
          stop(14f, product(literal(10.5f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(16.5f, product(literal(15.5f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(19f, product(literal(24f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(22f, product(literal(29f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale))))
        )
      ),
      lineColor(
        switchCase(
          get(PRIMARY_ROUTE_PROPERTY_KEY), color(routeShieldColor),
          color(alternativeRouteShieldColor)
        )
      )
    );
    return shieldLayer;
  }

  LineLayer initializeRouteLayer(Style style, boolean roundedLineCap, float routeScale,
                                 float alternativeRouteScale, int routeDefaultColor, int routeModerateColor,
                                 int routeSevereColor, int alternativeRouteDefaultColor,
                                 int alternativeRouteModerateColor, int alternativeRouteSevereColor) {
    LineLayer routeLayer = style.getLayerAs(ROUTE_LAYER_ID);
    if (routeLayer != null) {
      style.removeLayer(routeLayer);
    }

    String lineCap = Property.LINE_CAP_ROUND;
    String lineJoin = Property.LINE_JOIN_ROUND;
    if (!roundedLineCap) {
      lineCap = Property.LINE_CAP_BUTT;
      lineJoin = Property.LINE_JOIN_BEVEL;
    }

    routeLayer = new LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
      lineCap(lineCap),
      lineJoin(lineJoin),
      lineWidth(
        interpolate(
          exponential(1.5f), zoom(),
          stop(4f, product(literal(3f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(10f, product(literal(4f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(13f, product(literal(6f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(16f, product(literal(10f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(19f, product(literal(14f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale)))),
          stop(22f, product(literal(18f),
            switchCase(
              get(PRIMARY_ROUTE_PROPERTY_KEY), literal(routeScale),
              literal(alternativeRouteScale))))
        )
      ),
      lineColor(
        switchCase(
          get(PRIMARY_ROUTE_PROPERTY_KEY), match(
            Expression.toString(get(RouteConstants.CONGESTION_KEY)),
            color(routeDefaultColor),
            stop(MODERATE_CONGESTION_VALUE, color(routeModerateColor)),
            stop(HEAVY_CONGESTION_VALUE, color(routeSevereColor)),
            stop(SEVERE_CONGESTION_VALUE, color(routeSevereColor))
          ),
          match(
            Expression.toString(get(RouteConstants.CONGESTION_KEY)),
            color(alternativeRouteDefaultColor),
            stop(MODERATE_CONGESTION_VALUE, color(alternativeRouteModerateColor)),
            stop(HEAVY_CONGESTION_VALUE, color(alternativeRouteSevereColor)),
            stop(SEVERE_CONGESTION_VALUE, color(alternativeRouteSevereColor))
          )
        )
      )
    );
    return routeLayer;
  }

  SymbolLayer initializeWayPointLayer(Style style, Drawable originIcon,
                                      Drawable destinationIcon) {
    SymbolLayer wayPointLayer = style.getLayerAs(WAYPOINT_LAYER_ID);
    if (wayPointLayer != null) {
      style.removeLayer(wayPointLayer);
    }

    Bitmap bitmap = MapImageUtils.getBitmapFromDrawable(originIcon);
    style.addImage(ORIGIN_MARKER_NAME, bitmap);
    bitmap = MapImageUtils.getBitmapFromDrawable(destinationIcon);
    style.addImage(DESTINATION_MARKER_NAME, bitmap);

    wayPointLayer = new SymbolLayer(WAYPOINT_LAYER_ID, WAYPOINT_SOURCE_ID).withProperties(
      iconImage(
        match(
          Expression.toString(get(WAYPOINT_PROPERTY_KEY)), literal(ORIGIN_MARKER_NAME),
          stop(WAYPOINT_ORIGIN_VALUE, literal(ORIGIN_MARKER_NAME)),
          stop(WAYPOINT_DESTINATION_VALUE, literal(DESTINATION_MARKER_NAME))
        )),
      iconSize(
        interpolate(
          exponential(1.5f), zoom(),
          stop(0f, 0.6f),
          stop(10f, 0.8f),
          stop(12f, 1.3f),
          stop(22f, 2.8f)
        )
      ),
      iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP),
      iconAllowOverlap(true),
      iconIgnorePlacement(true)
    );
    return wayPointLayer;
  }
}