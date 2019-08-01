package com.mapbox.services.android.navigation.ui.v5;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

class NavigationViewSubscriber {

  private NavigationPresenter navigationPresenter;

  NavigationViewSubscriber(NavigationPresenter navigationPresenter) {
    this.navigationPresenter = navigationPresenter;
  }

  void subscribe(LifecycleOwner owner, final NavigationViewModel navigationViewModel) {

    navigationViewModel.retrieveRoute().observe(owner, new Observer<DirectionsRoute>() {
      @Override
      public void onChanged(@Nullable DirectionsRoute directionsRoute) {
        if (directionsRoute != null) {
          navigationPresenter.onRouteUpdate(directionsRoute);
        }
      }
    });

    navigationViewModel.retrieveDestination().observe(owner, new Observer<Point>() {
      @Override
      public void onChanged(@Nullable Point point) {
        if (point != null) {
          navigationPresenter.onDestinationUpdate(point);
        }
      }
    });

    navigationViewModel.retrieveNavigationLocation().observe(owner, new Observer<Location>() {
      @Override
      public void onChanged(@Nullable Location location) {
        if (location != null) {
          navigationPresenter.onNavigationLocationUpdate(location);
        }
      }
    });

    navigationViewModel.retrieveShouldRecordScreenshot().observe(owner, new Observer<Boolean>() {
      @Override
      public void onChanged(@Nullable Boolean shouldRecordScreenshot) {
        if (shouldRecordScreenshot != null && shouldRecordScreenshot) {
          navigationPresenter.onShouldRecordScreenshot();
        }
      }
    });
  }

  void unsubscribe(@NonNull LifecycleOwner owner, @NonNull NavigationViewModel navigationViewModel) {
    navigationViewModel.retrieveRoute().removeObservers(owner);
    navigationViewModel.retrieveDestination().removeObservers(owner);
    navigationViewModel.retrieveNavigationLocation().removeObservers(owner);
    navigationViewModel.retrieveShouldRecordScreenshot().removeObservers(owner);
  }
}
