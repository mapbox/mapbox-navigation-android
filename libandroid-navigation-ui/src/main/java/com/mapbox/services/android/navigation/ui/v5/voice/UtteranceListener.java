package com.mapbox.services.android.navigation.ui.v5.voice;

import android.os.Build;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
class UtteranceListener extends UtteranceProgressListener {
  private InstructionListener instructionListener;

  UtteranceListener(InstructionListener instructionListener) {
    this.instructionListener = instructionListener;
  }

  @Override
  public void onStart(String utteranceId) {
    if (instructionListener != null) {
      instructionListener.onStart();
    }
  }

  @Override
  public void onDone(String utteranceId) {
    if (instructionListener != null) {
      instructionListener.onDone();
    }
  }

  @Override
  public void onError(String utteranceId) {
    // Intentionally empty
  }
}