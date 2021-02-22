package com.mapbox.navigation.ui.voice;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigation.ui.internal.ConnectivityStatusProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.TestOnly;

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
 */
public class SpeechPlayerProvider {

  private static final int FIRST_PLAYER = 0;

  private AndroidSpeechPlayer androidSpeechPlayer;
  @NonNull
  private List<SpeechPlayer> speechPlayers = new ArrayList<>(2);
  private VoiceInstructionLoader voiceInstructionLoader;
  private ConnectivityStatusProvider connectivityStatus;
  @NonNull
  private SpeechPlayerState speechPlayerState = SpeechPlayerState.OFFLINE_PLAYER_INITIALIZING;
  @Nullable
  private SpeechPlayerStateChangeObserver observer = null;
  private boolean isFallbackAlwaysEnabled = true;

  /**
   * Constructed when creating an instance of {@link NavigationSpeechPlayer}.
   *
   * @param context                for the initialization of the speech players
   * @param language               to be used
   * @param voiceLanguageSupported true if <tt>voiceLanguage</tt> is not null, false otherwise
   * @param voiceInstructionLoader voice instruction loader
   * @deprecated use {@link Builder} instead
   */
  @Deprecated
  public SpeechPlayerProvider(
    @NonNull Context context,
    String language,
    boolean voiceLanguageSupported,
    @NonNull VoiceInstructionLoader voiceInstructionLoader
  ) {
    this(
      context,
      language,
      voiceLanguageSupported,
      voiceInstructionLoader,
      Builder.DEFAULT_FOCUS_GAIN
    );
  }

  private SpeechPlayerProvider(
    @NonNull Context context,
    String language,
    boolean voiceLanguageSupported,
    @NonNull VoiceInstructionLoader voiceInstructionLoader,
    int focusGain
  ) {
    initialize(
      context,
      language,
      voiceLanguageSupported,
      voiceInstructionLoader,
      focusGain
    );
  }

  @TestOnly
  SpeechPlayerProvider(
    @NonNull Context context,
    String language,
    boolean voiceLanguageSupported,
    @NonNull VoiceInstructionLoader voiceInstructionLoader,
    ConnectivityStatusProvider connectivityStatus
  ) {
    this(
      context,
      language,
      voiceLanguageSupported,
      voiceInstructionLoader,
      Builder.DEFAULT_FOCUS_GAIN
    );
    this.connectivityStatus = connectivityStatus;
  }

  private void initialize(
    @NonNull Context context,
    String language,
    boolean voiceLanguageSupported,
    @NonNull VoiceInstructionLoader voiceInstructionLoader,
    int focusGain
  ) {
    AudioFocusDelegateProvider provider = buildAudioFocusDelegateProvider(context, focusGain);
    SpeechAudioFocusManager audioFocusManager = new SpeechAudioFocusManager(provider);
    VoiceListener voiceListener = new NavigationVoiceListener(this, audioFocusManager);
    initializeMapboxSpeechPlayer(context, language, voiceLanguageSupported, voiceListener, voiceInstructionLoader);
    initializeAndroidSpeechPlayer(context, language, voiceListener);
    this.voiceInstructionLoader = voiceInstructionLoader;
    connectivityStatus = new ConnectivityStatusProvider(context);
  }

  /**
   * Set false to not fallback to TTS for voice guidance when the connection is slow.
   * The default setting is enabled. The TTS is used in two cases:
   * 1. when request polly voice fails
   * 2. when connection is slow
   *
   * This setting only impact the 2nd case.
   *
   * @param isFallbackAlwaysEnabled true to use TTS when connection is slow, false otherwise
   */
  public void setIsFallbackAlwaysEnabled(boolean isFallbackAlwaysEnabled) {
    this.isFallbackAlwaysEnabled = isFallbackAlwaysEnabled;
  }

