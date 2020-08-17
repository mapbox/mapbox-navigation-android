package com.mapbox.navigation.ui.voice;

import android.media.AudioManager;
import android.os.Build;

import androidx.annotation.NonNull;

class AudioFocusDelegateProvider {

  @NonNull
  private final AudioFocusDelegate audioFocusDelegate;

  AudioFocusDelegateProvider(AudioManager audioManager) {
    audioFocusDelegate = buildAudioFocusDelegate(audioManager);
  }

  @NonNull
  AudioFocusDelegate retrieveAudioFocusDelegate() {
    return audioFocusDelegate;
  }

  @NonNull
  private AudioFocusDelegate buildAudioFocusDelegate(AudioManager audioManager) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      return new Api26AudioFocusDelegate(audioManager);
    }
    return new SpeechAudioFocusDelegate(audioManager);
  }
}
