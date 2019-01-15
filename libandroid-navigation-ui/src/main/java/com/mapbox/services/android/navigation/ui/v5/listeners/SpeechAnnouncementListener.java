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
   * <p>
   * If the {@code SpeechAnnouncement#ssmlAnnouncement} is malformed,
   * the {@link com.mapbox.services.android.navigation.ui.v5.voice.SpeechPlayer} will fall back to what is
   * included in the {@code SpeechAnnouncement#announcement} with {@link android.speech.tts.TextToSpeech}.
   *
   * @param announcement about to be announced
   * @return speech announcement to be played; null if should be ignored
   */
  SpeechAnnouncement willVoice(SpeechAnnouncement announcement);
}
