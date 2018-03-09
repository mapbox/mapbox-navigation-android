package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.File;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static com.mapbox.core.constants.Constants.BASE_API_URL;

@AutoValue
public abstract class MapboxSpeech {
  public void getInstruction(String instruction, Callback<ResponseBody> callback) {
    voiceService().getInstruction(
      instruction, textType(),
      language(),
      outputType(),
      accessToken()).enqueue(callback);
  }

  public void cacheInstruction(String instruction) {
    getInstruction(instruction, new Callback<ResponseBody>() {
      @Override
      public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

      }

      @Override
      public void onFailure(Call<ResponseBody> call, Throwable throwable) {

      }
    });
  }

  @Nullable
  abstract String language();

  @Nullable
  abstract String textType();

  @Nullable
  abstract String outputType();

  abstract File cacheDirectory();

  abstract String accessToken();

  abstract VoiceService voiceService();


  @AutoValue.Builder
  public abstract static class Builder {
    long cacheSize;

    public abstract Builder language(String language);

    public abstract Builder textType(String textType);

    public abstract Builder outputType(String outputType);

    public abstract Builder accessToken(String accessToken);

    public Builder cacheSize(long cacheSize) {
      this.cacheSize = cacheSize;
      return this;
    }

    long cacheSize() {
      return cacheSize;
    }

    abstract Builder voiceService(VoiceService voiceService);

    public abstract Builder cacheDirectory(File cacheDirectory);

    abstract File cacheDirectory();

    abstract MapboxSpeech autoBuild();

    public MapboxSpeech build() {
      voiceService(getVoiceService());
      return autoBuild();
    }

    private VoiceService getVoiceService() {
      Cache cache = new Cache(cacheDirectory(), cacheSize());

      OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .cache(cache)
        .build();

      Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(BASE_API_URL)
        .client(okHttpClient)
        .build();

      return retrofit.create(VoiceService.class);
    }
  }

  public static Builder builder() {
    return new AutoValue_MapboxSpeech.Builder()
      .cacheSize(10 * 1024 * 1024); // default cache size is 10 MB
  }
}