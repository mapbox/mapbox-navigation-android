package com.mapbox.navigation.ui.voice;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.VoiceInstructions;

import timber.log.Timber;

class NavigationVoiceListener implements VoiceListener {

  private SpeechPlayerProvider speechPlayerProvider;
  private SpeechAudioFocusManager audioFocusManager;

  NavigationVoiceListener(SpeechPlayerProvider speechPlayerProvider,
      SpeechAudioFocusManager audioFocusManager) {
    this.speechPlayerProvider = speechPlayerProvider;
    this.audioFocusManager = audioFocusManager;
  }

  @Override
  public void onStart(@NonNull SpeechPlayerState state) {
    speechPlayerProvider.onSpeechPlayerStateChanged(state);
    audioFocusManager.requestAudioFocus();
  }

  @Override
  public void onDone(@NonNull SpeechPlayerState state) {
    speechPlayerProvider.onSpeechPlayerStateChanged(state);
    if (state == SpeechPlayerState.IDLE) {
      audioFocusManager.abandonAudioFocus();
    }
  }

  @Override
  public void onError(String errorText, VoiceInstructions voiceInstructions) {
    Timber.e(errorText);
    speechPlayerProvider.onSpeechPlayerStateChanged(SpeechPlayerState.IDLE);
    speechPlayerProvider.retrieveAndroidSpeechPlayer().play(voiceInstructions);
  }
}
