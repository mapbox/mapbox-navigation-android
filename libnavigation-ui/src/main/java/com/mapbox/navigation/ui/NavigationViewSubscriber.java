package com.mapbox.navigation.ui;

import android.location.Location;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.navigation.base.trip.model.RouteProgress;

class NavigationViewSubscriber implements LifecycleObserver {

  private final LifecycleOwner lifecycleOwner;
  private final NavigationViewModel navigationViewModel;
  private final NavigationPresenter navigationPresenter;

  NavigationViewSubscriber(final LifecycleOwner owner, final NavigationViewModel navigationViewModel,
      final NavigationPresenter navigationPresenter) {
    lifecycleOwner = owner;
    lifecycleOwner.getLifecycle().addObserver(this);
    this.navigationViewModel = navigationViewModel;
    this.navigationPresenter = navigationPresenter;
  }

  void subscribe() {
    navigationViewModel.retrieveRoute().observe(lifecycleOwner, new Observer<DirectionsRoute>() {
      @Override
      public void onChanged(@Nullable DirectionsRoute directionsRoute) {
        if (directionsRoute != null) {
          navigationPresenter.onRouteUpdate(directionsRoute);
        }
      }
    });

    navigationViewModel.retrieveDestination().observe(lifecycleOwner, new Observer<Point>() {
      @Override
      public void onChanged(@Nullable Point point) {
        if (point != null) {
          navigationPresenter.onDestinationUpdate(point);
        }
      }
    });

    navigationViewModel.retrieveNavigationLocation().observe(lifecycleOwner, new Observer<Location>() {
      @Override
      public void onChanged(@Nullable Location location) {
        if (location != null) {
          navigationPresenter.onNavigationLocationUpdate(location);
        }
      }
    });

    navigationViewModel.retrieveRouteProgressUpdates().observe(lifecycleOwner, new Observer<RouteProgress>() {
      @Override
      public void onChanged(RouteProgress routeProgress) {
        if (routeProgress != null) {
          navigationPresenter.onRouteProgress(routeProgress);
        }
      }
    });

    navigationViewModel.retrieveShouldRecordScreenshot().observe(lifecycleOwner, new Observer<Boolean>() {
      @Override
      public void onChanged(@Nullable Boolean shouldRecordScreenshot) {
        if (shouldRecordScreenshot != null && shouldRecordScreenshot) {
          navigationPresenter.onShouldRecordScreenshot();
        }
      }
    });
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  void unsubscribe() {
    navigationViewModel.retrieveRoute().removeObservers(lifecycleOwner);
    navigationViewModel.retrieveDestination().removeObservers(lifecycleOwner);
    navigationViewModel.retrieveNavigationLocation().removeObservers(lifecycleOwner);
    navigationViewModel.retrieveShouldRecordScreenshot().removeObservers(lifecycleOwner);
  }
}
