package com.mapbox.services.android.navigation.v5.navigation.camera;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfMeasurement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The default camera used by {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}.
 *
 * @since 0.10.0
 */
public class SimpleCamera extends Camera {

  protected static final int DEFAULT_TILT = 50;
  protected static final double DEFAULT_ZOOM = 15d;

  private List<Point> routeCoordinates = new ArrayList<>();
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
      lng = routeCoordinates.get(0).longitude();
      lat = routeCoordinates.get(0).latitude();
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
    return DEFAULT_TILT;
  }

  @Override
  public double zoom(RouteInformation routeInformation) {
    return DEFAULT_ZOOM;
  }

  @Override
  public List<Point> overview(RouteInformation routeInformation) {
    boolean invalidCoordinates = routeCoordinates == null || routeCoordinates.isEmpty();
    if (invalidCoordinates) {
      buildRouteCoordinatesFromRouteData(routeInformation);
    }
    return routeCoordinates;
  }

  private void buildRouteCoordinatesFromRouteData(RouteInformation routeInformation) {
    if (routeInformation.route() != null) {
      setupLineStringAndBearing(routeInformation.route());
    } else if (routeInformation.routeProgress() != null) {
      setupLineStringAndBearing(routeInformation.routeProgress().directionsRoute());
    }
  }

  private void setupLineStringAndBearing(DirectionsRoute route) {
    if (initialRoute != null && route.equals(initialRoute)) {
      return; //no need to recalculate these values
    }
    initialRoute = route;
    routeCoordinates = generateRouteCoordinates(route);
    initialBearing = TurfMeasurement.bearing(
      Point.fromLngLat(routeCoordinates.get(0).longitude(), routeCoordinates.get(0).latitude()),
      Point.fromLngLat(routeCoordinates.get(1).longitude(), routeCoordinates.get(1).latitude())
    );
  }

  private List<Point> generateRouteCoordinates(DirectionsRoute route) {
    if (route == null) {
      return Collections.emptyList();
    }
    LineString lineString = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);
    return lineString.coordinates();
  }
}
