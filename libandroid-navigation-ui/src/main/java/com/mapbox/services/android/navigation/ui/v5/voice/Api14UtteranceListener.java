package com.mapbox.services.android.navigation.ui.v5.voice;

import android.speech.tts.TextToSpeech;

class Api14UtteranceListener implements TextToSpeech.OnUtteranceCompletedListener {
  private SpeechListener speechListener;

  Api14UtteranceListener(SpeechListener speechListener) {
    this.speechListener = speechListener;
  }

  @Override
  public void onUtteranceCompleted(String utteranceId) {
    speechListener.onDone();
  }
}