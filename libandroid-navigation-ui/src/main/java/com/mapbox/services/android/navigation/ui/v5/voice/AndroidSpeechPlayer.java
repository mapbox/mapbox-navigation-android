package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Locale;

/**
 * Default player used to play voice instructions when
 * {@link com.mapbox.services.android.navigation.ui.v5.NavigationView} is launched without an AWS Cognito Pool ID.
 * <p>
 * This instruction player uses {@link TextToSpeech} to play voice instructions.
 *
 * @since 0.6.0
 */
public class AndroidSpeechPlayer implements InstructionPlayer, TextToSpeech.OnInitListener {

  private static final String DEFAULT_UTTERANCE_ID = "default_id";

  private InstructionListener instructionListener;
  private TextToSpeech textToSpeech;
  private boolean isMuted;
  private Locale locale;

  /**
   * Creates an instance of {@link AndroidSpeechPlayer}.
   *
   * @param context used to create an instance of {@link TextToSpeech}
   * @since 0.6.0
   */
  AndroidSpeechPlayer(Context context, Locale locale, InstructionListener instructionListener) {
    this.locale = locale;
    this.instructionListener = instructionListener;
    textToSpeech = new TextToSpeech(context, this);
    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
      @Override
      public void onStart(String utteranceId) {
        if (AndroidSpeechPlayer.this.instructionListener != null) {
          AndroidSpeechPlayer.this.instructionListener.onStart();
        }
      }

      @Override
      public void onDone(String utteranceId) {
        if (AndroidSpeechPlayer.this.instructionListener != null) {
          AndroidSpeechPlayer.this.instructionListener.onDone();
        }
      }

      @Override
      public void onError(String utteranceId) {
        if (AndroidSpeechPlayer.this.instructionListener != null) {
          AndroidSpeechPlayer.this.instructionListener.onError(false);
        }
      }
    });
  }

  /**
   * @param instruction voice instruction to be synthesized and played
   */
  @Override
  public void play(String instruction) {
    if (!isMuted && !TextUtils.isEmpty(instruction)) {
      HashMap<String, String> params = new HashMap<>(1);
      params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, DEFAULT_UTTERANCE_ID);
      textToSpeech.speak(instruction, TextToSpeech.QUEUE_ADD, params);
    }
  }

  /**
   * @return true if muted, false if not
   */
  @Override
  public boolean isMuted() {
    return isMuted;
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
      textToSpeech.setLanguage(locale);
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
