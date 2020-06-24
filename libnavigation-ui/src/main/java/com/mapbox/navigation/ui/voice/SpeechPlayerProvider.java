package com.mapbox.navigation.ui.voice;

import android.content.Context;
import android.media.AudioManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigation.ui.internal.ConnectivityStatusProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Given to the constructor of {@link NavigationSpeechPlayer}, this class decides which
 * {@link SpeechPlayer} should be used based on voice language compatibility.
 * <p>
 * If the given {@link DirectionsRoute#voiceLanguage()} is not <tt>null</tt>, this means the language is
 * supported by the Mapbox Voice API, which can parse SSML.  The boolean <tt>voiceLanguageSupported</tt> should
 * be try in this case.
 * <p>
 * If false, an instance of {@link MapboxSpeechPlayer} will never be provided to the {@link NavigationSpeechPlayer}.
 * The SDK will default to the {@link AndroidSpeechPlayer} powered by {@link android.speech.tts.TextToSpeech}.
 *
 */
public class SpeechPlayerProvider {

  private static final int FIRST_PLAYER = 0;

  private AndroidSpeechPlayer androidSpeechPlayer;
  private List<SpeechPlayer> speechPlayers = new ArrayList<>(2);
  private VoiceInstructionLoader voiceInstructionLoader;
  private ConnectivityStatusProvider connectivityStatus;
  @NonNull
  private SpeechPlayerState speechPlayerState = SpeechPlayerState.IDLE;
  @Nullable
  private SpeechPlayerStateChangeObserver observer = null;

  /**
   * Constructed when creating an instance of {@link NavigationSpeechPlayer}.
   *
   * @param context                for the initialization of the speech players
   * @param language               to be used
   * @param voiceLanguageSupported true if <tt>voiceLanguage</tt> is not null, false otherwise
   * @param voiceInstructionLoader voice instruction loader
   */
  public SpeechPlayerProvider(@NonNull Context context, String language,
                              boolean voiceLanguageSupported, VoiceInstructionLoader voiceInstructionLoader) {
    initialize(context, language, voiceLanguageSupported, voiceInstructionLoader);
  }

  // Package private (no modifier) for testing purposes
  SpeechPlayerProvider(@NonNull Context context, String language,
                       boolean voiceLanguageSupported, VoiceInstructionLoader voiceInstructionLoader,
                       ConnectivityStatusProvider connectivityStatus) {
    this(context, language, voiceLanguageSupported, voiceInstructionLoader);
    this.connectivityStatus = connectivityStatus;
  }

  SpeechPlayer retrieveSpeechPlayer() {
    if (speechPlayerState == SpeechPlayerState.OFFLINE_PLAYING) {
      return null;
    }

    if (voiceInstructionLoader.hasCache() || connectivityStatus.isConnectedFast()) {
      speechPlayerState = SpeechPlayerState.ONLINE_PLAYING;
      return speechPlayers.get(FIRST_PLAYER);
    } else if (speechPlayerState == SpeechPlayerState.IDLE) {
      speechPlayerState = SpeechPlayerState.OFFLINE_PLAYING;
      return androidSpeechPlayer;
    } else {
      return null;
    }
  }

  AndroidSpeechPlayer retrieveAndroidSpeechPlayer() {
    return androidSpeechPlayer;
  }

  void setMuted(boolean isMuted) {
    for (SpeechPlayer player : speechPlayers) {
      player.setMuted(isMuted);
    }
  }

  void onOffRoute() {
    for (SpeechPlayer player : speechPlayers) {
      player.onOffRoute();
    }
  }

  void onDestroy() {
    for (SpeechPlayer player : speechPlayers) {
      player.onDestroy();
    }
  }

  void onSpeechPlayerStateChanged(@NonNull SpeechPlayerState speechPlayerState) {
    this.speechPlayerState = speechPlayerState;
    if (observer != null) {
      observer.onStateChange(speechPlayerState);
    }
  }

  void setSpeechPlayerStateChangeObserver(SpeechPlayerStateChangeObserver observer) {
    this.observer = observer;
  }

  private void initialize(@NonNull Context context, String language,
                          boolean voiceLanguageSupported, VoiceInstructionLoader voiceInstructionLoader) {
    AudioFocusDelegateProvider provider = buildAudioFocusDelegateProvider(context);
    SpeechAudioFocusManager audioFocusManager = new SpeechAudioFocusManager(provider);
    VoiceListener voiceListener = new NavigationVoiceListener(this, audioFocusManager);
    initializeMapboxSpeechPlayer(context, language, voiceLanguageSupported, voiceListener, voiceInstructionLoader);
    initializeAndroidSpeechPlayer(context, language, voiceListener);
    this.voiceInstructionLoader = voiceInstructionLoader;
    connectivityStatus = new ConnectivityStatusProvider(context);
  }

  private AudioFocusDelegateProvider buildAudioFocusDelegateProvider(Context context) {
    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    return new AudioFocusDelegateProvider(audioManager);
  }

  private void initializeMapboxSpeechPlayer(Context context, String language, boolean voiceLanguageSupported,
                                            VoiceListener listener, VoiceInstructionLoader voiceInstructionLoader) {
    if (!voiceLanguageSupported) {
      return;
    }
    voiceInstructionLoader.setupMapboxSpeechBuilder(language);
    MapboxSpeechPlayer mapboxSpeechPlayer = new MapboxSpeechPlayer(context, listener, voiceInstructionLoader);
    speechPlayers.add(mapboxSpeechPlayer);
  }

  private void initializeAndroidSpeechPlayer(Context context, String language,
                                             VoiceListener listener) {
    androidSpeechPlayer = new AndroidSpeechPlayer(context, language, listener);
    speechPlayers.add(androidSpeechPlayer);
  }
}
