package com.mapbox.services.android.navigation.v5;


import android.location.Location;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.v5.instruction.DefaultInstruction;
import com.mapbox.services.android.navigation.v5.instruction.Instruction;
import com.mapbox.services.android.navigation.v5.listeners.OffRouteListener;
import com.mapbox.services.android.navigation.v5.listeners.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.snap.Snap;
import com.mapbox.services.android.navigation.v5.snap.SnapToRoute;
import com.mapbox.services.android.telemetry.utils.MathUtils;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.api.utils.turf.TurfMisc;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.commons.utils.PolylineUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @since 0.2.0
 */
class NavigationEngine {

  // Listeners
  private CopyOnWriteArrayList<ProgressChangeListener> progressChangeListeners;
  private CopyOnWriteArrayList<OffRouteListener> offRouteListeners;
  private CopyOnWriteArrayList<MilestoneEventListener> milestoneEventListeners;
  private CopyOnWriteArrayList<Milestone> milestones;

  // Navigation state information
  private RouteProgress previousRouteProgress;
  private MapboxNavigationOptions options;
  private long timeIntervalSinceLastOffRoute;
  private Location previousLocation;
  private boolean isSnapEnabled;
  private int stepIndex;
  private int legIndex;

  /**
   * Constructs a new navigation engine instance.
   *
   * @param options       the initial {@link MapboxNavigationOptions} to be used (this can be updated using the setter)
   * @param isSnapEnabled boolean true if the snapping to route features enabled, otherwise false
   * @since 0.2.0
   */
  NavigationEngine(@NonNull MapboxNavigationOptions options, boolean isSnapEnabled) {
    this.isSnapEnabled = isSnapEnabled;
    this.options = options;
    stepIndex = 0;
    legIndex = 0;
  }

  /**
   * When the {@link NavigationService} recieves a new location update, this methods called which coordinates the
   * updating of events and creating the new {@link RouteProgress} object.
   *
   * @param directionsRoute takes in the directions route which ensures if a reroute occurs we are using it
   * @param location        the user location
   * @since 0.2.0
   */
  void onLocationChanged(DirectionsRoute directionsRoute, Location location) {
    // if the previousRouteProgress is null, the route has just begun and one needs to be created
    if (previousRouteProgress == null) {
      Position currentPosition = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
      previousRouteProgress = RouteProgress.create(directionsRoute, currentPosition,
        0, 0);
    }

    if (!TextUtils.equals(directionsRoute.getGeometry(), previousRouteProgress.getRoute().getGeometry())) {
      resetRouteProgress();
    }

    // If the locations the same as previous, no need to recalculate things
    if (location.equals(previousLocation)) {
      return;
    }

    previousLocation = location;

    if (bearingMatchesManeuverFinalHeading(
      location, previousRouteProgress, options.getMaxTurnCompletionOffset())
      && calculateSnappedDistanceToNextStep(location, previousRouteProgress) < options.getManeuverZoneRadius()) {
      increaseIndex(previousRouteProgress);
    }

    Snap snapToRoute = new SnapToRoute(previousRouteProgress, options);
    Location snappedLocation = snapToRoute.getSnappedLocation(location);

    // Create a RouteProgress.create object using the latest user location
    RouteProgress routeProgress = RouteProgress.create(directionsRoute,
      Position.fromCoordinates(snappedLocation.getLongitude(), snappedLocation.getLatitude()), legIndex, stepIndex);

    for (Milestone milestone : milestones) {
      if (milestone.isOccurring(previousRouteProgress, routeProgress)) {
        for (MilestoneEventListener listener : milestoneEventListeners) {
          // Create a new DefaultInstruction based on the current RouteProgress and Milestone identifier
          Instruction instruction = new DefaultInstruction(routeProgress, milestone.getIdentifier());
          listener.onMilestoneEvent(routeProgress, instruction.getInstruction(), milestone.getIdentifier());
        }
      }
    }

    // Determine if the user is off route or not
    UserOffRouteState userOffRouteState = new UserOffRouteState(location, routeProgress, options);
    boolean isUserOffRoute = userOffRouteState.isUserOffRoute();

    // Snap location to the route if they aren't off route and return the location object
    if (isSnapEnabled && !isUserOffRoute) {
      location = snappedLocation;
    }

    notifyOffRouteChange(isUserOffRoute, location);
    notifyProgressChange(location, routeProgress);

    previousRouteProgress = routeProgress;
  }

