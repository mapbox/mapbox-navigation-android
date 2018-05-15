package com.mapbox.services.android.navigation.ui.v5.voice;

interface InstructionListener {

  void onStart();

  void onDone();

  void onError(boolean isMapboxPlayer);
}
