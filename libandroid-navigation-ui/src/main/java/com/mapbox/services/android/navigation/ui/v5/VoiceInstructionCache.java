package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.services.android.navigation.ui.v5.voice.VoiceInstructionLoader;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import java.util.ArrayList;
import java.util.List;

class VoiceInstructionCache {

  private static final int MAX_VOICE_INSTRUCTIONS_TO_CACHE = 10;
  private static final int VOICE_INSTRUCTIONS_TO_CACHE_THRESHOLD = 5;
  private final MapboxNavigation navigation;
  private final VoiceInstructionLoader voiceInstructionLoader;
  private int totalVoiceInstructions = 0;
  private int currentVoiceInstructionsCachedIndex = 0;
  private boolean isVoiceInstructionsToCacheThresholdReached = false;

  VoiceInstructionCache(MapboxNavigation navigation, VoiceInstructionLoader voiceInstructionLoader) {
    this.navigation = navigation;
    this.voiceInstructionLoader = voiceInstructionLoader;
  }

  void preCache(DirectionsRoute route) {
    totalVoiceInstructions = 0;
    currentVoiceInstructionsCachedIndex = 0;
    isVoiceInstructionsToCacheThresholdReached = false;
    for (int i = 0; i < route.legs().size(); i++) {
      RouteLeg leg = route.legs().get(i);
      for (int j = 0; j < leg.steps().size(); j++) {
        LegStep step = leg.steps().get(j);
        for (VoiceInstructions ignored : step.voiceInstructions()) {
          totalVoiceInstructions++;
        }
      }
    }
    List<String> voiceInstructionsToCache = new ArrayList<>();
    for (int i = currentVoiceInstructionsCachedIndex; i < totalVoiceInstructions; i++) {
      voiceInstructionsToCache.add(navigation.retrieveSsmlAnnouncementInstruction(i));
      currentVoiceInstructionsCachedIndex++;
      if ((currentVoiceInstructionsCachedIndex + 1) % MAX_VOICE_INSTRUCTIONS_TO_CACHE == 0) {
        break;
      }
    }
    voiceInstructionLoader.cacheInstructions(voiceInstructionsToCache);
  }

  void cache() {
    if (isVoiceInstructionsToCacheThresholdReached) {
      isVoiceInstructionsToCacheThresholdReached = false;
      voiceInstructionLoader.evictVoiceInstructions();
      List<String> voiceInstructionsToCache = new ArrayList<>();
      for (int i = currentVoiceInstructionsCachedIndex; i < totalVoiceInstructions; i++) {
        voiceInstructionsToCache.add(navigation.retrieveSsmlAnnouncementInstruction(i));
        currentVoiceInstructionsCachedIndex++;
        if ((currentVoiceInstructionsCachedIndex + 1) % MAX_VOICE_INSTRUCTIONS_TO_CACHE == 0) {
          break;
        }
      }
      voiceInstructionLoader.cacheInstructions(voiceInstructionsToCache);
    }
  }

  void update(int voiceInstructionsToAnnounce) {
    if (voiceInstructionsToAnnounce % VOICE_INSTRUCTIONS_TO_CACHE_THRESHOLD == 0) {
      isVoiceInstructionsToCacheThresholdReached = true;
    }
  }
}
