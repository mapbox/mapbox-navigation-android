package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.offroute.OffRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteStepProgress;
import com.mapbox.services.android.navigation.v5.snap.Snap;
import com.mapbox.services.android.telemetry.utils.MathUtils;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.api.utils.turf.TurfMisc;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.commons.utils.PolylineUtils;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.services.Constants.PRECISION_6;

class NavigationEngine extends HandlerThread {

  private RouteProgress previousRouteProgress;
  private Location previousLocation;
  private int stepIndex;
  private int legIndex;
  private Handler responseHandler;
  private Handler workerHandler;
  private Callback callback;

  NavigationEngine(String name, int priority, Handler responseHandler, Callback callback) {
    super(name, priority);
    this.responseHandler = responseHandler;
    this.callback = callback;
    stepIndex = 0;
    legIndex = 0;
  }

  void queueTask(int msgIdentifier, NewLocationModel newLocationModel) {
    workerHandler.obtainMessage(msgIdentifier, newLocationModel)
      .sendToTarget();
  }

  void prepareHandler() {
    workerHandler = new Handler(getLooper(), new Handler.Callback() {
      @Override
      public boolean handleMessage(Message msg) {
        NewLocationModel newLocationModel = (NewLocationModel) msg.obj;
        handleRequest(newLocationModel);
//        msg.recycle();
        return true;
      }
    });
  }

  private void handleRequest(final NewLocationModel newLocationModel) {

    final RouteProgress routeProgress = generateNewRouteProgress(newLocationModel.mapboxNavigation(), newLocationModel.location());
    final List<Milestone> milestones = checkMilestones(routeProgress, newLocationModel.mapboxNavigation());
    final boolean userOffRoute = isUserOffRoute(newLocationModel, routeProgress);
    final Location location = !userOffRoute && newLocationModel.mapboxNavigation().options().snapToRoute() ?
      getSnappedLocation(newLocationModel.mapboxNavigation(), newLocationModel.location(), routeProgress)
      : newLocationModel.location();

    responseHandler.post(new Runnable() {
      @Override
      public void run() {
        callback.onNewRouteProgress(location, routeProgress);
        callback.onMilestoneTrigger(milestones, routeProgress);
        callback.onUserOffRoute(location, userOffRoute);
      }
    });
  }

  private boolean validLocationUpdate(Location location) {
    // If the locations the same as previous, no need to recalculate things
    if (location.equals(previousLocation) || location.getSpeed() <= 0) {
      return false;
    }
    // TODO filter out terrible location accuracy
    return true;
  }

  private RouteProgress generateNewRouteProgress(MapboxNavigation mapboxNavigation, Location location) {
    DirectionsRoute directionsRoute = mapboxNavigation.getRoute();
    MapboxNavigationOptions options = mapboxNavigation.options();

    if (previousRouteProgress == null) {

      RouteLeg firstLeg = directionsRoute.getLegs().get(0);
      LegStep firstStep = firstLeg.getSteps().get(0);

      RouteLegProgress routeLegProgress = RouteLegProgress.builder()
        .currentStepProgress(RouteStepProgress.create(firstStep, firstStep.getDistance()))
        .legDistanceRemaining(firstLeg.getDistance())
        .routeLeg(firstLeg)
        .stepIndex(0)
        .build();

      previousRouteProgress = RouteProgress.builder()
        .directionsRoute(directionsRoute)
        .distanceRemaining(directionsRoute.getDistance())
        .currentLegProgress(routeLegProgress)
        .location(location)
        .legIndex(0)
        .build();
    }

    if (!TextUtils.equals(directionsRoute.getGeometry(), previousRouteProgress.directionsRoute().getGeometry())) {
      resetRouteProgress();
    }

    if (!validLocationUpdate(location)) {
      return previousRouteProgress;
    }

    previousLocation = location;

    if (bearingMatchesManeuverFinalHeading(location, previousRouteProgress, options.maxTurnCompletionOffset())
      && calculateSnappedDistanceToNextStep(location, previousRouteProgress) < options.maneuverZoneRadius()) {
      increaseIndex(previousRouteProgress);
    }

    RouteStepProgress routeStepProgress = RouteStepProgress.create(
      directionsRoute.getLegs().get(legIndex).getSteps().get(stepIndex),
      getStepDistanceRemaining(location, directionsRoute));

    RouteLegProgress routeLegProgress = RouteLegProgress.builder()
      .routeLeg(directionsRoute.getLegs().get(legIndex))
      .currentStepProgress(routeStepProgress)
      .legDistanceRemaining(getLegDistanceRemaining(location, directionsRoute))
      .stepIndex(stepIndex)
      .build();

    // Create a RouteProgress.create object using the latest user location
    RouteProgress routeProgress = RouteProgress.builder()
      .directionsRoute(directionsRoute)
      .distanceRemaining(getRouteDistanceRemaining(directionsRoute, location))
      .location(location)
      .currentLegProgress(routeLegProgress)
      .legIndex(legIndex)
      .build();


    previousRouteProgress = routeProgress;
    return routeProgress;
  }

