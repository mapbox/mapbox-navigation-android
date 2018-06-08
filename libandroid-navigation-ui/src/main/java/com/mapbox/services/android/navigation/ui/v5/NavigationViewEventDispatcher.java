package com.mapbox.services.android.navigation.ui.v5;


import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback;
import android.view.View;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackItem;
import com.mapbox.services.android.navigation.ui.v5.listeners.FeedbackListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.InstructionListListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

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
  private InstructionListListener instructionListListener;

  /**
   * Initializes the listeners in the dispatcher, as well as the listeners in the {@link MapboxNavigation}
   *
   * @param navigationViewOptions that contains all listeners to attach
   */
  void initializeListeners(NavigationViewOptions navigationViewOptions, MapboxNavigation navigation) {
    assignFeedbackListener(navigationViewOptions.feedbackListener());
    assignNavigationListener(navigationViewOptions.navigationListener());
    assignRouteListener(navigationViewOptions.routeListener());
    assignBottomSheetCallback(navigationViewOptions.bottomSheetCallback());
    assignProgressChangeListener(navigationViewOptions, navigation);
    assignMilestoneEventListener(navigationViewOptions, navigation);
    assignInstructionListListener(navigationViewOptions.instructionListListener());
  }

  void assignFeedbackListener(@Nullable FeedbackListener feedbackListener) {
    this.feedbackListener = feedbackListener;
  }

  void assignNavigationListener(@Nullable NavigationListener navigationListener) {
    this.navigationListener = navigationListener;
  }

  void assignRouteListener(@Nullable RouteListener routeListener) {
    this.routeListener = routeListener;
  }

  void assignBottomSheetCallback(@Nullable BottomSheetCallback bottomSheetCallback) {
    this.bottomSheetCallback = bottomSheetCallback;
  }

  void assignInstructionListListener(@Nullable InstructionListListener instructionListListener) {
    this.instructionListListener = instructionListListener;
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

  void onArrival() {
    if (routeListener != null) {
      routeListener.onArrival();
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

  void onInstructionListVisibilityChanged(boolean shown) {
    if (instructionListListener != null) {
      instructionListListener.onInstructionListVisibilityChanged(shown);
    }
  }

  private void assignProgressChangeListener(NavigationViewOptions navigationViewOptions, MapboxNavigation navigation) {
    if (navigationViewOptions.progressChangeListener() != null) {
      navigation.addProgressChangeListener(navigationViewOptions.progressChangeListener());
    }
  }

  private void assignMilestoneEventListener(NavigationViewOptions navigationViewOptions, MapboxNavigation navigation) {
    if (navigationViewOptions.milestoneEventListener() != null) {
      navigation.addMilestoneEventListener(navigationViewOptions.milestoneEventListener());
    }
  }
}
