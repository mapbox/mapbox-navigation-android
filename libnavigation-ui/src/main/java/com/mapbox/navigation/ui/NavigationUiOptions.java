package com.mapbox.navigation.ui;

import androidx.annotation.Nullable;

import com.mapbox.navigation.base.route.model.Route;

public abstract class NavigationUiOptions {

  public abstract Route route();

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
}
