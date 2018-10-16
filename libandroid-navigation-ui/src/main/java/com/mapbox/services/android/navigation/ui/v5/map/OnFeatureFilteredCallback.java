package com.mapbox.services.android.navigation.ui.v5.map;

import android.support.annotation.NonNull;

import com.mapbox.geojson.Feature;

interface OnFeatureFilteredCallback {
  void onFeatureFiltered(@NonNull Feature feature);
}
