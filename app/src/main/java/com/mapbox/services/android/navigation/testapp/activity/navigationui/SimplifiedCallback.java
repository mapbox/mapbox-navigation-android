package com.mapbox.services.android.navigation.testapp.activity.navigationui;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.navigation.base.logger.model.Message;
import com.mapbox.navigation.logger.MapboxLogger;

import retrofit2.Call;
import retrofit2.Callback;

/**
 * Helper class to reduce redundant logging code when no other action is taken in onFailure
 */
public abstract class SimplifiedCallback implements Callback<DirectionsResponse> {
  @Override
  public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
    MapboxLogger.INSTANCE.e(new Message(throwable.getMessage()), throwable);
  }
}
