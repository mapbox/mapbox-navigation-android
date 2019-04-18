package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.android.telemetry.TelemetryUtils;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;

import java.util.Date;
import java.util.List;

public class NavigationEventFactory {
  static final int EVENT_VERSION = 7;

  private NavigationEventFactory() {

  }

  public static NavigationDepartEvent buildNavigationDepartEvent(@NonNull PhoneState phoneState,
                                                                 @NonNull SessionState sessionState,
                                                                 @NonNull MetricsRouteProgress metricProgress,
                                                                 @NonNull Location location,
                                                                 @NonNull String sdkIdentifier) {
    NavigationDepartEvent navigationDepartEvent = new NavigationDepartEvent(phoneState);
    setEvent(sessionState, metricProgress, location, sdkIdentifier, navigationDepartEvent);
    return navigationDepartEvent;
  }

  public static NavigationCancelEvent buildNavigationCancelEvent(@NonNull PhoneState phoneState,
                                                                 @NonNull SessionState sessionState,
                                                                 @NonNull MetricsRouteProgress metricProgress,
                                                                 @NonNull Location location,
                                                                 @NonNull String sdkIdentifier) {

    NavigationCancelEvent navigationCancelEvent = new NavigationCancelEvent(phoneState);
    setEvent(sessionState, metricProgress, location, sdkIdentifier, navigationCancelEvent);
    String arrivalTimestamp = TelemetryUtils.generateCreateDateFormatted(sessionState.arrivalTimestamp());
    navigationCancelEvent.setArrivalTimestamp(arrivalTimestamp);
    return navigationCancelEvent;
  }

  public static NavigationArriveEvent buildNavigationArriveEvent(@NonNull PhoneState phoneState,
                                                                 @NonNull SessionState sessionState,
                                                                 @NonNull MetricsRouteProgress metricProgress,
                                                                 @NonNull Location location,
                                                                 @NonNull String sdkIdentifier) {
    NavigationArriveEvent navigationArriveEvent = new NavigationArriveEvent(phoneState);
    setEvent(sessionState, metricProgress, location, sdkIdentifier, navigationArriveEvent);
    return navigationArriveEvent;
  }


  public static NavigationRerouteEvent buildNavigationRerouteEvent(@NonNull PhoneState phoneState,
                                                                   @NonNull SessionState sessionState,
                                                                   @NonNull MetricsRouteProgress metricProgress,
                                                                   @NonNull Location location,
                                                                   @NonNull String sdkIdentifier,
                                                                   @NonNull RerouteEvent rerouteEvent) {
    NavigationRerouteEvent navigationRerouteEvent =
      new NavigationRerouteEvent(phoneState, rerouteEvent, metricProgress);
    setEvent(sessionState, metricProgress, location, sdkIdentifier, navigationRerouteEvent);
    List<Location> locationList = sessionState.beforeEventLocations();
    navigationRerouteEvent.setLocationsBefore(convertToArray(locationList));
    navigationRerouteEvent.setLocationsAfter(convertToArray(sessionState.afterEventLocations()));
    navigationRerouteEvent.setSecondsSinceLastReroute(sessionState.secondsSinceLastReroute());
    return navigationRerouteEvent;
  }


  public static NavigationFeedbackEvent buildNavigationFeedbackEvent(@NonNull PhoneState phoneState,
                                                                     @NonNull SessionState sessionState,
                                                                     @NonNull MetricsRouteProgress metricProgress,
                                                                     @NonNull Location location,
                                                                     @NonNull String sdkIdentifier,
                                                                     String description, String feedbackType,
                                                                     String screenshot, String feedbackSource) {
    NavigationFeedbackEvent navigationFeedbackEvent = new NavigationFeedbackEvent(phoneState, metricProgress);
    setEvent(sessionState, metricProgress, location, sdkIdentifier, navigationFeedbackEvent);
    navigationFeedbackEvent.setLocationsBefore(convertToArray(sessionState.beforeEventLocations()));
    navigationFeedbackEvent.setLocationsAfter(convertToArray(sessionState.afterEventLocations()));
    navigationFeedbackEvent.setDescription(description);
    navigationFeedbackEvent.setFeedbackType(feedbackType);
    navigationFeedbackEvent.setScreenshot(screenshot);
    navigationFeedbackEvent.setSource(feedbackSource);
    return navigationFeedbackEvent;
  }

  private static void setEvent(SessionState sessionState, MetricsRouteProgress metricProgress, Location location,
                               String sdkIdentifier, NavigationEvent navigationEvent) {
    navigationEvent
      .setAbsoluteDistanceToDestination(DistanceFormatter.calculateAbsoluteDistance(location, metricProgress));
    navigationEvent
      .setDistanceCompleted((int) (sessionState.eventRouteDistanceCompleted() + metricProgress.getDistanceTraveled()));
    navigationEvent.setDistanceRemaining(metricProgress.getDistanceRemaining());
    navigationEvent.setDurationRemaining(metricProgress.getDurationRemaining());
    navigationEvent.setProfile(metricProgress.getDirectionsRouteProfile());
    navigationEvent.setLegIndex(metricProgress.getLegIndex());
    navigationEvent.setLegCount(metricProgress.getLegCount());
    navigationEvent.setStepIndex(metricProgress.getStepIndex());
    navigationEvent.setStepCount(metricProgress.getStepCount());
    navigationEvent.setEstimatedDistance(metricProgress.getDirectionsRouteDistance());
    navigationEvent.setEstimatedDuration(metricProgress.getDirectionsRouteDuration());
    navigationEvent.setStartTimestamp(obtainStartTimestamp(sessionState));
    navigationEvent.setEventVersion(EVENT_VERSION);
    navigationEvent.setSdkIdentifier(sdkIdentifier);
    navigationEvent.setSessionIdentifier(sessionState.sessionIdentifier());
    navigationEvent.setLat(location.getLatitude());
    navigationEvent.setLng(location.getLongitude());
    navigationEvent.setGeometry(sessionState.currentGeometry());
    navigationEvent.setSimulation(sessionState.mockLocation());
    navigationEvent.setLocationEngine(sessionState.locationEngineName());
    navigationEvent.setTripIdentifier(sessionState.tripIdentifier());
    navigationEvent.setRerouteCount(sessionState.rerouteCount());
    navigationEvent.setOriginalRequestIdentifier(sessionState.originalRequestIdentifier());
    navigationEvent.setRequestIdentifier(sessionState.requestIdentifier());
    navigationEvent.setOriginalGeometry(sessionState.originalGeometry());
    navigationEvent.setOriginalEstimatedDistance(sessionState.originalDistance());
    navigationEvent.setOriginalEstimatedDuration(sessionState.originalDuration());
    navigationEvent.setOriginalStepCount(sessionState.originalStepCount());
    navigationEvent.setPercentTimeInForeground(sessionState.percentInForeground());
    navigationEvent.setPercentTimeInPortrait(sessionState.percentInPortrait());
    navigationEvent.setTotalStepCount(sessionState.currentStepCount());
  }

  private static String obtainStartTimestamp(SessionState sessionState) {
    Date date;
    if (sessionState.startTimestamp() == null) {
      date = new Date();
    } else {
      date = sessionState.startTimestamp();
    }
    return TelemetryUtils.generateCreateDateFormatted(date);
  }

  private static Location[] convertToArray(List<Location> locationList) {
    if (locationList == null) {
      return new Location[0];
    }
    return locationList.toArray(new Location[0]);
  }
}
