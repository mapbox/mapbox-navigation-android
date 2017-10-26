package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.constants.Constants;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@AutoValue
abstract class SessionState {

  String originalGeometry() {
    List<Point> geometryPositions
      = PolylineUtils.decode(originalDirectionRoute().geometry(), Constants.PRECISION_6);
    return PolylineUtils.encode(geometryPositions, Constants.PRECISION_5);
  }

  String currentGeometry() {
    List<Point> geometryPositions
      = PolylineUtils.decode(currentDirectionRoute().geometry(), Constants.PRECISION_6);
    return PolylineUtils.encode(geometryPositions, Constants.PRECISION_5);
  }

  int originalDuration() {
    return originalDirectionRoute().duration().intValue();
  }

  int originalDistance() {
    return originalDirectionRoute().distance().intValue();
  }

  int originalStepCount() {
    int stepCount = 0;
    for (RouteLeg leg : originalDirectionRoute().legs()) {
      stepCount += leg.steps().size();
    }
    return stepCount;
  }

  int currentStepCount() {
    int stepCount = 0;
    for (RouteLeg leg : currentDirectionRoute().legs()) {
      stepCount += leg.steps().size();
    }
    return stepCount;
  }

  abstract String sessionIdentifier();

  @Nullable
  abstract String originalRequestIdentifier();

  @Nullable
  abstract String requestIdentifier();

  abstract DirectionsRoute originalDirectionRoute();

  abstract DirectionsRoute currentDirectionRoute();

  @Nullable
  abstract Date rerouteDate();

  int secondsSinceLastReroute() {
    if (lastRerouteDate() == null || rerouteDate() == null) {
      return -1;
    }
    long diffInMs = rerouteDate().getTime() - lastRerouteDate().getTime();
    return (int) TimeUnit.MILLISECONDS.toSeconds(diffInMs);
  }

  @Nullable
  abstract Date lastRerouteDate();

  @Nullable
  abstract Location lastRerouteLocation();

  abstract Date startTimestamp();

  @Nullable
  abstract Date arrivalTimestamp();

  abstract boolean mockLocation();

  abstract int rerouteCount();

  abstract double previousRouteDistancesCompleted();

  @Nullable
  abstract List<Location> beforeRerouteLocations();

  @Nullable
  abstract List<Location> afterRerouteLocations();

  @Nullable
  abstract RouteProgress routeProgressBeforeReroute();

  abstract Builder toBuilder();

  static Builder builder() {
    return new AutoValue_SessionState.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder routeProgressBeforeReroute(@Nullable RouteProgress routeProgress);

    abstract Builder lastRerouteLocation(@Nullable Location lastReroutePosition);

    abstract Builder afterRerouteLocations(@Nullable List<Location> afterLocations);

    abstract Builder beforeRerouteLocations(@Nullable List<Location> beforeLocations);

    abstract Builder originalDirectionRoute(@NonNull DirectionsRoute originalDirectionsRoute);

    abstract Builder currentDirectionRoute(@NonNull DirectionsRoute currentDirectionsRoute);

    abstract Builder sessionIdentifier(@NonNull String sessionIdentifier);

    abstract Builder originalRequestIdentifier(@Nullable String originalRequestIdentifier);

    abstract Builder requestIdentifier(@Nullable String requestIdentifier);

    abstract Builder rerouteDate(@Nullable Date rerouteDate);

    abstract Builder lastRerouteDate(@Nullable Date lastRerouteDate);

    abstract Builder mockLocation(boolean mockLocation);

    abstract Builder rerouteCount(int rerouteCount);

    abstract Builder startTimestamp(@NonNull Date startTimeStamp);

    abstract Builder arrivalTimestamp(@Nullable Date arrivalTimestamp);

    abstract Builder previousRouteDistancesCompleted(double previousRouteDistancesCompleted);

    abstract SessionState build();

  }
}
