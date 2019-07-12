package com.mapbox.services.android.navigation.ui.v5.voice;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Used to play {@link SpeechAnnouncement}s.
 * <p>
 * Takes a {@link SpeechPlayerProvider} which will provide either a {@link MapboxSpeechPlayer}
 * or {@link AndroidSpeechPlayer} based on the given language - if it is supported by our Voice API.
 * <p>
 * {@link MapboxSpeechPlayer} requires Internet connectivity.  In cases where a connection is not
 * available, the provider will fall back to the {@link AndroidSpeechPlayer}.
 *
 * @since 0.16.0
 */
public class NavigationSpeechPlayer implements SpeechPlayer {

  private SpeechPlayerProvider speechPlayerProvider;
  private boolean isMuted;

  public NavigationSpeechPlayer(SpeechPlayerProvider speechPlayerProvider) {
    this.speechPlayerProvider = speechPlayerProvider;
  }

  /**
   * Plays the given {@link SpeechAnnouncement}.
   *
   * @param speechAnnouncement with SSML and normal announcement text
   * @since 0.16.0
   */
  @Override
  public void play(SpeechAnnouncement speechAnnouncement) {
    speechPlayerProvider.retrieveSpeechPlayer().play(speechAnnouncement);
  }

  /**
   * Returns the current muted state of the player.
   *
   * @return current muted state
   * @since 0.16.0
   */
  @Override
  public boolean isMuted() {
    return isMuted;
  }

  /**
   * Mutes or un-mutes the {@link SpeechPlayer}.
   * <p>
   * If an announcement is playing at the time this method is called,
   * the announcement will be stopped immediately.
   *
   * @param isMuted true to mute, false to un-mute
   * @since 0.16.0
   */
  @Override
  public void setMuted(boolean isMuted) {
    this.isMuted = isMuted;
    speechPlayerProvider.setMuted(isMuted);
  }

  /**
   * Optional method to implement in an {@link com.mapbox.services.android.navigation.v5.offroute.OffRouteListener}.
   * <p>
   * During an off-route scenario, you can use this method to cancel existing announcements without
   * completely muting the player.
   *
   * @since 0.16.0
   */
  @Override
  public void onOffRoute() {
    speechPlayerProvider.onOffRoute();
  }

  /**
   * Required method to implement in {@link FragmentActivity#onDestroy()} or
   * {@link Fragment#onDestroy()}.
   * <p>
   * Ensures the player is properly shutdown and finishes any running announcements.
   *
   * @since 0.16.0
   */
  @Override
  public void onDestroy() {
    speechPlayerProvider.onDestroy();
  }
}
