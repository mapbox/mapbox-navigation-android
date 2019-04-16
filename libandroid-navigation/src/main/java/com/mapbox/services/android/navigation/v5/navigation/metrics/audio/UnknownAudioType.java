package com.mapbox.services.android.navigation.v5.navigation.metrics.audio;

import android.content.Context;

public class UnknownAudioType implements AudioTypeResolver {
  private static final String UNKNOWN = "unknown";

  @Override
  public void nextChain(AudioTypeResolver chain) {
  }

  @Override
  public String obtainAudioType(Context context) {
    return UNKNOWN;
  }
}
