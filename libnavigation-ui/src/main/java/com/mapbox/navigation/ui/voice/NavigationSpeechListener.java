package com.mapbox.navigation.ui.voice;

import com.mapbox.api.directions.v5.models.VoiceInstructions;

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
    VoiceInstructions announcement = VoiceInstructions.builder()
      .announcement(speechAnnouncement.announcement())
      .ssmlAnnouncement(speechAnnouncement.ssmlAnnouncement())
      .build();
    speechPlayerProvider.retrieveAndroidSpeechPlayer().play(announcement);
  }
}
