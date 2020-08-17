package com.mapbox.navigation.ui.voice;

import androidx.annotation.NonNull;

class SpeechAudioFocusManager {

  private final AudioFocusDelegate audioFocusDelegate;

  SpeechAudioFocusManager(@NonNull AudioFocusDelegateProvider provider) {
    audioFocusDelegate = provider.retrieveAudioFocusDelegate();
  }

  void requestAudioFocus() {
    audioFocusDelegate.requestFocus();
  }

  void abandonAudioFocus() {
    audioFocusDelegate.abandonFocus();
  }
}
