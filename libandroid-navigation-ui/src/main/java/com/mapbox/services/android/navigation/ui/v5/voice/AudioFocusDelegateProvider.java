package com.mapbox.services.android.navigation.ui.v5.voice;

import android.media.AudioManager;
import android.os.Build;

class AudioFocusDelegateProvider {

  private final AudioFocusDelegate audioFocusDelegate;

  AudioFocusDelegateProvider(AudioManager audioManager) {
    audioFocusDelegate = buildAudioFocusDelegate(audioManager);
  }

  AudioFocusDelegate retrieveAudioFocusDelegate() {
    return audioFocusDelegate;
  }

  private AudioFocusDelegate buildAudioFocusDelegate(AudioManager audioManager) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      return new Api26AudioFocusDelegate(audioManager);
    }
    return new SpeechAudioFocusDelegate(audioManager);
  }
}
