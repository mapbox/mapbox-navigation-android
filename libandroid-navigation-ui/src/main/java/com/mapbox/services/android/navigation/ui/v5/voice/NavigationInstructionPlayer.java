package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mapbox.services.android.navigation.ui.v5.voice.polly.PollyPlayer;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;

import java.util.Locale;

public class NavigationInstructionPlayer implements InstructionPlayer, InstructionListener {

  private AudioManager instructionAudioManager;
  private AudioFocusRequest instructionFocusRequest;
  private InstructionPlayer instructionPlayer;
  private InstructionListener instructionListener;
  private boolean isPollyPlayer;

  public NavigationInstructionPlayer(@NonNull Context context, @Nullable String awsPoolId,
                                     Locale locale) {
    initAudioManager(context);
    initAudioFocusRequest();
    initInstructionPlayer(context, awsPoolId, locale);
  }

  private void initAudioFocusRequest() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      instructionFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .build();
    }
  }

  private void initInstructionPlayer(Context context, @Nullable String awsPoolId,
                                     Locale locale) {
    if (!TextUtils.isEmpty(awsPoolId)) {
      instructionPlayer = new PollyPlayer(context, awsPoolId, locale);
      isPollyPlayer = true;
    } else {
      instructionPlayer = new DefaultPlayer(context, locale);
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

  public boolean isPollyPlayer() {
    return isPollyPlayer;
  }

  private void initAudioManager(Context context) {
    instructionAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
  }

  private void requestAudioFocus() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      instructionAudioManager.requestAudioFocus(instructionFocusRequest);
    } else {
      instructionAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
    }
  }

  private void abandonAudioFocus() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      instructionAudioManager.abandonAudioFocusRequest(instructionFocusRequest);
    } else {
      instructionAudioManager.abandonAudioFocus(null);
    }
  }
}
