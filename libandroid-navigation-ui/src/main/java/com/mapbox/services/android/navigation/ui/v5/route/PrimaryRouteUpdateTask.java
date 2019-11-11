package com.mapbox.services.android.navigation.ui.v5.route;

import android.os.Handler;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.FIRST_COLLECTION_INDEX;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.PRIMARY_ROUTE_PROPERTY_KEY;

class PrimaryRouteUpdateTask extends Thread {

  private final int newPrimaryIndex;
  private final List<FeatureCollection> routeFeatureCollections;
  private final WeakReference<OnPrimaryRouteUpdatedCallback> callbackWeakReference;
  private AtomicBoolean cancelThread = new AtomicBoolean(false);
  private Handler postHandler;

  PrimaryRouteUpdateTask(int newPrimaryIndex, List<FeatureCollection> routeFeatureCollections,
                         OnPrimaryRouteUpdatedCallback callback, Handler handler) {
    this.newPrimaryIndex = newPrimaryIndex;
    this.routeFeatureCollections = routeFeatureCollections;
    this.callbackWeakReference = new WeakReference<>(callback);
    this.postHandler = handler;
  }

  void cancel() {
    cancelThread.set(true);
  }

  @Override
  public void run() {
    List<FeatureCollection> updatedRouteCollections = new ArrayList<>(routeFeatureCollections);
    if (updatedRouteCollections.isEmpty()) {
      return;
    }

    // Update the primary new collection
    if (cancelThread.get()) {
      return;
    }
    FeatureCollection primaryCollection = updatedRouteCollections.remove(newPrimaryIndex);
    List<Feature> primaryFeatures = primaryCollection.features();
    if (primaryFeatures == null || primaryFeatures.isEmpty()) {
      return;
    }
    for (Feature feature : primaryFeatures) {
      if (cancelThread.get()) {
        return;
      }
      feature.addBooleanProperty(PRIMARY_ROUTE_PROPERTY_KEY, true);
    }
    // Update non-primary collections (not including the primary)
    for (FeatureCollection nonPrimaryCollection : updatedRouteCollections) {
      if (cancelThread.get()) {
        return;
      }
      List<Feature> nonPrimaryFeatures = nonPrimaryCollection.features();
      if (nonPrimaryFeatures == null || nonPrimaryFeatures.isEmpty()) {
        continue;
      }
      for (Feature feature : nonPrimaryFeatures) {
        if (cancelThread.get()) {
          return;
        }
        feature.addBooleanProperty(PRIMARY_ROUTE_PROPERTY_KEY, false);
      }
    }
    if (cancelThread.get()) {
      return;
    }
    updatedRouteCollections.add(FIRST_COLLECTION_INDEX, primaryCollection);
    if (!cancelThread.get()) {
      complete(updatedRouteCollections);
    }
  }

  private void complete(final List<FeatureCollection> updatedRouteCollections) {
    final OnPrimaryRouteUpdatedCallback callback = callbackWeakReference.get();
    if (callback != null) {
      postHandler.post(new Runnable() {
        @Override
        public void run() {
          if (cancelThread.get()) {
            return;
          }
          callback.onPrimaryRouteUpdated(updatedRouteCollections);
        }
      });
    }
  }
}
