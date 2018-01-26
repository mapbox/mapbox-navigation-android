package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.location.Location;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class SessionState {

  /*
   * Original route values
   */
  public String originalGeometry() {
    if (originalDirectionRoute() == null || TextUtils.isEmpty(originalDirectionRoute().geometry())) {
      return "";
    }

    List<Point> geometryPositions
      = PolylineUtils.decode(originalDirectionRoute().geometry(), Constants.PRECISION_6);
    return PolylineUtils.encode(geometryPositions, Constants.PRECISION_5);
  }

  public int originalDistance() {
    if (originalDirectionRoute() == null) {
      return 0;
    }
    return originalDirectionRoute().distance().intValue();
  }

  public int originalStepCount() {
    if (originalDirectionRoute() == null) {
      return 0;
    }
    int stepCount = 0;
    for (RouteLeg leg : originalDirectionRoute().legs()) {
      stepCount += leg.steps().size();
    }
    return stepCount;
  }

  public int originalDuration() {
    if (originalDirectionRoute() == null) {
      return 0;
    }
    return originalDirectionRoute().duration().intValue();
  }

  /*
   * Current route values
   */
  public int currentStepCount() {
    if (currentDirectionRoute() == null) {
      return 0;
    }
    int stepCount = 0;
    for (RouteLeg leg : currentDirectionRoute().legs()) {
      stepCount += leg.steps().size();
    }
    return stepCount;
  }

  public String currentGeometry() {
    if (currentDirectionRoute() == null || TextUtils.isEmpty(currentDirectionRoute().geometry())) {
      return "";
    }

    List<Point> geometryPositions
      = PolylineUtils.decode(currentDirectionRoute().geometry(), Constants.PRECISION_6);
    return PolylineUtils.encode(geometryPositions, Constants.PRECISION_5);
  }

  public abstract int secondsSinceLastReroute();

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

  public abstract boolean mockLocation();

  public abstract int rerouteCount();

  @Nullable
  public abstract Date startTimestamp();

  @Nullable
  public abstract Date arrivalTimestamp();

  public abstract String locationEngineName();

  public abstract int percentInForeground();

  public abstract int percentInPortrait();

  public abstract Builder toBuilder();

  public static Builder builder() {
    return new AutoValue_SessionState.Builder()
      .eventRouteDistanceCompleted(0d)
      .sessionIdentifier("")
      .mockLocation(false)
      .rerouteCount(0)
      .secondsSinceLastReroute(-1)
      .eventRouteProgress(new MetricsRouteProgress(null))
      .locationEngineName("")
      .percentInForeground(100)
      .percentInPortrait(100);
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder eventRouteProgress(MetricsRouteProgress routeProgress);

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

    public abstract Builder secondsSinceLastReroute(int seconds);

    public abstract Builder mockLocation(boolean mockLocation);

    public abstract Builder rerouteCount(int rerouteCount);

    public abstract Builder startTimestamp(Date startTimeStamp);

    public abstract Builder arrivalTimestamp(@Nullable Date arrivalTimestamp);

    public abstract Builder locationEngineName(String locationEngineName);

    public abstract Builder percentInForeground(int percentInForeground);

    public abstract Builder percentInPortrait(int percentInPortrait);

    public abstract SessionState build();
  }
}
