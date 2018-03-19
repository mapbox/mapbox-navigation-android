package com.mapbox.services.android.navigation.v5.navigation;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoiceInstructionLoader {
  private static VoiceInstructionLoader instance;
  private MapboxSpeech.Builder mapboxSpeech;

  private VoiceInstructionLoader() {

  }

  public static synchronized VoiceInstructionLoader getInstance() {
    if (instance == null) {
      instance = new VoiceInstructionLoader();
    }

    return instance;
  }

  public void initialize(MapboxSpeech.Builder builder) {
    mapboxSpeech = builder;
  }

  public void getInstruction(String instruction, Callback<ResponseBody> callback) {
    mapboxSpeech.instruction(instruction).build().enqueueCall(callback);
  }

  public void cacheInstruction(String instruction) {
    mapboxSpeech.instruction(instruction).build().enqueueCall(new Callback<ResponseBody>() {
      @Override
      public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

      }

      @Override
      public void onFailure(Call<ResponseBody> call, Throwable t) {

      }
    });
  }
}
