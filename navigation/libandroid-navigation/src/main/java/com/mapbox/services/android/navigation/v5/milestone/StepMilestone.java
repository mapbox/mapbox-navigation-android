package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.services.android.navigation.v5.NavigationException;
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
    Map<Integer, Number[]> statementObjects = new HashMap<>();
    statementObjects.put(TriggerProperty.STEP_DISTANCE_TOTAL, new Number[] {routeProgress.getCurrentLegProgress().getCurrentStep().getDistance()});
    statementObjects.put(TriggerProperty.STEP_DURATION_TOTAL, new Number[] {routeProgress.getCurrentLegProgress().getCurrentStep().getDuration()});
    statementObjects.put(TriggerProperty.STEP_DISTANCE_REMAINING, new Number[] {routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining()});
    statementObjects.put(TriggerProperty.STEP_DURATION_REMAINING, new Number[] {routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDurationRemaining()});
    statementObjects.put(TriggerProperty.STEP_INDEX, new Number[] {routeProgress.getCurrentLegProgress().getStepIndex()});
    statementObjects.put(TriggerProperty.NEW_STEP, new Number[] {previousRouteProgress.getCurrentLegProgress().getStepIndex(), routeProgress.getCurrentLegProgress().getStepIndex()});
    statementObjects.put(TriggerProperty.LAST_STEP, new Number[] {routeProgress.getCurrentLegProgress().getStepIndex(), (routeProgress.getCurrentLeg().getSteps().size() - 1)});


    if (previousRouteProgress.getCurrentLegProgress().getStepIndex() != routeProgress.getCurrentLegProgress().getStepIndex()) {
      called = false;
    }
    if (builder.getTrigger().validate(statementObjects) && !called) {
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
    public StepMilestone build() throws NavigationException {

      return new StepMilestone(this);
    }
  }

}
