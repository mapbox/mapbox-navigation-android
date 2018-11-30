package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;

import com.mapbox.api.routetiles.v1.versions.MapboxRouteTileVersions;
import com.mapbox.api.routetiles.v1.versions.models.RouteTileVersionsResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This is a wrapper class for the {@link MapboxRouteTileVersions} class. It returns a list of
 * all available versions of Routing Tiles available via {@link OfflineTiles}. This class
 * encapsulates the unwrapping of the list from the response.
 */
class OfflineTileVersions {

  /**
   * Call to receive all the available versions of Offline Tiles available.
   *
   * @param accessToken for the API call
   * @param callback    to be updated with the versions
   */
  void fetchRouteTileVersions(String accessToken, final OnTileVersionsFoundCallback callback) {
    MapboxRouteTileVersions mapboxRouteTileVersions = buildTileVersionsWith(accessToken);
    mapboxRouteTileVersions.enqueueCall(new Callback<RouteTileVersionsResponse>() {
      @Override
      public void onResponse(@NonNull Call<RouteTileVersionsResponse> call,
                             @NonNull Response<RouteTileVersionsResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
          callback.onVersionsFound(response.body().availableVersions());
        } else {
          callback.onError(new OfflineError("Tile version response was unsuccessful"));
        }
      }

      @Override
      public void onFailure(@NonNull Call<RouteTileVersionsResponse> call, @NonNull Throwable throwable) {
        OfflineError error = new OfflineError(throwable.getMessage());
        callback.onError(error);
      }
    });
  }

  private MapboxRouteTileVersions buildTileVersionsWith(String accessToken) {
    return MapboxRouteTileVersions.builder()
      .accessToken(accessToken)
      .build();
  }
}
