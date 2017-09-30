package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.services.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.api.directions.v5.MapboxDirections;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.commons.models.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Callback;

@AutoValue
public abstract class NavigationRoute {

  /**
   * @inheritDoc MapboxDirections#user
   */
  abstract String user();

  abstract String profile();

  @Nullable
  abstract Position origin();

  @Nullable
  abstract Position destination();

  abstract List<Position> coordinates();

  abstract String accessToken();

  abstract boolean alternatives();

  @Nullable
  abstract List<Double[]> bearings();

  @Nullable
  abstract double[] radiuses();

  abstract boolean congestion();

  @Nullable
  abstract String language();

  public void getRoute(@NonNull Callback<DirectionsResponse> callback) {
    List<Callback<DirectionsResponse>> callbacks = new ArrayList<>(1);
    callbacks.add(callback);
    getRoute(callbacks);
  }

  public void getRoute(@NonNull List<Callback<DirectionsResponse>> callbacks) {

    for (Callback<DirectionsResponse> callback : callbacks) {
      // TODO am I making the request here multiple times?
      getDirectionsRequest().enqueueCall(callback);
    }
  }

  MapboxDirections getDirectionsRequest() {


    MapboxDirections.Builder builder = new MapboxDirections.Builder()
      .setUser(user())
      .setProfile(profile())
      .setCoordinates(coordinates())
      .setAccessToken(accessToken()) // TODO use access token in mapboxNavigation
      .setAlternatives(alternatives())
      .setRadiuses(radiuses())
      .setAnnotation(congestion() ? DirectionsCriteria.ANNOTATION_CONGESTION : null)
      .setLanguage(language())
      .setGeometry(DirectionsCriteria.GEOMETRY_POLYLINE6)
      .setOverview(DirectionsCriteria.OVERVIEW_FULL)
      .setSteps(true)
      .setRoundaboutExits(true);

    if (!bearings().isEmpty()) {
      builder.setBearings(formatBearingValues());
    }

    return builder.build();
  }

  private double[][] formatBearingValues() {
    double[][] bearings = new double[coordinates().size()][];
    for (int i = 0; i < bearings().size(); i++) {
      double angle = bearings().get(i)[0];
      double tolerance = bearings().get(i)[1];
      bearings[i] = new double[] {angle, tolerance};
    }
    for (int i = bearings().size(); i < coordinates().size(); i++) {
      bearings[i] = new double[] {};
    }
    return bearings;
  }

  @AutoValue.Builder
  public abstract static class Builder {

    List<Double[]> bearings = new ArrayList<>();
    List<Position> coordinates = new ArrayList<>();

    public abstract Builder user(String user);

    public abstract Builder profile(String profile);

    abstract List<Position> coordinates();

    abstract Builder coordinates(List<Position> coordinates);

    public Builder addWaypoint(@NonNull Position waypoint) {
      coordinates.add(waypoint);
      return this;
    }

    abstract Position origin();

    public abstract Builder origin(Position origin);

    abstract Position destination();

    public abstract Builder destination(Position destination);

    public abstract Builder accessToken(String accessToken);

    public abstract Builder alternatives(boolean alternatives);

    abstract Builder bearings(List<Double[]> bearings);

    public Builder addBearing(@FloatRange(from = 0, to = 360) double angle,
                              @FloatRange(from = 0, to = 360) double tolerance) {
      bearings.add(new Double[] {angle, tolerance});
      return this;
    }

    public abstract Builder radiuses(double... radiuses);

    public abstract Builder congestion(boolean congestion);

    public abstract Builder language(String language);

    abstract NavigationRoute autoBuild(); // not public

    public NavigationRoute build() {
      if (origin() != null) {
        coordinates.add(0, origin());
      }
      if (destination() != null) {
        coordinates.add(destination());
      }

      bearings(Collections.unmodifiableList(bearings));
      coordinates(Collections.unmodifiableList(coordinates));
      return autoBuild();
    }
  }

  public static Builder builder() {
    return new AutoValue_NavigationRoute.Builder()
      .user(DirectionsCriteria.PROFILE_DEFAULT_USER)
      .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
      .alternatives(false)
      .congestion(true);
  }
}
