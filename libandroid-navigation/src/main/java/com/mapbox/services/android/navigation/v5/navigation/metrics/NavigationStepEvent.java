package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;

/**
 * Class that contains step meta data
 */
abstract class NavigationStepEvent extends NavigationEvent {
  private final String upcomingInstruction;
  private final String upcomingType;
  private final String upcomingModifier;
  private final String upcomingName;
  private final String previousInstruction;
  private final String previousType;
  private final String previousModifier;
  private final String previousName;
  private int distance;
  private int duration;

  NavigationStepEvent(@NonNull PhoneState phoneState, @NonNull MetricsRouteProgress metricsRouteProgress) {
    super(phoneState);
    this.upcomingInstruction = metricsRouteProgress.getUpcomingStepInstruction();
    this.upcomingModifier = metricsRouteProgress.getUpcomingStepModifier();
    this.upcomingName = metricsRouteProgress.getUpcomingStepName();
    this.upcomingType = metricsRouteProgress.getUpcomingStepType();
    this.previousInstruction = metricsRouteProgress.getPreviousStepInstruction();
    this.previousModifier = metricsRouteProgress.getPreviousStepModifier();
    this.previousType = metricsRouteProgress.getPreviousStepType();
    this.previousName = metricsRouteProgress.getPreviousStepName();
  }

  String getUpcomingInstruction() {
    return upcomingInstruction;
  }

  String getUpcomingType() {
    return upcomingType;
  }

  String getUpcomingModifier() {
    return upcomingModifier;
  }

  String getUpcomingName() {
    return upcomingName;
  }

  String getPreviousInstruction() {
    return previousInstruction;
  }

  String getPreviousType() {
    return previousType;
  }

  String getPreviousModifier() {
    return previousModifier;
  }

  String getPreviousName() {
    return previousName;
  }

  int getDistance() {
    return distance;
  }

  void setDistance(int distance) {
    this.distance = distance;
  }

  int getDuration() {
    return duration;
  }

  void setDuration(int duration) {
    this.duration = duration;
  }
}
