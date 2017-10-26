package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
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

  static void arriveEvent(SessionState sessionState, RouteProgress routeProgress, Location location) {
    MapboxTelemetry.getInstance().pushEvent(MapboxNavigationEvent.buildArriveEvent(
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
    ));
  }

  static void cancelEvent(SessionState sessionState, RouteProgress routeProgress, Location location) {
    MapboxTelemetry.getInstance().pushEvent(MapboxNavigationEvent.buildCancelEvent(
      sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
      sessionState.sessionIdentifier(),
      location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), routeProgress.directionsRoute().routeOptions().profile(),
      routeProgress.directionsRoute().distance().intValue(),
      routeProgress.directionsRoute().duration().intValue(),
      sessionState.rerouteCount(), sessionState.startTimestamp(),
      (int) (sessionState.previousRouteDistancesCompleted() + routeProgress.distanceTraveled()),
      (int) routeProgress.distanceRemaining(), (int) routeProgress.durationRemaining(),
      sessionState.mockLocation(),
      sessionState.originalRequestIdentifier(), sessionState.requestIdentifier(),
      sessionState.originalGeometry(),
      sessionState.originalDistance(), sessionState.originalDuration(), null,
      sessionState.arrivalTimestamp(), sessionState.currentStepCount(), sessionState.originalStepCount()
    ));
  }

  static void departEvent(SessionState sessionState, RouteProgress routeProgress, Location location) {
    MapboxTelemetry.getInstance().pushEvent(MapboxNavigationEvent.buildDepartEvent(
      sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
      sessionState.sessionIdentifier(), location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), routeProgress.directionsRoute().routeOptions().profile(),
      routeProgress.directionsRoute().distance().intValue(),
      routeProgress.directionsRoute().duration().intValue(),
      sessionState.rerouteCount(), sessionState.mockLocation(),
      sessionState.originalRequestIdentifier(), sessionState.requestIdentifier(),
      sessionState.originalGeometry(), sessionState.originalDistance(), sessionState.originalDuration(),
      null, sessionState.currentStepCount(), sessionState.originalStepCount(),
      (int) routeProgress.distanceTraveled(), (int) routeProgress.distanceRemaining(),
      (int) routeProgress.durationRemaining(), sessionState.startTimestamp()
    ));
  }

  static void rerouteEvent(SessionState sessionState, RouteProgress routeProgress, Location location) {

    updateRouteProgressSessionData(routeProgress, sessionState);

    Hashtable<String, Object> rerouteEvent = MapboxNavigationEvent.buildRerouteEvent(
      sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME, sessionState.sessionIdentifier(),
      location.getLatitude(), location.getLongitude(),
      sessionState.currentGeometry(), routeProgress.directionsRoute().routeOptions().profile(),
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
      sessionState.originalRequestIdentifier(), sessionState.requestIdentifier(), sessionState.originalGeometry(),
      sessionState.originalDistance(), sessionState.originalDuration(), null,
      upcomingInstruction, upcomingType, upcomingModifier, upcomingName, previousInstruction,
      previousType, previousModifier,
      previousName, routeProgress.currentLegProgress().currentStep().distance().intValue(),
      routeProgress.currentLegProgress().currentStep().duration().intValue(),
      (int) routeProgress.currentLegProgress().currentStepProgress().distanceRemaining(),
      (int) routeProgress.currentLegProgress().currentStepProgress().durationRemaining(),
      sessionState.currentStepCount(), sessionState.originalStepCount());
    rerouteEvent.put(MapboxNavigationEvent.KEY_CREATED, TelemetryUtils.generateCreateDate(location));
    MapboxTelemetry.getInstance().pushEvent(rerouteEvent);
  }

  static void feedbackEvent(SessionState sessionState, RouteProgress routeProgress, Location location,
                            String description, String feedbackType, String screenshot) {
    updateRouteProgressSessionData(routeProgress, sessionState);


    MapboxTelemetry.getInstance().pushEvent(MapboxNavigationEvent.buildFeedbackEvent(sdkIdentifier,
      BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME, sessionState.sessionIdentifier(), location.getLatitude(),
      location.getLongitude(), sessionState.currentGeometry(), routeProgress.directionsRoute().routeOptions().profile(),
      routeProgress.directionsRoute().distance().intValue(), routeProgress.directionsRoute().duration().intValue(),
      sessionState.rerouteCount(), sessionState.startTimestamp(), feedbackType, beforeLocations, afterLocations,
      (int) sessionState.routeProgressBeforeReroute().distanceTraveled(),
      (int) sessionState.routeProgressBeforeReroute().distanceRemaining(),
      (int) sessionState.routeProgressBeforeReroute().durationRemaining(), description, TelemetryUtils.buildUUID(),
      TelemetryUtils.buildUUID(), screenshot, sessionState.mockLocation(), sessionState.originalRequestIdentifier(),
      sessionState.requestIdentifier(), sessionState.originalGeometry(), sessionState.originalDistance(),
      sessionState.originalDuration(), null, upcomingInstruction, upcomingType, upcomingModifier, upcomingName,
      previousInstruction, previousType, previousModifier, previousName,
      routeProgress.currentLegProgress().currentStep().distance().intValue(),
      routeProgress.currentLegProgress().currentStep().duration().intValue(),
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

  private static Location[] obtainLocations(List<Location> locations) {
    // Check if the list of locations is empty and if so return an empty array
    if (locations == null || locations.isEmpty()) {
      return new Location[0];
    }
    return locations.toArray(new Location[locations.size()]);
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

    // Check if location update happened before or after the reroute
    List<Location> afterLoc = new ArrayList<>();

    if (sessionState.afterRerouteLocations() != null) {
      for (Location loc : sessionState.afterRerouteLocations()) {
        if (loc.getTime() > sessionState.rerouteDate().getTime()) {
          afterLoc.add(loc);
        }
      }
    }

    previousName = routeProgress.currentLegProgress().currentStep().name();

    beforeLocations = obtainLocations(sessionState.beforeRerouteLocations());

    afterLocations = obtainLocations(afterLoc);
  }
}
