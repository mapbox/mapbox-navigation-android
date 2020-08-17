package com.mapbox.navigation.ui.map;

import android.location.Location;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;

import java.util.List;

class FeatureFilterTask extends AsyncTask<Void, Void, Feature> {

  @NonNull
  private final WaynameFeatureFilter filter;
  private final OnFeatureFilteredCallback callback;

  FeatureFilterTask(List<Feature> queriedFeatures, @NonNull Location currentLocation,
                    List<Point> currentStepPoints, OnFeatureFilteredCallback callback) {
    filter = new WaynameFeatureFilter(queriedFeatures, currentLocation, currentStepPoints);
    this.callback = callback;
  }

  @NonNull
  @Override
  protected Feature doInBackground(Void... voids) {
    return filter.filterFeatures();
  }

  @Override
  protected void onPostExecute(@NonNull Feature feature) {
    super.onPostExecute(feature);
    callback.onFeatureFiltered(feature);
  }
}
