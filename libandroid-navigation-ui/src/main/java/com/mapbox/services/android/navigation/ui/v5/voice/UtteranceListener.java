package com.mapbox.services.android.navigation.ui.v5.voice;

import android.os.Build;
import android.speech.tts.UtteranceProgressListener;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
class UtteranceListener extends UtteranceProgressListener {
  private SpeechListener speechListener;

  UtteranceListener(SpeechListener speechListener) {
    this.speechListener = speechListener;
  }

  @Override
  public void onStart(String utteranceId) {
    speechListener.onStart();
  }

  @Override
  public void onDone(String utteranceId) {
    speechListener.onDone();
  }

  @Override
  public void onError(String utteranceId) {
    // Intentionally empty
  }
}