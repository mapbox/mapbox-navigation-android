package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.services.android.navigation.v5.RouteProgress;

public abstract class Milestone {

  private Builder builder;

  public Milestone(Builder builder) {
    this.builder = builder;
  }

  public int getIdentifier() {
    return builder.getIdentifier();
  }

  public abstract boolean validate(RouteProgress previousRouteProgress, RouteProgress routeProgress);

  public abstract static class Builder {

    private int identifier;

    public Builder() {

    }

    public int getIdentifier() {
      return identifier;
    }

    public Builder setIdentifier(int identifier) {
      this.identifier = identifier;
      return this;
    }

    public abstract Builder setTrigger(Trigger.Statement trigger);

    abstract Trigger.Statement getTrigger();

    public abstract Milestone build();
  }

}
