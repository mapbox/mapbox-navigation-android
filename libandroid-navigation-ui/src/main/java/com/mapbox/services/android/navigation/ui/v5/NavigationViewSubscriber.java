package com.mapbox.services.android.navigation.ui.v5;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.location.Location;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackItem;
import com.mapbox.services.android.navigation.ui.v5.location.LocationViewModel;
import com.mapbox.services.android.navigation.ui.v5.route.OffRouteEvent;
import com.mapbox.services.android.navigation.ui.v5.route.RouteViewModel;
import com.mapbox.services.android.telemetry.location.LocationEngine;

class NavigationViewSubscriber {

  private boolean isOffRoute;
  private NavigationPresenter navigationPresenter;
  private NavigationViewEventDispatcher navigationViewEventDispatcher;

  NavigationViewSubscriber(NavigationPresenter navigationPresenter,
                           NavigationViewEventDispatcher navigationViewEventDispatcher) {
    this.navigationPresenter = navigationPresenter;
    this.navigationViewEventDispatcher = navigationViewEventDispatcher;
  }

  /**
   * Initiate observing of ViewModels by Views.
   */
  void subscribe(LifecycleOwner owner, final LocationViewModel locationViewModel,
                 final RouteViewModel routeViewModel, final NavigationViewModel navigationViewModel) {

    navigationViewModel.addCallback(new NavigationViewModel.Callback() {
      @Override
      public void userOffRoute(OffRouteEvent offRouteEvent) {
        isOffRoute = true;
        Point newOrigin = offRouteEvent.getNewOrigin();
        if (navigationViewEventDispatcher.allowRerouteFrom(newOrigin)) {
          navigationViewEventDispatcher.onOffRoute(newOrigin);
          routeViewModel.fetchRouteFromOffRouteEvent(offRouteEvent);
        }
      }

      @Override
      public void fasterRouteFound(DirectionsRoute directionsRoute) {
        if (directionsRoute != null) {
          navigationViewModel.updateRoute(directionsRoute);
          locationViewModel.updateRoute(directionsRoute);
          navigationPresenter.onRouteUpdate(directionsRoute);
        }
      }
    });

    routeViewModel.setCallback(new RouteViewModel.Callback() {
      @Override
      public void onRouteChanged(DirectionsRoute directionsRoute) {
        if (directionsRoute != null) {
          navigationViewModel.updateRoute(directionsRoute);
          locationViewModel.updateRoute(directionsRoute);
          navigationPresenter.onRouteUpdate(directionsRoute);

          if (isOffRoute) {
            navigationViewEventDispatcher.onRerouteAlong(directionsRoute);
            isOffRoute = false;
          }
        }
      }
    });

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

    routeViewModel.requestErrorMessage.observe(owner, new Observer<String>() {
      @Override
      public void onChanged(@Nullable String requestErrorMessage) {
        if (isOffRoute && requestErrorMessage != null) {
          navigationViewEventDispatcher.onFailedReroute(requestErrorMessage);
          // Discard message after firing the listener
          routeViewModel.requestErrorMessage.setValue(null);
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
            navigationViewEventDispatcher.onNavigationFinished();
          } else {
            navigationViewEventDispatcher.onNavigationRunning();
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

    navigationViewModel.shouldRecordScreenshot.observe(owner, new Observer<Boolean>() {
      @Override
      public void onChanged(@Nullable Boolean shouldRecordScreenshot) {
        if (shouldRecordScreenshot != null && shouldRecordScreenshot) {
          navigationPresenter.onShouldRecordScreenshot();
        }
      }
    });

    navigationViewModel.isFeedbackShowing.observe(owner, new Observer<Boolean>() {
      @Override
      public void onChanged(@Nullable Boolean isFeedbackShowing) {
        if (isFeedbackShowing != null) {
          if (isFeedbackShowing) {
            navigationViewEventDispatcher.onFeedbackOpened();
          } else {
            // If not showing, a user has cancelled / dismissed the feedback UI
            navigationViewEventDispatcher.onFeedbackCancelled();
          }
        }
      }
    });

    navigationViewModel.selectedFeedbackItem.observe(owner, new Observer<FeedbackItem>() {
      @Override
      public void onChanged(@Nullable FeedbackItem feedbackItem) {
        if (feedbackItem != null) {
          navigationViewEventDispatcher.onFeedbackSent(feedbackItem);
        }
      }
    });
  }
}
