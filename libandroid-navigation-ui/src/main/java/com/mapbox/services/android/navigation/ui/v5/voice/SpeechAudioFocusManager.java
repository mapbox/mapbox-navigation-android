package com.mapbox.services.android.navigation.ui.v5.voice;

class SpeechAudioFocusManager {

  private final AudioFocusDelegate audioFocusDelegate;

  SpeechAudioFocusManager(AudioFocusDelegateProvider provider) {
    audioFocusDelegate = provider.retrieveAudioFocusDelegate();
  }

  void requestAudioFocus() {
    audioFocusDelegate.requestFocus();
  }

  void abandonAudioFocus() {
    audioFocusDelegate.abandonFocus();
  }
}