  private List<Milestone> checkMilestones(RouteProgress routeProgress, MapboxNavigation mapboxNavigation) {
    List<Milestone> milestones = new ArrayList<>();
    for (Milestone milestone : mapboxNavigation.getMilestones()) {
      if (milestone.isOccurring(previousRouteProgress, routeProgress)) {
        milestones.add(milestone);
      }
    }
    return milestones;
  }

  private Location getSnappedLocation(MapboxNavigation mapboxNavigation, Location location, RouteProgress routeProgress) {
    Snap snap = mapboxNavigation.getSnapEngine();
    return snap.getSnappedLocation(location, routeProgress);
  }

  private boolean isUserOffRoute(NewLocationModel newLocationModel, RouteProgress routeProgress) {
    OffRoute offRoute = newLocationModel.mapboxNavigation().getOffRouteEngine();
    return offRoute.isUserOffRoute(newLocationModel.location(), routeProgress,
      newLocationModel.mapboxNavigation().options());
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
    if (routeProgress.currentLegProgress().getUpComingStep() == null) {
      return false;
    }

    // Bearings need to be normalized so when the bearingAfter is 359 and the user heading is 1, we count this as
    // within the MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION.
    double finalHeading = routeProgress.currentLegProgress().getUpComingStep().getManeuver().getBearingAfter();
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
    if (stepIndex >= routeProgress.directionsRoute().getLegs().get(routeProgress.getLegIndex()).getSteps().size() - 2
      && legIndex < routeProgress.directionsRoute().getLegs().size() - 1) {
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
    String stepGeometry = routeProgress.directionsRoute().getLegs().get(legIndex).getSteps().get(stepIndex).getGeometry();

    // Decode the geometry
    List<Position> coords = PolylineUtils.decode(stepGeometry, Constants.PRECISION_6);

    LineString slicedLine = TurfMisc.lineSlice(
      Point.fromCoordinates(truePosition),
      Point.fromCoordinates(coords.get(coords.size() - 1)),
      LineString.fromCoordinates(coords)
    );

    double distance = TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);

    // Prevents the distance to next step from increasing causing a previous alert level to occur again.
    if (distance > routeProgress.currentLegProgress().getCurrentStepProgress().getDistanceRemaining()) {
      return routeProgress.currentLegProgress().getCurrentStepProgress().getDistanceRemaining();
    }
    return distance;
  }

  private void resetRouteProgress() {
    legIndex = 0;
    stepIndex = 0;
  }

  // TODO use upcoming maneuver instead of decoding route?
  private double getRouteDistanceRemaining(DirectionsRoute directionsRoute, Location location) {
    double distanceRemaining = 0;
    Position snappedPosition = userSnappedToRoutePosition(location, legIndex, stepIndex, directionsRoute);
    List<Position> coords = PolylineUtils.decode(directionsRoute.getLegs().get(legIndex).getSteps().get(stepIndex).getGeometry(),
      Constants.PRECISION_6);
    if (coords.size() > 1 && !snappedPosition.equals(coords.get(coords.size() - 1))) {
      LineString slicedLine = TurfMisc.lineSlice(
        Point.fromCoordinates(snappedPosition),
        Point.fromCoordinates(coords.get(coords.size() - 1)),
        LineString.fromCoordinates(coords)
      );
      distanceRemaining += TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);
    }

