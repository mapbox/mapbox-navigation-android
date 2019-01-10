package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.RouterResult;

import timber.log.Timber;

class OfflineRouteRetrievalTask extends AsyncTask<OfflineRoute, Void, DirectionsRoute> {
  private static final int FIRST_ROUTE = 0;
  private final Navigator navigator;
  private final OnOfflineRouteFoundCallback callback;
  private RouterResult routerResult;

  OfflineRouteRetrievalTask(Navigator navigator, OnOfflineRouteFoundCallback callback) {
    this.navigator = navigator;
    this.callback = callback;
  }

  // For testing only
  OfflineRouteRetrievalTask(Navigator navigator, OnOfflineRouteFoundCallback callback, RouterResult routerResult) {
    this(navigator, callback);
    this.routerResult = routerResult;
  }

  @Override
  protected DirectionsRoute doInBackground(OfflineRoute... offlineRoutes) {
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
      String errorMessage = generateErrorMessage();
      OfflineError error = new OfflineError(errorMessage);
      callback.onError(error);
    }
  }

  @NonNull
  private String generateErrorMessage() {
    String jsonResponse = routerResult.getJson();
    Gson gson = new Gson();
    OfflineRouteError routeError = gson.fromJson(jsonResponse, OfflineRouteError.class);
    String errorMessage = String.format("Error occurred fetching offline route: %s - Code: %d", routeError.getError(),
      routeError.getErrorCode());
    Timber.e(errorMessage);
    return errorMessage;
  }
}
