package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;

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
  public abstract String offlineMapDatabasePath();
}
