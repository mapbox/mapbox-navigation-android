package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.support.annotation.Nullable;

import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.MapboxTelemetry;
import com.mapbox.services.android.telemetry.navigation.MapboxNavigationEvent;
import com.mapbox.services.android.telemetry.utils.TelemetryUtils;

import java.util.Hashtable;
import java.util.List;

class NavigationMetricsWrapper {
  static String sdkIdentifier;

  private NavigationMetricsWrapper() {
    // Empty private constructor for preventing initialization of this class.
  }

  static void arriveEvent(SessionState sessionState, RouteProgress routeProgress, Location location,
                          String locationEngineName) {
    Hashtable<String, Object> arriveEvent = MapboxNavigationEvent.buildArriveEvent(
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
    );
    MapboxTelemetry.getInstance().addLocationEngineName(locationEngineName, arriveEvent);
    MapboxTelemetry.getInstance().pushEvent(arriveEvent);
  }

  static void cancelEvent(SessionState sessionState, RouteProgress routeProgress, Location location,
                          String locationEngineName) {
    Hashtable<String, Object> cancelEvent = MapboxNavigationEvent.buildCancelEvent(
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
    );
    MapboxTelemetry.getInstance().addLocationEngineName(locationEngineName, cancelEvent);
    MapboxTelemetry.getInstance().pushEvent(cancelEvent);
  }

  static void departEvent(SessionState sessionState, RouteProgress routeProgress, Location location,
                          String locationEngineName) {
    Hashtable<String, Object> departEvent = MapboxNavigationEvent.buildDepartEvent(
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
    );
    MapboxTelemetry.getInstance().addLocationEngineName(locationEngineName, departEvent);
    MapboxTelemetry.getInstance().pushEvent(departEvent);
  }

  static void rerouteEvent(SessionState sessionState, RouteProgress routeProgress, Location location,
                           String locationEngineName) {
    String upcomingInstruction = null;
    String previousInstruction = null;
    String upcomingModifier = null;
    String previousModifier = null;
    String upcomingType = null;
    String upcomingName = null;
    String previousType = null;

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

    Location[] beforeLocations = obtainLocations(sessionState.beforeRerouteLocations());

    Location[] afterLocations = obtainLocations(sessionState.afterRerouteLocations());

    String previousName = routeProgress.currentLegProgress().currentStep().getName();

    Hashtable<String, Object> rerouteEvent = MapboxNavigationEvent.buildRerouteEvent(
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
    );
    MapboxTelemetry.getInstance().addLocationEngineName(locationEngineName, rerouteEvent);
    MapboxTelemetry.getInstance().pushEvent(rerouteEvent);
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
