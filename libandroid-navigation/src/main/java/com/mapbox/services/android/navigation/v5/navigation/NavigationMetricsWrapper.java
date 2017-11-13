package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;
import com.mapbox.services.android.telemetry.MapboxTelemetry;
import com.mapbox.services.android.telemetry.navigation.MapboxNavigationEvent;
import com.mapbox.services.android.telemetry.utils.TelemetryUtils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

final class NavigationMetricsWrapper {
  static String sdkIdentifier;
  private static String upcomingInstruction;
  private static String previousInstruction;
  private static String upcomingModifier;
  private static String previousModifier;
  private static String upcomingType;
  private static String upcomingName;
  private static String previousType;
  private static String previousName;
  private static Location[] beforeLocations;
  private static Location[] afterLocations;

  private NavigationMetricsWrapper() {
    // Empty private constructor for preventing initialization of this class.
  }

  static void arriveEvent(SessionState sessionState, RouteProgress routeProgress, Location location,
                          String locationEngineName) {
    Hashtable<String, Object> arriveEvent = MapboxNavigationEvent.buildArriveEvent(
      sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
      sessionState.sessionIdentifier(), location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), routeProgress.directionsRoute().routeOptions().profile(),
      routeProgress.directionsRoute().distance().intValue(),
      routeProgress.directionsRoute().duration().intValue(),
      sessionState.rerouteCount(), sessionState.startTimestamp(),
      (int) (sessionState.previousRouteDistancesCompleted() + routeProgress.distanceTraveled()),
      (int) routeProgress.distanceRemaining(), (int) routeProgress.durationRemaining(),
      sessionState.mockLocation(), sessionState.originalRequestIdentifier(),
      sessionState.requestIdentifier(),
      sessionState.originalGeometry(), sessionState.originalDistance(),
      sessionState.originalDuration(), null, sessionState.currentStepCount(),
      sessionState.originalStepCount()
    );

    MetricsRouteProgress metricsRouteProgress = new MetricsRouteProgress(routeProgress);
    int absoluteDistance = DistanceUtils.calculateAbsoluteDistance(location, metricsRouteProgress);