  @Nullable
  SpeechPlayer retrieveSpeechPlayer() {
    if (speechPlayerState == SpeechPlayerState.OFFLINE_PLAYING) {
      return null;
    }

    if (voiceInstructionLoader.hasCache()
        || connectivityStatus.isConnectedFast()
        || (connectivityStatus.isConnected() && !isFallbackAlwaysEnabled)) {
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

  @NonNull
  private AudioFocusDelegateProvider buildAudioFocusDelegateProvider(
    @NonNull Context context,
    int focusGain
  ) {
    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    return new AudioFocusDelegateProvider(audioManager, focusGain);
  }

  void onSpeechPlayerStateChanged(@NonNull SpeechPlayerState speechPlayerState) {
    if (speechPlayerState == SpeechPlayerState.OFFLINE_PLAYER_INITIALIZED) {
      if (this.speechPlayerState == SpeechPlayerState.OFFLINE_PLAYER_INITIALIZING) {
        this.speechPlayerState = SpeechPlayerState.IDLE;
      }
    } else {
      this.speechPlayerState = speechPlayerState;
    }

    if (observer != null) {
      observer.onStateChange(this.speechPlayerState);
    }
  }

  void setSpeechPlayerStateChangeObserver(SpeechPlayerStateChangeObserver observer) {
    this.observer = observer;
  }

  private void initializeMapboxSpeechPlayer(
          @NonNull Context context,
          String language,
          boolean voiceLanguageSupported,
          @NonNull VoiceListener listener,
          @NonNull VoiceInstructionLoader voiceInstructionLoader) {
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

  /**
   * The Builder of {@link SpeechPlayerProvider}.
   */
  public static class Builder {
    // static field to support SpeechPlayerProvider deprecated constructor
    private static final int DEFAULT_FOCUS_GAIN = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;
    private static final List<Integer> validFocusGains = Arrays.asList(
      AudioManager.AUDIOFOCUS_GAIN,
      AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
      AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
      AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
    );

    @NonNull
    private final  Context context;
    private final  String language;
    private final boolean voiceLanguageSupported;
    @NonNull
    private final VoiceInstructionLoader voiceInstructionLoader;
    private int focusGain = DEFAULT_FOCUS_GAIN;

    /**
     * A new instance of {@link Builder}.
     *
     * @param context                for the initialization of the speech players
     * @param language               to be used
     * @param voiceLanguageSupported true if <tt>voiceLanguage</tt> is not null, false otherwise
     * @param voiceInstructionLoader voice instruction loader
     */
    public Builder(
      @NonNull Context context,
      String language,
      boolean voiceLanguageSupported,
      @NonNull VoiceInstructionLoader voiceInstructionLoader
    ) {
      this.context = context;
      this.language = language;
      this.voiceLanguageSupported = voiceLanguageSupported;
      this.voiceInstructionLoader = voiceInstructionLoader;
    }

    /**
     * Audio focus gain. One of {@link AudioManager#AUDIOFOCUS_GAIN_TRANSIENT},
     * {@link AudioManager#AUDIOFOCUS_GAIN}, and etc.
     * <p>
     * Default is {@link AudioManager#AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK}
     * @see AudioManager#requestAudioFocus(AudioManager.OnAudioFocusChangeListener, int, int)
     * @see AudioManager#requestAudioFocus(AudioFocusRequest)
     */
    public Builder audioFocusGain(int focusGain) {
      if (!validFocusGains.contains(focusGain)) {
        throw new IllegalStateException(
          "Valid values for focus requests are AudioManager.AUDIOFOCUS_GAIN, "
            + "AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, "
            + "AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK and "
            + "AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE.");
      }
      this.focusGain = focusGain;
      return this;
    }

    /**
     * Build a new instance of {@link SpeechPlayerProvider}.
     */
    public SpeechPlayerProvider build() {
      return new SpeechPlayerProvider(
        context,
        language,
        voiceLanguageSupported,
        voiceInstructionLoader,
        focusGain
      );
    }
  }
}
