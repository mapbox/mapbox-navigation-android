package com.mapbox.navigation.ui.voice;

import android.content.Context;

import com.mapbox.navigation.ui.internal.ConnectivityStatusProvider;

import org.junit.Test;

import java.util.Locale;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SpeechPlayerProviderTest {

  @Test
  public void sanity() {
    SpeechPlayerProvider provider = buildSpeechPlayerProvider(true);

    assertNotNull(provider);
  }

  @Test
  public void voiceLanguageSupported_returnsMapboxSpeechPlayer() {
    boolean voiceLanguageSupported = true;
    SpeechPlayerProvider provider = buildSpeechPlayerProvider(voiceLanguageSupported);

    SpeechPlayer speechPlayer = provider.retrieveSpeechPlayer();

    assertTrue(speechPlayer instanceof MapboxSpeechPlayer);
  }

  @Test
  public void voiceLanguageNotSupported_returnsAndroidSpeechPlayer() {
    boolean voiceLanguageNotSupported = false;
    SpeechPlayerProvider provider = buildSpeechPlayerProvider(voiceLanguageNotSupported);

    SpeechPlayer speechPlayer = provider.retrieveSpeechPlayer();

    assertTrue(speechPlayer instanceof AndroidSpeechPlayer);
  }

  @Test
  public void retrieveAndroidSpeechPlayer_alwaysReturnsAndroidSpeechPlayer() {
    SpeechPlayerProvider provider = buildSpeechPlayerProvider(true);

    AndroidSpeechPlayer speechPlayer = provider.retrieveAndroidSpeechPlayer();

    assertNotNull(speechPlayer);
  }

  @Test
  public void noConnectivity_alwaysReturnsAndroidSpeechPlayer() {
    Context context = mock(Context.class);
    String language = Locale.US.getLanguage();
    VoiceInstructionLoader voiceInstructionLoader = mock(VoiceInstructionLoader.class);
    ConnectivityStatusProvider connectivityStatus = mock(ConnectivityStatusProvider.class);
    when(connectivityStatus.isConnectedFast()).thenReturn(false);
    SpeechPlayerProvider provider = new SpeechPlayerProvider(context, language, true,
      voiceInstructionLoader, connectivityStatus);
    provider.onSpeechPlayerStateChanged(SpeechPlayerState.OFFLINE_PLAYER_INITIALIZED);

    SpeechPlayer speechPlayer = provider.retrieveSpeechPlayer();

    assertTrue(speechPlayer instanceof AndroidSpeechPlayer);
  }

  @Test
  public void noCache_alwaysReturnsAndroidSpeechPlayer() {
    Context context = mock(Context.class);
    String language = Locale.US.getLanguage();
    VoiceInstructionLoader voiceInstructionLoader = mock(VoiceInstructionLoader.class);
    when(voiceInstructionLoader.hasCache()).thenReturn(false);
    ConnectivityStatusProvider connectivityStatus = mock(ConnectivityStatusProvider.class);
    when(connectivityStatus.isConnectedFast()).thenReturn(false);
    SpeechPlayerProvider provider = new SpeechPlayerProvider(context, language, true,
      voiceInstructionLoader, connectivityStatus);
    provider.onSpeechPlayerStateChanged(SpeechPlayerState.OFFLINE_PLAYER_INITIALIZED);

    SpeechPlayer speechPlayer = provider.retrieveSpeechPlayer();

    assertTrue(speechPlayer instanceof AndroidSpeechPlayer);
  }

  @Test
  public void hasCache_alwaysReturnsMapboxSpeechPlayer() {
    Context context = mock(Context.class);
    String language = Locale.US.getLanguage();
    VoiceInstructionLoader voiceInstructionLoader = mock(VoiceInstructionLoader.class);
    when(voiceInstructionLoader.hasCache()).thenReturn(true);
    ConnectivityStatusProvider connectivityStatus = mock(ConnectivityStatusProvider.class);
    SpeechPlayerProvider provider = new SpeechPlayerProvider(context, language, true,
      voiceInstructionLoader, connectivityStatus);

    SpeechPlayer speechPlayer = provider.retrieveSpeechPlayer();

    assertTrue(speechPlayer instanceof MapboxSpeechPlayer);
  }

  @Test
  public void offLinePlaying_alwaysReturnsNullSpeechPlayer() {
    SpeechPlayerProvider provider = buildSpeechPlayerProvider(true);
    provider.onSpeechPlayerStateChanged(SpeechPlayerState.OFFLINE_PLAYING);

    SpeechPlayer speechPlayer = provider.retrieveSpeechPlayer();

    assertNull(speechPlayer);
  }

  @Test
  public void noCacheNoConnectivityOnlinePlaying_alwaysReturnsNullSpeechPlayer() {
    Context context = mock(Context.class);
    String language = Locale.US.getLanguage();
    VoiceInstructionLoader voiceInstructionLoader = mock(VoiceInstructionLoader.class);
    when(voiceInstructionLoader.hasCache()).thenReturn(false);
    ConnectivityStatusProvider connectivityStatus = mock(ConnectivityStatusProvider.class);
    when(connectivityStatus.isConnectedFast()).thenReturn(false);
    SpeechPlayerProvider provider = new SpeechPlayerProvider(context, language, true,
            voiceInstructionLoader, connectivityStatus);
    provider.onSpeechPlayerStateChanged(SpeechPlayerState.ONLINE_PLAYING);

    SpeechPlayer speechPlayer = provider.retrieveSpeechPlayer();

    assertNull(speechPlayer);
  }

  @Test
  public void speechPlayerStateChanged_observerCalled() {
    SpeechPlayerProvider provider = buildSpeechPlayerProvider(true);
    SpeechPlayerStateChangeObserver observer = mock(SpeechPlayerStateChangeObserver.class);
    provider.setSpeechPlayerStateChangeObserver(observer);
    SpeechPlayerState state = mock(SpeechPlayerState.class);

    provider.onSpeechPlayerStateChanged(state);

    verify(observer).onStateChange(state);
  }

  @Test
  public void slowConnectionEnableFallback_returnAndroidSpeechPlayer() {
    Context context = mock(Context.class);
    String language = Locale.US.getLanguage();
    VoiceInstructionLoader voiceInstructionLoader = mock(VoiceInstructionLoader.class);
    ConnectivityStatusProvider connectivityStatus = mock(ConnectivityStatusProvider.class);
    when(connectivityStatus.isConnectedFast()).thenReturn(false);
    when(connectivityStatus.isConnected()).thenReturn(true);
    SpeechPlayerProvider provider = new SpeechPlayerProvider(context, language, true,
        voiceInstructionLoader, connectivityStatus);
    provider.onSpeechPlayerStateChanged(SpeechPlayerState.OFFLINE_PLAYER_INITIALIZED);

    SpeechPlayer speechPlayer = provider.retrieveSpeechPlayer();

    assertTrue(speechPlayer instanceof AndroidSpeechPlayer);
  }

  @Test
  public void slowConnectionDisableFallback_returnMapboxSpeechPlayer() {
    Context context = mock(Context.class);
    String language = Locale.US.getLanguage();
    VoiceInstructionLoader voiceInstructionLoader = mock(VoiceInstructionLoader.class);
    ConnectivityStatusProvider connectivityStatus = mock(ConnectivityStatusProvider.class);
    when(connectivityStatus.isConnectedFast()).thenReturn(false);
    when(connectivityStatus.isConnected()).thenReturn(true);
    SpeechPlayerProvider provider = new SpeechPlayerProvider(context, language, true,
        voiceInstructionLoader, connectivityStatus);
    provider.setIsFallbackAlwaysEnabled(false);

    SpeechPlayer speechPlayer = provider.retrieveSpeechPlayer();

    assertTrue(speechPlayer instanceof MapboxSpeechPlayer);
  }

  @Test
  public void offlinePlayerInitializingNoCacheNoConnectivity_returnNullSpeechPlayer() {
    Context context = mock(Context.class);
    String language = Locale.US.getLanguage();
    VoiceInstructionLoader voiceInstructionLoader = mock(VoiceInstructionLoader.class);
    ConnectivityStatusProvider connectivityStatus = mock(ConnectivityStatusProvider.class);
    when(connectivityStatus.isConnectedFast()).thenReturn(false);
    SpeechPlayerProvider provider = new SpeechPlayerProvider(context, language, true,
        voiceInstructionLoader, connectivityStatus);

    SpeechPlayer speechPlayer = provider.retrieveSpeechPlayer();

    assertNull(speechPlayer);
  }

  @Test
  public void offlinePlayerInitializedBeforePlaying_idleStateObserved() {
    SpeechPlayerProvider provider = buildSpeechPlayerProvider(true);
    SpeechPlayerStateChangeObserver observer = mock(SpeechPlayerStateChangeObserver.class);
    provider.setSpeechPlayerStateChangeObserver(observer);

    provider.onSpeechPlayerStateChanged(SpeechPlayerState.OFFLINE_PLAYER_INITIALIZED);

    verify(observer).onStateChange(SpeechPlayerState.IDLE);
  }

  @Test
  public void offlinePlayerInitializedAfterPlaying_noStateObserved() {
    SpeechPlayerProvider provider = buildSpeechPlayerProvider(true);
    provider.onSpeechPlayerStateChanged(SpeechPlayerState.ONLINE_PLAYING);
    SpeechPlayerStateChangeObserver observer = mock(SpeechPlayerStateChangeObserver.class);
    provider.setSpeechPlayerStateChangeObserver(observer);

    provider.onSpeechPlayerStateChanged(SpeechPlayerState.OFFLINE_PLAYER_INITIALIZED);

    verify(observer).onStateChange(SpeechPlayerState.ONLINE_PLAYING);
  }

  private SpeechPlayerProvider buildSpeechPlayerProvider(boolean voiceLanguageSupported) {
    Context context = mock(Context.class);
    String language = Locale.US.getLanguage();
    VoiceInstructionLoader voiceInstructionLoader = mock(VoiceInstructionLoader.class);
    when(voiceInstructionLoader.hasCache()).thenReturn(true);
    ConnectivityStatusProvider connectivityStatus = mock(ConnectivityStatusProvider.class);
    return new SpeechPlayerProvider(context, language, voiceLanguageSupported,
      voiceInstructionLoader, connectivityStatus);
  }
}
