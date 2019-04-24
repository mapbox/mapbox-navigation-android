package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.geojson.BoundingBox;

/**
 * Listener that needs to be added to
 * {@link MapboxOfflineRouter#removeTiles(String, BoundingBox, OnOfflineTilesRemovedCallback)} to know when the routing
 * tiles within the provided {@link BoundingBox} have been removed
 */
public interface OnOfflineTilesRemovedCallback {

  /**
   * Called when the routing tiles within the provided {@link BoundingBox} have been removed completely.
   *
   * @param numberOfTiles removed within the {@link BoundingBox} provided
   */
  void onRemoved(long numberOfTiles);
}
