package com.mapbox.navigation.ui.voice;

import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.O)
class Api26AudioFocusDelegate implements AudioFocusDelegate {

  private final AudioManager audioManager;
  private final AudioFocusRequest audioFocusRequest;

  Api26AudioFocusDelegate(AudioManager audioManager, int focusGain) {
    this.audioManager = audioManager;
    audioFocusRequest = new AudioFocusRequest.Builder(focusGain).build();
  }

  @Override
  public void requestFocus() {
    audioManager.requestAudioFocus(audioFocusRequest);
  }

  @Override
  public void abandonFocus() {
    audioManager.abandonAudioFocusRequest(audioFocusRequest);
  }
}
