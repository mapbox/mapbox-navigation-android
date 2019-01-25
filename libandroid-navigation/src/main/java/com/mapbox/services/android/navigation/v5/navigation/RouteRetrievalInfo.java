package com.mapbox.services.android.navigation.v5.navigation;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.services.android.navigation.v5.exception.NavigationException;

import java.util.List;

@AutoValue
abstract class RouteRetrievalInfo {
  abstract long elapsedTime();

  abstract double distance();

  abstract int stepCount();

  abstract int coordinateCount();

  abstract String profile();

  abstract boolean isOffline();

  abstract int numberOfRoutes();

  public static Builder builder() {
    return new AutoValue_RouteRetrievalInfo.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {
    private Long start = null;

    Builder start() {
      start = System.nanoTime();
      return this;
    }

    Builder end() {
      long end = System.nanoTime();

      if (start == null) {
        throw new NavigationException("Must call `start` before calling `end`.");
      }

      elapsedTime(end - start);
      return this;
    }

    abstract Builder elapsedTime(long elapsedTime);

    abstract Builder distance(double distance);

    abstract Builder stepCount(int stepCount);

    abstract Builder coordinateCount(int coordinateCount);

    abstract Builder profile(String profile);

    abstract Builder isOffline(boolean isOffline);

    abstract Builder numberOfRoutes(int numberOfRoutes);

    Builder route(DirectionsRoute directionsRoute) {
      distance(directionsRoute.distance());
      stepCount(getStepCount(directionsRoute.legs()));
      RouteOptions routeOptions = directionsRoute.routeOptions();
      coordinateCount(routeOptions.coordinates().size());
      profile(routeOptions.profile());
      return this;
    }

    private int getStepCount(List<RouteLeg> legs) {
      int count = 0;
      for (RouteLeg leg : legs) {
        count += leg.steps().size();
      }
      return count;
    }

    abstract RouteRetrievalInfo build();
  }
}
