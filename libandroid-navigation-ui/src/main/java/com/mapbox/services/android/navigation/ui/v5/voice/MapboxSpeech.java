package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;

import com.mapbox.services.android.navigation.ui.v5.voice.polly.VoiceService;

import java.io.File;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MapboxSpeech {
  private final VoiceService voiceService;
  private final SpeechOptions speechOptions;
  private Context context;
  File file;
  InstructionDownloadListener listener;

  public MapboxSpeech(Context context, SpeechOptions speechOptions, InstructionDownloadListener listener) {
    this.listener = listener;
    this.context = context;
    this.speechOptions = speechOptions;
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl("https://api.mapbox.com/")
      .build();

    voiceService = retrofit.create(VoiceService.class);
  }

  public void getInstruction(String text) {
    Call<ResponseBody> call =  voiceService.getInstruction(
      text, speechOptions.textType(),
      speechOptions.language(),
      speechOptions.outputType(),
      "pk.eyJ1IjoiZGV2b3RhYWFiZWwiLCJhIjoiY2pheDBnNXQ2MHNpZjJ3bzlkZGoxd3hwcyJ9.WZ0SWvP_AipnhXhOCOWN_g");
  }

  public interface InstructionDownloadListener {
    public void onSuccess(File file);

    public void onFailure();

  }
}
