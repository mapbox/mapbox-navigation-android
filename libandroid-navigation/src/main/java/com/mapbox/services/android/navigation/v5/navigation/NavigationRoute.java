package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.directions.v5.DirectionsCriteria;
import com.mapbox.directions.v5.MapboxDirections;
import com.mapbox.geojson.Point;

import java.util.Locale;

public class NavigationRoute {

  private final MapboxDirections mapboxDirections;

  private NavigationRoute(MapboxDirections mapboxDirections) {
    this.mapboxDirections = mapboxDirections;
  }


  public static final class Builder {

    private final MapboxDirections.Builder builder;

    /**
     * Private constructor for initializing the raw MapboxDirections.Builder
     */
    private Builder() {
      builder = MapboxDirections.builder();
    }

    /**
     * The username for the account that the directions engine runs on. In most cases, this should
     * always remain the default value of {@link DirectionsCriteria#PROFILE_DEFAULT_USER}.
     *
     * @param user a non-null string which will replace the default user used in the directions
     *             request
     * @return this builder for chaining options together
     * @since 1.0.0
     */
    public Builder user(@NonNull String user) {
      builder.user(user);
      return this;
    }

    public Builder profile(@NonNull @DirectionsCriteria.ProfileCriteria String profile) {
      builder.profile(profile);
      return this;
    }

    public Builder origin(@NonNull Point origin) {
      builder.origin(origin);
      return this;
    }

    public Builder destination(@NonNull Point destination) {
      builder.destination(destination);
      return this;
    }

    public Builder addWaypoint(@NonNull Point waypoint) {
      builder.addWaypoint(waypoint);
      return this;
    }

    public Builder alternatives(@Nullable Boolean alternatives) {
      builder.alternatives(alternatives);
      return this;
    }

    public Builder language(@Nullable Locale language) {
      builder.language(language);
      return this;
    }
    public Builder annotations(@Nullable @DirectionsCriteria.AnnotationCriteria String... annotations) {
      builder.annotations(annotations);
      return this;
    }

    public Builder addBearing(@Nullable @FloatRange(from = 0, to = 360) Double angle,
                              @Nullable @FloatRange(from = 0, to = 360) Double tolerance) {
      builder.addBearing(angle, tolerance);
      return this;
    }

    public Builder radiuses(@FloatRange(from = 0) double... radiuses) {
      builder.radiuses(radiuses);
      return this;
    }

    public Builder clientAppName(@NonNull String clientAppName) {
      builder.clientAppName(clientAppName);
      return this;
    }

    public Builder accessToken(@NonNull String accessToken) {
      builder.accessToken(accessToken);
      return this;
    }

    public Builder baseUrl(String baseUrl) {
      builder.baseUrl(baseUrl);
      return this;
    }

    public NavigationRoute build() {
      builder.steps(true);
      builder.continueStraight(true);
      builder.geometries(DirectionsCriteria.GEOMETRY_POLYLINE6);
      builder.overview(DirectionsCriteria.OVERVIEW_FULL)

      // TODO add roundaboutExit=true
      return new NavigationRoute(builder.build());
    }
  }
}