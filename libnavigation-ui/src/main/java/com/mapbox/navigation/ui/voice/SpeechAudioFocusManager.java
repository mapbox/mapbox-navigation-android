package com.mapbox.navigation.ui.voice;

import androidx.annotation.NonNull;

class SpeechAudioFocusManager {

  private final AudioFocusDelegate audioFocusDelegate;

  SpeechAudioFocusManager(@NonNull AudioFocusDelegate audioFocusDelegate) {
    this.audioFocusDelegate = audioFocusDelegate;
  }

  void requestAudioFocus() {
    audioFocusDelegate.requestFocus();
  }

  void abandonAudioFocus() {
    audioFocusDelegate.abandonFocus();
  }
}
