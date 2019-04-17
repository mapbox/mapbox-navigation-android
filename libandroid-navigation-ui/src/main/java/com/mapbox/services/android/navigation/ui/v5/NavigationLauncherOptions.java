package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.mapboxsdk.camera.CameraPosition;

@AutoValue
public abstract class NavigationLauncherOptions extends NavigationUiOptions {

  @Nullable
  public abstract CameraPosition initialMapCameraPosition();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder directionsRoute(DirectionsRoute directionsRoute);

    public abstract Builder lightThemeResId(Integer lightThemeResId);

    public abstract Builder darkThemeResId(Integer darkThemeResId);

    public abstract Builder shouldSimulateRoute(boolean shouldSimulateRoute);

    public abstract Builder waynameChipEnabled(boolean waynameChipEnabled);

    public abstract Builder initialMapCameraPosition(@Nullable CameraPosition initialMapCameraPosition);

    /**
     * Add an offline path for loading offline routing data.
     * <p>
     * When added, the {@link NavigationView} will try to initialize and use this data
     * for offline routing when no or poor internet connection is found.
     *
     * @param offlinePath to offline data on device
     * @return this builder
     */
    public abstract Builder offlineRoutingTilesPath(String offlinePath);

    /**
     * Add an offline tile version.  When providing a routing tile path, this version
     * is also required for configuration.
     * <p>
     * This version should directly correspond to the data in the offline path also provided.
     *
     * @param offlineVersion of data in tile path
     * @return this builder
     */
    public abstract Builder offlineRoutingTilesVersion(String offlineVersion);

    /**
     * Add an offline path for loading an offline map database.
     * <p>
     * When added, the {@link NavigationView} will try to initialize and use this data
     * for offline maps while navigating.
     *
     * @param offlinePath to offline database on device
     * @return this builder
     */
    public abstract Builder offlineMapDatabasePath(String offlinePath);

    public abstract NavigationLauncherOptions build();
  }

  public static NavigationLauncherOptions.Builder builder() {
    return new AutoValue_NavigationLauncherOptions.Builder()
      .shouldSimulateRoute(false)
      .waynameChipEnabled(true);
  }
}
