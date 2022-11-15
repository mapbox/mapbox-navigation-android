package com.mapbox.navigation.examples.androidauto.testing;

import static com.mapbox.maps.extension.style.StyleExtensionImplKt.style;

import android.app.Application;
import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.model.Distance;
import androidx.lifecycle.LifecycleOwner;

import com.mapbox.androidauto.internal.search.CarPlaceSearch;
import com.mapbox.androidauto.map.MapboxCarMapLoader;
import com.mapbox.androidauto.navigation.CarDistanceFormatter;
import com.mapbox.androidauto.navigation.MapboxCarNavigationManager;
import com.mapbox.maps.extension.androidauto.MapboxCarMap;
import com.mapbox.maps.extension.style.StyleContract;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver;
import com.mapbox.navigation.ui.maps.NavigationStyles;

import java.util.List;

import kotlin.Unit;
import kotlinx.coroutines.CoroutineScope;

class CarJavaInterfaceChecker {

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

    MapboxNavigationObserver otherObserver = MapboxNavigationApp.getObserver(CarPlaceSearch.class);
    List<CarPlaceSearch> otherObservers = MapboxNavigationApp.getObservers(CarPlaceSearch.class);
  }

  void MapboxNavigationObserver() {
    MapboxNavigationObserver observer = new MapboxNavigationObserver() {
      @Override
      public void onAttached(@NonNull MapboxNavigation mapboxNavigation) {

      }

      @Override
      public void onDetached(@NonNull MapboxNavigation mapboxNavigation) {

      }
    };
  }

  void MapboxCarNavigationManager(
          CarContext carContext,
          LifecycleOwner lifecycleOwner,
          MapboxCarMap mapboxCarMap
  ) {
    // Constructor
    MapboxCarNavigationManager sut = new MapboxCarNavigationManager(carContext);

    // Observing auto drive
    CoroutineScope scope = JavaFlow.lifecycleScope(lifecycleOwner);
    JavaFlow.collect(sut.getAutoDriveEnabledFlow(), scope, (enabled) -> {
      // check enabled
    });

    // Get auto drive value
    boolean isEnabled = sut.getAutoDriveEnabledFlow().getValue();

    // Register onto MapboxNavigationAPp
    MapboxNavigationApp.registerObserver(sut);
    MapboxNavigationApp.unregisterObserver(sut);
  }

  void CarDistanceFormatter() {
    SpannableString spannableString = CarDistanceFormatter.formatDistance(1200.0);
    Distance distance = CarDistanceFormatter.carDistance(1200.0);
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
