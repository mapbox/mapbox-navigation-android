package com.mapbox.services.android.navigation.v5.navigation;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class NavigationIndices {

  static NavigationIndices create(int legIndex, int stepIndex) {
    return new AutoValue_NavigationIndices(legIndex, stepIndex);
  }

  abstract int legIndex();

  abstract int stepIndex();
}