package com.mapbox.services.android.navigation.ui.v5;


import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback;
import android.view.View;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackItem;
import com.mapbox.services.android.navigation.ui.v5.listeners.FeedbackListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;

/**
 * In charge of holding any {@link NavigationView} related listeners {@link NavigationListener},
 * {@link RouteListener}, or {@link FeedbackListener} and firing listener events when
 * triggered by the {@link NavigationView}.
 */
class NavigationViewEventDispatcher {

  private FeedbackListener feedbackListener;
  private NavigationListener navigationListener;
  private RouteListener routeListener;
  private BottomSheetCallback bottomSheetCallback;

  void setFeedbackListener(@Nullable FeedbackListener feedbackListener) {
    this.feedbackListener = feedbackListener;
  }

  void setNavigationListener(@Nullable NavigationListener navigationListener) {
    this.navigationListener = navigationListener;
  }

  void setRouteListener(@Nullable RouteListener routeListener) {
    this.routeListener = routeListener;
  }

  void setBottomSheetCallback(@Nullable BottomSheetCallback bottomSheetCallback) {
    this.bottomSheetCallback = bottomSheetCallback;
  }

  /*
   * Feedback listeners
   */

  void onFeedbackOpened() {
    if (feedbackListener != null) {
      feedbackListener.onFeedbackOpened();
    }
  }

  void onFeedbackCancelled() {
    if (feedbackListener != null) {
      feedbackListener.onFeedbackCancelled();
    }
  }

  void onFeedbackSent(FeedbackItem feedbackItem) {
    if (feedbackListener != null) {
      feedbackListener.onFeedbackSent(feedbackItem);
    }
  }

  /*
   * Navigation listeners
   */

  void onNavigationFinished() {
    if (navigationListener != null) {
      navigationListener.onNavigationFinished();
    }
  }

  void onCancelNavigation() {
    if (navigationListener != null) {
      navigationListener.onCancelNavigation();
    }
  }

  void onNavigationRunning() {
    if (navigationListener != null) {
      navigationListener.onNavigationRunning();
    }
  }

  /*
   * Route listeners
   */

  boolean allowRerouteFrom(Point point) {
    return routeListener == null || routeListener.allowRerouteFrom(point);
  }

  void onOffRoute(Point point) {
    if (routeListener != null) {
      routeListener.onOffRoute(point);
    }
  }

  void onRerouteAlong(DirectionsRoute directionsRoute) {
    if (routeListener != null) {
      routeListener.onRerouteAlong(directionsRoute);
    }
  }

  void onFailedReroute(String errorMessage) {
    if (routeListener != null) {
      routeListener.onFailedReroute(errorMessage);
    }
  }

  /*
   * BottomSheetCallbacks
   */

  void onBottomSheetStateChanged(View bottomSheet, int newState) {
    if (bottomSheetCallback != null) {
      bottomSheetCallback.onStateChanged(bottomSheet, newState);
    }
  }
}
