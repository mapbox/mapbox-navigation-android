package com.mapbox.services.android.navigation.ui.v5.route;

import android.os.AsyncTask;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.FIRST_COLLECTION_INDEX;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.PRIMARY_ROUTE_PROPERTY_KEY;

class PrimaryRouteUpdateTask extends AsyncTask<Void, Void, List<FeatureCollection>> {

  private final int newPrimaryIndex;
  private final List<FeatureCollection> routeFeatureCollections;
  private final OnPrimaryRouteUpdatedCallback callback;

  PrimaryRouteUpdateTask(int newPrimaryIndex, List<FeatureCollection> routeFeatureCollections,
                                OnPrimaryRouteUpdatedCallback callback) {
    this.newPrimaryIndex = newPrimaryIndex;
    this.routeFeatureCollections = routeFeatureCollections;
    this.callback = callback;
  }

  @Override
  protected List<FeatureCollection> doInBackground(Void... voids) {
    List<FeatureCollection> updatedRouteCollections = new ArrayList<>(routeFeatureCollections);
    // Update the primary new collection
    FeatureCollection primaryCollection = updatedRouteCollections.remove(newPrimaryIndex);
    List<Feature> primaryFeatures = primaryCollection.features();
    if (primaryFeatures == null || primaryFeatures.isEmpty()) {
      return routeFeatureCollections;
    }
    for (Feature feature : primaryFeatures) {
      feature.addBooleanProperty(PRIMARY_ROUTE_PROPERTY_KEY, true);
    }
    // Update non-primary collections (not including the primary)
    for (FeatureCollection nonPrimaryCollection : updatedRouteCollections) {
      List<Feature> nonPrimaryFeatures = nonPrimaryCollection.features();
      if (nonPrimaryFeatures == null || nonPrimaryFeatures.isEmpty()) {
        continue;
      }
      for (Feature feature : nonPrimaryFeatures) {
        feature.addBooleanProperty(PRIMARY_ROUTE_PROPERTY_KEY, false);
      }
    }
    updatedRouteCollections.add(FIRST_COLLECTION_INDEX, primaryCollection);
    return updatedRouteCollections;
  }

  @Override
  protected void onPostExecute(List<FeatureCollection> updatedRouteCollections) {
    callback.onPrimaryRouteUpdated(updatedRouteCollections);
  }
}
