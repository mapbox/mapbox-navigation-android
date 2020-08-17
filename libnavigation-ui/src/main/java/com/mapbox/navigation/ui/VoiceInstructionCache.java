package com.mapbox.navigation.ui;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.ui.internal.ConnectivityStatusProvider;
import com.mapbox.navigation.ui.voice.VoiceInstructionLoader;

import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;

class VoiceInstructionCache {

  private static final int MAX_VOICE_INSTRUCTIONS_TO_CACHE = 10;
  private static final int VOICE_INSTRUCTIONS_TO_CACHE_THRESHOLD = 5;
  private final MapboxNavigation navigation;
  private final VoiceInstructionLoader voiceInstructionLoader;
  private final ConnectivityStatusProvider connectivityStatus;
  private int totalVoiceInstructions = 0;
  private int currentVoiceInstructionsCachedIndex = 0;
  private boolean isVoiceInstructionsToCacheThresholdReached = true;

  VoiceInstructionCache(MapboxNavigation navigation, VoiceInstructionLoader voiceInstructionLoader,
                        ConnectivityStatusProvider connectivityStatus) {
    this.navigation = navigation;
    this.voiceInstructionLoader = voiceInstructionLoader;
    this.connectivityStatus = connectivityStatus;
  }

  void initCache(@NonNull DirectionsRoute route) {
    totalVoiceInstructions = 0;

    List<RouteLeg> routeLegs = route.legs();
    if (routeLegs == null) {
      return;
    }

    for (int i = 0; i < routeLegs.size(); i++) {
      RouteLeg leg = routeLegs.get(i);
      if (leg == null) {
        continue;
      }

      List<LegStep> legSteps = leg.steps();
      if (legSteps == null) {
        continue;
      }

      for (int j = 0; j < legSteps.size(); j++) {
        LegStep step = legSteps.get(j);
        if (step == null) {
          continue;
        }

        List<VoiceInstructions> voiceInstructions = step.voiceInstructions();
        if (voiceInstructions != null) {
          totalVoiceInstructions += voiceInstructions.size();
        }
      }
    }
  }

  void cache() {
    if (!connectivityStatus.isConnected()) {
      return;
    }

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

  @TestOnly
  int getTotalVoiceInstructionNumber() {
    return totalVoiceInstructions;
  }
}
