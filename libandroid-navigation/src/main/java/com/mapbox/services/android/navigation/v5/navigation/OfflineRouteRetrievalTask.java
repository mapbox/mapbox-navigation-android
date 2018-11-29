package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.RouterResult;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

class OfflineRouteRetrievalTask extends AsyncTask<OfflineRoute, Void, List<DirectionsRoute>> {
  private final Navigator navigator;
  private final OnOfflineRouteFoundCallback callback;

  OfflineRouteRetrievalTask(Navigator navigator, OnOfflineRouteFoundCallback callback) {
    this.navigator = navigator;
    this.callback = callback;
  }

  @Override
  protected List<DirectionsRoute> doInBackground(OfflineRoute... offlineRoutes) {
    RouterResult routerResult;

    String url = offlineRoutes[0].buildUrl();
    Timber.d("Request Url: " + url);

    synchronized (navigator) {
      routerResult = navigator.getRoute(url);
    }

    List<DirectionsRoute> routes = new ArrayList<>();

    DirectionsRoute directionsRoute = offlineRoutes[0].retrieveOfflineRoute(routerResult);
    if (directionsRoute != null) {
      routes.add(directionsRoute);
    }

    return routes;
  }

  @Override
  protected void onPostExecute(List<DirectionsRoute> routes) {
    callback.routesFound(routes);
  }
}
