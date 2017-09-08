package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.MapboxTelemetry;
import com.mapbox.services.android.telemetry.navigation.MapboxNavigationEvent;
import com.mapbox.services.android.telemetry.utils.TelemetryUtils;

class NavigationMetricsWrapper {

  private static final String UUID = TelemetryUtils.buildUUID();

  static void arriveEvent(RouteProgress routeProgress, Location location) {
    MapboxTelemetry.getInstance().pushEvent(MapboxNavigationEvent.buildArriveEvent(
      BuildConfig.APPLICATION_ID, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME, UUID,
      location.getLatitude(), location.getLongitude(),
      routeProgress.directionsRoute().getGeometry(), null,
      (int) routeProgress.directionsRoute().getDistance(),
      (int) routeProgress.directionsRoute().getDuration(),
      -1, null, (int) routeProgress.distanceTraveled(), false,
      null, null, null,
      -1, -1, null, -1,
      -1
    ));
  }

  static void cancelEvent(RouteProgress routeProgress, Location location) {
    MapboxTelemetry.getInstance().pushEvent(MapboxNavigationEvent.buildCancelEvent(
      BuildConfig.APPLICATION_ID, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME, UUID,
      location.getLatitude(), location.getLongitude(),
      routeProgress.directionsRoute().getGeometry(), null,
      (int) routeProgress.directionsRoute().getDistance(),
      (int) routeProgress.directionsRoute().getDuration(),
      -1, null, (int) routeProgress.distanceTraveled(),
      (int) routeProgress.distanceRemaining(), (int) routeProgress.durationRemaining(),
      false,
      null, null, null,
      -1, -1, null, null,
      -1, -1
    ));
  }

  static void departEvent(RouteProgress routeProgress, Location location) {

    MapboxTelemetry.getInstance().pushEvent(MapboxNavigationEvent.buildDepartEvent(
      BuildConfig.APPLICATION_ID, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME, UUID,
      location.getLatitude(), location.getLongitude(),
      routeProgress.directionsRoute().getGeometry(), null,
      (int) routeProgress.directionsRoute().getDistance(),
      (int) routeProgress.directionsRoute().getDuration(),
      -1, false, null, null,
      null, -1, -1,
      null, -1, -1
    ));
  }

  static void turnstileEvent() {
    MapboxTelemetry.getInstance().setCustomTurnstileEvent(
      MapboxNavigationEvent.buildTurnstileEvent(BuildConfig.APPLICATION_ID,
        BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME)
    );
  }


}
