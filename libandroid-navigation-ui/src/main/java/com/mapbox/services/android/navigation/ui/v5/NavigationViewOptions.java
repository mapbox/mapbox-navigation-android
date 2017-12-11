package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;

@AutoValue
public abstract class NavigationViewOptions {

  @Nullable
  public abstract DirectionsRoute directionsRoute();

  @Nullable
  public abstract String directionsProfile();

  @Nullable
  public abstract Point origin();

  @Nullable
  public abstract Point destination();

  @Nullable
  public abstract String awsPoolId();

  public abstract MapboxNavigationOptions navigationOptions();

  public abstract boolean shouldSimulateRoute();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder directionsRoute(DirectionsRoute directionsRoute);

    public abstract Builder directionsProfile(@DirectionsCriteria.ProfileCriteria String directionsProfile);

    public abstract Builder origin(Point origin);

    public abstract Builder destination(Point destination);

    public abstract Builder awsPoolId(String awsPoolId);

    public abstract Builder navigationOptions(MapboxNavigationOptions navigationOptions);

    public abstract Builder shouldSimulateRoute(boolean shouldSimulateRoute);

    public abstract NavigationViewOptions build();
  }

  public static Builder builder() {
    return new AutoValue_NavigationViewOptions.Builder()
      .awsPoolId(null)
      .navigationOptions(MapboxNavigationOptions.builder().build())
      .shouldSimulateRoute(false);
  }
}