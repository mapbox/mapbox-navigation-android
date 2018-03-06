package com.mapbox.services.android.navigation.ui.v5.voice;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.voice.polly.VoiceService;

import okhttp3.ResponseBody;
import retrofit2.Callback;
import retrofit2.Retrofit;

public class MapboxSpeech {
  private final VoiceService voiceService;
  private final SpeechOptions speechOptions;
  private Callback<ResponseBody> callback;

  public MapboxSpeech(SpeechOptions speechOptions, Callback<ResponseBody> callback) {
    this.speechOptions = speechOptions;
    this.callback = callback;

    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl("https://api.mapbox.com/")
      .build();

    voiceService = retrofit.create(VoiceService.class);
  }

  public void getInstruction(String text) {
    voiceService.getInstruction(
      text, speechOptions.textType(),
      speechOptions.language(),
      speechOptions.outputType(),
      Mapbox.getAccessToken()).enqueue(callback);
  }
}
