package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.directions.v5.models.VoiceInstructions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

public class VoiceInstructionMilestone extends Milestone {

  private String announcement;
  private DirectionsRoute currentRoute;
  private LegStep currentStep;
  private List<VoiceInstructions> stepVoiceInstructions;

  public VoiceInstructionMilestone(Builder builder) {
    super(builder);
  }

  @Override
  public boolean isOccurring(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    if (newRoute(routeProgress)) {
      clearInstructionList();
    }
    if (shouldAddInstructions(routeProgress)) {
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
   * @param routeProgress the current route progress
   * @return true if new instructions should be added to the list, false if not
   */
  private boolean shouldAddInstructions(RouteProgress routeProgress) {
    return newStep(routeProgress) || stepVoiceInstructions == null;
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
   * Looks to see if we have a new step.
   *
   * @param routeProgress provides updated step information
   * @return true if new step, false if not
   */
  private boolean newStep(RouteProgress routeProgress) {
    boolean newStep = currentStep == null || !currentStep.equals(routeProgress.currentLegProgress().currentStep());
    currentStep = routeProgress.currentLegProgress().currentStep();
    return newStep;
  }

  /**
   * Looks to see if we have a new route.
   *
   * @param routeProgress provides updated route information
   * @return true if new route, false if not
   */
  private boolean newRoute(RouteProgress routeProgress) {
    boolean newRoute = currentRoute == null || !currentRoute.equals(routeProgress.directionsRoute());
    currentRoute = routeProgress.directionsRoute();
    return newRoute;
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