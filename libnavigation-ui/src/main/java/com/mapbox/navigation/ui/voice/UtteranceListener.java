package com.mapbox.navigation.ui.voice;

import android.os.Build;
import android.speech.tts.UtteranceProgressListener;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
class UtteranceListener extends UtteranceProgressListener {
  private VoiceListener voiceListener;

  UtteranceListener(VoiceListener voiceListener) {
    this.voiceListener = voiceListener;
  }

  @Override
  public void onStart(String utteranceId) {
    voiceListener.onStart();
  }

  @Override
  public void onDone(String utteranceId) {
    voiceListener.onDone();
  }

  @Override
  public void onError(String utteranceId) {
    // Intentionally empty
  }
}