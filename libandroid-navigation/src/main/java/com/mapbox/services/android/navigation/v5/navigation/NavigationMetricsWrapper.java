package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;
import android.location.Location;

import com.mapbox.android.telemetry.AppUserTurnstile;
import com.mapbox.android.telemetry.Event;
import com.mapbox.android.telemetry.MapboxTelemetry;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.navigation.metrics.NavigationEventFactory;
import com.mapbox.services.android.navigation.v5.navigation.metrics.NavigationRerouteEvent;
import com.mapbox.services.android.navigation.v5.navigation.metrics.PhoneState;
import com.mapbox.services.android.navigation.v5.navigation.metrics.RerouteEvent;
import com.mapbox.services.android.navigation.v5.navigation.metrics.SessionState;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

final class NavigationMetricsWrapper {

  static String sdkIdentifier;
  // TODO Where are we going to create MapboxTelemetry instance? Which class is going to hold it?
  private static MapboxTelemetry mapboxTelemetry;

  private NavigationMetricsWrapper() {
    // Empty private constructor for preventing initialization of this class.
  }

  static void init(Context context, String accessToken, String userAgent) {
    mapboxTelemetry = new MapboxTelemetry(context, accessToken, userAgent);
    mapboxTelemetry.enable();
  }

  static void toggleLogging(boolean isDebugLoggingEnabled) {
    mapboxTelemetry.updateDebugLoggingEnabled(isDebugLoggingEnabled);
  }

  static void disable() {
    if (mapboxTelemetry != null) {
      mapboxTelemetry.disable();
    }
  }

  static void push(Event event) {
    mapboxTelemetry.push(event);
  }

  static void arriveEvent(SessionState sessionState, RouteProgress routeProgress, Location location, Context context) {
    MetricsRouteProgress metricsRouteProgress = new MetricsRouteProgress(routeProgress);
    Event arriveEvent = NavigationEventFactory
      .buildNavigationArriveEvent(new PhoneState(context), sessionState, metricsRouteProgress, location, sdkIdentifier);
    mapboxTelemetry.push(arriveEvent);
  }

  static void cancelEvent(SessionState sessionState, MetricsRouteProgress metricProgress, Location location,
                          Context context) {
    Event cancelEvent = NavigationEventFactory
      .buildNavigationCancelEvent(new PhoneState(context), sessionState, metricProgress, location, sdkIdentifier);
    mapboxTelemetry.push(cancelEvent);
  }

  static void departEvent(SessionState sessionState, MetricsRouteProgress metricsRouteProgress, Location location,
                          Context context) {
    Event departEvent = NavigationEventFactory
      .buildNavigationDepartEvent(new PhoneState(context), sessionState, metricsRouteProgress, location, sdkIdentifier);
    mapboxTelemetry.push(departEvent);
  }

  static void rerouteEvent(RerouteEvent rerouteEvent, MetricsRouteProgress metricProgress,
                           Location location, Context context) {
    SessionState sessionState = rerouteEvent.getSessionState();
    NavigationRerouteEvent navRerouteEvent = NavigationEventFactory.buildNavigationRerouteEvent(
      new PhoneState(context), sessionState, metricProgress, location, sdkIdentifier, rerouteEvent);
    mapboxTelemetry.push(navRerouteEvent);
  }

  static void feedbackEvent(SessionState sessionState, MetricsRouteProgress metricProgress, Location location,
                            String description, String feedbackType, String screenshot, String feedbackSource,
                            Context context) {
    Event feedbackEvent = NavigationEventFactory.buildNavigationFeedbackEvent(
      new PhoneState(context), sessionState, metricProgress, location, sdkIdentifier, description, feedbackType,
      screenshot, feedbackSource);
    mapboxTelemetry.push(feedbackEvent);
  }

  static void routeRetrievalEvent(double elapsedTime, String routeUuid,
                                  String sessionId, NavigationPerformanceMetadata metadata) {
    push(new RouteRetrievalEvent(elapsedTime, routeUuid, sessionId, metadata));
  }

  static void sendInitialGpsEvent(double elapsedTime, String sessionId) {
    push(new InitialGpsEvent(elapsedTime, sessionId));
  }

  static Event turnstileEvent() {
    return new AppUserTurnstile(sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME);
  }

}
