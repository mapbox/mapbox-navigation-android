package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mapbox.services.android.navigation.ui.v5.voice.polly.PollyPlayer;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;

public class NavigationInstructionPlayer implements InstructionPlayer {

  private InstructionPlayer instructionPlayer;

  public NavigationInstructionPlayer(@NonNull Context context, @Nullable String awsPoolId) {
    if (!TextUtils.isEmpty(awsPoolId)) {
      instructionPlayer = new PollyPlayer(context, awsPoolId);
    } else {
      instructionPlayer = new DefaultPlayer(context);
    }
  }

  @Override
  public void play(String instruction) {
    instructionPlayer.play(instruction);
  }

  @Override
  public void setMuted(boolean isMuted) {
    instructionPlayer.setMuted(isMuted);
  }

  @Override
  public boolean isMuted() {
    return instructionPlayer.isMuted();
  }

  @Override
  public void onOffRoute() {
    instructionPlayer.onOffRoute();
    play(NavigationConstants.NAVIGATION_VIEW_REROUTING);
  }

  @Override
  public void onDestroy() {
    instructionPlayer.onDestroy();
  }
}
