package com.mapbox.services.android.navigation.v5.navigation.offline;

import com.mapbox.api.routetiles.v1.versions.MapboxRouteTileVersions;

import retrofit2.Callback;

/**
 * This is a wrapper class for the {@link MapboxRouteTileVersions} class. It returns a list of
 * all available versions of Routing Tiles available via {@link OfflineTiles}. This class
 * encapsulates the unwrapping of the list from the response.
 */
public class OfflineTileVersions {

  private final MapboxRouteTileVersions mapboxRouteTileVersions;

  /**
   * Creates a new OfflineTileVersions object with the given access token.
   *
   * @param accessToken to use
   */
  public OfflineTileVersions(String accessToken) {
    this.mapboxRouteTileVersions =
      MapboxRouteTileVersions.builder()
        .accessToken(accessToken)
        .build();
  }

  /**
   * Call to receive all the available versions of Offline Tiles available.
   *
   * @param callback to be updated with the versions
   */
  public void getRouteTileVersions(Callback callback) {
    mapboxRouteTileVersions.enqueueCall(callback);
  }
}
