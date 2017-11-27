package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;
import com.mapbox.services.constants.Constants;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@AutoValue
public abstract class SessionState {

  /*
   * Original route values
   */
  public String originalGeometry() {
    List<Point> geometryPositions
      = PolylineUtils.decode(originalDirectionRoute().geometry(), Constants.PRECISION_6);
    return PolylineUtils.encode(geometryPositions, Constants.PRECISION_5);
  }

  public int originalDistance() {
    return originalDirectionRoute().distance().intValue();
  }

  public int originalStepCount() {
    int stepCount = 0;
    for (RouteLeg leg : originalDirectionRoute().legs()) {
      stepCount += leg.steps().size();
    }
    return stepCount;
  }

  public int originalDuration() {
    return originalDirectionRoute().duration().intValue();
  }

  /*
   * Current route values
   */
  public int currentStepCount() {
    int stepCount = 0;
    for (RouteLeg leg : currentDirectionRoute().legs()) {
      stepCount += leg.steps().size();
    }
    return stepCount;
  }

  public String currentGeometry() {
    List<Point> geometryPositions
      = PolylineUtils.decode(currentDirectionRoute().geometry(), Constants.PRECISION_6);
    return PolylineUtils.encode(geometryPositions, Constants.PRECISION_5);
  }

  public int secondsSinceLastReroute() {
    if (lastRerouteDate() == null || eventDate() == null) {
      return -1;
    }
    long diffInMs = eventDate().getTime() - lastRerouteDate().getTime();
    return (int) TimeUnit.MILLISECONDS.toSeconds(diffInMs);
  }

  @Nullable
  public abstract MetricsRouteProgress eventRouteProgress();

  @Nullable
  public abstract Location eventLocation();

  @Nullable
  public abstract Date eventDate();

  public abstract double eventRouteDistanceCompleted();

  @Nullable
  public abstract List<Location> afterEventLocations();

  @Nullable
  public abstract List<Location> beforeEventLocations();

  @Nullable
  public abstract DirectionsRoute originalDirectionRoute();

  @Nullable
  public abstract DirectionsRoute currentDirectionRoute();

  public abstract String sessionIdentifier();

  @Nullable
  public abstract String originalRequestIdentifier();

  @Nullable
  public abstract String requestIdentifier();

  @Nullable
  public abstract Date lastRerouteDate();

  public abstract boolean mockLocation();

  public abstract int rerouteCount();

  public abstract Date startTimestamp();

  @Nullable
  public abstract Date arrivalTimestamp();

  public abstract String locationEngineName();

  public abstract Builder toBuilder();

  public static Builder builder() {
    return new AutoValue_SessionState.Builder()
      .eventRouteDistanceCompleted(0d)
      .sessionIdentifier("")
      .mockLocation(false)
      .startTimestamp(new Date())
      .rerouteCount(0)
      .locationEngineName("");
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder eventRouteProgress(@Nullable MetricsRouteProgress routeProgress);

    public abstract Builder eventLocation(@Nullable Location eventLocation);

    public abstract Builder eventDate(@Nullable Date eventDate);

    public abstract Builder eventRouteDistanceCompleted(double routeDistanceCompleted);

    public abstract Builder afterEventLocations(@Nullable List<Location> afterLocations);

    public abstract Builder beforeEventLocations(@Nullable List<Location> beforeLocations);

    public abstract Builder originalDirectionRoute(@Nullable DirectionsRoute originalDirectionsRoute);

    public abstract Builder currentDirectionRoute(@Nullable DirectionsRoute currentDirectionsRoute);

    public abstract Builder sessionIdentifier(String sessionIdentifier);

    public abstract Builder originalRequestIdentifier(@Nullable String originalRequestIdentifier);

    public abstract Builder requestIdentifier(@Nullable String requestIdentifier);

    public abstract Builder lastRerouteDate(@Nullable Date lastRerouteDate);

    public abstract Builder mockLocation(boolean mockLocation);

    public abstract Builder rerouteCount(int rerouteCount);

    public abstract Builder startTimestamp(@NonNull Date startTimeStamp);

    public abstract Builder arrivalTimestamp(@Nullable Date arrivalTimestamp);

    public abstract Builder locationEngineName(String locationEngineName);

    public abstract SessionState build();
  }
}
