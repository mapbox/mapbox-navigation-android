package com.mapbox.navigation.qa_test_app.testing;

import android.app.Application;

import androidx.lifecycle.LifecycleOwner;

import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver;
import com.mapbox.navigation.ui.voice.api.MapboxAudioGuidance;

import kotlinx.coroutines.CoroutineScope;

class JavaInterfaceChecker {

  void MapboxNavigationApp(
          Application application,
          LifecycleOwner lifecycleOwner,
          NavigationOptions navigationOptions,
          MapboxNavigationObserver observer
  ) {
    // Set up now
    MapboxNavigationApp.setup(navigationOptions);

    // Set up provider
    MapboxNavigationApp.setup(() -> navigationOptions);

    // Control lifecycles
    MapboxNavigationApp.attach(lifecycleOwner);
    MapboxNavigationApp.disable();
    MapboxNavigationApp.detach(lifecycleOwner);
    MapboxNavigationApp.attachAllActivities(application);
    MapboxNavigationApp.getLifecycleOwner();

    // Get current instance
    MapboxNavigation mapboxNavigation = MapboxNavigationApp.current();

    // Register and unregister observer
    MapboxNavigationApp.registerObserver(observer);
    MapboxNavigationApp.unregisterObserver(observer);
  }

  void MapboxAudioGuidance(
          CoroutineScope coroutineScope
  ) {
    MapboxAudioGuidance sut = MapboxAudioGuidance.getRegisteredInstance();
    sut.mute();
    sut.unmute();
    sut.toggle();
    MapboxNavigationApp.registerObserver(sut);
    MapboxNavigationApp.unregisterObserver(sut);
    JavaFlow.collect(sut.stateFlow(), coroutineScope, (enabled) -> {
      // observe state
    });
  }
}
