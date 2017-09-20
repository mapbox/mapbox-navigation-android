package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.MapboxTelemetry;
import com.mapbox.services.android.telemetry.navigation.MapboxNavigationEvent;

import java.util.Date;

class NavigationMetricsWrapper {

  private NavigationMetricsWrapper() {
    // private constructor to prevent initializing this class
  }

  static void arriveEvent(SessionState sessionState, RouteProgress routeProgress, Location location) {
    MapboxTelemetry.getInstance().pushEvent(MapboxNavigationEvent.buildArriveEvent(
      BuildConfig.MAPBOX_NAVIGATION_SDK_IDENTIFIER, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
      sessionState.sessionIdentifier(), location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), sessionState.profile(),
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
      BuildConfig.MAPBOX_NAVIGATION_SDK_IDENTIFIER, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
      sessionState.sessionIdentifier(),
      location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), sessionState.profile(),
      (int) routeProgress.directionsRoute().getDistance(),
      (int) routeProgress.directionsRoute().getDuration(),
      sessionState.rerouteCount(), sessionState.startTimestamp(),
      (int) (sessionState.previousRouteDistancesCompleted() + routeProgress.distanceTraveled()),
      (int) routeProgress.distanceRemaining(), (int) routeProgress.durationRemaining(),
      sessionState.mockLocation(),
      null, null, sessionState.originalGeometry(),
      sessionState.originalDistance(), sessionState.originalDuration(), null,
      new Date(), sessionState.currentStepCount(), sessionState.originalStepCount()
    ));
  }

  static void departEvent(SessionState sessionState, RouteProgress routeProgress, Location location) {
    MapboxTelemetry.getInstance().pushEvent(MapboxNavigationEvent.buildDepartEvent(
      BuildConfig.MAPBOX_NAVIGATION_SDK_IDENTIFIER, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
      sessionState.sessionIdentifier(), location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), sessionState.profile(),
      (int) routeProgress.directionsRoute().getDistance(),
      (int) routeProgress.directionsRoute().getDuration(),
      sessionState.rerouteCount(), sessionState.mockLocation(), null, null,
      sessionState.originalGeometry(), sessionState.originalDistance(), sessionState.originalDuration(),
      null, sessionState.currentStepCount(), sessionState.originalStepCount(),
      (int) routeProgress.distanceTraveled(), (int) routeProgress.distanceRemaining(),
      (int) routeProgress.durationRemaining(), sessionState.startTimestamp()
    ));
  }

  static void turnstileEvent() {
    MapboxTelemetry.getInstance().setCustomTurnstileEvent(
      MapboxNavigationEvent.buildTurnstileEvent(BuildConfig.APPLICATION_ID,
        BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME)
    );
  }
}
