package com.mapbox.services.android.navigation.ui.v5.voice;

import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.O)
class Api26AudioFocusDelegate implements AudioFocusDelegate {

  private static final int FOCUS_GAIN = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;

  private final AudioManager audioManager;
  private final AudioFocusRequest audioFocusRequest;

  Api26AudioFocusDelegate(AudioManager audioManager) {
    this.audioManager = audioManager;
    audioFocusRequest = new AudioFocusRequest.Builder(FOCUS_GAIN).build();
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