  private void notifyOffRouteChange(boolean isUserOffRoute, Location location) {
    // Only report user off route once.

    if (isUserOffRoute) {
      if (location.getTime() > timeIntervalSinceLastOffRoute
        + TimeUnit.SECONDS.toMillis(NavigationConstants.SECONDS_BEFORE_REROUTE)) {
        for (OffRouteListener offRouteListener : offRouteListeners) {
          offRouteListener.userOffRoute(location);
        }
        timeIntervalSinceLastOffRoute = location.getTime();
      }
    } else {
      timeIntervalSinceLastOffRoute = location.getTime();
    }
  }

  private void notifyProgressChange(Location location, RouteProgress routeProgress) {
    for (ProgressChangeListener progressChangeListener : progressChangeListeners) {
      progressChangeListener.onProgressChange(location, routeProgress);
    }
  }

  /**
   * Checks whether the user's bearing matches the next step's maneuver provided bearingAfter variable. This is one of
   * the criteria's required for the user location to be recognized as being on the next step or potentially arriving.
   *
   * @param userLocation  the location of the user
   * @param routeProgress used for getting route information
   * @return boolean true if the user location matches (using a tolerance) the final heading
   * @since 0.2.0
   */
  private static boolean bearingMatchesManeuverFinalHeading(Location userLocation, RouteProgress routeProgress,
                                                            double maxTurnCompletionOffset) {
    if (routeProgress.getCurrentLegProgress().getUpComingStep() == null) {
      return false;
    }

    // Bearings need to be normalized so when the bearingAfter is 359 and the user heading is 1, we count this as
    // within the MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION.
    double finalHeading = routeProgress.getCurrentLegProgress().getUpComingStep().getManeuver().getBearingAfter();
    double finalHeadingNormalized = MathUtils.wrap(finalHeading, 0, 360);
    double userHeadingNormalized = MathUtils.wrap(userLocation.getBearing(), 0, 360);
    return MathUtils.differenceBetweenAngles(finalHeadingNormalized, userHeadingNormalized)
      <= maxTurnCompletionOffset;
  }

  /**
   * When the user proceeds to a new step (or leg) the index needs to be increased. We determine the amount of legs
   * and steps and increase accordingly till the last step's reached.
   *
   * @param routeProgress used for getting the route index sizes
   * @since 0.2.0
   */
  private void increaseIndex(RouteProgress routeProgress) {
    // Check if we are in the last step in the current routeLeg and iterate it if needed.
    if (stepIndex >= routeProgress.getRoute().getLegs().get(routeProgress.getLegIndex()).getSteps().size() - 2
      && legIndex < routeProgress.getRoute().getLegs().size() - 1) {
      legIndex += 1;
      stepIndex = 0;
    } else {
      stepIndex += 1;
    }
  }

  /**
   * Provides the distance from the users snapped location to the next maneuver location.
   *
   * @param location      the users location
   * @param routeProgress used to get the steps geometry
   * @return distance in meters (by default)
   * @since 0.2.0
   */
  private double calculateSnappedDistanceToNextStep(Location location, RouteProgress routeProgress) {
    Position truePosition = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
    String stepGeometry = routeProgress.getRoute().getLegs().get(legIndex).getSteps().get(stepIndex).getGeometry();

    // Decode the geometry
    List<Position> coords = PolylineUtils.decode(stepGeometry, Constants.PRECISION_6);

    LineString slicedLine = TurfMisc.lineSlice(
      Point.fromCoordinates(truePosition),
      Point.fromCoordinates(coords.get(coords.size() - 1)),
      LineString.fromCoordinates(coords)
    );

    double distance = TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);

    // Prevents the distance to next step from increasing causing a previous alert level to occur again.
    if (distance > routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining()) {
      return routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
    }
    return distance;
  }

  private void resetRouteProgress() {
    legIndex = 0;
    stepIndex = 0;
  }

  public boolean isSnapEnabled() {
    return isSnapEnabled;
  }

  void setSnapEnabled(boolean snapEnabled) {
    isSnapEnabled = snapEnabled;
  }

  public MapboxNavigationOptions getOptions() {
    return options;
  }

  public void setOptions(MapboxNavigationOptions options) {
    this.options = options;
  }

  void setProgressChangeListeners(CopyOnWriteArrayList<ProgressChangeListener> progressChangeListeners) {
    this.progressChangeListeners = progressChangeListeners;
  }

  void setOffRouteListeners(CopyOnWriteArrayList<OffRouteListener> offRouteListeners) {
    this.offRouteListeners = offRouteListeners;
  }

  void setMilestoneEventListeners(CopyOnWriteArrayList<MilestoneEventListener> milestoneEventListeners) {
    this.milestoneEventListeners = milestoneEventListeners;
  }

  void setMilestones(CopyOnWriteArrayList<Milestone> milestones) {
    this.milestones = milestones;
  }
}