    for (int i = stepIndex + 1; i < directionsRoute.getLegs().get(legIndex).getSteps().size(); i++) {
      distanceRemaining += directionsRoute.getLegs().get(legIndex).getSteps().get(i).getDistance();
    }

    // Add any additional leg distances the user hasn't navigated to yet.
    if (directionsRoute.getLegs().size() - 1 > legIndex) {
      for (int i = legIndex + 1; i < directionsRoute.getLegs().size(); i++) {
        distanceRemaining += directionsRoute.getLegs().get(i).getDistance();
      }
    }
    return distanceRemaining;
  }

  // Always get the closest position on the route to the actual
  // raw location so that can accurately calculate values.
  private static Position userSnappedToRoutePosition(Location location, int legIndex, int stepIndex,
                                                     DirectionsRoute route) {
    Point locationToPoint = Point.fromCoordinates(
      new double[] {location.getLongitude(), location.getLatitude()}
    );

    // Decode the geometry
    List<Position> coords = PolylineUtils.decode(route.getLegs().get(legIndex).getSteps().get(stepIndex).getGeometry(),
      PRECISION_6);

    // Uses Turf's pointOnLine, which takes a Point and a LineString to calculate the closest
    // Point on the LineString.
    Feature feature = TurfMisc.pointOnLine(locationToPoint, coords);
    return ((Point) feature.getGeometry()).getCoordinates();
  }

  private double getStepDistanceRemaining(Location location, DirectionsRoute directionsRoute) {
    double distanceRemaining = 0;
    Position snappedPosition = userSnappedToRoutePosition(location, legIndex, stepIndex, directionsRoute);

    // Decode the geometry
    List<Position> coords = PolylineUtils.decode(directionsRoute.getLegs().get(legIndex).getSteps().get(stepIndex)
      .getGeometry(), Constants.PRECISION_6);

    if (coords.size() > 1 && !snappedPosition.equals(coords.get(coords.size() - 1))) {
      LineString slicedLine = TurfMisc.lineSlice(
        Point.fromCoordinates(snappedPosition),
        Point.fromCoordinates(coords.get(coords.size() - 1)),
        LineString.fromCoordinates(coords)
      );
      distanceRemaining = TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);
    }
    return distanceRemaining;
  }

  private double getLegDistanceRemaining(Location location, DirectionsRoute directionsRoute) {
    double distanceRemaining = 0;
    Position snappedPosition = userSnappedToRoutePosition(location, legIndex, stepIndex, directionsRoute);

    List<Position> coords = PolylineUtils.decode(directionsRoute.getLegs().get(legIndex).getSteps().get(stepIndex)
      .getGeometry(), Constants.PRECISION_6);
    if (coords.size() > 1 && !snappedPosition.equals(coords.get(coords.size() - 1))) {
      LineString slicedLine = TurfMisc.lineSlice(
        Point.fromCoordinates(snappedPosition),
        Point.fromCoordinates(coords.get(coords.size() - 1)),
        LineString.fromCoordinates(coords)
      );
      distanceRemaining += TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);
    }
    for (int i = stepIndex + 1; i < directionsRoute.getLegs().get(legIndex).getSteps().size(); i++) {
      distanceRemaining += directionsRoute.getLegs().get(legIndex).getSteps().get(i).getDistance();
    }

    return distanceRemaining;
  }

  interface Callback {
    void onNewRouteProgress(Location location, RouteProgress routeProgress);

    void onMilestoneTrigger(List<Milestone> triggeredMilestones, RouteProgress routeProgress);

    void onUserOffRoute(Location location, boolean userOffRoute);
  }
}
