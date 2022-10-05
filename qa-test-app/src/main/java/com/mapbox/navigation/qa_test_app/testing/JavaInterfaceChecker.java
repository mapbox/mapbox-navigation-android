package com.mapbox.navigation.qa_test_app.testing;

import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp;
import com.mapbox.navigation.ui.voice.api.MapboxAudioGuidance;

import kotlinx.coroutines.CoroutineScope;

class JavaInterfaceChecker {

  void MapboxAudioGuidance(
          CoroutineScope coroutineScope
  ) {
    MapboxAudioGuidance sut = MapboxAudioGuidance.getRegisteredInstance();
    sut.mute();
    sut.unMute();
    sut.toggle();
    MapboxNavigationApp.registerObserver(sut);
    MapboxNavigationApp.unregisterObserver(sut);
    JavaFlow.collect(sut.stateFlow(), coroutineScope, (enabled) -> {
      // observe state
    });
  }
}
