package com.mapbox.services.android.navigation.ui.v5;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;

@AutoValue
public abstract class NavigationLauncherOptions extends NavigationUiOptions {

  public abstract boolean enableOffRouteDetection();

  public abstract boolean snapToRoute();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder directionsRoute(DirectionsRoute directionsRoute);

    public abstract Builder directionsProfile(@DirectionsCriteria.ProfileCriteria String directionsProfile);

    public abstract Builder lightThemeResId(Integer lightThemeResId);

    public abstract Builder darkThemeResId(Integer darkThemeResId);

    public abstract Builder shouldSimulateRoute(boolean shouldSimulateRoute);

    public abstract Builder waynameChipEnabled(boolean waynameChipEnabled);

    public abstract Builder enableOffRouteDetection(boolean enableOffRouteDetection);

    public abstract Builder snapToRoute(boolean snapToRoute);

    public abstract NavigationLauncherOptions build();
  }

  public static NavigationLauncherOptions.Builder builder() {
    return new AutoValue_NavigationLauncherOptions.Builder()
      .shouldSimulateRoute(false)
      .enableOffRouteDetection(true)
      .snapToRoute(true)
      .waynameChipEnabled(true);
  }
}
