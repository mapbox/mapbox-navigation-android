package com.mapbox.navigation.ui.voice;

interface SpeechListener {

  void onStart();

  void onDone();

  void onError(String errorText, SpeechAnnouncement speechAnnouncement);
}
