package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;

/**
 * Class that contains step meta data
 */
class NavigationStepData {
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
  private int distanceRemaining;
  private int durationRemaining;

  NavigationStepData(@NonNull MetricsRouteProgress metricsRouteProgress) {
    this.upcomingInstruction = metricsRouteProgress.getUpcomingStepInstruction();
    this.upcomingModifier = metricsRouteProgress.getUpcomingStepModifier();
    this.upcomingName = metricsRouteProgress.getUpcomingStepName();
    this.upcomingType = metricsRouteProgress.getUpcomingStepType();
    this.previousInstruction = metricsRouteProgress.getPreviousStepInstruction();
    this.previousModifier = metricsRouteProgress.getPreviousStepModifier();
    this.previousType = metricsRouteProgress.getPreviousStepType();
    this.previousName = metricsRouteProgress.getPreviousStepName();
    this.distance = metricsRouteProgress.getCurrentStepDistance();
    this.duration = metricsRouteProgress.getCurrentStepDuration();
    this.distanceRemaining = metricsRouteProgress.getCurrentStepDistanceRemaining();
    this.durationRemaining = metricsRouteProgress.getCurrentStepDurationRemaining();
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

  int getDuration() {
    return duration;
  }

  public int getDistanceRemaining() {
    return distanceRemaining;
  }

  public int getDurationRemaining() {
    return durationRemaining;
  }
}
