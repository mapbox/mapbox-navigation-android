package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone;

import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NavigationInstructionPlayer implements InstructionListener {

  private AudioManager instructionAudioManager;
  private AudioFocusRequest instructionFocusRequest;
  private MapboxSpeechPlayer mapboxSpeechPlayer;
  private AndroidSpeechPlayer androidSpeechPlayer;
  private InstructionListener instructionListener;
  private Queue<VoiceInstructionMilestone> instructionQueue;
  private boolean isMuted;

  public NavigationInstructionPlayer(@NonNull Context context, Locale locale) {
    initAudioManager(context);
    initAudioFocusRequest();
    initInstructionPlayers(context, locale);
    instructionQueue = new ConcurrentLinkedQueue<>();
  }

  public void play(VoiceInstructionMilestone voiceInstructionMilestone) {
    instructionQueue.add(voiceInstructionMilestone);
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
    instructionQueue.clear();
    mapboxSpeechPlayer.onOffRoute();
    androidSpeechPlayer.onOffRoute();
  }

  public void onDestroy() {
    mapboxSpeechPlayer.onDestroy();
    androidSpeechPlayer.onDestroy();
  }

  public void setInstructionListener(InstructionListener instructionListener) {
    this.instructionListener = instructionListener;
  }

  @Override
  public void onStart() {
    if (instructionListener != null) {
      instructionListener.onStart();
    }

    requestAudioFocus();
    instructionQueue.remove();
  }

  @Override
  public void onDone() {
    if (instructionListener != null) {
      instructionListener.onDone();
    }

    abandonAudioFocus();
  }

  @Override
  public void onError(boolean isMapboxPlayer) {
    if (instructionListener != null) {
      instructionListener.onError(isMapboxPlayer);
    }

    if (isMapboxPlayer) { // If mapbox player failed, try android speech player
      androidSpeechPlayer.play(instructionQueue.peek().getAnnouncement());
    } else { // If android speech player fails, just drop the instruction
      instructionQueue.remove();
    }
  }

  private void initAudioFocusRequest() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      instructionFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).build();
    }
  }

  private void initInstructionPlayers(Context context, Locale locale) {
    mapboxSpeechPlayer = new MapboxSpeechPlayer(context, locale);
    mapboxSpeechPlayer.setInstructionListener(this);
    androidSpeechPlayer = new AndroidSpeechPlayer(context, locale);
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
