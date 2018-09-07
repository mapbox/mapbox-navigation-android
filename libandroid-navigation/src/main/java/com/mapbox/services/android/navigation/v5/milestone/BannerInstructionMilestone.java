package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

/**
 * A default milestone that is added to {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}
 * when default milestones are enabled.
 * <p>
 * Please note, this milestone has a custom trigger based on location progress along a route.  If you
 * set custom triggers, they will be ignored in favor of this logic.
 */
public class BannerInstructionMilestone extends Milestone {

  private BannerInstructions instructions;
  private RouteUtils routeUtils;

  BannerInstructionMilestone(Builder builder) {
    super(builder);
    routeUtils = new RouteUtils();
  }

  @Override
  public boolean isOccurring(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    RouteLegProgress legProgress = routeProgress.currentLegProgress();
    LegStep currentStep = legProgress.currentStep();
    double stepDistanceRemaining = legProgress.currentStepProgress().distanceRemaining();
    BannerInstructions instructions = routeUtils.findCurrentBannerInstructions(currentStep, stepDistanceRemaining);
    if (shouldBeShown(instructions, stepDistanceRemaining)) {
      this.instructions = instructions;
      return true;
    }
    return false;
  }

  /**
   * Returns the given {@link BannerInstructions} for the time that the milestone is triggered.
   *
   * @return current banner instructions based on distance along the current step
   * @since 0.13.0
   */
  public BannerInstructions getBannerInstructions() {
    return instructions;
  }

  /**
   * Uses the current step distance remaining to check against banner instructions distance.
   *
   * @param instructions          given banner instructions from the list of step instructions
   * @param stepDistanceRemaining distance remaining along the current step
   * @return true if time to show the instructions, false if not
   */
  private boolean shouldBeShown(BannerInstructions instructions, double stepDistanceRemaining) {
    boolean isNewInstruction = this.instructions == null || !this.instructions.equals(instructions);
    boolean isValidNewInstruction = instructions != null && isNewInstruction;
    boolean withinDistanceAlongGeometry = isValidNewInstruction
      && instructions.distanceAlongGeometry() >= stepDistanceRemaining;
    boolean isFirstInstruction = this.instructions == null && instructions != null;
    return isFirstInstruction || withinDistanceAlongGeometry;
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