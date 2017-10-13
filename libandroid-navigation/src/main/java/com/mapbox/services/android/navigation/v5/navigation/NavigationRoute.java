package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.directions.v5.DirectionsCriteria;
import com.mapbox.directions.v5.DirectionsCriteria.AnnotationCriteria;
import com.mapbox.directions.v5.DirectionsCriteria.ProfileCriteria;
import com.mapbox.directions.v5.MapboxDirections;
import com.mapbox.directions.v5.models.DirectionsResponse;
import com.mapbox.geojson.Point;

import java.util.Locale;

import retrofit2.Callback;

public final class NavigationRoute {

  private final MapboxDirections mapboxDirections;

  public static Builder builder() {
    return new Builder();
  }

  private NavigationRoute(MapboxDirections mapboxDirections) {
    this.mapboxDirections = mapboxDirections;
  }

  public void getRoute(Callback<DirectionsResponse> callback) {
    mapboxDirections.enqueueCall(callback);
  }

  public static final class Builder {

    private final MapboxDirections.Builder directionsBuilder;

    /**
     * Private constructor for initializing the raw MapboxDirections.Builder
     */
    private Builder() {
      directionsBuilder = MapboxDirections.builder();
    }

    /**
     * The username for the account that the directions engine runs on. In most cases, this should
     * always remain the default value of {@link DirectionsCriteria#PROFILE_DEFAULT_USER}.
     *
     * @param user a non-null string which will replace the default user used in the directions
     *             request
     * @return this directionsBuilder for chaining options together
     * @since 1.0.0
     */
    public Builder user(@NonNull String user) {
      directionsBuilder.user(user);
      return this;
    }

    public Builder profile(@NonNull @ProfileCriteria String profile) {
      directionsBuilder.profile(profile);
      return this;
    }

    public Builder origin(@NonNull Point origin) {
      directionsBuilder.origin(origin);
      return this;
    }

    public Builder destination(@NonNull Point destination) {
      directionsBuilder.destination(destination);
      return this;
    }

    public Builder addWaypoint(@NonNull Point waypoint) {
      directionsBuilder.addWaypoint(waypoint);
      return this;
    }

    public Builder alternatives(@Nullable Boolean alternatives) {
      directionsBuilder.alternatives(alternatives);
      return this;
    }

    public Builder language(@Nullable Locale language) {
      directionsBuilder.language(language);
      return this;
    }

    public Builder annotations(@Nullable @AnnotationCriteria String... annotations) {
      directionsBuilder.annotations(annotations);
      return this;
    }

    public Builder addBearing(@Nullable @FloatRange(from = 0, to = 360) Double angle,
                              @Nullable @FloatRange(from = 0, to = 360) Double tolerance) {
      directionsBuilder.addBearing(angle, tolerance);
      return this;
    }

    public Builder radiuses(@FloatRange(from = 0) double... radiuses) {
      directionsBuilder.radiuses(radiuses);
      return this;
    }

    public Builder clientAppName(@NonNull String clientAppName) {
      directionsBuilder.clientAppName(clientAppName);
      return this;
    }

    public Builder accessToken(@NonNull String accessToken) {
      directionsBuilder.accessToken(accessToken);
      return this;
    }

    public Builder baseUrl(String baseUrl) {
      directionsBuilder.baseUrl(baseUrl);
      return this;
    }

    public NavigationRoute build() {
      // These are the default values required to have a directions
      // route worthy of navigating along.
      directionsBuilder.steps(true);
      directionsBuilder.continueStraight(true);
      directionsBuilder.geometries(DirectionsCriteria.GEOMETRY_POLYLINE6);
      directionsBuilder.overview(DirectionsCriteria.OVERVIEW_FULL);
      directionsBuilder.voiceInstructions(true);
      directionsBuilder.roundaboutExits(true);
      return new NavigationRoute(directionsBuilder.build());
    }
  }
}