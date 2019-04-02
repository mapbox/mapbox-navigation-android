package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;

import com.mapbox.geojson.Point;
import com.mapbox.navigator.Navigator;

class RemoveTilesTask extends AsyncTask<Void, Void, Long> {
  private final Navigator navigator;
  private final String tilePath;
  private final Point southwest;
  private final Point northeast;
  private final OnOfflineTilesRemovedCallback callback;

  RemoveTilesTask(Navigator navigator, String tilePath, Point southwest, Point northeast,
                  OnOfflineTilesRemovedCallback callback) {
    this.navigator = navigator;
    this.tilePath = tilePath;
    this.southwest = southwest;
    this.northeast = northeast;
    this.callback = callback;
  }

  @Override
  protected Long doInBackground(Void... paramsUnused) {
    return navigator.removeTiles(tilePath, southwest, northeast);
  }

  @Override
  protected void onPostExecute(Long numberOfTiles) {
    callback.onRemoved(numberOfTiles);
  }
}
