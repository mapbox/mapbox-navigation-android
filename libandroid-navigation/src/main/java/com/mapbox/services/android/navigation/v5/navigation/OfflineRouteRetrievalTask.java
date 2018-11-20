package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.RouterResult;

class OfflineRouteRetrievalTask extends CallbackAsyncTask<OfflineRoute, Void, DirectionsRoute> {
  private final Navigator navigator;

  OfflineRouteRetrievalTask(Navigator navigator, Callback<DirectionsRoute> callback) {
    super(callback);
    this.navigator = navigator;
  }

  @Override
  protected DirectionsRoute doInBackground(OfflineRoute... offlineRoutes) {
    RouterResult routerResult;

    synchronized (navigator) {
      routerResult = navigator.getRoute(offlineRoutes[0].buildUrl());
    }

    return offlineRoutes[0].retrieveOfflineRoute(routerResult);
  }
}
