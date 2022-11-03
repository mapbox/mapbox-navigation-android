package com.mapbox.navigation.qa_test_app.testing;

import static com.mapbox.maps.extension.style.StyleExtensionImplKt.style;

import com.mapbox.androidauto.map.MapboxCarMapLoader;
import com.mapbox.maps.extension.style.StyleContract;
import com.mapbox.maps.extension.style.StyleExtensionImpl;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp;
import com.mapbox.navigation.ui.maps.NavigationStyles;
import com.mapbox.navigation.ui.voice.api.MapboxAudioGuidance;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlinx.coroutines.CoroutineScope;

class JavaInterfaceChecker {

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

  void MapboxCarMapLoader() {
    MapboxCarMapLoader sut = new MapboxCarMapLoader();
    sut.setDarkStyleOverride(styleExtension(NavigationStyles.NAVIGATION_NIGHT_STYLE));
    sut.setLightStyleOverride(styleExtension(NavigationStyles.NAVIGATION_DAY_STYLE));
  }

  StyleContract.StyleExtension styleExtension(String styleUri) {
    return style(styleUri, builder -> Unit.INSTANCE);
  }
}
