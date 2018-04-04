package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.api.speech.v1.MapboxSpeech;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoiceInstructionLoader {
  private static final int NUMBER_TO_CACHE = 3;
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

  /**
   * Initializes singleton with details that will exist for all calls to MapboxSpeech, i.e. cache,
   * locale, accessToken.
   *
   * @param builder to reuse
   */
  public void initialize(MapboxSpeech.Builder builder) {
    mapboxSpeech = builder;
  }

  /**
   * Makes the call to MapboxSpeech to get the given string instruction as a sound file.
   *
   * @param instruction text to dictate
   * @param textType "ssml" or "text"
   * @param callback to relay retrofit status
   */
  public void getInstruction(String instruction, String textType, Callback<ResponseBody> callback) {
    mapboxSpeech.instruction(instruction)
      .textType(textType)
      .build()
      .enqueueCall(callback);
  }

  /**
   * Makes call to MapboxSpeech
   *
   * @param routeProgress
   * @param first
   */
  public void cacheInstructions(RouteProgress routeProgress, boolean first) {
    List<VoiceInstructions> voiceInstructions = getNextInstructions(routeProgress);

    if (first) {
      for (int i = 0; i <= NUMBER_TO_CACHE - 1; i++) {
        if (voiceInstructions.size() > i) {
          cacheInstruction(voiceInstructions.get(i).ssmlAnnouncement());
        }
      }
    } else {
      if (voiceInstructions.size() >= NUMBER_TO_CACHE) {
        cacheInstruction(voiceInstructions.get(NUMBER_TO_CACHE - 1).ssmlAnnouncement());
      }
    }
  }

  private void cacheInstruction(String instruction) {
    mapboxSpeech.instruction(instruction).build().enqueueCall(new Callback<ResponseBody>() {
      @Override
      public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

      }

      @Override
      public void onFailure(Call<ResponseBody> call, Throwable throwable) {

      }
    });
  }

  private List<VoiceInstructions> getNextInstructions(RouteProgress routeProgress) {
    int stepIndex = routeProgress.currentLegProgress().stepIndex();
    List<LegStep> steps = routeProgress.currentLeg().steps();
    List<VoiceInstructions> instructions = new ArrayList<>();

    while (instructions.size() < NUMBER_TO_CACHE && stepIndex < steps.size()) {
      List<VoiceInstructions> currentStepInstructions = steps.get(stepIndex++).voiceInstructions();
      if (currentStepInstructions.size() <= NUMBER_TO_CACHE) {
        instructions.addAll(currentStepInstructions);
      } else {
        instructions.addAll(currentStepInstructions.subList(0, NUMBER_TO_CACHE));
      }
    }
  }
}
