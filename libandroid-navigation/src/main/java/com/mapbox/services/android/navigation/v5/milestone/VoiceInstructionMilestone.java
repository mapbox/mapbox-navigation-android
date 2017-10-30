package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.directions.v5.models.VoiceInstructions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.util.List;

public class VoiceInstructionMilestone extends Milestone {

  private String announcement;
  private List<VoiceInstructions> stepVoiceInstructions;

  public VoiceInstructionMilestone(Builder builder) {
    super(builder);
  }

  @Override
  public boolean isOccurring(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    if (RouteUtils.isNewRoute(previousRouteProgress, routeProgress)) {
      clearInstructionList();
    }
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

  /**
   * Check if a new set of step instructions should be set.
   *
   * @param previousRouteProgress most recent progress before the current progress
   * @param routeProgress         the current route progress
   * @return true if new instructions should be added to the list, false if not
   */
  private boolean shouldAddInstructions(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    return newStep(previousRouteProgress, routeProgress)
      || stepVoiceInstructions == null;
  }

  /**
   * Called when adding new instructions to the list.
   * <p>
   * Make sure old announcements are not called (can happen in reroute scenarios).
   */
  private void clearInstructionList() {
    if (stepVoiceInstructions != null && !stepVoiceInstructions.isEmpty()) {
      stepVoiceInstructions.clear();
    }
  }

  /**
   * @param previousRouteProgress most recent progress before the current progress
   * @param routeProgress         the current route progress
   * @return true if on a new step, false if not
   */
  private boolean newStep(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    return previousRouteProgress.currentLegProgress().stepIndex()
      < routeProgress.currentLegProgress().stepIndex();
  }

  /**
   * Uses the current step distance remaining to check against voice instruction distance.
   *
   * @param routeProgress the current route progress
   * @param voice         a given voice instruction from the list of step instructions
   * @return true if time to voice the announcement, false if not
   */
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
    public VoiceInstructionMilestone build() {
      return new VoiceInstructionMilestone(this);
    }
  }
}