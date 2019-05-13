package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.WalkingOptions;

public final class NavigationWalkingOptions {
  private final WalkingOptions walkingOptions;

  NavigationWalkingOptions(WalkingOptions walkingOptions) {
    this.walkingOptions = walkingOptions;
  }

  WalkingOptions getWalkingOptions() {
    return walkingOptions;
  }

  public static Builder builder() {
    return new Builder(WalkingOptions.builder());
  }

  public static final class Builder {
    private final WalkingOptions.Builder builder;

    Builder(WalkingOptions.Builder builder) {
      this.builder = builder;
    }

    public NavigationWalkingOptions build() {
      return new NavigationWalkingOptions(builder.build());
    }

    public Builder walkingSpeed(Double walkingSpeed) {
      builder.walkingSpeed(walkingSpeed);
      return this;
    }

    public Builder walkwayBias(Double walkwayBias) {
      builder.walkwayBias(walkwayBias);
      return this;
    }

    public Builder alleyBias(Double alleyBias) {
      builder.alleyBias(alleyBias);
      return this;
    }
  }
}
