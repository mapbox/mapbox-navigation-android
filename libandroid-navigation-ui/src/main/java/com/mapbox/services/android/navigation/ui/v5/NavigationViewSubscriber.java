package com.mapbox.services.android.navigation.ui.v5;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.location.Location;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

class NavigationViewSubscriber {

  private NavigationPresenter navigationPresenter;

  NavigationViewSubscriber(NavigationPresenter navigationPresenter) {
    this.navigationPresenter = navigationPresenter;
  }

  void subscribe(LifecycleOwner owner, final NavigationViewModel navigationViewModel) {

    navigationViewModel.route.observe(owner, new Observer<DirectionsRoute>() {
      @Override
      public void onChanged(@Nullable DirectionsRoute directionsRoute) {
        if (directionsRoute != null) {
          navigationPresenter.onRouteUpdate(directionsRoute);
        }
      }
    });

    navigationViewModel.destination.observe(owner, new Observer<Point>() {
      @Override
      public void onChanged(@Nullable Point point) {
        if (point != null) {
          navigationPresenter.onDestinationUpdate(point);
        }
      }
    });

    navigationViewModel.navigationLocation.observe(owner, new Observer<Location>() {
      @Override
      public void onChanged(@Nullable Location location) {
        if (location != null) {
          navigationPresenter.onNavigationLocationUpdate(location);
        }
      }
    });

    navigationViewModel.shouldRecordScreenshot.observe(owner, new Observer<Boolean>() {
      @Override
      public void onChanged(@Nullable Boolean shouldRecordScreenshot) {
        if (shouldRecordScreenshot != null && shouldRecordScreenshot) {
          navigationPresenter.onShouldRecordScreenshot();
        }
      }
    });
  }
}
