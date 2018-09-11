package com.mapbox.services.android.navigation.ui.v5.listeners;

import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement;

/**
 * This listener will be triggered when a voice announcement is about to be voiced.
 * <p>
 * The listener gives you the option to override any values and pass as the return value,
 * which will be the value used for the voice announcement.
 *
 * @since 0.16.0
 */
public interface SpeechAnnouncementListener {

  /**
   * Listener tied to voice announcements that are about to be voiced.
   * <p>
   * To prevent the given announcement from being announced, you can return null
   * and it will be ignored.
   *
   * @param announcement about to be announced
   * @return text announcement to be played; null if should be ignored
   * @since 0.19.0
   */
  String willVoice(SpeechAnnouncement announcement);
}
