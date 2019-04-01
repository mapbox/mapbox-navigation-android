package com.mapbox.services.android.navigation.ui.v5;

import com.google.auto.value.AutoValue;
import com.mapbox.services.android.navigation.ui.v5.map.ThrottleConfig;

@AutoValue
public abstract class NavigationAnimationOptions {

  public abstract ThrottleConfig throttleMapFps();

  public abstract boolean mapThrottleEnabled();

  public abstract ThrottleConfig throttleLocationComponentFps();

  public abstract boolean locationThrottleEnabled();

  public abstract Builder toBuilder();

  public static Builder builder() {
    return new AutoValue_NavigationAnimationOptions.Builder();
  }

  public static Builder efficiencyProfile() {
    return builder()
      .locationThrottleEnabled(true)
      .mapThrottleEnabled(true)
      .throttleMapFps(ThrottleConfigFactory.efficiencyMapProfile())
      .throttleLocationComponentFps(ThrottleConfigFactory.efficiencyLocationProfile());
  }

  public static Builder defaultProfile() {
    ThrottleConfigFactory factory = new ThrottleConfigFactory();
    return builder()
      .locationThrottleEnabled(true)
      .mapThrottleEnabled(true)
      .throttleMapFps(factory.defaultMapProfile())
      .throttleLocationComponentFps(factory.defaultLocationProfile());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder throttleMapFps(ThrottleConfig throttleConfig);

    public abstract Builder mapThrottleEnabled(boolean isEnabled);

    public abstract Builder throttleLocationComponentFps(ThrottleConfig throttleConfig);

    public abstract Builder locationThrottleEnabled(boolean isEnabled);

    public abstract NavigationAnimationOptions build();
  }
}
