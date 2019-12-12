package com.mapbox.navigation.ui.voice;

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
