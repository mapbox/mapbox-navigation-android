package com.mapbox.services.android.navigation.ui.v5.map;

import android.location.Location;
import android.os.AsyncTask;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;

import java.util.List;

class FeatureFilterTask extends AsyncTask<Void, Void, Feature> {

  private final WaynameFeatureFilter filter;
  private final OnFeatureFilteredCallback callback;

  FeatureFilterTask(List<Feature> queriedFeatures, Location currentLocation,
                    List<Point> currentStepPoints, OnFeatureFilteredCallback callback) {
    filter = new WaynameFeatureFilter(queriedFeatures, currentLocation, currentStepPoints);
    this.callback = callback;
  }

  @Override
  protected Feature doInBackground(Void... voids) {
    return filter.filterFeatures();
  }

  @Override
  protected void onPostExecute(Feature feature) {
    super.onPostExecute(feature);
    callback.onFeatureFiltered(feature);
  }
}
