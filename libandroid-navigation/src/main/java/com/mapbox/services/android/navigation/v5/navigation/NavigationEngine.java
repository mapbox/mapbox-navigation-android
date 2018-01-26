package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RingBuffer;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.util.List;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.bearingMatchesManeuverFinalHeading;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.checkMilestones;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.getSnappedLocation;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.increaseIndex;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.isUserOffRoute;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.legDistanceRemaining;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.routeDistanceRemaining;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.stepDistanceRemaining;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.userSnappedToRoutePosition;
import static com.mapbox.core.constants.Constants.PRECISION_6;

/**
 * This class extends handler thread to run most of the navigation calculations on a separate
 * background thread.
 */
class NavigationEngine {

  private static final String THREAD_NAME = "NavThread";
  private RouteProgress previousRouteProgress;
  private List<Point> stepPositions;
  private NavigationIndices indices;

    private final RouteStateWrapper mRouteStateWrapper = new RouteStateWrapper();
    class RouteStateWrapper {
        Location mCurrentLocation;
        RouteProgress mRouteProgress;
        List<Milestone> mMilestones;
        boolean mIsUserOffRoute;

        private void setProgress(Location location, RouteProgress routeProgress, List<Milestone> milestones, boolean userOffRoute) {
            mCurrentLocation = location;
            mRouteProgress = routeProgress;
            mMilestones = milestones;
            mIsUserOffRoute = userOffRoute;
        }
    }

    NavigationEngine() {
    indices = NavigationIndices.create(0, 0);
  }

  RouteStateWrapper handleRequest(final NewLocationModel newLocationModel) {
    final RouteProgress routeProgress = generateNewRouteProgress(
      newLocationModel.mapboxNavigation(), newLocationModel.location(),
      newLocationModel.recentDistancesFromManeuverInMeters());
    final List<Milestone> milestones = checkMilestones(
      previousRouteProgress, routeProgress, newLocationModel.mapboxNavigation());
    final boolean userOffRoute = isUserOffRoute(newLocationModel, routeProgress);
    final Location location = !userOffRoute && newLocationModel.mapboxNavigation().options().snapToRoute()
      ? getSnappedLocation(newLocationModel.mapboxNavigation(), newLocationModel.location(),
      routeProgress, stepPositions)
      : newLocationModel.location();

    previousRouteProgress = routeProgress;

    mRouteStateWrapper.setProgress(location, routeProgress, milestones, userOffRoute) ;

    return mRouteStateWrapper;
  }

  private RouteProgress generateNewRouteProgress(MapboxNavigation mapboxNavigation, Location location,
                                                 RingBuffer recentDistances) {
    DirectionsRoute directionsRoute = mapboxNavigation.getRoute();
    MapboxNavigationOptions options = mapboxNavigation.options();

    if (RouteUtils.isNewRoute(previousRouteProgress, directionsRoute)) {
      // Decode the first steps geometry and hold onto the resulting Position objects till the users
      // on the next step. Indices are both 0 since the user just started on the new route.
      stepPositions = PolylineUtils.decode(
        directionsRoute.legs().get(0).steps().get(0).geometry(), PRECISION_6);

      previousRouteProgress = RouteProgress.builder()
        .stepDistanceRemaining(directionsRoute.legs().get(0).steps().get(0).distance())
        .legDistanceRemaining(directionsRoute.legs().get(0).distance())
        .distanceRemaining(directionsRoute.distance())
        .directionsRoute(directionsRoute)
        .stepIndex(0)
        .legIndex(0)
        .build();

      indices = NavigationIndices.create(0, 0);
    }

    Point snappedPosition = userSnappedToRoutePosition(location, stepPositions);
    double stepDistanceRemaining = stepDistanceRemaining(
      snappedPosition, indices.legIndex(), indices.stepIndex(), directionsRoute, stepPositions);
    double legDistanceRemaining = legDistanceRemaining(
      stepDistanceRemaining, indices.legIndex(), indices.stepIndex(), directionsRoute);
    double routeDistanceRemaining = routeDistanceRemaining(
      legDistanceRemaining, indices.legIndex(), directionsRoute);

    if (bearingMatchesManeuverFinalHeading(location, previousRouteProgress, options.maxTurnCompletionOffset())
      && stepDistanceRemaining < options.maneuverZoneRadius()) {
      // First increase the indices and then update the majority of information for the new
      // routeProgress.
      indices = increaseIndex(previousRouteProgress, indices);
      stepPositions = PolylineUtils.decode(
        directionsRoute.legs().get(
          indices.legIndex()).steps().get(indices.stepIndex()).geometry(), PRECISION_6);
      snappedPosition = userSnappedToRoutePosition(location, stepPositions);
      stepDistanceRemaining = stepDistanceRemaining(
        snappedPosition, indices.legIndex(), indices.stepIndex(), directionsRoute, stepPositions);
      legDistanceRemaining = legDistanceRemaining(
        stepDistanceRemaining, indices.legIndex(), indices.stepIndex(), directionsRoute);
      routeDistanceRemaining = routeDistanceRemaining(
        legDistanceRemaining, indices.legIndex(), directionsRoute);

      // Remove all distance values from recentDistancesFromManeuverInMeters
      recentDistances.clear();
    }

    // Create a RouteProgress.create object using the latest user location
    return RouteProgress.builder()
      .stepDistanceRemaining(stepDistanceRemaining)
      .legDistanceRemaining(legDistanceRemaining)
      .distanceRemaining(routeDistanceRemaining)
      .directionsRoute(directionsRoute)
      .stepIndex(indices.stepIndex())
      .legIndex(indices.legIndex())
      .build();
  }

  /**
   * Callbacks for posting back to the Navigation Service once the thread finishes calculations.
   * No matter what, with each new message added to the queue, these callbacks get invoked once
   * finished and within Navigation Service it is determined if the public corresponding listeners
   * need invoking or not; the Navigation event dispatcher class handles those callbacks.
   */
  interface Callback {
    void onNewRouteProgress(Location location, RouteProgress routeProgress);

    void onMilestoneTrigger(List<Milestone> triggeredMilestones, RouteProgress routeProgress);

    void onUserOffRoute(Location location, boolean userOffRoute);
  }
}
