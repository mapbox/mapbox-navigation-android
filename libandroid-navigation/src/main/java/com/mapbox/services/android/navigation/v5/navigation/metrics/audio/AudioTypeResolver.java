package com.mapbox.services.android.navigation.v5.navigation.metrics.audio;

import android.content.Context;

public interface AudioTypeResolver {
  void nextChain(AudioTypeResolver chain);

  String obtainAudioType(Context context);
}