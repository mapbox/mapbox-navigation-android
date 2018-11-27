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
  private final RouteFoundCallback callback;

  OfflineRouteRetrievalTask(Navigator navigator, RouteFoundCallback callback) {
    this.navigator = navigator;
    this.callback = callback;
  }

  @Override
  protected List<DirectionsRoute> doInBackground(OfflineRoute... offlineRoutes) {
    RouterResult routerResult;

    synchronized (navigator) {
      String str = offlineRoutes[0].buildUrl();
      Timber.d("Request Url: " + str);

      routerResult = navigator.getRoute(str);
    }

    List<DirectionsRoute> routes = new ArrayList<>();

    for (OfflineRoute offlineRoute : offlineRoutes) {
      DirectionsRoute directionsRoute = offlineRoute.retrieveOfflineRoute(routerResult);
      if (directionsRoute != null) {
        routes.add(directionsRoute);
      }
    }

    return routes;
  }

  @Override
  protected void onPostExecute(List<DirectionsRoute> routes) {
    callback.routesFound(routes);
  }
}
