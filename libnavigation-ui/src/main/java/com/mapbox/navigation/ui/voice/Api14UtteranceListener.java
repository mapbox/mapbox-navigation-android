package com.mapbox.navigation.ui.voice;

import android.speech.tts.TextToSpeech;

class Api14UtteranceListener implements TextToSpeech.OnUtteranceCompletedListener {
  private VoiceListener voiceListener;

  Api14UtteranceListener(VoiceListener voiceListener) {
    this.voiceListener = voiceListener;
  }

  @Override
  public void onUtteranceCompleted(String utteranceId) {
    voiceListener.onDone();
  }
}