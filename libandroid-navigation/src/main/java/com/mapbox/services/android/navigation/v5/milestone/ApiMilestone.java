package com.mapbox.services.android.navigation.v5.milestone;

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
    if (previousRouteProgress.currentLegProgress().stepIndex()
      < routeProgress.currentLegProgress().stepIndex() || stepVoiceInstructions == null) {
      stepVoiceInstructions = routeProgress.currentLegProgress().currentStep().voiceInstructions();
    }


    for (VoiceInstructions voice : routeProgress.currentLegProgress().currentStep().voiceInstructions()) {
      if (voice.distanceAlongGeometry()
        >= routeProgress.currentLegProgress().currentStepProgress().distanceRemaining()) {
        announcement = routeProgress.currentLegProgress().currentStep().voiceInstructions().get(0).announcement();
        stepVoiceInstructions.remove(voice);
        return true;
      }
    }
    return false;
  }

  public String announcement() {
    return announcement;
  }

  public static final class Builder extends Milestone.Builder {

    private Trigger.Statement trigger;

    public Builder() {
      super();
    }

    @Override
    public Builder setTrigger(Trigger.Statement trigger) {
      this.trigger = trigger;
      return this;
    }

    @Override
    Trigger.Statement getTrigger() {
      return trigger;
    }

    @Override
    public ApiMilestone build() {
      return new ApiMilestone(this);
    }
  }
}