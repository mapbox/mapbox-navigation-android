package com.mapbox.services.android.navigation.ui.v5;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.location.Location;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.location.LocationViewModel;
import com.mapbox.services.android.navigation.ui.v5.route.RouteViewModel;
import com.mapbox.services.android.telemetry.location.LocationEngine;

class NavigationViewSubscriber {

  private NavigationPresenter navigationPresenter;
  private NavigationViewListener navigationViewListener;

  NavigationViewSubscriber(NavigationPresenter navigationPresenter, NavigationViewListener navigationViewListener) {
    this.navigationPresenter = navigationPresenter;
    this.navigationViewListener = navigationViewListener;
  }

  /**
   * Initiate observing of ViewModels by Views.
   */
  void subscribe(LifecycleOwner owner, final LocationViewModel locationViewModel,
                 final RouteViewModel routeViewModel, final NavigationViewModel navigationViewModel) {

    locationViewModel.rawLocation.observe(owner, new Observer<Location>() {
      @Override
      public void onChanged(@Nullable Location location) {
        if (location != null) {
          routeViewModel.updateRawLocation(location);
        }
      }
    });

    locationViewModel.locationEngine.observe(owner, new Observer<LocationEngine>() {
      @Override
      public void onChanged(@Nullable LocationEngine locationEngine) {
        if (locationEngine != null) {
          navigationViewModel.updateLocationEngine(locationEngine);
        }
      }
    });

    routeViewModel.route.observe(owner, new Observer<DirectionsRoute>() {
      @Override
      public void onChanged(@Nullable DirectionsRoute directionsRoute) {
        if (directionsRoute != null) {
          navigationViewModel.updateRoute(directionsRoute);
          locationViewModel.updateRoute(directionsRoute);
          navigationPresenter.onRouteUpdate(directionsRoute);
        }
      }
    });

    routeViewModel.destination.observe(owner, new Observer<Point>() {
      @Override
      public void onChanged(@Nullable Point point) {
        if (point != null) {
          navigationPresenter.onDestinationUpdate(point);
        }
      }
    });

    navigationViewModel.isRunning.observe(owner, new Observer<Boolean>() {
      @Override
      public void onChanged(@Nullable Boolean isRunning) {
        if (isRunning != null) {
          if (!isRunning) {
            navigationViewListener.onNavigationFinished();
          }
        }
      }
    });

    navigationViewModel.navigationLocation.observe(owner, new Observer<Location>() {
      @Override
      public void onChanged(@Nullable Location location) {
        if (location != null && location.getLongitude() != 0 && location.getLatitude() != 0) {
          navigationPresenter.onNavigationLocationUpdate(location);
        }
      }
    });

    navigationViewModel.newOrigin.observe(owner, new Observer<Point>() {
      @Override
      public void onChanged(@Nullable Point newOrigin) {
        if (newOrigin != null) {
          routeViewModel.fetchRouteNewOrigin(newOrigin);
          // To prevent from firing on rotation
          navigationViewModel.newOrigin.setValue(null);
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
