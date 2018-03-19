package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;

import java.util.Locale;

@AutoValue
public abstract class NavigationLauncherOptions extends NavigationOptions {

  @Nullable
  public abstract Locale locale();

  @Nullable
  public abstract Integer unitType();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder directionsRoute(DirectionsRoute directionsRoute);

    public abstract Builder directionsProfile(@DirectionsCriteria.ProfileCriteria String directionsProfile);

    public abstract Builder origin(Point origin);

    public abstract Builder destination(Point destination);

    public abstract Builder awsPoolId(String awsPoolId);

    public abstract Builder lightThemeResId(Integer lightThemeResId);

    public abstract Builder darkThemeResId(Integer darkThemeResId);

    public abstract Builder shouldSimulateRoute(boolean shouldSimulateRoute);

    public abstract Builder locale(Locale locale);

    public abstract Builder unitType(@NavigationUnitType.UnitType Integer unitType);

    public abstract NavigationLauncherOptions build();
  }

  public static NavigationLauncherOptions.Builder builder() {
    return new AutoValue_NavigationLauncherOptions.Builder()
      .awsPoolId(null)
      .shouldSimulateRoute(false);
  }
}
