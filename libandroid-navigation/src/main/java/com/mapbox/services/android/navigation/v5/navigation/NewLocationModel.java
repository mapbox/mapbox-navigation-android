package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.google.auto.value.AutoValue;
import com.mapbox.services.android.navigation.v5.utils.RingBuffer;

@AutoValue
abstract class NewLocationModel {

  static NewLocationModel create(Location location, MapboxNavigation mapboxNavigation,
                                 RingBuffer distancesAwayFromManeuver) {
    return new AutoValue_NewLocationModel(location, mapboxNavigation,
      distancesAwayFromManeuver);
  }

  abstract Location location();

  abstract MapboxNavigation mapboxNavigation();

  abstract RingBuffer distancesAwayFromManeuver();
}
