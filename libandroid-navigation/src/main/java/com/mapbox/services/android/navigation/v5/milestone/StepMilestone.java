package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.services.android.navigation.v5.exception.NavigationException;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

/**
 * Using a Step Milestone will result in
 * {@link MilestoneEventListener#onMilestoneEvent(RouteProgress, String, Milestone)}
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

    // Determine if the step index has changed and set called accordingly. This prevents multiple calls to
    // onMilestoneEvent per Step.
    if (previousRouteProgress.currentLegProgress().stepIndex()
      != routeProgress.currentLegProgress().stepIndex()) {
      called = false;
    }
    // If milestone's been called already on current step, no need to check triggers.
    if (called) {
      return false;
    }
    if (builder.getTrigger().isOccurring(
      TriggerProperty.getSparseArray(previousRouteProgress, routeProgress))) {
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
