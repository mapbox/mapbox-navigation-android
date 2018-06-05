package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone;

public class NavigationInstructionPlayer implements InstructionListener {

  private AudioManager instructionAudioManager;
  private AudioFocusRequest instructionFocusRequest;
  private MapboxSpeechPlayer mapboxSpeechPlayer;
  private AndroidSpeechPlayer androidSpeechPlayer;
  private VoiceInstructionMilestone voiceInstructionMilestone;
  private boolean isMuted;

  public NavigationInstructionPlayer(@NonNull Context context, String language, String accessToken) {
    initAudioManager(context);
    initAudioFocusRequest();
    initInstructionPlayers(context, language, accessToken);
  }

  public void play(VoiceInstructionMilestone voiceInstructionMilestone) {
    this.voiceInstructionMilestone = voiceInstructionMilestone;
    mapboxSpeechPlayer.play(voiceInstructionMilestone.getSsmlAnnouncement());
  }

  public boolean isMuted() {
    return isMuted;
  }

  public void setMuted(boolean isMuted) {
    this.isMuted = isMuted;
    mapboxSpeechPlayer.setMuted(isMuted);
    androidSpeechPlayer.setMuted(isMuted);
  }

  public void onOffRoute() {
    mapboxSpeechPlayer.onOffRoute();
    androidSpeechPlayer.onOffRoute();
  }

  public void onDestroy() {
    mapboxSpeechPlayer.onDestroy();
    androidSpeechPlayer.onDestroy();
  }

  @Override
  public void onStart() {
    requestAudioFocus();
  }

  @Override
  public void onDone() {
    abandonAudioFocus();
  }

  @Override
  public void onError(boolean isMapboxPlayer) {
    if (isMapboxPlayer) {
      androidSpeechPlayer.play(voiceInstructionMilestone.getAnnouncement());
    }
  }

  private void initAudioFocusRequest() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      instructionFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).build();
    }
  }

  private void initInstructionPlayers(Context context, String language, String accessToken) {
    mapboxSpeechPlayer = new MapboxSpeechPlayer(context, language, accessToken);
    mapboxSpeechPlayer.setInstructionListener(this);
    androidSpeechPlayer = new AndroidSpeechPlayer(context, language);
    androidSpeechPlayer.setInstructionListener(this);
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
