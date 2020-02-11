package com.mapbox.navigation.ui;

import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigation.ui.camera.Camera;

public abstract class NavigationUiOptions {

  public abstract DirectionsRoute directionsRoute();

  @Nullable
  public abstract Integer lightThemeResId();

  @Nullable
  public abstract Integer darkThemeResId();

  public abstract boolean shouldSimulateRoute();

  public abstract boolean waynameChipEnabled();

  @Nullable
  public abstract String offlineRoutingTilesPath();

  @Nullable
  public abstract String offlineRoutingTilesVersion();

  @Nullable
  public abstract MapOfflineOptions offlineMapOptions();

  @Nullable
  public abstract Camera camera();
}
