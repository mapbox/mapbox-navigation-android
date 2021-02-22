package com.mapbox.navigation.ui.voice;

import android.media.AudioManager;

class SpeechAudioFocusDelegate implements AudioFocusDelegate {

  private final AudioManager audioManager;
  private final int focusGain;

  SpeechAudioFocusDelegate(AudioManager audioManager, int focusGain) {
    this.audioManager = audioManager;
    this.focusGain = focusGain;
  }

  @Override
  public void requestFocus() {
    audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, focusGain);
  }

  @Override
  public void abandonFocus() {
    audioManager.abandonAudioFocus(null);
  }
}
