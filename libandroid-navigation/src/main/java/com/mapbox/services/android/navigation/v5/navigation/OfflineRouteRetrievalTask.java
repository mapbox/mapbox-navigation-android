package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.RouterResult;

class OfflineRouteRetrievalTask extends AsyncTask<OfflineRoute, Void, DirectionsRoute> {
  private static final int FIRST_ROUTE = 0;
  private final Navigator navigator;
  private final OnOfflineRouteFoundCallback callback;

  OfflineRouteRetrievalTask(Navigator navigator, OnOfflineRouteFoundCallback callback) {
    this.navigator = navigator;
    this.callback = callback;
  }

  @Override
  protected DirectionsRoute doInBackground(OfflineRoute... offlineRoutes) {
    RouterResult routerResult;
    String url = offlineRoutes[FIRST_ROUTE].buildUrl();

    synchronized (navigator) {
      routerResult = navigator.getRoute(url);
    }

    return offlineRoutes[FIRST_ROUTE].retrieveOfflineRoute(routerResult);
  }

  @Override
  protected void onPostExecute(DirectionsRoute offlineRoute) {
    if (offlineRoute != null) {
      callback.onRouteFound(offlineRoute);
    } else {
      OfflineError error = new OfflineError("Offline route was not found");
      callback.onError(error);
    }
  }
}
