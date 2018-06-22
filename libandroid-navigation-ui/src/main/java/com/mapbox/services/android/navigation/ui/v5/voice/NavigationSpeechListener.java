package com.mapbox.services.android.navigation.ui.v5.voice;

import timber.log.Timber;

class NavigationSpeechListener implements SpeechListener {

  private SpeechPlayerProvider speechPlayerProvider;
  private SpeechAudioFocusManager audioFocusManager;

  NavigationSpeechListener(SpeechPlayerProvider speechPlayerProvider,
                           SpeechAudioFocusManager audioFocusManager) {
    this.speechPlayerProvider = speechPlayerProvider;
    this.audioFocusManager = audioFocusManager;
  }

  @Override
  public void onStart() {
    audioFocusManager.requestAudioFocus();
  }

  @Override
  public void onDone() {
    audioFocusManager.abandonAudioFocus();
  }

  @Override
  public void onError(String errorText, SpeechAnnouncement speechAnnouncement) {
    Timber.e(errorText);
    speechPlayerProvider.retrieveAndroidSpeechPlayer().play(speechAnnouncement);
  }
}
