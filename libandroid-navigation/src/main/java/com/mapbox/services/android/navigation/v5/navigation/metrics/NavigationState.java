package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.content.Context;

public class NavigationState {
  private NavigationMetadata navigationMetadata;
  private NavigationStepMetadata navigationStepMetadata;
  private NavigationCancelData navigationCancelData;
  private NavigationLocationData navigationLocationData;
  private NavigationRerouteData navigationRerouteData;
  private FeedbackEventData feedbackEventData;
  private FeedbackData feedbackData;

  @Deprecated
  public NavigationState(NavigationMetadata navigationMetadata) {
    this.navigationMetadata = navigationMetadata;
  }

  public static NavigationState create(NavigationMetadata navigationMetadata, Context context) {
    return new NavigationState(navigationMetadata.setDeviceInfo(context));
  }

  NavigationMetadata getNavigationMetadata() {
    return navigationMetadata;
  }

  NavigationStepMetadata getNavigationStepMetadata() {
    return navigationStepMetadata;
  }

  public void setNavigationStepMetadata(NavigationStepMetadata navigationStepMetadata) {
    this.navigationStepMetadata = navigationStepMetadata;
  }

  NavigationCancelData getNavigationCancelData() {
    return navigationCancelData;
  }

  public void setNavigationCancelData(NavigationCancelData navigationCancelData) {
    this.navigationCancelData = navigationCancelData;
  }

  NavigationLocationData getNavigationLocationData() {
    return navigationLocationData;
  }

  public void setNavigationLocationData(NavigationLocationData navigationLocationData) {
    this.navigationLocationData = navigationLocationData;
  }

  NavigationRerouteData getNavigationRerouteData() {
    return navigationRerouteData;
  }

  public void setNavigationRerouteData(NavigationRerouteData navigationRerouteData) {
    this.navigationRerouteData = navigationRerouteData;
  }

  FeedbackEventData getFeedbackEventData() {
    return feedbackEventData;
  }

  public void setFeedbackEventData(FeedbackEventData feedbackEventData) {
    this.feedbackEventData = feedbackEventData;
  }

  FeedbackData getFeedbackData() {
    return feedbackData;
  }

  public void setFeedbackData(FeedbackData feedbackData) {
    this.feedbackData = feedbackData;
  }
}