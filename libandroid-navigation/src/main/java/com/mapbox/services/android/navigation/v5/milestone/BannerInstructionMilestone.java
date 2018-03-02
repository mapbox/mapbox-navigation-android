package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

public class BannerInstructionMilestone extends Milestone {

  private BannerInstructions instructions;
  private DirectionsRoute currentRoute;
  private LegStep currentStep;
  private List<BannerInstructions> stepBannerInstructions;

  BannerInstructionMilestone(Builder builder) {
    super(builder);
  }

  @Override
  public boolean isOccurring(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    if (newRoute(routeProgress)) {
      clearInstructionList();
    }
    if (shouldAddInstructions(routeProgress)) {
      stepBannerInstructions = routeProgress.currentLegProgress().currentStep().bannerInstructions();
    }
    for (BannerInstructions instructions : stepBannerInstructions) {
      if (shouldBeShown(routeProgress, instructions)) {
        this.instructions = instructions;
        stepBannerInstructions.remove(instructions);
        return true;
      }
    }
    return false;
  }

  public BannerInstructions getBannerInstructions() {
    return instructions;
  }

  /**
   * Check if a new set of step instructions should be set.
   *
   * @param routeProgress the current route progress
   * @return true if new instructions should be added to the list, false if not
   */
  private boolean shouldAddInstructions(RouteProgress routeProgress) {
    return newStep(routeProgress) || stepBannerInstructions == null;
  }

  /**
   * Called when adding new instructions to the list.
   * <p>
   * Make sure old announcements are not called (can happen in reroute scenarios).
   */
  private void clearInstructionList() {
    if (validInstructions(stepBannerInstructions)) {
      stepBannerInstructions.clear();
    }
  }

  private boolean validInstructions(List<BannerInstructions> instructions) {
    return instructions != null && !instructions.isEmpty();
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
   * Uses the current step distance remaining to check against banner instructions distance.
   *
   * @param routeProgress      the current route progress
   * @param bannerInstructions given banner instructions from the list of step instructions
   * @return true if time to show the instructions, false if not
   */
  private boolean shouldBeShown(RouteProgress routeProgress, BannerInstructions bannerInstructions) {
    return (bannerInstructions.distanceAlongGeometry()
      >= routeProgress.currentLegProgress().currentStepProgress().distanceRemaining())
      || isFirstInstruction(routeProgress);
  }

  /**
   * Used to determine if the user is currently on the first step instruction.
   * <p>
   * In this case, the instructions should be shown immediately, rather than waiting for the
   * valid distance.
   *
   * @param routeProgress for current list of instructions
   * @return true if the first instruction on the step, false otherwise
   */
  private boolean isFirstInstruction(RouteProgress routeProgress) {
    List<BannerInstructions> instructions = routeProgress.currentLegProgress().currentStep().bannerInstructions();
    return validInstructions(stepBannerInstructions)
      && validInstructions(instructions)
      && instructions.get(0).equals(stepBannerInstructions.get(0));
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
    public BannerInstructionMilestone build() {
      return new BannerInstructionMilestone(this);
    }
  }
}