package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.RouteLeg;

import java.util.Date;

@AutoValue
abstract class SessionState {

  String originalGeometry() {
    return originalDirectionRoute().getGeometry();
  }

  int originalDuration() {
    return (int) originalDirectionRoute().getDuration();
  }

  int originalDistance() {
    return (int) originalDirectionRoute().getDistance();
  }

  int originalStepCount() {
    int stepCount = 0;
    for (RouteLeg leg : originalDirectionRoute().getLegs()) {
      stepCount += leg.getSteps().size();
    }
    return stepCount;
  }

  int currentStepCount() {
    int stepCount = 0;
    for (RouteLeg leg : currentDirectionRoute().getLegs()) {
      stepCount += leg.getSteps().size();
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

  abstract Date startTimestamp();

  @Nullable
  abstract Date arrivalTimestamp();

  abstract boolean mockLocation();

  abstract int rerouteCount();

  abstract Builder toBuilder();

  static Builder builder() {
    return new AutoValue_SessionState.Builder();
  }


  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder originalDirectionRoute(@NonNull DirectionsRoute directionsRoute);

    abstract Builder currentDirectionRoute(@NonNull DirectionsRoute directionsRoute);

    abstract Builder sessionIdentifier(@NonNull String sessionIdentifier);

    abstract Builder originalRequestIdentifier(@Nullable String originalRequestIdentifier);

    abstract Builder requestIdentifier(@Nullable String requestIdentifier);

    abstract Builder mockLocation(boolean mockLocation);

    abstract Builder rerouteCount(int rerouteCount);

    abstract Builder startTimestamp(@NonNull Date startTimeStamp);

    abstract Builder arrivalTimestamp(@Nullable Date arrivalTimestamp);

    abstract SessionState build();

  }
}
