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
    updateRouteProgressSessionData(routeProgress, sessionState);

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
      sessionState.secondsSinceLastReroute(), TelemetryUtils.buildUUID(),
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

  static void feedbackEvent(SessionState sessionState, RouteProgress routeProgress, Location location,
                            String description, String feedbackType, String screenshot) {
    updateRouteProgressSessionData(routeProgress, sessionState);

    MapboxTelemetry.getInstance().pushEvent(MapboxNavigationEvent.buildFeedbackEvent(sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
          sessionState.sessionIdentifier(), location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), "unknown", (int) routeProgress.directionsRoute().getDistance(),
      (int) routeProgress.directionsRoute().getDuration(), sessionState.rerouteCount(), sessionState.startTimestamp(),
      feedbackType, beforeLocations, afterLocations, (int) sessionState.routeProgressBeforeReroute().distanceTraveled(),
      (int) sessionState.routeProgressBeforeReroute().distanceRemaining(),
      (int) sessionState.routeProgressBeforeReroute().durationRemaining(), description, TelemetryUtils.buildUUID(),
      TelemetryUtils.buildUUID(), screenshot, sessionState.mockLocation(), null, null, sessionState.originalGeometry(),
      sessionState.originalDistance(), sessionState.originalDuration(), null, upcomingInstruction, upcomingType,
      upcomingModifier, upcomingName, previousInstruction, previousType, previousModifier, previousName,
      (int) routeProgress.currentLegProgress().currentStep().getDistance(),
      (int) routeProgress.currentLegProgress().currentStep().getDuration(),
      (int) routeProgress.currentLegProgress().currentStepProgress().distanceRemaining(),
      (int) routeProgress.currentLegProgress().currentStepProgress().durationRemaining(),
      sessionState.currentStepCount(), sessionState.originalStepCount()
      )
    );
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

  private static void updateRouteProgressSessionData(RouteProgress routeProgress, SessionState sessionState) {
    upcomingName = null;
    upcomingInstruction = null;
    upcomingType = null;
    upcomingModifier = null;
    previousInstruction = null;
    previousType = null;
    previousModifier = null;

    if (routeProgress.currentLegProgress().upComingStep() != null) {
      upcomingName = routeProgress.currentLegProgress().upComingStep().getName();
      if (routeProgress.currentLegProgress().upComingStep().getManeuver() != null) {
        upcomingInstruction = routeProgress.currentLegProgress().upComingStep().getManeuver().getInstruction();
        upcomingType = routeProgress.currentLegProgress().upComingStep().getManeuver().getType();
        upcomingModifier = routeProgress.currentLegProgress().upComingStep().getManeuver().getModifier();
      }
    }

    if (routeProgress.currentLegProgress().currentStep().getManeuver() != null) {
      previousInstruction = routeProgress.currentLegProgress().currentStep().getManeuver().getInstruction();
      previousType = routeProgress.currentLegProgress().currentStep().getManeuver().getType();
      previousModifier = routeProgress.currentLegProgress().currentStep().getManeuver().getModifier();
    }

    previousName = routeProgress.currentLegProgress().currentStep().getName();

    beforeLocations = obtainLocations(sessionState.beforeRerouteLocations());

    afterLocations = obtainLocations(sessionState.afterRerouteLocations());
  }
}
