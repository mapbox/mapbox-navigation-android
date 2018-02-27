package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

/**
 * Using a Route Milestone will result in
 * {@link MilestoneEventListener#onMilestoneEvent(RouteProgress, String, Milestone)} being invoked only
 * once during a navigation session.
 *
 * @since 0.4.0
 */
public class RouteMilestone extends Milestone {

  private Builder builder;
  private boolean called;

  private RouteMilestone(Builder builder) {
    super(builder);
    this.builder = builder;
  }

  @Override
  public boolean isOccurring(RouteProgress previousRouteProgress, RouteProgress routeProgress) {

    if (builder.getTrigger().isOccurring(
      TriggerProperty.getSparseArray(previousRouteProgress, routeProgress)) && !called) {
      called = true;
      return true;
    }
    return false;
  }

  /**
   * Build a new {@link RouteMilestone}
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
    public RouteMilestone build() {
      return new RouteMilestone(this);
    }
  }
}
