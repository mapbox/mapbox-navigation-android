package com.mapbox.services.android.navigation.v5.navigation.camera;

import android.util.SparseArray;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.Point;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

public class SimpleCamera extends Camera {

  private static final int CAMERA_TILT = 45;

  DirectionsRoute initialRoute;
  LineString lineString;
  double initialBearing;
  OrientationMap orientationMap;

  public SimpleCamera() {
    orientationMap = new OrientationMap();
  }

  @Override
  public double bearing(RouteInformation routeInformation) {
    if (routeInformation.route() != null) {
      setupLineStringAndBearing(routeInformation.route());
      return initialBearing;
    }
    else if (routeInformation.location() != null) {
      return routeInformation.location().getBearing();
    }
    return 0;
  }

  @Override
  public Point target(RouteInformation routeInformation) {
    double lng = 0;
    double lat = 0;
    double bearing = 0;
    if (routeInformation.route() != null) {
      setupLineStringAndBearing(routeInformation.route());
      lng = lineString.getCoordinates().get(0).getLongitude();
      lat = lineString.getCoordinates().get(0).getLatitude();
      bearing = initialBearing;
    } else if (routeInformation.location() != null) {
      lng = routeInformation.location().getLongitude();
      lat = routeInformation.location().getLatitude();
      bearing = routeInformation.location().getBearing();
    }

    Point targetPoint = TurfMeasurement.destination(
            Point.fromLngLat(lng, lat),
            routeInformation.targetDistance(), bearing, TurfConstants.UNIT_METERS
    );

    return targetPoint;
  }

  @Override
  public double tilt(RouteInformation routeInformation) {
    return CAMERA_TILT;
  }

  @Override
  public double zoom(RouteInformation routeInformation) {
    return orientationMap.get(routeInformation.configuration().orientation);
  }

  private void setupLineStringAndBearing(DirectionsRoute route) {
    if (initialRoute != null && route.equals(initialRoute)) {
      return; //no need to recalculate these values
    }
    lineString = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);
    initialBearing = TurfMeasurement.bearing(
            Point.fromLngLat(
                    lineString.getCoordinates().get(0).getLongitude(), lineString.getCoordinates().get(0).getLatitude()
            ),
            Point.fromLngLat(
                    lineString.getCoordinates().get(1).getLongitude(), lineString.getCoordinates().get(1).getLatitude()
            )
    );
  }

  /**
   * Holds the two different screen orientations
   * and their corresponding zoom levels.
   */
  static class OrientationMap extends SparseArray<Integer> {

    OrientationMap() {
      put(ORIENTATION_PORTRAIT, 17);
      put(ORIENTATION_LANDSCAPE, 16);
    }
  }
}
