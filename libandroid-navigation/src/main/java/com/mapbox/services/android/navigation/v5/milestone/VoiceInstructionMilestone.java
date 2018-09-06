package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.services.android.navigation.v5.instruction.Instruction;
import com.mapbox.services.android.navigation.v5.navigation.VoiceInstructionLoader;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

/**
 * A default milestone that is added to {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}
 * when default milestones are enabled.
 * <p>
 * Please note, this milestone has a custom trigger based on location progress along a route.  If you
 * set custom triggers, they will be ignored in favor of this logic.
 */
public class VoiceInstructionMilestone extends Milestone {

  private static final String EMPTY_STRING = "";

  private VoiceInstructions instructions;
  private DirectionsRoute currentRoute;
  private RouteUtils routeUtils;

  VoiceInstructionMilestone(Builder builder) {
    super(builder);
    routeUtils = new RouteUtils();
  }

  @Override
  public boolean isOccurring(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    if (isNewRoute(routeProgress)) {
      cacheInstructions(routeProgress, true);
    }
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();
    VoiceInstructions instructions = routeUtils.findCurrentVoiceInstructions(currentStep, stepDistanceRemaining);
    if (shouldBeVoiced(instructions, stepDistanceRemaining)) {
      return updateInstructions(routeProgress, instructions);
    }
    return false;
  }

  @Override
  public Instruction getInstruction() {
    return new Instruction() {
      @Override
      public String buildInstruction(RouteProgress routeProgress) {
        if (instructions == null) {
          return routeProgress.currentLegProgress().currentStep().name();
        }
        return instructions.announcement();
      }
    };
  }

  /**
   * Provide the SSML instruction that can be used with Mapbox's API Voice.
   * <p>
   * This String will provide special markup denoting how certain portions of the announcement
   * should be pronounced.
   *
   * @return announcement with SSML markup
   * @since 0.8.0
   */
  public String getSsmlAnnouncement() {
    if (instructions == null) {
      return EMPTY_STRING;
    }
    return instructions.ssmlAnnouncement();
  }

  /**
   * Provide the instruction that can be used with Android's TextToSpeech.
   * <p>
   * This string will be in plain text.
   *
   * @return announcement in plain text
   * @since 0.12.0
   */
  public String getAnnouncement() {
    if (instructions == null) {
      return EMPTY_STRING;
    }
    return instructions.announcement();
  }

  /**
   * Looks to see if we have a new route.
   *
   * @param routeProgress provides updated route information
   * @return true if new route, false if not
   */
  private boolean isNewRoute(RouteProgress routeProgress) {
    boolean newRoute = currentRoute == null || !currentRoute.equals(routeProgress.directionsRoute());
    currentRoute = routeProgress.directionsRoute();
    return newRoute;
  }

  /**
   * Checks if the current instructions are different from the instructions
   * determined by the step distance remaining.
   *
   * @param instructions          the current voice instructions from the list of step instructions
   * @param stepDistanceRemaining the current step distance remaining
   * @return true if time to voice the announcement, false if not
   */
  private boolean shouldBeVoiced(VoiceInstructions instructions, double stepDistanceRemaining) {
    boolean isNewInstruction = this.instructions == null || !this.instructions.equals(instructions);
    boolean isValidNewInstruction = instructions != null && isNewInstruction;
    return isValidNewInstruction && instructions.distanceAlongGeometry() >= stepDistanceRemaining;
  }

  private boolean updateInstructions(RouteProgress routeProgress, VoiceInstructions instructions) {
    cacheInstructions(routeProgress, false);
    this.instructions = instructions;
    return true;
  }

  /**
   * Caches the instructions in the VoiceInstructionLoader if it has been initialized
   *
   * @param routeProgress containing the instructions
   * @param isFirst       whether it's the first routeProgress of the route
   */
  private void cacheInstructions(RouteProgress routeProgress, boolean isFirst) {
    VoiceInstructionLoader voiceInstructionLoader = VoiceInstructionLoader.getInstance();
    if (voiceInstructionLoader != null) {
      voiceInstructionLoader.cacheInstructions(routeProgress, isFirst);
    }
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