package com.mapbox.services.android.navigation.ui.v5.voice;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.voice.polly.VoiceService;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;

public class MapboxSpeech {
  private final VoiceService voiceService;
  private final SpeechOptions speechOptions;
  private Callback callback;

  public MapboxSpeech(SpeechOptions speechOptions) {
    this.speechOptions = speechOptions;
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl("https://api.mapbox.com/")
      .build();

    voiceService = retrofit.create(VoiceService.class);
  }

  public void setCallback(Callback callback) {
    this.callback = callback;
  }

  public Call<ResponseBody> getInstruction(String text) {
    return voiceService.getInstruction(
      text, speechOptions.textType(),
      speechOptions.language(),
      speechOptions.outputType(),
      Mapbox.getAccessToken());
  }
}
