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
  protected static MapboxSpeech mapboxSpeech;

  protected MapboxSpeech() {
    mapboxSpeech = this;
  }

  public void getInstruction(String instruction) {
    voiceService().getInstruction(
      instruction, textType(),
      language(),
      outputType(),
      accessToken()).enqueue(callback());
  }

  public void getInstruction(String instruction, Callback<ResponseBody> callback) {
    voiceService().getInstruction(
      instruction, textType(),
      language(),
      outputType(),
      accessToken()).enqueue(callback);
  }

  public static void cacheInstruction(String instruction) {
    if (mapboxSpeech != null) {
      mapboxSpeech.getInstruction(instruction, new Callback<ResponseBody>() {
        @Override
        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable throwable) {

        }
      });
    }
  }

  @Nullable
  abstract String language();

  @Nullable
  abstract String textType();

  @Nullable
  abstract String outputType();

  abstract File cacheDirectory();

  abstract String accessToken();

  abstract Callback<ResponseBody> callback();

  abstract VoiceService voiceService();


  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder language(String language);

    public abstract Builder textType(String textType);

    public abstract Builder outputType(String outputType);

    public abstract Builder accessToken(String accessToken);

    public abstract Builder callback(Callback<ResponseBody> callback);

    abstract Builder voiceService(VoiceService voiceService);

    public abstract Builder cacheDirectory(File cacheDirectory);

    abstract File cacheDirectory(); // doesn't reset cache directory if service alredy created

    abstract MapboxSpeech autoBuild();

    public MapboxSpeech build() {
      voiceService(getVoiceService());
      return autoBuild();
    }

    private VoiceService getVoiceService() {
      int cacheSize = 10 * 1024 * 1024; // 10 MB
      Cache cache = new Cache(cacheDirectory(), cacheSize);

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
      .language("en-us")
      .textType("text")
      .outputType("mp3");
  }
}
