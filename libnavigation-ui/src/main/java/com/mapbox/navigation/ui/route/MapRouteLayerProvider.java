package com.mapbox.navigation.ui.route;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.navigation.ui.utils.MapImageUtils;

import static com.mapbox.mapboxsdk.style.expressions.Expression.color;
import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.match;
import static com.mapbox.mapboxsdk.style.expressions.Expression.product;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
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
import static com.mapbox.navigation.ui.route.RouteConstants.ALTERNATIVE_ROUTE_LAYER_ID;
import static com.mapbox.navigation.ui.route.RouteConstants.ALTERNATIVE_ROUTE_SHIELD_LAYER_ID;
import static com.mapbox.navigation.ui.route.RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID;
import static com.mapbox.navigation.ui.route.RouteConstants.DESTINATION_MARKER_NAME;
import static com.mapbox.navigation.ui.route.RouteConstants.ORIGIN_MARKER_NAME;
import static com.mapbox.navigation.ui.route.RouteConstants.PRIMARY_ROUTE_LAYER_ID;
import static com.mapbox.navigation.ui.route.RouteConstants.PRIMARY_ROUTE_SHIELD_LAYER_ID;
import static com.mapbox.navigation.ui.route.RouteConstants.PRIMARY_ROUTE_SOURCE_ID;
import static com.mapbox.navigation.ui.route.RouteConstants.WAYPOINT_DESTINATION_VALUE;
import static com.mapbox.navigation.ui.route.RouteConstants.WAYPOINT_LAYER_ID;
import static com.mapbox.navigation.ui.route.RouteConstants.WAYPOINT_ORIGIN_VALUE;
import static com.mapbox.navigation.ui.route.RouteConstants.WAYPOINT_PROPERTY_KEY;
import static com.mapbox.navigation.ui.route.RouteConstants.WAYPOINT_SOURCE_ID;

public class MapRouteLayerProvider {

  LineLayer initializePrimaryRouteShieldLayer(Style style, float routeScale, int routeShieldColor) {
    LineLayer primaryShieldLayer = style.getLayerAs(PRIMARY_ROUTE_SHIELD_LAYER_ID);
    if (primaryShieldLayer != null) {
      style.removeLayer(primaryShieldLayer);
    }

    // fixme reduce all the duplicate code here

    primaryShieldLayer = new LineLayer(PRIMARY_ROUTE_SHIELD_LAYER_ID, PRIMARY_ROUTE_SOURCE_ID).withProperties(
       lineCap(Property.LINE_CAP_ROUND),
       lineJoin(Property.LINE_JOIN_ROUND),
       lineWidth(
           interpolate(
               exponential(1.5f), zoom(),
                   stop(10f, 7f),
                   stop(14f, product(literal(10.5f), literal(routeScale))),
                   stop(16.5f, product(literal(15.5f), literal(routeScale))),
                   stop(19f, product(literal(24f), literal(routeScale))),
                   stop(22f, product(literal(29f), literal(routeScale)))
               )
       ),
       lineColor(color(routeShieldColor))
   );
    return primaryShieldLayer;
  }

  LineLayer initializeAlternativeRouteShieldLayer(
    Style style,
    float alternativeRouteScale,
    int alternativeRouteShieldColor) {
    LineLayer alternativeShieldLayer = style.getLayerAs(ALTERNATIVE_ROUTE_SHIELD_LAYER_ID);
    if (alternativeShieldLayer != null) {
      style.removeLayer(alternativeShieldLayer);
    }

    alternativeShieldLayer = new LineLayer(
            ALTERNATIVE_ROUTE_SHIELD_LAYER_ID,
            ALTERNATIVE_ROUTE_SOURCE_ID)
            .withProperties(
       lineCap(Property.LINE_CAP_ROUND),
       lineJoin(Property.LINE_JOIN_ROUND),
       lineWidth(
           interpolate(
               exponential(1.5f), zoom(),
                   stop(10f, 7f),
                   stop(14f, product(literal(10.5f), literal(alternativeRouteScale))),
                   stop(16.5f, product(literal(15.5f), literal(alternativeRouteScale))),
                   stop(19f, product(literal(24f), literal(alternativeRouteScale))),
                   stop(22f, product(literal(29f), literal(alternativeRouteScale)))
               )
       ),
       lineColor(alternativeRouteShieldColor)
    );
    return alternativeShieldLayer;
  }

  LineLayer initializePrimaryRouteLayer(Style style,
                                          boolean roundedLineCap,
                                          float routeScale,
                                          int routeDefaultColor) {
    LineLayer primaryRouteLayer = style.getLayerAs(PRIMARY_ROUTE_LAYER_ID);
    if (primaryRouteLayer != null) {
      style.removeLayer(primaryRouteLayer);
    }

    String lineCap = Property.LINE_CAP_ROUND;
    String lineJoin = Property.LINE_JOIN_ROUND;
    if (!roundedLineCap) {
      lineCap = Property.LINE_CAP_BUTT;
      lineJoin = Property.LINE_JOIN_BEVEL;
    }

    primaryRouteLayer = new LineLayer(PRIMARY_ROUTE_LAYER_ID, PRIMARY_ROUTE_SOURCE_ID).withProperties(
       lineCap(lineCap),
       lineJoin(lineJoin),
       lineWidth(
           interpolate(
               exponential(1.5f), zoom(),
                   stop(4f, product(literal(3f), literal(routeScale))),
                   stop(10f, product(literal(4f), literal(routeScale))),
                   stop(13f, product(literal(6f), literal(routeScale))),
                   stop(16f, product(literal(10f), literal(routeScale))),
                   stop(19f, product(literal(14f), literal(routeScale))),
                   stop(22f, product(literal(18f), literal(routeScale)))
           )
       ),
       lineColor(color(routeDefaultColor))
       );
    return primaryRouteLayer;
  }

  LineLayer initializeAlternativeRouteLayer(Style style,
                                              boolean roundedLineCap,
                                              float alternativeRouteScale,
                                              int alternativeRouteDefaultColor) {
    LineLayer alternativeRouteLayer = style.getLayerAs(ALTERNATIVE_ROUTE_LAYER_ID);
    if (alternativeRouteLayer != null) {
      style.removeLayer(alternativeRouteLayer);
    }

    String lineCap = Property.LINE_CAP_ROUND;
    String lineJoin = Property.LINE_JOIN_ROUND;
    if (!roundedLineCap) {
      lineCap = Property.LINE_CAP_BUTT;
      lineJoin = Property.LINE_JOIN_BEVEL;
    }

    alternativeRouteLayer = new LineLayer(ALTERNATIVE_ROUTE_LAYER_ID, ALTERNATIVE_ROUTE_SOURCE_ID).withProperties(
       lineCap(lineCap),
       lineJoin(lineJoin),
       lineWidth(
           interpolate(
               exponential(1.5f), zoom(),
                   stop(4f, product(literal(3f), literal(alternativeRouteScale))),
                   stop(10f, product(literal(4f), literal(alternativeRouteScale))),
                   stop(13f, product(literal(6f), literal(alternativeRouteScale))),
                   stop(16f, product(literal(10f), literal(alternativeRouteScale))),
                   stop(19f, product(literal(14f), literal(alternativeRouteScale))),
                   stop(22f, product(literal(18f), literal(alternativeRouteScale)))
           )
       ),
       lineColor(alternativeRouteDefaultColor)
    );
    return alternativeRouteLayer;
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