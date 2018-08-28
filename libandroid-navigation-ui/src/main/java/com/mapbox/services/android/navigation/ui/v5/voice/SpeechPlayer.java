package com.mapbox.services.android.navigation.ui.v5.voice;

/**
 * Defines a contract for speech players
 * used in {@link com.mapbox.services.android.navigation.ui.v5.NavigationView}.
 *
 * @since 0.6.0
 */
public interface SpeechPlayer {

  /**
   * Will play the given string speechAnnouncement.  If a voice speechAnnouncement is already playing or
   * other speechAnnouncement are already queued, the given speechAnnouncement will be queued to play after.
   *
   * @param speechAnnouncement with voice speechAnnouncement data.
   * @since 0.15.1
   */
  void play(SpeechAnnouncement speechAnnouncement);

  /**
   * @return true if currently muted, false if not
   * @since 0.6.0
   */
  boolean isMuted();

  /**
   * Will determine if voice announcements will be played or not.
   * <p>
   * If called while an announcement is currently playing, the announcement should end immediately and any
   * announcements queued should be cleared.
   *
   * @param isMuted true if should be muted, false if should not
   * @since 0.6.0
   */
  void setMuted(boolean isMuted);

  /**
   * Used in off-route scenarios to stop current
   * announcement (if playing) and voice a rerouting cue.
   *
   * @since 0.6.0
   */
  void onOffRoute();

  /**
   * Used to stop and release the media (if needed).
   *
   * @since 0.6.0
   */
  void onDestroy();
}
