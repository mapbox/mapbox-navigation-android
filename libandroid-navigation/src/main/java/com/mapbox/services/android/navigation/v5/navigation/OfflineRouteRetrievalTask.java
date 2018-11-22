package com.mapbox.services.android.navigation.v5.navigation;


import android.os.AsyncTask;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.RouterResult;

import timber.log.Timber;

class OfflineRouteRetrievalTask extends AsyncTask<OfflineRoute, Void, DirectionsRoute> {
  private final Navigator navigator;
  private final OfflineRouteFoundCallback callback;
  RouterResult routerResult;

  OfflineRouteRetrievalTask(Navigator navigator, OfflineRouteFoundCallback callback) {
    this.navigator = navigator;
    this.callback = callback;
  }

  @Override
  protected DirectionsRoute doInBackground(OfflineRoute... offlineRoutes) {
    synchronized (navigator) {
      String s = offlineRoutes[0].buildUrl();
      Timber.d("Offline Route Url: " + s);

      routerResult = navigator.getRoute(s);
    }

    return offlineRoutes[0].retrieveOfflineRoute(routerResult);
  }

  @Override
  protected void onPostExecute(DirectionsRoute directionsRoute) {
      callback.onOfflineRouteFound(directionsRoute);
  }
}
