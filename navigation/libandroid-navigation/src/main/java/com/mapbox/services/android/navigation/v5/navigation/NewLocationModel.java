package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class NewLocationModel {

  static NewLocationModel create(Location location, MapboxNavigation mapboxNavigation) {
    return new AutoValue_NewLocationModel(location, mapboxNavigation);
  }

  abstract Location location();

  abstract MapboxNavigation mapboxNavigation();
}
