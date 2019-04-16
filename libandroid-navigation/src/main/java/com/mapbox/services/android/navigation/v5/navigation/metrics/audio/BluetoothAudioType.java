package com.mapbox.services.android.navigation.v5.navigation.metrics.audio;

import android.content.Context;
import android.media.AudioManager;

public class BluetoothAudioType implements AudioTypeResolver {
  private static final String BLUETOOTH = "bluetooth";
  private AudioTypeResolver chain;

  @Override
  public void nextChain(AudioTypeResolver chain) {
    this.chain = chain;
  }

  @Override
  public String obtainAudioType(Context context) {
    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    if (audioManager == null) {
      return "unknown";
    }
    return audioManager.isBluetoothScoOn() ? BLUETOOTH : chain.obtainAudioType(context);
  }
}
