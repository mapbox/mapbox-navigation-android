package com.mapbox.services.android.navigation.ui.v5.voice;

import android.content.Context;

import com.mapbox.services.android.navigation.ui.v5.ConnectivityStatusProvider;

import org.junit.Test;

import java.util.Locale;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
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
