package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;

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
      public void onFailure(Call<ResponseBody> call, Throwable throwable) {

      }
    });
  }

  public void cacheInstructions(RouteProgress routeProgress, boolean first) {
    int stepIndex = routeProgress.currentLegProgress().stepIndex();
    List<LegStep> steps = routeProgress.currentLeg().steps();
    List<VoiceInstructions> instructions = new ArrayList<>();

    while (instructions.size() < 3 && stepIndex < steps.size()) {
      List<VoiceInstructions> currentStepInstructions = steps.get(stepIndex++).voiceInstructions();
      if (currentStepInstructions.size() < 4) {
        instructions.addAll(currentStepInstructions);
      } else { // in case there are a large number of instructions
        instructions.addAll(currentStepInstructions.subList(0, 3));
      }
    }

    if (first) {
      if (instructions.size() > 0) {
        cacheInstruction(instructions.get(0).ssmlAnnouncement());
      }

      if (instructions.size() > 1) {
        cacheInstruction(instructions.get(1).ssmlAnnouncement());
      }
    } else {
      if (instructions.size() > 2) {
        cacheInstruction(instructions.get(2).ssmlAnnouncement());
      }
    }
  }
}
