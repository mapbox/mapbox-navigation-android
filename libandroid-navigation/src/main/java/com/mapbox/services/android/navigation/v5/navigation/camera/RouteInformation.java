package com.mapbox.services.android.navigation.v5.navigation.camera;

import android.content.res.Configuration;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

@AutoValue
public abstract class RouteInformation {

  @NonNull
  public abstract Configuration configuration();

  public abstract double targetDistance();

  @Nullable
  public abstract DirectionsRoute route();

  @Nullable
  public abstract Location location();

  @Nullable
  public abstract RouteProgress routeProgress();

  public static RouteInformation create(Configuration configuration, double targetDistance,
                                        DirectionsRoute route, Location location,
                                        RouteProgress routeProgress) {
    return new AutoValue_RouteInformation(configuration, targetDistance, route, location,
            routeProgress);
  }
}
