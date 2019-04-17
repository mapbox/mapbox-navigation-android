package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;

/**
 * Class that contains step meta data
 */
public abstract class NavigationStepEvent extends NavigationEvent {
  private String upcomingInstruction;
  private String upcomingType;
  private String upcomingModifier;
  private String upcomingName;
  private String previousInstruction;
  private String previousType;
  private String previousModifier;
  private String previousName;
  private int distance;
  private int duration;
  private int distanceRemaining;
  private int durationRemaining;

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
    this.distance = metricsRouteProgress.getCurrentStepDistance();
    this.duration = metricsRouteProgress.getCurrentStepDuration();
    this.distanceRemaining = metricsRouteProgress.getDistanceRemaining();
    this.durationRemaining = metricsRouteProgress.getDurationRemaining();
  }

  public String getUpcomingInstruction() {
    return upcomingInstruction;
  }

  public void setUpcomingInstruction(String upcomingInstruction) {
    this.upcomingInstruction = upcomingInstruction;
  }

  public String getUpcomingType() {
    return upcomingType;
  }

  public void setUpcomingType(String upcomingType) {
    this.upcomingType = upcomingType;
  }

  public String getUpcomingModifier() {
    return upcomingModifier;
  }

  public void setUpcomingModifier(String upcomingModifier) {
    this.upcomingModifier = upcomingModifier;
  }

  public String getUpcomingName() {
    return upcomingName;
  }

  public void setUpcomingName(String upcomingName) {
    this.upcomingName = upcomingName;
  }

  public String getPreviousInstruction() {
    return previousInstruction;
  }

  public void setPreviousInstruction(String previousInstruction) {
    this.previousInstruction = previousInstruction;
  }

  public String getPreviousType() {
    return previousType;
  }

  public void setPreviousType(String previousType) {
    this.previousType = previousType;
  }

  public String getPreviousModifier() {
    return previousModifier;
  }

  public void setPreviousModifier(String previousModifier) {
    this.previousModifier = previousModifier;
  }

  public String getPreviousName() {
    return previousName;
  }

  public void setPreviousName(String previousName) {
    this.previousName = previousName;
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

  @Override
  public int getDistanceRemaining() {
    return distanceRemaining;
  }

  @Override
  public void setDistanceRemaining(int distanceRemaining) {
    this.distanceRemaining = distanceRemaining;
  }

  @Override
  public int getDurationRemaining() {
    return durationRemaining;
  }

  @Override
  public void setDurationRemaining(int durationRemaining) {
    this.durationRemaining = durationRemaining;
  }
}
