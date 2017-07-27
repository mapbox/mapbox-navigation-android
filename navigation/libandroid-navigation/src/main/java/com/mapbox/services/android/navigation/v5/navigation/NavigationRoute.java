package com.mapbox.services.android.navigation.v5.navigation;

import com.google.auto.value.AutoValue;
import com.mapbox.services.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.api.directions.v5.MapboxDirections;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.commons.models.Position;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Callback;

//@AutoValue
public abstract class NavigationRoute {

  abstract String user();

  abstract String profile();

  abstract List<Position> coordinates();

  abstract String accessToken();

  abstract boolean alternatives();

  abstract double[][] bearings();

  abstract double[] radiuses();

  abstract boolean congestion();

  abstract String language();

  public void getRoute(Callback<DirectionsResponse> callback) {
    List<Callback<DirectionsResponse>> callbacks = new ArrayList<>(1);
    callbacks.add(callback);
    getRoute(callbacks);
  }

  public void getRoute(List<Callback<DirectionsResponse>> callbacks) {
    MapboxDirections mapboxDirections = new MapboxDirections.Builder()
      .setUser(user())
      .setProfile(profile())
      .setCoordinates(coordinates())
      .setAccessToken(accessToken())
      .setAlternatives(alternatives())
      .setBearings(bearings())
      .setRadiuses(radiuses())
      .setAnnotation(congestion() ? DirectionsCriteria.ANNOTATION_CONGESTION : null)
//      .setLanguage(language())
      .setGeometry(DirectionsCriteria.OVERVIEW_FULL)
      .build();

    for (Callback<DirectionsResponse> callback : callbacks) {
      mapboxDirections.enqueueCall(callback);
    }
  }

//  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder user(String user);

    public abstract Builder profile(String profile);

    public abstract Builder coordinates(List<Position> coordinates);

    public abstract Builder origin(Position origin);

    public abstract Builder destination(Position destination);

    public abstract Builder accessToken(String accessToken);

    public abstract Builder alternatives(boolean alternatives);

    public abstract Builder bearings(double[]... bearings);

    public abstract Builder radiuses(double... radiuses);

    public abstract Builder congestion(boolean congestion);

    public abstract Builder language(String language);

    public abstract NavigationRoute build();
  }

//  public static Builder builder() {
//    return new AutoValue_NavigationRoute.Builder();
//  }
}
