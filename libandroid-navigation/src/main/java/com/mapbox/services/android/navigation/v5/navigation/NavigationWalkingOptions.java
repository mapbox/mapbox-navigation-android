package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.WalkingOptions;

/**
 * Class for specifying options for use with the walking profile.
 */
public final class NavigationWalkingOptions {
  private final WalkingOptions walkingOptions;

  NavigationWalkingOptions(WalkingOptions walkingOptions) {
    this.walkingOptions = walkingOptions;
  }

  WalkingOptions getWalkingOptions() {
    return walkingOptions;
  }

  /**
   * Build a new {@link WalkingOptions} object with no defaults.
   *
   * @return a {@link Builder} object for creating a {@link NavigationWalkingOptions} object
   */
  public static Builder builder() {
    return new Builder(WalkingOptions.builder());
  }

  /**
   * This builder is used to create a new object with specifications relating to walking directions.
   */
  public static final class Builder {
    private final WalkingOptions.Builder builder;

    Builder(WalkingOptions.Builder builder) {
      this.builder = builder;
    }

    /**
     * Builds a {@link NavigationWalkingOptions} object with the specified configurations.
     *
     * @return a NavigationWalkingOptions object
     */
    public NavigationWalkingOptions build() {
      return new NavigationWalkingOptions(builder.build());
    }

    /**
     * Walking speed in meters per second. Must be between 0.14 and 6.94 meters per second.
     * Defaults to 1.42 meters per second
     *
     * @param walkingSpeed in meters per second
     * @return this builder
     */
    public Builder walkingSpeed(Double walkingSpeed) {
      builder.walkingSpeed(walkingSpeed);
      return this;
    }

    /**
     * A bias which determines whether the route should prefer or avoid the use of roads or paths
     * that are set aside for pedestrian-only use (walkways). The allowed range of values is from
     * -1 to 1, where -1 indicates preference to avoid walkways, 1 indicates preference to favor
     * walkways, and 0 indicates no preference (the default).
     *
     * @param walkwayBias bias to prefer or avoid walkways
     * @return this builder
     */
    public Builder walkwayBias(Double walkwayBias) {
      builder.walkwayBias(walkwayBias);
      return this;
    }

    /**
     * A bias which determines whether the route should prefer or avoid the use of alleys. The
     * allowed range of values is from -1 to 1, where -1 indicates preference to avoid alleys, 1
     * indicates preference to favor alleys, and 0 indicates no preference (the default).
     *
     * @param alleyBias bias to prefer or avoid alleys
     * @return this builder
     */
    public Builder alleyBias(Double alleyBias) {
      builder.alleyBias(alleyBias);
      return this;
    }
  }
}
