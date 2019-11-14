package com.mapbox.services.android.navigation.v5.navigation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.api.speech.v1.MapboxSpeech;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cache;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AutoValue
public abstract class VoiceInstructionLoader {
  private static final int NUMBER_TO_CACHE = 3;
  private static final int CACHE_INDEX = NUMBER_TO_CACHE - 1;
  private static VoiceInstructionLoader instance = null;

  /**
   * Returns the singleton instance of VoiceInstructionLoader. It must first be initialized through
   * a builder.
   *
   * @return current instance if it's initialized, or null
   */
  public static synchronized VoiceInstructionLoader getInstance() {
    return instance;
  }

  /**
   * Makes the call to MapboxSpeech to get the given string instruction as a sound file.
   *
   * @param instruction text to dictate
   * @param textType "ssml" or "text"
   * @param callback to relay retrofit status
   */
  public void getInstruction(String instruction, String textType, Callback<ResponseBody> callback) {
    getMapboxBuilder()
      .instruction(instruction)
      .textType(textType)
      .build()
      .enqueueCall(callback);
  }

  /**
   * Makes call to MapboxSpeech with empty callbacks. This is so that the result is cached in the
   * cache specified in the builder.
   *
   * @param routeProgress to get instructions from
   * @param isFirst whether this is the first call. This way, if we're caching three ahead, on the
   *                first call the first two instructions will also be cached.
   */
  public void cacheInstructions(RouteProgress routeProgress, boolean isFirst) {
    List<VoiceInstructions> voiceInstructionsList = getNextInstructions(routeProgress);

    if (isFirst) {
      cacheUpToNthInstruction(voiceInstructionsList, CACHE_INDEX);
    } else {
      cacheNthInstruction(voiceInstructionsList, CACHE_INDEX);
    }
  }

  private void cacheUpToNthInstruction(List<VoiceInstructions> voiceInstructionsList, int exclusiveIndex) {
    for (int i = 0; i < exclusiveIndex; i++) {
      cacheNthInstruction(voiceInstructionsList, i);
    }
  }

  private void cacheNthInstruction(List<VoiceInstructions> voiceInstructionsList, int index) {
    if (voiceInstructionsList.size() > index) {
      cacheInstruction(voiceInstructionsList.get(index).ssmlAnnouncement());
    }
  }

  private void cacheInstruction(String instruction) {
    getMapboxBuilder().instruction(instruction).build().enqueueCall(new Callback<ResponseBody>() {
      @Override
      public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        // Intentionally empty
      }

      @Override
      public void onFailure(Call<ResponseBody> call, Throwable throwable) {
        // Intentionally empty
      }
    });
  }

  @Nullable
  abstract String language();

  @Nullable
  abstract String textType();

  @Nullable
  abstract String outputType();

  @Nullable
  abstract Cache cache();

  @NonNull
  abstract String accessToken();

  private MapboxSpeech.Builder getMapboxBuilder() {
    MapboxSpeech.Builder builder = MapboxSpeech.builder().accessToken(accessToken());

    if (language() != null) {
      builder.language(language());
    }
    if (textType() != null) {
      builder.textType(textType());
    }
    if (outputType() != null) {
      builder.outputType(outputType());
    }
    if (cache() != null) {
      builder.cache(cache());
    }

    return builder;
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

    return instructions;
  }


  @AutoValue.Builder
  public abstract static class Builder {
    /**
     * Language of which to request the instructions be spoken. Default is "en-us"
     *
     * @param language as a string, i.e., "en-us"
     * @return this builder for chaining options together
     */
    public abstract Builder language(String language);

    /**
     * Format which the input is specified. If not specified, default is text
     *
     * @param textType either text or ssml
     * @return this builder for chaining options together
     */
    public abstract Builder textType(String textType);

    /**
     * Output format for spoken instructions. If not specified, default is mp3
     *
     * @param outputType either mp3 or json
     * @return this builder for chaining options together
     */
    public abstract Builder outputType(String outputType);

    /**
     * Required to call when this is being built.
     *
     * @param accessToken Mapbox access token, You must have a Mapbox account in order to use
     *                    the Optimization API
     * @return this builder for chaining options together
     */
    public abstract Builder accessToken(@NonNull String accessToken);

    /**
     * Adds an optional cache to set in the OkHttp client.
     *
     * @param cache to set for OkHttp
     * @return this builder for chaining options together
     */
    public abstract Builder cache(Cache cache);

    abstract VoiceInstructionLoader autoBuild();

    public VoiceInstructionLoader build() {
      instance = autoBuild();
      return instance;
    }
  }

  public static Builder builder() {
    return new AutoValue_VoiceInstructionLoader.Builder();
  }
}
