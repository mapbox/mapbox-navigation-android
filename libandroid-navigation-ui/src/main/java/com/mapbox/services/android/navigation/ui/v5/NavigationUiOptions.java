package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

public abstract class NavigationUiOptions {

  @Nullable
  public abstract DirectionsRoute directionsRoute();

  @Nullable
  public abstract String directionsProfile();

  @Nullable
  public abstract Point origin();

  @Nullable
  public abstract Point destination();

  @Nullable
  public abstract Integer lightThemeResId();

  @Nullable
  public abstract Integer darkThemeResId();

  public abstract boolean shouldSimulateRoute();
}
