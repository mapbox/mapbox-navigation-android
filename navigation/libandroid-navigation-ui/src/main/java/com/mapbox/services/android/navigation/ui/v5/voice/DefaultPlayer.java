package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import java.util.Locale;

/**
 * Default player used to play voice instructions when
 * {@link com.mapbox.services.android.navigation.ui.v5.NavigationView} is launched without an AWS Cognito Pool ID.
 * <p>
 * This instruction player uses {@link TextToSpeech} to play voice instructions.
 *
 * @since 0.6.0
 */
public class DefaultPlayer implements InstructionPlayer, TextToSpeech.OnInitListener {

  private TextToSpeech textToSpeech;
  private boolean isMuted;

  /**
   * Creates an instance of {@link DefaultPlayer}.
   *
   * @param context used to create an instance of {@link TextToSpeech}
   * @since 0.6.0
   */
  DefaultPlayer(Context context) {
    textToSpeech = new TextToSpeech(context, this);
  }

  /**
   * @param instruction voice instruction to be synthesized and played
   */
  @Override
  public void play(String instruction) {
    if (!isMuted && !TextUtils.isEmpty(instruction)) {
      textToSpeech.speak(instruction, TextToSpeech.QUEUE_ADD, null);
    }
  }

  /**
   * @param isMuted true if should be muted, false if should not
   */
  @Override
  public void setMuted(boolean isMuted) {
    this.isMuted = isMuted;
    if (isMuted) {
      muteTts();
    }
  }

  /**
   * @return true if muted, false if not
   */
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

  /**
   * Called when setting muted mid-instruction.
   * Mutes TTS only if currently speaking.
   */
  private void muteTts() {
    if (textToSpeech.isSpeaking()) {
      textToSpeech.stop();
    }
  }
}
