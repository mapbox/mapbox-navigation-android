package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mapbox.services.android.navigation.ui.v5.voice.polly.PollyPlayer;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;

public class NavigationInstructionPlayer implements InstructionPlayer, InstructionListener {

  private AudioManager instructionAudioManager;
  private InstructionPlayer instructionPlayer;
  private InstructionListener instructionListener;

  public NavigationInstructionPlayer(@NonNull Context context, @Nullable String awsPoolId) {
    initAudioManager(context);
    if (!TextUtils.isEmpty(awsPoolId)) {
      instructionPlayer = new PollyPlayer(context, awsPoolId);
    } else {
      instructionPlayer = new DefaultPlayer(context);
    }
    instructionPlayer.addInstructionListener(this);
  }

  @Override
  public void play(String instruction) {
    instructionPlayer.play(instruction);
  }

  @Override
  public boolean isMuted() {
    return instructionPlayer.isMuted();
  }

  @Override
  public void setMuted(boolean isMuted) {
    instructionPlayer.setMuted(isMuted);
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

  @Override
  public void addInstructionListener(InstructionListener instructionListener) {
    this.instructionListener = instructionListener;
  }

  @Override
  public void onStart() {
    if (instructionListener != null) {
      instructionListener.onStart();
    }

    // Request audio focus
    requestAudioFocus();
  }

  @Override
  public void onDone() {
    if (instructionListener != null) {
      instructionListener.onDone();
    }

    // Abandon audio focus
    abandonAudioFocus();
  }

  @Override
  public void onError() {
    if (instructionListener != null) {
      instructionListener.onError();
    }
  }

  private void initAudioManager(Context context) {
    instructionAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
  }

  private void requestAudioFocus() {
    instructionAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
      AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
  }

  private void abandonAudioFocus() {
    instructionAudioManager.abandonAudioFocus(null);
  }
}
