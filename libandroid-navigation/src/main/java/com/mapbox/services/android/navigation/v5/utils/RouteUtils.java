package com.mapbox.services.android.navigation.v5.utils;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.turf.TurfMisc;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.services.constants.Constants.PRECISION_6;

public final class RouteUtils {

  private RouteUtils() {
    // Utils class therefore, shouldn't be initialized.
  }

  /**
   * Compares a new routeProgress geometry to a previousRouteProgress geometry to determine if the
   * user is traversing along a new route. If the route geometries do not match, this returns true.
   *
   * @param previousRouteProgress the past route progress with the directions route included
   * @param routeProgress         the route progress with the directions route included
   * @return true if the direction route geometries do not match up, otherwise, false
   * @since 0.7.0
   */
  public static boolean isNewRoute(@Nullable RouteProgress previousRouteProgress,
                                   @NonNull RouteProgress routeProgress) {
    return isNewRoute(previousRouteProgress, routeProgress.directionsRoute());
  }

  /**
   * Compares a new routeProgress geometry to a previousRouteProgress geometry to determine if the
   * user is traversing along a new route. If the route geometries do not match, this returns true.
   *
   * @param previousRouteProgress the past route progress with the directions route included
   * @param directionsRoute       the current directions route
   * @return true if the direction route geometries do not match up, otherwise, false
   * @since 0.7.0
   */
  public static boolean isNewRoute(@Nullable RouteProgress previousRouteProgress,
                                   @NonNull DirectionsRoute directionsRoute) {
    return previousRouteProgress == null || !previousRouteProgress.directionsRoute().geometry()
      .equals(directionsRoute.geometry());
  }








  /**
   * Takes in a raw location, converts it to a point, and snaps it to the closest point along the
   * route. This is isolated as separate logic from the snap logic provided because we will always
   * need to snap to the route in order to get the most accurate information.
   */
  public static Point userSnappedToRoutePosition(Location location, RouteProgress routeProgress) {
    Point locationToPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());


    // Uses Turf's pointOnLine, which takes a Point and a LineString to calculate the closest
    // Point on the LineString.
    Feature feature = TurfMisc.pointOnLine(locationToPoint, routeProgress.currentStepCoordinates());
    return ((Point) feature.geometry());
  }



//  public static List<Point> calculateNearbyPoints(RouteProgress routeProgress) {
//    int stepIndex = routeProgress.currentLegProgress().stepIndex();
//    int legIndex = routeProgress.legIndex();
//
//    List<Point> nearbyPoints = new ArrayList<>();
//    // Adds the prior step geometry Points
//    if (stepIndex != 0) {
//      // Users not on the first step
//      nearbyPoints.addAll(
//        PolylineUtils.decode(
//          routeProgress.directionsRoute().legs().get(legIndex)
//            .steps().get(stepIndex - 1).geometry(), PRECISION_6)
//      );
//    }
//
//    // Current step points
//    nearbyPoints.addAll(
//      PolylineUtils.decode(
//        routeProgress.directionsRoute().legs().get(0).steps().get(0).geometry(), PRECISION_6)
//    );
//
//
//    if ((routeProgress.currentLeg().steps().size() - 1) > stepIndex) {
//      nearbyPoints.addAll(PolylineUtils.decode(
//        routeProgress.directionsRoute().legs().get(legIndex).steps().get(stepIndex + 1).geometry(),
//        PRECISION_6));
//    }
//    return nearbyPoints;
//  }
}
