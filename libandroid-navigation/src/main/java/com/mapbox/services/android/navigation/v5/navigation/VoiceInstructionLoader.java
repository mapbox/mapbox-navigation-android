package com.mapbox.services.android.navigation.v5.navigation;

import okhttp3.ResponseBody;
import retrofit2.Callback;

public class VoiceInstructionLoader {
  private static VoiceInstructionLoader instance;
  private MapboxSpeech mapboxSpeech;

  private VoiceInstructionLoader() {

  }

  public static synchronized VoiceInstructionLoader getInstance() {
    if (instance == null) {
      instance = new VoiceInstructionLoader();
    }

    return instance;
  }

  public void initialize(MapboxSpeech.Builder builder) {
    mapboxSpeech = builder.build();
  }

  public void getInstruction(String instruction, Callback<ResponseBody> callback) {
    mapboxSpeech.getInstruction(instruction, callback);
  }

  public void cacheInstruction(String instruction) {
    mapboxSpeech.cacheInstruction(instruction);
  }
}
