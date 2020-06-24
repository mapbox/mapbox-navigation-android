package com.mapbox.navigation.ui.voice;

import android.speech.tts.UtteranceProgressListener;

class UtteranceListener extends UtteranceProgressListener {
  private VoiceListener voiceListener;

  UtteranceListener(VoiceListener voiceListener) {
    this.voiceListener = voiceListener;
  }

  @Override
  public void onStart(String utteranceId) {
    voiceListener.onStart(SpeechPlayerState.OFFLINE_PLAYING);
  }

  @Override
  public void onDone(String utteranceId) {
    voiceListener.onDone();
  }

  @Override
  public void onError(String utteranceId) {
    // Intentionally empty
    voiceListener.onDone();
  }
}