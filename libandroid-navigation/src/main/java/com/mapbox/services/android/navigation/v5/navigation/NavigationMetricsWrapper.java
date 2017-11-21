package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.navigation.metrics.RerouteEvent;
import com.mapbox.services.android.navigation.v5.navigation.metrics.SessionState;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;
import com.mapbox.services.android.telemetry.MapboxTelemetry;
import com.mapbox.services.android.telemetry.navigation.MapboxNavigationEvent;
import com.mapbox.services.android.telemetry.utils.TelemetryUtils;

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

  private NavigationMetricsWrapper() {
    // Empty private constructor for preventing initialization of this class.
  }

  static void arriveEvent(SessionState sessionState, RouteProgress routeProgress, Location location) {
    Hashtable<String, Object> arriveEvent = MapboxNavigationEvent.buildArriveEvent(
      sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
      sessionState.sessionIdentifier(), location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), routeProgress.directionsRoute().routeOptions().profile(),
      routeProgress.directionsRoute().distance().intValue(),
      routeProgress.directionsRoute().duration().intValue(),
      sessionState.rerouteCount(), sessionState.startTimestamp(),
      (int) (sessionState.eventRouteDistanceCompleted() + routeProgress.distanceTraveled()),
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
    MapboxTelemetry.getInstance().addLocationEngineName(sessionState.locationEngineName(), arriveEvent);
    MapboxTelemetry.getInstance().pushEvent(arriveEvent);
    MapboxTelemetry.getInstance().flushEventsQueueImmediately(false);
  }

  static void cancelEvent(SessionState sessionState, MetricsRouteProgress routeProgress, Location location) {
    Hashtable<String, Object> cancelEvent = MapboxNavigationEvent.buildCancelEvent(
      sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
      sessionState.sessionIdentifier(),
      location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), routeProgress.getDirectionsRouteProfile(),
      routeProgress.getDirectionsRouteDistance(),
      routeProgress.getDirectionsRouteDuration(),
      sessionState.rerouteCount(), sessionState.startTimestamp(),
      (int) (sessionState.eventRouteDistanceCompleted() + routeProgress.getDistanceTraveled()),
      (int) routeProgress.getDistanceRemaining(), (int) routeProgress.getDurationRemaining(),
      sessionState.mockLocation(),
      sessionState.originalRequestIdentifier(), sessionState.requestIdentifier(),
      sessionState.originalGeometry(),
      sessionState.originalDistance(), sessionState.originalDuration(), null,
      sessionState.arrivalTimestamp(), sessionState.currentStepCount(), sessionState.originalStepCount()
    );

    int absoluteDistance = DistanceUtils.calculateAbsoluteDistance(location, routeProgress);

    MapboxTelemetry.getInstance().addAbsoluteDistanceToDestination(absoluteDistance, cancelEvent);
    MapboxTelemetry.getInstance().addLocationEngineName(sessionState.locationEngineName(), cancelEvent);
    MapboxTelemetry.getInstance().pushEvent(cancelEvent);
    MapboxTelemetry.getInstance().flushEventsQueueImmediately(false);
  }

  static void departEvent(SessionState sessionState, MetricsRouteProgress routeProgress, Location location) {
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
    MapboxTelemetry.getInstance().addLocationEngineName(sessionState.locationEngineName(), departEvent);
    MapboxTelemetry.getInstance().pushEvent(departEvent);
    MapboxTelemetry.getInstance().flushEventsQueueImmediately(false);
  }

  static void rerouteEvent(RerouteEvent rerouteEvent, MetricsRouteProgress routeProgress,
                           Location location) {

    SessionState sessionState = rerouteEvent.getSessionState();
    updateRouteProgressSessionData(routeProgress);

    Hashtable<String, Object> buildRerouteEvent = MapboxNavigationEvent.buildRerouteEvent(
      sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME, sessionState.sessionIdentifier(),
      location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), routeProgress.getDirectionsRouteProfile(),
      routeProgress.getDirectionsRouteDistance(),
      routeProgress.getDirectionsRouteDuration(),
      sessionState.rerouteCount(), sessionState.startTimestamp(),
      convertToArray(sessionState.beforeEventLocations()),
      convertToArray(sessionState.afterEventLocations()),
      (int) sessionState.eventRouteDistanceCompleted(), // distanceCompleted
      (int) sessionState.eventRouteProgress().getDistanceRemaining(), // distanceRemaining
      (int) sessionState.eventRouteProgress().getDurationRemaining(), // durationRemaining
      rerouteEvent.getNewDistanceRemaining(), // new distanceRemaining
      rerouteEvent.getNewDurationRemaining(), // new durationRemaining
      sessionState.secondsSinceLastReroute(), TelemetryUtils.buildUUID(),
      rerouteEvent.getNewRouteGeometry(), sessionState.mockLocation(),
      sessionState.originalRequestIdentifier(), sessionState.requestIdentifier(), sessionState.originalGeometry(),
      sessionState.originalDistance(), sessionState.originalDuration(), null,
      upcomingInstruction, upcomingType, upcomingModifier, upcomingName, previousInstruction,
      previousType, previousModifier,
      previousName, routeProgress.getCurrentStepDistance(),
      routeProgress.getCurrentStepDuration(),
      (int) routeProgress.getCurrentStepProgressDistanceRemaining(),
      (int) routeProgress.getCurrentStepProgressDurationRemaining(),
      sessionState.currentStepCount(), sessionState.originalStepCount());
    buildRerouteEvent.put(MapboxNavigationEvent.KEY_CREATED, TelemetryUtils.generateCreateDate(location));

    int absoluteDistance = DistanceUtils.calculateAbsoluteDistance(location, routeProgress);

    MapboxTelemetry.getInstance().addAbsoluteDistanceToDestination(absoluteDistance, buildRerouteEvent);
    MapboxTelemetry.getInstance().addLocationEngineName(sessionState.locationEngineName(), buildRerouteEvent);
    MapboxTelemetry.getInstance().pushEvent(buildRerouteEvent);
    MapboxTelemetry.getInstance().flushEventsQueueImmediately(false);
  }

  static void feedbackEvent(SessionState sessionState, MetricsRouteProgress routeProgress, Location location,
                            String description, String feedbackType, String screenshot, String feedbackId,
                            String vendorId) {

    updateRouteProgressSessionData(routeProgress);

    Hashtable<String, Object> feedbackEvent = MapboxNavigationEvent.buildFeedbackEvent(sdkIdentifier,
      BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME, sessionState.sessionIdentifier(), location.getLatitude(),
      location.getLongitude(), sessionState.currentGeometry(), routeProgress.getDirectionsRouteProfile(),
      routeProgress.getDirectionsRouteDistance(), routeProgress.getDirectionsRouteDuration(),
      sessionState.rerouteCount(), sessionState.startTimestamp(), feedbackType,
      convertToArray(sessionState.beforeEventLocations()),
      convertToArray(sessionState.afterEventLocations()),
      (int) sessionState.eventRouteDistanceCompleted(),
      (int) sessionState.eventRouteProgress().getDistanceRemaining(),
      (int) sessionState.eventRouteProgress().getDurationRemaining(),
      description, vendorId, feedbackId, screenshot,
      sessionState.mockLocation(), sessionState.originalRequestIdentifier(),
      sessionState.requestIdentifier(), sessionState.originalGeometry(),
      sessionState.originalDistance(), sessionState.originalDuration(), null,
      upcomingInstruction, upcomingType, upcomingModifier, upcomingName,
      previousInstruction, previousType, previousModifier, previousName,
      routeProgress.getCurrentStepDistance(),
      routeProgress.getCurrentStepDuration(),
      (int) routeProgress.getCurrentStepProgressDistanceRemaining(),
      (int) routeProgress.getCurrentStepProgressDurationRemaining(),
      sessionState.currentStepCount(), sessionState.originalStepCount()
    );

    int absoluteDistance = DistanceUtils.calculateAbsoluteDistance(location, routeProgress);

    MapboxTelemetry.getInstance().addAbsoluteDistanceToDestination(absoluteDistance, feedbackEvent);
    feedbackEvent.put(MapboxNavigationEvent.KEY_CREATED, TelemetryUtils.generateCreateDate(location));
    MapboxTelemetry.getInstance().addLocationEngineName(sessionState.locationEngineName(), feedbackEvent);
    MapboxTelemetry.getInstance().pushEvent(feedbackEvent);
    MapboxTelemetry.getInstance().flushEventsQueueImmediately(false);
  }

  static void turnstileEvent() {
    MapboxTelemetry.getInstance().setCustomTurnstileEvent(
      MapboxNavigationEvent.buildTurnstileEvent(sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME)
    );
  }

  private static void updateRouteProgressSessionData(MetricsRouteProgress routeProgress) {
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

    previousName = routeProgress.getCurrentStepName();
  }

  private static Location[] convertToArray(List<Location> locationList) {
    return locationList.toArray(new Location[locationList.size()]);
  }
}
