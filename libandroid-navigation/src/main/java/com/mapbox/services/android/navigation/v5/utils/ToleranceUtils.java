package com.mapbox.services.android.navigation.v5.utils;

import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.turf.TurfClassification;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import java.util.ArrayList;
import java.util.List;

public final class ToleranceUtils {

  private ToleranceUtils() {
    // Utils class therefore, shouldn't be initialized.
  }

  public static double dynamicRerouteDistanceTolerance(Point snappedPoint,
                                                       RouteProgress routeProgress) {
    List<StepIntersection> intersections
      = routeProgress.currentLegProgress().currentStepProgress().intersections();
    List<Point> intersectionsPoints = new ArrayList<>();
    for (StepIntersection intersection : intersections) {
      intersectionsPoints.add(intersection.location());
    }

    Point closestIntersection = TurfClassification.nearestPoint(snappedPoint, intersectionsPoints);

    if (closestIntersection.equals(snappedPoint)) {
      return NavigationConstants.MINIMUM_DISTANCE_BEFORE_REROUTING;
    }

    double distanceToNextIntersection = TurfMeasurement.distance(snappedPoint, closestIntersection,
      TurfConstants.UNIT_METERS);

    if (distanceToNextIntersection <= NavigationConstants.MANEUVER_ZONE_RADIUS) {
      return NavigationConstants.MINIMUM_DISTANCE_BEFORE_REROUTING / 2;
    }
    return NavigationConstants.MINIMUM_DISTANCE_BEFORE_REROUTING;
  }
}