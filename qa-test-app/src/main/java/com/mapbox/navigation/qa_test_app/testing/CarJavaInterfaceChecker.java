package com.mapbox.navigation.qa_test_app.testing;

import android.app.Application;
import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.model.Distance;
import androidx.lifecycle.LifecycleOwner;

import com.mapbox.androidauto.MapboxCarNavigationManager;
import com.mapbox.androidauto.car.navigation.CarDistanceFormatter;
import com.mapbox.androidauto.internal.car.search.CarPlaceSearch;
import com.mapbox.maps.extension.androidauto.MapboxCarMap;
import com.mapbox.navigation.base.formatter.Rounding;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver;

import java.util.List;

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
}
