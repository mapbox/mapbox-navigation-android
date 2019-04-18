package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.annotation.SuppressLint;
import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;

@SuppressLint("ParcelCreator")
public class NavigationRerouteEvent extends NavigationStepEvent {
  private static final String NAVIGATION_REROUTE = "navigation.reroute";
  private final int newDistanceRemaining;
  private final int newDurationRemaining;
  private final String feedbackId;
  private final String newGeometry;
  private int secondsSinceLastReroute;
  private Location[] locationsBefore;
  private Location[] locationsAfter;
  private String screenshot;

  NavigationRerouteEvent(@NonNull PhoneState phoneState, @NonNull RerouteEvent rerouteEvent,
                         @NonNull MetricsRouteProgress metricsRouteProgress) {
    super(phoneState, metricsRouteProgress);
    this.newDistanceRemaining = rerouteEvent.getNewDistanceRemaining();
    this.newDurationRemaining = rerouteEvent.getNewDurationRemaining();
    this.newGeometry = rerouteEvent.getNewRouteGeometry();
    this.feedbackId = phoneState.getFeedbackId();
  }

  @Override
  protected String getEventName() {
    return NAVIGATION_REROUTE;
  }

  public int getNewDistanceRemaining() {
    return newDistanceRemaining;
  }

  public int getNewDurationRemaining() {
    return newDurationRemaining;
  }

  public String getNewGeometry() {
    return newGeometry;
  }

  public int getSecondsSinceLastReroute() {
    return secondsSinceLastReroute;
  }

  public void setSecondsSinceLastReroute(int secondsSinceLastReroute) {
    this.secondsSinceLastReroute = secondsSinceLastReroute;
  }

  public Location[] getLocationsBefore() {
    return locationsBefore;
  }

  public void setLocationsBefore(Location[] locationsBefore) {
    this.locationsBefore = locationsBefore;
  }

  public Location[] getLocationsAfter() {
    return locationsAfter;
  }

  public void setLocationsAfter(Location[] locationsAfter) {
    this.locationsAfter = locationsAfter;
  }

  public String getFeedbackId() {
    return feedbackId;
  }

  public String getScreenshot() {
    return screenshot;
  }

  public void setScreenshot(String screenshot) {
    this.screenshot = screenshot;
  }
}
