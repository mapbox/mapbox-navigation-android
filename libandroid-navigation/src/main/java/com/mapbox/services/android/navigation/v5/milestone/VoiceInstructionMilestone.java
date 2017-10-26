package com.mapbox.services.android.navigation.v5.milestone;

import android.text.TextUtils;

import com.mapbox.directions.v5.models.VoiceInstructions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

public class VoiceInstructionMilestone extends Milestone {

  private String announcement;
  private List<VoiceInstructions> stepVoiceInstructions;

  public VoiceInstructionMilestone(Builder builder) {
    super(builder);
  }

  @Override
  public boolean isOccurring(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    if (shouldAddInstructions(previousRouteProgress, routeProgress)) {
      clearInstructionList();
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
      || newRoute(previousRouteProgress, routeProgress)
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
   * Used to check for a new route.  Route geometries will be the same only on the first
   * update.  This is because the previousRouteProgress is reset and the current routeProgress is generated
   * from the previous.
   *
   * @param previousRouteProgress most recent progress before the current progress
   * @param routeProgress         the current route progress
   * @return true if there's a new route, false if not
   */
  private boolean newRoute(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    return TextUtils.equals(previousRouteProgress.directionsRoute().geometry(),
      routeProgress.directionsRoute().geometry());
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