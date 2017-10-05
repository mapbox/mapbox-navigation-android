package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.MapboxTelemetry;
import com.mapbox.services.android.telemetry.navigation.MapboxNavigationEvent;

class NavigationMetricsWrapper {
  static String sdkIdentifier;

  private NavigationMetricsWrapper() {
    // Empty private constructor for preventing initialization of this class.
  }

  static void arriveEvent(SessionState sessionState, RouteProgress routeProgress, Location location) {
    MapboxTelemetry.getInstance().pushEvent(MapboxNavigationEvent.buildArriveEvent(
      sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
      sessionState.sessionIdentifier(), location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), "unknown",
      (int) routeProgress.directionsRoute().getDistance(),
      (int) routeProgress.directionsRoute().getDuration(),
      sessionState.rerouteCount(), sessionState.startTimestamp(),
      (int) (sessionState.previousRouteDistancesCompleted() + routeProgress.distanceTraveled()),
      (int) routeProgress.distanceRemaining(), (int) routeProgress.durationRemaining(),
      sessionState.mockLocation(), null, null,
      sessionState.originalGeometry(), sessionState.originalDistance(),
      sessionState.originalDuration(), null, sessionState.currentStepCount(),
      sessionState.originalStepCount()
    ));
  }

  static void cancelEvent(SessionState sessionState, RouteProgress routeProgress, Location location) {
    MapboxTelemetry.getInstance().pushEvent(MapboxNavigationEvent.buildCancelEvent(
      sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
      sessionState.sessionIdentifier(),
      location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), "unknown",
      (int) routeProgress.directionsRoute().getDistance(),
      (int) routeProgress.directionsRoute().getDuration(),
      sessionState.rerouteCount(), sessionState.startTimestamp(),
      (int) (sessionState.previousRouteDistancesCompleted() + routeProgress.distanceTraveled()),
      (int) routeProgress.distanceRemaining(), (int) routeProgress.durationRemaining(),
      sessionState.mockLocation(),
      null, null, sessionState.originalGeometry(),
      sessionState.originalDistance(), sessionState.originalDuration(), null,
      sessionState.arrivalTimestamp(), sessionState.currentStepCount(), sessionState.originalStepCount()
    ));
  }

  static void departEvent(SessionState sessionState, RouteProgress routeProgress, Location location) {
    MapboxTelemetry.getInstance().pushEvent(MapboxNavigationEvent.buildDepartEvent(
      sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
      sessionState.sessionIdentifier(), location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), "unknown",
      (int) routeProgress.directionsRoute().getDistance(),
      (int) routeProgress.directionsRoute().getDuration(),
      sessionState.rerouteCount(), sessionState.mockLocation(), null, null,
      sessionState.originalGeometry(), sessionState.originalDistance(), sessionState.originalDuration(),
      null, sessionState.currentStepCount(), sessionState.originalStepCount(),
      (int) routeProgress.distanceTraveled(), (int) routeProgress.distanceRemaining(),
      (int) routeProgress.durationRemaining(), sessionState.startTimestamp()
    ));
  }

  static void rerouteEvent(SessionState sessionState, RouteProgress routeProgress, Location location) {
    String upcomingInstruction = "";
    String upcomingType = "";
    String upcomingModifier = "";
    String upcomingName = "";
    String previousInstruction = "";
    String previousType = "";
    String previousModifier = "";

    if (routeProgress.currentLegProgress().upComingStep() != null) {
      // TODO upcomingInstruction should be able to handle null instruction
      upcomingInstruction = routeProgress.currentLegProgress().upComingStep().getManeuver() != null ?
        routeProgress.currentLegProgress().upComingStep().getManeuver().getInstruction() : "";

      upcomingType = routeProgress.currentLegProgress().upComingStep().getManeuver() != null ?
        routeProgress.currentLegProgress().upComingStep().getManeuver().getType() : "";

      upcomingModifier = routeProgress.currentLegProgress().upComingStep().getManeuver() != null ?
        routeProgress.currentLegProgress().upComingStep().getManeuver().getModifier() : "";

      upcomingName = routeProgress.currentLegProgress().upComingStep().getName() != null ?
        routeProgress.currentLegProgress().upComingStep().getName() : "";
    }

    if (routeProgress.currentLegProgress().currentStep().getManeuver() != null) {

      previousInstruction = routeProgress.currentLegProgress().currentStep().getManeuver().getInstruction() != null ?
        routeProgress.currentLegProgress().currentStep().getManeuver().getInstruction() : "";

      previousType = routeProgress.currentLegProgress().currentStep().getManeuver().getType() != null ?
        routeProgress.currentLegProgress().currentStep().getManeuver().getType() : "";

      previousModifier = routeProgress.currentLegProgress().currentStep().getManeuver().getModifier() != null ?
        routeProgress.currentLegProgress().currentStep().getManeuver().getModifier() : "";

    }
    String previousName = routeProgress.currentLegProgress().currentStep().getName() != null ?
      routeProgress.currentLegProgress().currentStep().getName() : "";

    Location[] beforeLocations = sessionState.beforeRerouteLocations() != null ?
      (Location[]) sessionState.beforeRerouteLocations().toArray() : null;

    Location[] afterLocations = sessionState.afterRerouteLocations() != null ?
      (Location[]) sessionState.afterRerouteLocations().toArray() : null;

    MapboxTelemetry.getInstance().pushEvent(MapboxNavigationEvent.buildRerouteEvent(
      sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME, sessionState.sessionIdentifier(),
      location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), "unknown",
      (int) routeProgress.directionsRoute().getDistance(),
      (int) routeProgress.directionsRoute().getDuration(),
      sessionState.rerouteCount(), sessionState.startTimestamp(), beforeLocations, afterLocations,
      (int) sessionState.routeProgressBeforeReroute().distanceTraveled(),
      (int) sessionState.routeProgressBeforeReroute().distanceRemaining(),
      (int) sessionState.routeProgressBeforeReroute().durationRemaining(),
      (int) routeProgress.distanceRemaining(),
      (int) routeProgress.durationRemaining(),
      sessionState.secondsSinceLastReroute(), "",
      routeProgress.directionsRoute().getGeometry(), sessionState.mockLocation(),
      null, null, sessionState.originalGeometry(),
      sessionState.originalDistance(), sessionState.originalDuration(), null,
      upcomingInstruction, upcomingType, upcomingModifier, upcomingName, previousInstruction,
      previousType, previousModifier,
      previousName, (int) routeProgress.currentLegProgress().currentStep().getDistance(),
      (int) routeProgress.currentLegProgress().currentStep().getDuration(),
      (int) routeProgress.currentLegProgress().currentStepProgress().distanceRemaining(),
      (int) routeProgress.currentLegProgress().currentStepProgress().durationRemaining(),
      sessionState.currentStepCount(), sessionState.originalStepCount()
    ));
  }

  static void turnstileEvent() {
    MapboxTelemetry.getInstance().setCustomTurnstileEvent(
      MapboxNavigationEvent.buildTurnstileEvent(sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME)
    );
  }
}
