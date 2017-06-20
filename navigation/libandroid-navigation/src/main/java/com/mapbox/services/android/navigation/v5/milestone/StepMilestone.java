package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.services.android.navigation.v5.NavigationException;
import com.mapbox.services.android.navigation.v5.RouteProgress;

import java.util.HashMap;
import java.util.Map;

/**
 * Using a Step Milestone will result in {@link MilestoneEventListener#onMilestoneEvent(RouteProgress, String, int)}
 * being invoked every step if the condition validation returns true.
 *
 * @since 0.4.0
 */
public class StepMilestone extends Milestone {

  private Builder builder;
  private boolean called;

  private StepMilestone(Builder builder) {
    super(builder);
    this.builder = builder;
  }

  @Override
  public boolean isOccurring(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    // Build hashMap matching the trigger properties to their corresponding current values.
    Map<Integer, Number[]> statementObjects = new HashMap<>();
    statementObjects.put(TriggerProperty.STEP_DISTANCE_TOTAL_METERS,
      new Number[] {routeProgress.getCurrentLegProgress().getCurrentStep().getDistance()});
    statementObjects.put(TriggerProperty.STEP_DURATION_TOTAL_SECONDS,
      new Number[] {routeProgress.getCurrentLegProgress().getCurrentStep().getDuration()});
    statementObjects.put(TriggerProperty.STEP_DISTANCE_REMAINING_METERS,
      new Number[] {routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining()});
    statementObjects.put(TriggerProperty.STEP_DURATION_REMAINING_SECONDS,
      new Number[] {routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDurationRemaining()});
    statementObjects.put(TriggerProperty.STEP_INDEX,
      new Number[] {routeProgress.getCurrentLegProgress().getStepIndex()});
    statementObjects.put(TriggerProperty.NEW_STEP,
      new Number[] {
        previousRouteProgress.getCurrentLegProgress().getStepIndex(),
        routeProgress.getCurrentLegProgress().getStepIndex()});
    statementObjects.put(TriggerProperty.LAST_STEP,
      new Number[] {routeProgress.getCurrentLegProgress().getStepIndex(),
        (routeProgress.getCurrentLeg().getSteps().size() - 1)});
    statementObjects.put(TriggerProperty.FIRST_STEP,
      new Number[] {routeProgress.getCurrentLegProgress().getStepIndex(), 0});
    statementObjects.put(TriggerProperty.NEXT_STEP_DISTANCE_METERS,
      new Number[] {
        routeProgress.getCurrentLegProgress().getUpComingStep() != null
          ? routeProgress.getCurrentLegProgress().getUpComingStep().getDistance() : 0});

    // Determine if the step index has changed and set called accordingly. This prevents multiple calls to
    // onMilestoneEvent per Step.
    if (previousRouteProgress.getCurrentLegProgress().getStepIndex()
      != routeProgress.getCurrentLegProgress().getStepIndex()) {
      called = false;
    }
    if (builder.getTrigger().isOccurring(statementObjects) && !called) {
      called = true;
      return true;
    }
    return false;
  }

  /**
   * Build a new {@link StepMilestone}
   *
   * @since 0.4.0
   */
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
    public StepMilestone build() throws NavigationException {
      return new StepMilestone(this);
    }
  }
}
