package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.annotation.SuppressLint;
import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;

@SuppressLint("ParcelCreator")
public class NavigationFeedbackEvent extends NavigationStepEvent {
  private static final String NAVIGATION_FEEDBACK = "navigation.feedback";
  private final String userId;
  private String feedbackType;
  private String source;
  private String description;
  private Location[] locationsBefore;
  private Location[] locationsAfter;
  private final String feedbackId;
  private String screenshot;

  NavigationFeedbackEvent(PhoneState phoneState, @NonNull MetricsRouteProgress metricsRouteProgress) {
    super(phoneState, metricsRouteProgress);
    this.userId = phoneState.getUserId();
    this.feedbackId = phoneState.getFeedbackId();
  }

  @Override
  protected String getEventName() {
    return NAVIGATION_FEEDBACK;
  }

  public String getUserId() {
    return userId;
  }

  public String getFeedbackType() {
    return feedbackType;
  }

  public void setFeedbackType(String feedbackType) {
    this.feedbackType = feedbackType;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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
