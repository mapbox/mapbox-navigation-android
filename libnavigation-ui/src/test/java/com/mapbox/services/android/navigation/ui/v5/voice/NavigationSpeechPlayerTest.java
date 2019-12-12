package com.mapbox.services.android.navigation.ui.v5.voice;

import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationSpeechPlayerTest {

  @Test
  public void onPlayAnnouncement_mapboxSpeechPlayerIsGivenAnnouncement() {
    MapboxSpeechPlayer speechPlayer = mock(MapboxSpeechPlayer.class);
    NavigationSpeechPlayer navigationSpeechPlayer = buildNavigationSpeechPlayer(speechPlayer);
    SpeechAnnouncement announcement = mock(SpeechAnnouncement.class);

    navigationSpeechPlayer.play(announcement);

    verify(speechPlayer).play(announcement);
  }

  @Test
  public void onPlayAnnouncement_androidSpeechPlayerIsGivenAnnouncement() {
    AndroidSpeechPlayer speechPlayer = mock(AndroidSpeechPlayer.class);
    NavigationSpeechPlayer navigationSpeechPlayer = buildNavigationSpeechPlayer(speechPlayer);
    SpeechAnnouncement announcement = mock(SpeechAnnouncement.class);

    navigationSpeechPlayer.play(announcement);

    verify(speechPlayer).play(announcement);
  }

  @Test
  public void onIsMuted_returnsCorrectBooleanMuteValue() {
    MapboxSpeechPlayer speechPlayer = mock(MapboxSpeechPlayer.class);
    NavigationSpeechPlayer navigationSpeechPlayer = buildNavigationSpeechPlayer(speechPlayer);

    navigationSpeechPlayer.setMuted(true);

    assertTrue(navigationSpeechPlayer.isMuted());
  }

  @Test
  public void onSetMuted_providerIsSetMuted() {
    SpeechPlayerProvider provider = mock(SpeechPlayerProvider.class);
    NavigationSpeechPlayer navigationSpeechPlayer = new NavigationSpeechPlayer(provider);

    navigationSpeechPlayer.setMuted(true);

    verify(provider).setMuted(true);
  }

  @Test
  public void onOffRoute_providerOnOffRouteIsCalled() {
    SpeechPlayerProvider provider = mock(SpeechPlayerProvider.class);
    NavigationSpeechPlayer navigationSpeechPlayer = new NavigationSpeechPlayer(provider);

    navigationSpeechPlayer.onOffRoute();

    verify(provider).onOffRoute();
  }

  @Test
  public void onDestroy_providerOnDestroyIsCalled() {
    SpeechPlayerProvider provider = mock(SpeechPlayerProvider.class);
    NavigationSpeechPlayer navigationSpeechPlayer = new NavigationSpeechPlayer(provider);

    navigationSpeechPlayer.onOffRoute();

    verify(provider).onOffRoute();
  }

  private NavigationSpeechPlayer buildNavigationSpeechPlayer(SpeechPlayer speechPlayer) {
    SpeechPlayerProvider provider = mock(SpeechPlayerProvider.class);
    when(provider.retrieveSpeechPlayer()).thenReturn(speechPlayer);
    return new NavigationSpeechPlayer(provider);
  }
}
