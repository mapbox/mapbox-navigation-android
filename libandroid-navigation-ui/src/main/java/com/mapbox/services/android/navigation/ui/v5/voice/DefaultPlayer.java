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
  private AudioManager audioManager;

  /**
   * Creates an instance of {@link DefaultPlayer}.
   *
   * @param context used to create an instance of {@link TextToSpeech}
   * @since 0.6.0
   */
  DefaultPlayer(Context context) {
    textToSpeech = new TextToSpeech(context, this);
    textToSpeech.setOnUtteranceProgressListener(this);
    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
  }

  /**
   * @param instruction voice instruction to be synthesized and played
   */
  @Override
  public void play(String instruction) {
    if (!isMuted && !TextUtils.isEmpty(instruction)) {
      HashMap<String, String> params = new HashMap<>();
      params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "default");
      textToSpeech.speak(instruction, TextToSpeech.QUEUE_ADD, params);
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


  private final AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener(){
    @Override
    public void onAudioFocusChange(int focusChange) {}
  };

  @Override
  public void onStart(String utteranceId) {
    audioManager.requestAudioFocus(onAudioFocusChangeListener,
            // Use the notification stream.
            AudioManager.STREAM_NOTIFICATION,
            // Request a temporary focus.
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
  }

  @Override
  public void onDone(String utteranceId) {
    audioManager.abandonAudioFocus(onAudioFocusChangeListener);
  }

  @Override
  public void onError(String utteranceId) {}
}
