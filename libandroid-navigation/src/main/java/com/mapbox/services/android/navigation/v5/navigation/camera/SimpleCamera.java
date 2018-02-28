package com.mapbox.services.android.navigation.v5.navigation.camera;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfMeasurement;

/**
 * The default camera used by {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}.
 *
 * @since 0.10.0
 */
public class SimpleCamera extends Camera {

  private static final int CAMERA_TILT = 50;
  private static final double CAMERA_ZOOM = 15d;

  private LineString lineString;
  private double initialBearing;
  private DirectionsRoute initialRoute;

  @Override
  public double bearing(RouteInformation routeInformation) {
    if (routeInformation.route() != null) {
      setupLineStringAndBearing(routeInformation.route());
      return initialBearing;
    } else if (routeInformation.location() != null) {
      return routeInformation.location().getBearing();
    }
    return 0;
  }

  @Override
  public Point target(RouteInformation routeInformation) {
    double lng;
    double lat;
    Point targetPoint = null;
    if (routeInformation.route() != null) {
      setupLineStringAndBearing(routeInformation.route());
      lng = lineString.coordinates().get(0).longitude();
      lat = lineString.coordinates().get(0).latitude();
      return Point.fromLngLat(lng, lat);
    } else if (routeInformation.location() != null) {
      lng = routeInformation.location().getLongitude();
      lat = routeInformation.location().getLatitude();
      targetPoint = Point.fromLngLat(lng, lat);
    }
    return targetPoint;
  }

  @Override
  public double tilt(RouteInformation routeInformation) {
    return CAMERA_TILT;
  }

  @Override
  public double zoom(RouteInformation routeInformation) {
    return CAMERA_ZOOM;
  }

  private void setupLineStringAndBearing(DirectionsRoute route) {
    if (initialRoute != null && route.equals(initialRoute)) {
      return; //no need to recalculate these values
    }
    initialRoute = route;

    lineString = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);
    initialBearing = TurfMeasurement.bearing(
      Point.fromLngLat(
        lineString.coordinates().get(0).longitude(), lineString.coordinates().get(0).latitude()
      ),
      Point.fromLngLat(
        lineString.coordinates().get(1).longitude(), lineString.coordinates().get(1).latitude()
      )
    );
  }
}
