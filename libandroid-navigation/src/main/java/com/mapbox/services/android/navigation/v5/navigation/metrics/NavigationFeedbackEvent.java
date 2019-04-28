package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.annotation.SuppressLint;
import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;

@SuppressLint("ParcelCreator")
class NavigationFeedbackEvent extends NavigationStepEvent {
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
  String getEventName() {
    return NAVIGATION_FEEDBACK;
  }

  String getUserId() {
    return userId;
  }

  String getFeedbackType() {
    return feedbackType;
  }

  void setFeedbackType(String feedbackType) {
    this.feedbackType = feedbackType;
  }

  String getSource() {
    return source;
  }

  void setSource(String source) {
    this.source = source;
  }

  String getDescription() {
    return description;
  }

  void setDescription(String description) {
    this.description = description;
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
