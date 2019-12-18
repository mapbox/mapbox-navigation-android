package com.mapbox.services.android.navigation.ui.v5.voice;


import androidx.annotation.NonNull;

import com.mapbox.navigation.base.logger.model.Message;
import com.mapbox.navigation.logger.MapboxLogger;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

class InstructionCacheCallback implements Callback<ResponseBody> {

  private final VoiceInstructionLoader loader;

  InstructionCacheCallback(VoiceInstructionLoader loader) {
    this.loader = loader;
  }

  @Override
  public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
    if (closeResponseBody(response)) {
      String url = call.request().url().toString();
      loader.addCachedUrl(url);
    }
  }

  @Override
  public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
    MapboxLogger.INSTANCE.e(new Message("onFailure cache instruction"), throwable);
  }

  private boolean closeResponseBody(@NonNull Response<ResponseBody> response) {
    ResponseBody body = response.body();
    if (body != null) {
      body.byteStream();
      body.close();
      return true;
    }
    return false;
  }
}