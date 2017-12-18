package com.mapbox.services.android.navigation.ui.v5;


import android.support.annotation.NonNull;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackItem;

class NavigationViewEventDispatcher {

  private FeedbackListener feedbackListener;
  private NavigationListener navigationListener;
  private RouteListener routeListener;

  void setFeedbackListener(@NonNull FeedbackListener feedbackListener) {
    this.feedbackListener = feedbackListener;
  }

  void setNavigationListener(@NonNull NavigationListener navigationListener) {
    this.navigationListener = navigationListener;
  }

  void setRouteListener(@NonNull RouteListener routeListener) {
    this.routeListener = routeListener;
  }

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

  boolean allowRerouteFrom(Point point) {
    return routeListener == null ? true : routeListener.allowRerouteFrom(point);
  }

  void onOffRoute(Point point) {
    if (routeListener != null) {
      routeListener.onOffRoute(point);
    }
  }
}