    MapboxTelemetry.getInstance().addAbsoluteDistanceToDestination(absoluteDistance, arriveEvent);
    MapboxTelemetry.getInstance().addLocationEngineName(locationEngineName, arriveEvent);
    MapboxTelemetry.getInstance().pushEvent(arriveEvent);
    MapboxTelemetry.getInstance().flushEventsQueueImmediately(false);
  }

  static void cancelEvent(SessionState sessionState, MetricsRouteProgress routeProgress, Location location,
                          String locationEngineName) {
    Hashtable<String, Object> cancelEvent = MapboxNavigationEvent.buildCancelEvent(
      sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
      sessionState.sessionIdentifier(),
      location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), routeProgress.getDirectionsRouteProfile(),
      routeProgress.getDirectionsRouteDistance(),
      routeProgress.getDirectionsRouteDuration(),
      sessionState.rerouteCount(), sessionState.startTimestamp(),
      (int) (sessionState.previousRouteDistancesCompleted() + routeProgress.getDistanceTraveled()),
      (int) routeProgress.getDistanceRemaining(), (int) routeProgress.getDurationRemaining(),
      sessionState.mockLocation(),
      sessionState.originalRequestIdentifier(), sessionState.requestIdentifier(),
      sessionState.originalGeometry(),
      sessionState.originalDistance(), sessionState.originalDuration(), null,
      sessionState.arrivalTimestamp(), sessionState.currentStepCount(), sessionState.originalStepCount()
    );

    int absoluteDistance = DistanceUtils.calculateAbsoluteDistance(location, routeProgress);

    MapboxTelemetry.getInstance().addAbsoluteDistanceToDestination(absoluteDistance, cancelEvent);
    MapboxTelemetry.getInstance().addLocationEngineName(locationEngineName, cancelEvent);
    MapboxTelemetry.getInstance().pushEvent(cancelEvent);
    MapboxTelemetry.getInstance().flushEventsQueueImmediately(false);
  }

  static void departEvent(SessionState sessionState, MetricsRouteProgress routeProgress, Location location,
                          String locationEngineName) {
    Hashtable<String, Object> departEvent = MapboxNavigationEvent.buildDepartEvent(
      sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
      sessionState.sessionIdentifier(), location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), routeProgress.getDirectionsRouteProfile(),
      routeProgress.getDirectionsRouteDistance(),
      routeProgress.getDirectionsRouteDuration(),
      sessionState.rerouteCount(), sessionState.mockLocation(),
      sessionState.originalRequestIdentifier(), sessionState.requestIdentifier(),
      sessionState.originalGeometry(), sessionState.originalDistance(), sessionState.originalDuration(),
      null, sessionState.currentStepCount(), sessionState.originalStepCount(),
      (int) routeProgress.getDistanceTraveled(), (int) routeProgress.getDistanceRemaining(),
      (int) routeProgress.getDurationRemaining(), sessionState.startTimestamp()
    );

    int absoluteDistance = DistanceUtils.calculateAbsoluteDistance(location, routeProgress);

    MapboxTelemetry.getInstance().addAbsoluteDistanceToDestination(absoluteDistance, departEvent);
    MapboxTelemetry.getInstance().addLocationEngineName(locationEngineName, departEvent);
    MapboxTelemetry.getInstance().pushEvent(departEvent);
    MapboxTelemetry.getInstance().flushEventsQueueImmediately(false);
  }

  static void rerouteEvent(SessionState sessionState, MetricsRouteProgress routeProgress, Location location,
                           String locationEngineName) {
    updateRouteProgressSessionData(routeProgress, sessionState);

    Hashtable<String, Object> rerouteEvent = MapboxNavigationEvent.buildRerouteEvent(
      sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME, sessionState.sessionIdentifier(),
      location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), routeProgress.getDirectionsRouteProfile(),
      routeProgress.getDirectionsRouteDistance(),
      routeProgress.getDirectionsRouteDuration(),
      sessionState.rerouteCount(), sessionState.startTimestamp(), beforeLocations, afterLocations,
      (int) (sessionState.previousRouteDistancesCompleted()
        + sessionState.routeProgressBeforeReroute().getDistanceTraveled()),
      (int) sessionState.routeProgressBeforeReroute().getDistanceRemaining(),
      (int) sessionState.routeProgressBeforeReroute().getDurationRemaining(),
      (int) routeProgress.getDistanceRemaining(),
      (int) routeProgress.getDurationRemaining(),
      sessionState.secondsSinceLastReroute(), TelemetryUtils.buildUUID(),
      routeProgress.getDirectionsRouteGeometry(), sessionState.mockLocation(),
      sessionState.originalRequestIdentifier(), sessionState.requestIdentifier(), sessionState.originalGeometry(),
      sessionState.originalDistance(), sessionState.originalDuration(), null,
      upcomingInstruction, upcomingType, upcomingModifier, upcomingName, previousInstruction,
      previousType, previousModifier,
      previousName, routeProgress.getCurrentStepDistance(),
      routeProgress.getCurrentStepDuration(),
      (int) routeProgress.getCurrentStepProgressDistanceRemaining(),
      (int) routeProgress.getCurrentStepProgressDurationRemaining(),
      sessionState.currentStepCount(), sessionState.originalStepCount());
    rerouteEvent.put(MapboxNavigationEvent.KEY_CREATED, TelemetryUtils.generateCreateDate(location));

    int absoluteDistance = DistanceUtils.calculateAbsoluteDistance(location, routeProgress);

    MapboxTelemetry.getInstance().addAbsoluteDistanceToDestination(absoluteDistance, rerouteEvent);
    MapboxTelemetry.getInstance().addLocationEngineName(locationEngineName, rerouteEvent);
    MapboxTelemetry.getInstance().pushEvent(rerouteEvent);
    MapboxTelemetry.getInstance().flushEventsQueueImmediately(false);
  }

  static void feedbackEvent(SessionState sessionState, MetricsRouteProgress routeProgress, Location location,
                            String description, String feedbackType, String screenshot, String feedbackId,
                            String vendorId, String locationEngineName) {
    updateRouteProgressSessionData(routeProgress, sessionState);

    Hashtable<String, Object> feedbackEvent = MapboxNavigationEvent.buildFeedbackEvent(sdkIdentifier,
      BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME, sessionState.sessionIdentifier(), location.getLatitude(),
      location.getLongitude(), sessionState.currentGeometry(), routeProgress.getDirectionsRouteProfile(),
      routeProgress.getDirectionsRouteDistance(), routeProgress.getDirectionsRouteDuration(),
      sessionState.rerouteCount(), sessionState.startTimestamp(), feedbackType, beforeLocations, afterLocations,
      (int) (sessionState.previousRouteDistancesCompleted()
        + sessionState.routeProgressBeforeReroute().getDistanceTraveled()),
      (int) sessionState.routeProgressBeforeReroute().getDistanceRemaining(),
      (int) sessionState.routeProgressBeforeReroute().getDurationRemaining(), description, vendorId,
      feedbackId, screenshot, sessionState.mockLocation(), sessionState.originalRequestIdentifier(),
      sessionState.requestIdentifier(), sessionState.originalGeometry(), sessionState.originalDistance(),
      sessionState.originalDuration(), null, upcomingInstruction, upcomingType, upcomingModifier, upcomingName,
      previousInstruction, previousType, previousModifier, previousName, routeProgress.getCurrentStepDistance(),
      routeProgress.getCurrentStepDuration(),
      (int) routeProgress.getCurrentStepProgressDistanceRemaining(),
      (int) routeProgress.getCurrentStepProgressDurationRemaining(),
      sessionState.currentStepCount(), sessionState.originalStepCount()
    );

    int absoluteDistance = DistanceUtils.calculateAbsoluteDistance(location, routeProgress);

    MapboxTelemetry.getInstance().addAbsoluteDistanceToDestination(absoluteDistance, feedbackEvent);
    feedbackEvent.put(MapboxNavigationEvent.KEY_CREATED, TelemetryUtils.generateCreateDate(location));
    MapboxTelemetry.getInstance().addLocationEngineName(locationEngineName, feedbackEvent);
    MapboxTelemetry.getInstance().pushEvent(feedbackEvent);
    MapboxTelemetry.getInstance().flushEventsQueueImmediately(false);
  }

  static void turnstileEvent() {
    MapboxTelemetry.getInstance().setCustomTurnstileEvent(
      MapboxNavigationEvent.buildTurnstileEvent(sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME)
    );
  }

  private static Location[] obtainLocations(List<Location> locations) {
    // Check if the list of locations is empty and if so return an empty array
    if (locations == null || locations.isEmpty()) {
      return new Location[0];
    }
    return locations.toArray(new Location[locations.size()]);
  }

  private static void updateRouteProgressSessionData(MetricsRouteProgress routeProgress, SessionState sessionState) {
    upcomingName = null;
    upcomingInstruction = null;
    upcomingType = null;
    upcomingModifier = null;
    previousInstruction = null;
    previousType = null;
    previousModifier = null;

    if (routeProgress.getCurrentLegProgress().upComingStep() != null) {
      upcomingName = routeProgress.getCurrentLegProgress().upComingStep().name();
      if (routeProgress.getCurrentLegProgress().upComingStep().maneuver() != null) {
        upcomingInstruction = routeProgress.getCurrentLegProgress().upComingStep().maneuver().instruction();
        upcomingType = routeProgress.getCurrentLegProgress().upComingStep().maneuver().type();
        upcomingModifier = routeProgress.getCurrentLegProgress().upComingStep().maneuver().modifier();
      }
    }

    if (routeProgress.getCurrentLegProgress().currentStep().maneuver() != null) {
      previousInstruction = routeProgress.getCurrentLegProgress().currentStep().maneuver().instruction();
      previousType = routeProgress.getCurrentLegProgress().currentStep().maneuver().type();
      previousModifier = routeProgress.getCurrentLegProgress().currentStep().maneuver().modifier();
    }

    // Check if location update happened before or after the reroute
    List<Location> afterLoc = new ArrayList<>();

    if (sessionState.afterRerouteLocations() != null) {
      for (Location loc : sessionState.afterRerouteLocations()) {
        if (loc.getTime() > sessionState.rerouteDate().getTime()) {
          afterLoc.add(loc);
        }
      }
    }

    previousName = routeProgress.getCurrentStepName();

    beforeLocations = obtainLocations(sessionState.beforeRerouteLocations());

    afterLocations = obtainLocations(afterLoc);
  }
}
