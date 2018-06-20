package com.mapbox.services.android.navigation.ui.v5.voice;

import android.speech.tts.TextToSpeech;

class Api14UtteranceListener implements TextToSpeech.OnUtteranceCompletedListener {
  private InstructionListener instructionListener;

  Api14UtteranceListener(InstructionListener instructionListener) {
    this.instructionListener = instructionListener;
  }

  @Override
  public void onUtteranceCompleted(String utteranceId) {
    instructionListener.onDone();
  }
}