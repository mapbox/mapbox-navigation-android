package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.mapboxsdk.camera.CameraPosition;

@AutoValue
public abstract class NavigationLauncherOptions extends NavigationUiOptions {

  @Nullable
  public abstract CameraPosition initialMapCameraPosition();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder directionsRoute(DirectionsRoute directionsRoute);

    public abstract Builder directionsProfile(@DirectionsCriteria.ProfileCriteria String directionsProfile);

    public abstract Builder lightThemeResId(Integer lightThemeResId);

    public abstract Builder darkThemeResId(Integer darkThemeResId);

    public abstract Builder shouldSimulateRoute(boolean shouldSimulateRoute);

    public abstract Builder waynameChipEnabled(boolean waynameChipEnabled);

    public abstract Builder initialMapCameraPosition(@Nullable CameraPosition initialMapCameraPosition);

    public abstract NavigationLauncherOptions build();
  }

  public static NavigationLauncherOptions.Builder builder() {
    return new AutoValue_NavigationLauncherOptions.Builder()
      .shouldSimulateRoute(false)
      .waynameChipEnabled(true);
  }
}
