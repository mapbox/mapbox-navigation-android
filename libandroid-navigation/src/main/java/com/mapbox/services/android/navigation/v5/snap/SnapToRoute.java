package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteStepProgress;
import com.mapbox.services.android.navigation.v5.utils.MathUtils;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;

import java.util.List;

import static com.mapbox.core.constants.Constants.PRECISION_6;

/**
 * This attempts to snap the user to the closest position along the route. Prior to snapping the
 * user, their location's checked to ensure that the user didn't veer off-route. If your application
 * uses the Mapbox Map SDK, querying the map and snapping the user to the road grid might be a
 * better solution.
 *
 * @since 0.4.0
 */
public class SnapToRoute extends Snap {

  @Override
  public Location getSnappedLocation(Location location, RouteProgress routeProgress) {
    Location snappedLocation = snapLocationLatLng(location, routeProgress.currentStepPoints());
    snappedLocation.setBearing(snapLocationBearing(routeProgress));
    return snappedLocation;
  }

  /**
   * Logic used to snap the users location coordinates to the closest position along the current
   * step.
   *
   * @param location        the raw location
   * @param stepCoordinates the list of step geometry coordinates
   * @return the altered user location
   * @since 0.4.0
   */
  private static Location snapLocationLatLng(Location location, List<Point> stepCoordinates) {
    Location snappedLocation = new Location(location);
    Point locationToPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());

    // Uses Turf's pointOnLine, which takes a Point and a LineString to calculate the closest
    // Point on the LineString.
    if (stepCoordinates.size() > 1) {
      Feature feature = TurfMisc.nearestPointOnLine(locationToPoint, stepCoordinates);
      Point point = ((Point) feature.geometry());
      snappedLocation.setLongitude(point.longitude());
      snappedLocation.setLatitude(point.latitude());
    }
    return snappedLocation;
  }

  /**
   * Creates a snapped bearing for the snapped {@link Location}.
   * <p>
   * This is done by measuring 1 meter ahead of the current step distance traveled and
   * creating a {@link Point} with this distance using {@link TurfMeasurement#along(LineString, double, String)}.
   * <p>
   * If the step distance remaining is zero, the distance ahead is 1 meter into the upcoming step.
   * This way, an accurate bearing is upheld transitioning between steps.
   *
   * @param routeProgress for all current progress values
   * @return float bearing snapped to route
   */
  private static float snapLocationBearing(RouteProgress routeProgress) {

    RouteLegProgress legProgress = routeProgress.currentLegProgress();
    RouteStepProgress stepProgress = legProgress.currentStepProgress();
    double distanceTraveled = stepProgress.distanceTraveled();
    double distanceRemaining = stepProgress.distanceRemaining();
    boolean distanceRemainingZero = distanceRemaining == 0;

    // Either want to measure our current step distance traveled + 1 or 1 meter into the upcoming step
    double distanceAhead = distanceRemainingZero ? 1 : distanceTraveled + 1;
    // Create the step linestring from the geometry
    LineString upcomingLineString = createUpcomingLineString(legProgress, distanceRemainingZero);
    LineString currentLineString = createCurrentLineString(legProgress);

    // Measure 1 meter ahead of the users current location, only if the distance remaining isn't zero
    Point futurePoint = createFuturePoint(distanceAhead, upcomingLineString, currentLineString);
    Point currentPoint = TurfMeasurement.along(currentLineString, distanceTraveled, TurfConstants.UNIT_METERS);

    // Get bearing and convert azimuth to degrees
    double azimuth = TurfMeasurement.bearing(currentPoint, futurePoint);
    return (float) MathUtils.wrap(azimuth, 0, 360);
  }

  @NonNull
  private static LineString createCurrentLineString(RouteLegProgress legProgress) {
    String currentGeometry = legProgress.currentStep().geometry();
    return LineString.fromPolyline(currentGeometry, PRECISION_6);
  }

  @Nullable
  private static LineString createUpcomingLineString(RouteLegProgress legProgress, boolean distanceRemainingZero) {
    LineString upcomingLineString = null;
    if (distanceRemainingZero && legProgress.upComingStep() != null) {
      String upcomingGeometry = legProgress.upComingStep().geometry();
      upcomingLineString = LineString.fromPolyline(upcomingGeometry, PRECISION_6);
    }
    return upcomingLineString;
  }

  @NonNull
  private static Point createFuturePoint(double distanceAhead, LineString upcomingLineString,
                                         LineString currentLineString) {
    Point futurePoint;
    if (upcomingLineString != null) {
      futurePoint = TurfMeasurement.along(upcomingLineString, distanceAhead, TurfConstants.UNIT_METERS);
    } else {
      futurePoint = TurfMeasurement.along(currentLineString, distanceAhead, TurfConstants.UNIT_METERS);
    }
    return futurePoint;
  }
}