package com.mapbox.services.android.navigation.ui.v5.voice;

import com.mapbox.navigation.base.logger.model.Message;
import com.mapbox.navigation.logger.MapboxLogger;

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
    MapboxLogger.INSTANCE.e(new Message(errorText));
    speechPlayerProvider.retrieveAndroidSpeechPlayer().play(speechAnnouncement);
  }
}
