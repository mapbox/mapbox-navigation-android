package com.mapbox.services.android.navigation.ui.v5.voice;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationSpeechListenerTest {

  @Test
  public void onStart_audioFocusIsRequested() {
    SpeechAudioFocusManager audioFocusManager = mock(SpeechAudioFocusManager.class);
    NavigationSpeechListener navigationSpeechListener = buildSpeechListener(audioFocusManager);

    navigationSpeechListener.onStart();

    verify(audioFocusManager).requestAudioFocus();
  }

  @Test
  public void onDone_audioFocusIsAbandoned() {
    SpeechAudioFocusManager audioFocusManager = mock(SpeechAudioFocusManager.class);
    NavigationSpeechListener navigationSpeechListener = buildSpeechListener(audioFocusManager);

    navigationSpeechListener.onDone();

    verify(audioFocusManager).abandonAudioFocus();
  }

  @Test
  public void onError_fallbackGoesToAndroidSpeechPlayer() {
    SpeechPlayerProvider provider = mock(SpeechPlayerProvider.class);
    AndroidSpeechPlayer androidSpeechPlayer = mock(AndroidSpeechPlayer.class);
    when(provider.retrieveAndroidSpeechPlayer()).thenReturn(androidSpeechPlayer);
    NavigationSpeechListener navigationSpeechListener = buildSpeechListener(provider);
    SpeechAnnouncement announcement = buildAnnouncement();

    navigationSpeechListener.onError("Error text", announcement);

    verify(androidSpeechPlayer).play(announcement);
  }

  private NavigationSpeechListener buildSpeechListener(SpeechAudioFocusManager audioFocusManager) {
    SpeechPlayerProvider provider = mock(SpeechPlayerProvider.class);
    return new NavigationSpeechListener(provider, audioFocusManager);
  }

  private NavigationSpeechListener buildSpeechListener(SpeechPlayerProvider provider) {
    SpeechAudioFocusManager audioFocusManager = mock(SpeechAudioFocusManager.class);
    return new NavigationSpeechListener(provider, audioFocusManager);
  }

  private SpeechAnnouncement buildAnnouncement() {
    return SpeechAnnouncement.builder()
      .ssmlAnnouncement("SSML announcement text")
      .announcement("Announcement text")
      .build();
  }
}
