package com.mapbox.services.android.navigation.ui.v5.voice;

public interface InstructionPlayer {

  void play(String instruction);

  void setMuted(boolean isMuted);

  boolean isMuted();

  void onDestroy();
}
