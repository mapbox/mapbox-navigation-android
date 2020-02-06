package com.mapbox.navigation.ui.voice;

import com.mapbox.api.directions.v5.models.VoiceInstructions;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationVoiceListenerTest {

  @Test
  public void onStart_audioFocusIsRequested() {
    SpeechAudioFocusManager audioFocusManager = mock(SpeechAudioFocusManager.class);
    NavigationVoiceListener navigationSpeechListener = buildSpeechListener(audioFocusManager);

    navigationSpeechListener.onStart();

    verify(audioFocusManager).requestAudioFocus();
  }

  @Test
  public void onDone_audioFocusIsAbandoned() {
    SpeechAudioFocusManager audioFocusManager = mock(SpeechAudioFocusManager.class);
    NavigationVoiceListener navigationSpeechListener = buildSpeechListener(audioFocusManager);

    navigationSpeechListener.onDone();

    verify(audioFocusManager).abandonAudioFocus();
  }

  @Test
  public void onError_fallbackGoesToAndroidSpeechPlayer() {
    SpeechPlayerProvider provider = mock(SpeechPlayerProvider.class);
    AndroidSpeechPlayer androidSpeechPlayer = mock(AndroidSpeechPlayer.class);
    when(provider.retrieveAndroidSpeechPlayer()).thenReturn(androidSpeechPlayer);
    NavigationVoiceListener navigationSpeechListener = buildSpeechListener(provider);
    VoiceInstructions announcement = buildAnnouncement();

    navigationSpeechListener.onError("Error text", announcement);
    verify(androidSpeechPlayer).play(announcement);
  }

  private NavigationVoiceListener buildSpeechListener(SpeechAudioFocusManager audioFocusManager) {
    SpeechPlayerProvider provider = mock(SpeechPlayerProvider.class);
    return new NavigationVoiceListener(provider, audioFocusManager);
  }

  private NavigationVoiceListener buildSpeechListener(SpeechPlayerProvider provider) {
    SpeechAudioFocusManager audioFocusManager = mock(SpeechAudioFocusManager.class);
    return new NavigationVoiceListener(provider, audioFocusManager);
  }

  private VoiceInstructions buildAnnouncement() {
    return VoiceInstructions.builder()
            .ssmlAnnouncement("SSML announcement text")
            .announcement("Announcement text")
            .build();
  }
}
