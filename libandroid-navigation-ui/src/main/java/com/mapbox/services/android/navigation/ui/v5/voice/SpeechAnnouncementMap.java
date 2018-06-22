package com.mapbox.services.android.navigation.ui.v5.voice;

import android.support.v4.util.Pair;

import java.util.HashMap;

class SpeechAnnouncementMap extends HashMap<Boolean, SpeechAnnouncementUpdate> {

  private static final String SSML_TEXT_TYPE = "ssml";
  private static final String TEXT_TYPE = "text";

  SpeechAnnouncementMap() {
    super(2);
    put(true, new SpeechAnnouncementUpdate() {
      @Override
      public Pair<String, String> buildTextAndTypeFrom(SpeechAnnouncement speechAnnouncement) {
        return new Pair<>(speechAnnouncement.ssmlAnnouncement(), SSML_TEXT_TYPE);
      }
    });
    put(false, new SpeechAnnouncementUpdate() {
      @Override
      public Pair<String, String> buildTextAndTypeFrom(SpeechAnnouncement speechAnnouncement) {
        return new Pair<>(speechAnnouncement.announcement(), TEXT_TYPE);
      }
    });
  }
}
