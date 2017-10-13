package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.support.annotation.Nullable;

import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.MapboxTelemetry;
import com.mapbox.services.android.telemetry.navigation.MapboxNavigationEvent;
import com.mapbox.services.android.telemetry.utils.TelemetryUtils;

import java.util.List;

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
      routeProgress.directionsRoute().distance().intValue(),
      routeProgress.directionsRoute().duration().intValue(),
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
      routeProgress.directionsRoute().distance().intValue(),
      routeProgress.directionsRoute().duration().intValue(),
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
      routeProgress.directionsRoute().distance().intValue(),
      routeProgress.directionsRoute().duration().intValue(),
      sessionState.rerouteCount(), sessionState.mockLocation(), null, null,
      sessionState.originalGeometry(), sessionState.originalDistance(), sessionState.originalDuration(),
      null, sessionState.currentStepCount(), sessionState.originalStepCount(),
      (int) routeProgress.distanceTraveled(), (int) routeProgress.distanceRemaining(),
      (int) routeProgress.durationRemaining(), sessionState.startTimestamp()
    ));
  }

  static void rerouteEvent(SessionState sessionState, RouteProgress routeProgress, Location location) {
    String upcomingInstruction = null;
    String previousInstruction = null;
    String upcomingModifier = null;
    String previousModifier = null;
    String upcomingType = null;
    String upcomingName = null;
    String previousType = null;

    if (routeProgress.currentLegProgress().upComingStep() != null) {
      upcomingName = routeProgress.currentLegProgress().upComingStep().name();
      if (routeProgress.currentLegProgress().upComingStep().maneuver() != null) {
        upcomingInstruction = routeProgress.currentLegProgress().upComingStep().maneuver().instruction();
        upcomingType = routeProgress.currentLegProgress().upComingStep().maneuver().type();
        upcomingModifier = routeProgress.currentLegProgress().upComingStep().maneuver().modifier();
      }
    }

    if (routeProgress.currentLegProgress().currentStep().maneuver() != null) {
      previousInstruction = routeProgress.currentLegProgress().currentStep().maneuver().instruction();
      previousType = routeProgress.currentLegProgress().currentStep().maneuver().type();
      previousModifier = routeProgress.currentLegProgress().currentStep().maneuver().modifier();
    }

    Location[] beforeLocations = obtainLocations(sessionState.beforeRerouteLocations());

    Location[] afterLocations = obtainLocations(sessionState.afterRerouteLocations());

    String previousName = routeProgress.currentLegProgress().currentStep().name();

    MapboxTelemetry.getInstance().pushEvent(MapboxNavigationEvent.buildRerouteEvent(
      sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME, sessionState.sessionIdentifier(),
      location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), "unknown",
      routeProgress.directionsRoute().distance().intValue(),
      routeProgress.directionsRoute().duration().intValue(),
      sessionState.rerouteCount(), sessionState.startTimestamp(), beforeLocations, afterLocations,
      (int) sessionState.routeProgressBeforeReroute().distanceTraveled(),
      (int) sessionState.routeProgressBeforeReroute().distanceRemaining(),
      (int) sessionState.routeProgressBeforeReroute().durationRemaining(),
      (int) routeProgress.distanceRemaining(),
      (int) routeProgress.durationRemaining(),
      sessionState.secondsSinceLastReroute(), TelemetryUtils.buildUUID(),
      routeProgress.directionsRoute().geometry(), sessionState.mockLocation(),
      null, null, sessionState.originalGeometry(),
      sessionState.originalDistance(), sessionState.originalDuration(), null,
      upcomingInstruction, upcomingType, upcomingModifier, upcomingName, previousInstruction,
      previousType, previousModifier,
      previousName, routeProgress.currentLegProgress().currentStep().distance().intValue(),
      routeProgress.currentLegProgress().currentStep().duration().intValue(),
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

  @Nullable
  private static Location[] obtainLocations(List<Location> rerouteLocations) {
    Location[] locations = new Location[0];
    if (rerouteLocations != null) {
      if (!rerouteLocations.isEmpty()) {
        locations = (Location[]) rerouteLocations.toArray();
      }
    }
    return locations;
  }
}
