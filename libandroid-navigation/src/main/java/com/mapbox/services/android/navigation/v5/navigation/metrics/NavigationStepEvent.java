package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;

/**
 * Class that contains step meta data
 */
public abstract class NavigationStepEvent extends NavigationEvent {
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

  public String getUpcomingInstruction() {
    return upcomingInstruction;
  }

  public String getUpcomingType() {
    return upcomingType;
  }

  public String getUpcomingModifier() {
    return upcomingModifier;
  }

  public String getUpcomingName() {
    return upcomingName;
  }

  public String getPreviousInstruction() {
    return previousInstruction;
  }

  public String getPreviousType() {
    return previousType;
  }

  public String getPreviousModifier() {
    return previousModifier;
  }

  public String getPreviousName() {
    return previousName;
  }

  public int getDistance() {
    return distance;
  }

  public void setDistance(int distance) {
    this.distance = distance;
  }

  public int getDuration() {
    return duration;
  }

  public void setDuration(int duration) {
    this.duration = duration;
  }
}
