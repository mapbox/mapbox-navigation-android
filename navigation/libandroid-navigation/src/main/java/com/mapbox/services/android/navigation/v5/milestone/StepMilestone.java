package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.services.android.navigation.v5.RouteProgress;

import java.util.HashMap;
import java.util.Map;

public class StepMilestone extends Milestone {

  private Builder builder;
  private boolean called;


  private StepMilestone(Builder builder) {
    super(builder);
    this.builder = builder;
  }

  @Override
  public boolean validate(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    Map<Integer, Number[]> factors = new HashMap<>();
    factors.put(TriggerValue.STEP_DISTANCE, new Number[] {routeProgress.getCurrentLegProgress().getCurrentStep().getDistance()});
    factors.put(TriggerValue.STEP_DURATION, new Number[] {routeProgress.getCurrentLegProgress().getCurrentStep().getDuration()});
    factors.put(TriggerValue.DURATION, new Number[] {routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDurationRemaining()});
    factors.put(TriggerValue.STEP_INDEX, new Number[] {routeProgress.getCurrentLegProgress().getStepIndex()});
    factors.put(TriggerValue.NEW_STEP, new Number[] {previousRouteProgress.getCurrentLegProgress().getStepIndex(), routeProgress.getCurrentLegProgress().getStepIndex()});
    factors.put(TriggerValue.LAST_STEP, new Number[] {routeProgress.getCurrentLegProgress().getStepIndex(), (routeProgress.getCurrentLeg().getSteps().size() - 1)});

    if (previousRouteProgress.getCurrentLegProgress().getStepIndex() != routeProgress.getCurrentLegProgress().getStepIndex()) {
      called = false;
    }
    if (builder.getTrigger().check(factors) && !called) {
      called = true;
      return true;
    }
    return false;
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
    public StepMilestone build() {
      return new StepMilestone(this);
    }
  }

}
