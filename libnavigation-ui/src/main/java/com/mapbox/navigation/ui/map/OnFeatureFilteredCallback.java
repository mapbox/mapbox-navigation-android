package com.mapbox.navigation.ui.map;

import androidx.annotation.NonNull;

import com.mapbox.geojson.Feature;

interface OnFeatureFilteredCallback {
  void onFeatureFiltered(@NonNull Feature feature);
}
