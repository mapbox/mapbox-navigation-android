package com.mapbox.services.android.navigation.v5.milestone;

import android.text.TextUtils;

import com.mapbox.directions.v5.models.VoiceInstructions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

public class ApiMilestone extends Milestone {

  private String announcement;
  private List<VoiceInstructions> stepVoiceInstructions;

  public ApiMilestone(Builder builder) {
    super(builder);
  }

  @Override
  public boolean isOccurring(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    if (shouldAddInstructions(previousRouteProgress, routeProgress)) {
      stepVoiceInstructions = routeProgress.currentLegProgress().currentStep().voiceInstructions();
    }
    for (VoiceInstructions voice : stepVoiceInstructions) {
      if (shouldBeVoiced(routeProgress, voice)) {
        announcement = voice.announcement();
        stepVoiceInstructions.remove(voice);
        return true;
      }
    }
    return false;
  }

  public String announcement() {
    return announcement;
  }

  private boolean shouldAddInstructions(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    return newStep(previousRouteProgress, routeProgress)
      || newRoute(previousRouteProgress, routeProgress)
      || stepVoiceInstructions == null;
  }

  private boolean newRoute(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    return !TextUtils.equals(previousRouteProgress.directionsRoute().geometry(),
      routeProgress.directionsRoute().geometry());
  }

  private boolean newStep(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    return previousRouteProgress.currentLegProgress().stepIndex()
      < routeProgress.currentLegProgress().stepIndex();
  }

  private boolean shouldBeVoiced(RouteProgress routeProgress, VoiceInstructions voice) {
    return voice.distanceAlongGeometry()
      >= routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();
  }

  public static final class Builder extends Milestone.Builder {

    private Trigger.Statement trigger;

    public Builder() {
      super();
    }

    @Override
    Trigger.Statement getTrigger() {
      return trigger;
    }

    @Override
    public Builder setTrigger(Trigger.Statement trigger) {
      this.trigger = trigger;
      return this;
    }

    @Override
    public ApiMilestone build() {
      return new ApiMilestone(this);
    }
  }
}