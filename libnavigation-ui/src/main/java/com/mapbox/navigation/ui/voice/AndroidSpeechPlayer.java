package com.mapbox.navigation.ui.voice;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import com.mapbox.api.directions.v5.models.VoiceInstructions;

import java.util.HashMap;
import java.util.Locale;

import timber.log.Timber;

/**
 * Default player used to play voice instructions when a connection to Polly is unable to be established.
 * <p>
 * This instruction player uses {@link TextToSpeech} to play voice instructions.
 */
class AndroidSpeechPlayer implements SpeechPlayer {

  private static final String DEFAULT_UTTERANCE_ID = "default_id";

  private TextToSpeech textToSpeech;

  private boolean isMuted;
  private boolean languageSupported = false;
  private VoiceListener voiceListener;

  /**
   * Creates an instance of {@link AndroidSpeechPlayer}.
   *
   * @param context used to create an instance of {@link TextToSpeech}
   * @param language to initialize locale to set
   */
  AndroidSpeechPlayer(Context context, final String language, final VoiceListener voiceListener) {
    this.voiceListener = voiceListener;
    textToSpeech = new TextToSpeech(context, status -> {
      boolean ableToInitialize = status == TextToSpeech.SUCCESS && language != null;
      if (!ableToInitialize) {
        return;
      }
      setVoiceListener(voiceListener);
      initializeWithLanguage(new Locale(language));
    });
  }

  /**
   * Plays the given voice instruction using TTS
   *
   * @param voiceInstructions with voice instruction to be synthesized and played
   */
  @Override
  public void play(VoiceInstructions voiceInstructions) {
    boolean isValidAnnouncement = voiceInstructions != null
        && !TextUtils.isEmpty(voiceInstructions.announcement());
    boolean canPlay = isValidAnnouncement && languageSupported && !isMuted;
    if (!canPlay) {
      if (voiceListener != null) {
        voiceListener.onDone(SpeechPlayerState.IDLE);
      }
      return;
    }

    HashMap<String, String> params = new HashMap<>(1);
    params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, DEFAULT_UTTERANCE_ID);
    textToSpeech.speak(voiceInstructions.announcement(), TextToSpeech.QUEUE_ADD, params);
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
    if (voiceListener != null) {
      voiceListener.onDone(SpeechPlayerState.OFFLINE_PLAYER_INITIALIZED);
    }
  }

  private void setVoiceListener(final VoiceListener voiceListener) {
    textToSpeech.setOnUtteranceProgressListener(new UtteranceListener(voiceListener));
  }
}
