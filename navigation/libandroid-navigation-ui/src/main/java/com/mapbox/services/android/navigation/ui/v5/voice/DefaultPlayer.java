package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import java.util.Locale;

public class DefaultPlayer implements InstructionPlayer, TextToSpeech.OnInitListener {

  private TextToSpeech textToSpeech;
  private boolean isMuted;

  DefaultPlayer(Context context) {
    textToSpeech = new TextToSpeech(context, this);
  }

  @Override
  public void play(String instruction) {
    if (!isMuted && !TextUtils.isEmpty(instruction)) {
      textToSpeech.speak(instruction, TextToSpeech.QUEUE_ADD, null);
    }
  }

  @Override
  public void setMuted(boolean isMuted) {
    this.isMuted = isMuted;
    if (isMuted) {
      muteTts();
    }
  }

  @Override
  public boolean isMuted() {
    return isMuted;
  }

  @Override
  public void onOffRoute() {
    muteTts();
  }

  @Override
  public void onDestroy() {
    if (textToSpeech != null) {
      textToSpeech.stop();
      textToSpeech.shutdown();
    }
  }

  @Override
  public void onInit(int status) {
    if (status != TextToSpeech.ERROR) {
      textToSpeech.setLanguage(Locale.getDefault());
    }
  }

  private void muteTts() {
    if (textToSpeech.isSpeaking()) {
      textToSpeech.stop();
    }
  }
}
