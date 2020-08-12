package com.mapbox.navigation.ui.voice;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.VoiceInstructions;

interface VoiceListener {

  void onStart(@NonNull SpeechPlayerState state);

  void onDone(@NonNull SpeechPlayerState state);

  void onError(String errorText, VoiceInstructions voiceInstructions);
}
