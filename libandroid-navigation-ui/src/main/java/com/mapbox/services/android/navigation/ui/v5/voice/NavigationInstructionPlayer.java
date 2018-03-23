package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.mapbox.services.android.navigation.ui.v5.voice.speech.MapboxSpeechPlayer;
import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;

import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NavigationInstructionPlayer implements InstructionListener {

  private AudioManager instructionAudioManager;
  private AudioFocusRequest instructionFocusRequest;
  private MapboxSpeechPlayer mapboxSpeechPlayer;
  private AndroidSpeechPlayer androidSpeechPlayer;
  private InstructionListener instructionListener;
  private Queue<Pair<String, VoiceInstructionMilestone>> instructionQueue;
  private boolean isMuted;

  public NavigationInstructionPlayer(@NonNull Context context, Locale locale) {
    initAudioManager(context);
    initAudioFocusRequest();
    initInstructionPlayers(context, locale);
    instructionQueue = new ConcurrentLinkedQueue<>();
  }

  private void initAudioFocusRequest() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      instructionFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .build();
    }
  }

  private void initInstructionPlayers(Context context, Locale locale) {
    mapboxSpeechPlayer = new MapboxSpeechPlayer(context, locale, this);
    androidSpeechPlayer = new AndroidSpeechPlayer(context, locale, this);
  }

  public void play(@NonNull String instruction, @Nullable VoiceInstructionMilestone voiceInstructionMilestone) {
    instructionQueue.add(Pair.create(instruction, voiceInstructionMilestone));
    if (voiceInstructionMilestone != null) {
      mapboxSpeechPlayer.play(voiceInstructionMilestone.getSsmlAnnouncement());
    } else {
      mapboxSpeechPlayer.play(instruction, "text"); // For cases like offroute
    }
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
    play(NavigationConstants.NAVIGATION_VIEW_REROUTING, null);
  }

  public void onDestroy() {
    mapboxSpeechPlayer.onDestroy();
    androidSpeechPlayer.onDestroy();
  }

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
    instructionQueue.remove();
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
  public void onError(boolean isMapboxPlayer) {
    if (instructionListener != null) {
      instructionListener.onError(isMapboxPlayer);
    }

    if (isMapboxPlayer) { // If mapbox player failed, try android speech player
      androidSpeechPlayer.play(instructionQueue.peek().first);
    }

    // If android speech player fails, just drop the instruction
    instructionQueue.remove();
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
