package com.mapbox.services.android.navigation.ui.v5.voice;

import android.support.annotation.NonNull;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

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
    Timber.e(throwable, "onFailure cache instruction");
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