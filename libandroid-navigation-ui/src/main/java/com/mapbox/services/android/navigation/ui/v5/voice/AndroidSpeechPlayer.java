package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Locale;

import timber.log.Timber;

/**
 * Default player used to play voice instructions when a connection to Polly is unable to be established.
 * <p>
 * This instruction player uses {@link TextToSpeech} to play voice instructions.
 *
 * @since 0.6.0
 */
class AndroidSpeechPlayer implements InstructionPlayer {

  private static final String DEFAULT_UTTERANCE_ID = "default_id";

  private TextToSpeech textToSpeech;
  private boolean isMuted;
  private boolean languageSupported = false;

  /**
   * Creates an instance of {@link AndroidSpeechPlayer}.
   *
   * @param context used to create an instance of {@link TextToSpeech}
   * @param language to initialize locale to set
   * @since 0.6.0
   */
  AndroidSpeechPlayer(Context context, final String language) {
    textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int status) {
        boolean ableToInitialize = status != TextToSpeech.ERROR && language != null;
        if (!ableToInitialize) {
          Timber.e("There was an error initializing native TTS");
        }
        initializeWithLanguage(new Locale(language));
      }
    });
  }

  /**
   * Plays the given voice instruction using TTS
   *
   * @param instruction voice instruction to be synthesized and played
   */
  @Override
  public void play(String instruction) {
    boolean canPlay = languageSupported && !isMuted && !TextUtils.isEmpty(instruction);
    if (!canPlay) {
      return;
    }
    HashMap<String, String> params = new HashMap<>(1);
    params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, DEFAULT_UTTERANCE_ID);
    textToSpeech.speak(instruction, TextToSpeech.QUEUE_ADD, params);
  }

  /**
   * Returns whether or not the AndroidSpeechPlayer is currently muted
   *
   * @return true if muted, false if not
   */
  @Override
  public boolean isMuted() {
    return isMuted;
  }

  /**
   * Mutes or un-mutes the AndroidSpeechPlayer, canceling any instruction currently being voiced,
   * and preventing subsequent instructions from being voiced
   *
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
   * To be called during an off-route event, mutes TTS
   */
  @Override
  public void onOffRoute() {
    muteTts();
  }

  /**
   * Stops and shuts down TTS
   */
  @Override
  public void onDestroy() {
    if (textToSpeech != null) {
      textToSpeech.stop();
      textToSpeech.shutdown();
    }
  }

  private void muteTts() {
    if (textToSpeech.isSpeaking()) {
      textToSpeech.stop();
    }
  }

  private void initializeWithLanguage(Locale language) {
    boolean isLanguageAvailable = textToSpeech.isLanguageAvailable(language) == TextToSpeech.LANG_AVAILABLE;
    if (!isLanguageAvailable) {
      Timber.w("The specified language is not supported by TTS");
      return;
    }
    languageSupported = true;
    textToSpeech.setLanguage(language);
  }

  void setInstructionListener(final InstructionListener instructionListener) {
    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
      @Override
      public void onStart(String utteranceId) {
        if (instructionListener != null) {
          instructionListener.onStart();
        }
      }

      @Override
      public void onDone(String utteranceId) {
        if (instructionListener != null) {
          instructionListener.onDone();
        }
      }

      @Override
      public void onError(String utteranceId) {
        if (instructionListener != null) {
          instructionListener.onError(false);
        }
      }
    });
  }
}
