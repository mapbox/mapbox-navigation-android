package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.annotation.SuppressLint;
import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;

@SuppressLint("ParcelCreator")
public class NavigationRerouteEvent extends NavigationEvent {
  private static final String NAVIGATION_REROUTE = "navigation.reroute";
  private final int newDistanceRemaining;
  private final int newDurationRemaining;
  private final String feedbackId;
  private final String newGeometry;
  private int secondsSinceLastReroute;
  private Location[] locationsBefore;
  private Location[] locationsAfter;
  private String screenshot;
  private NavigationStepData step;

  NavigationRerouteEvent(@NonNull PhoneState phoneState, @NonNull RerouteEvent rerouteEvent,
                         @NonNull MetricsRouteProgress metricsRouteProgress) {
    super(phoneState);
    this.newDistanceRemaining = rerouteEvent.getNewDistanceRemaining();
    this.newDurationRemaining = rerouteEvent.getNewDurationRemaining();
    this.newGeometry = rerouteEvent.getNewRouteGeometry();
    this.feedbackId = phoneState.getFeedbackId();
    this.step = new NavigationStepData(metricsRouteProgress);
  }

  @Override
  String getEventName() {
    return NAVIGATION_REROUTE;
  }

  NavigationStepData getStep() {
    return step;
  }

  int getNewDistanceRemaining() {
    return newDistanceRemaining;
  }

  int getNewDurationRemaining() {
    return newDurationRemaining;
  }

  String getNewGeometry() {
    return newGeometry;
  }

  int getSecondsSinceLastReroute() {
    return secondsSinceLastReroute;
  }

  void setSecondsSinceLastReroute(int secondsSinceLastReroute) {
    this.secondsSinceLastReroute = secondsSinceLastReroute;
  }

  Location[] getLocationsBefore() {
    return locationsBefore;
  }

  void setLocationsBefore(Location[] locationsBefore) {
    this.locationsBefore = locationsBefore;
  }

  Location[] getLocationsAfter() {
    return locationsAfter;
  }

  void setLocationsAfter(Location[] locationsAfter) {
    this.locationsAfter = locationsAfter;
  }

  String getFeedbackId() {
    return feedbackId;
  }

  String getScreenshot() {
    return screenshot;
  }

  void setScreenshot(String screenshot) {
    this.screenshot = screenshot;
  }
}
