package com.mapbox.services.android.navigation.ui.v5.voice;

import android.support.v4.util.Pair;

interface SpeechAnnouncementUpdate {

  Pair<String, String> buildTextAndTypeFrom(SpeechAnnouncement speechAnnouncement);
}
