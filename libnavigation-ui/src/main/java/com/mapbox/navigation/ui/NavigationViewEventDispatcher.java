package com.mapbox.navigation.ui;

import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.geojson.Point;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.ui.feedback.FeedbackItem;
import com.mapbox.navigation.ui.listeners.BannerInstructionsListener;
import com.mapbox.navigation.ui.listeners.FeedbackListener;
import com.mapbox.navigation.ui.listeners.InstructionListListener;
import com.mapbox.navigation.ui.listeners.NavigationListener;
import com.mapbox.navigation.ui.listeners.RouteListener;
import com.mapbox.navigation.ui.listeners.SpeechAnnouncementListener;

/**
 * In charge of holding any {@link NavigationView} related listeners {@link NavigationListener},
 * {@link RouteListener}, or {@link FeedbackListener} and firing listener events when
 * triggered by the {@link NavigationView}.
 */
class NavigationViewEventDispatcher {

  private RouteProgressObserver routeProgressObserver;
  private LocationObserver locationObserver;
  private FeedbackListener feedbackListener;
  private NavigationListener navigationListener;
  private RouteListener routeListener;
  private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback;
  private InstructionListListener instructionListListener;
  private SpeechAnnouncementListener speechAnnouncementListener;
  private BannerInstructionsListener bannerInstructionsListener;

  /**
   * Initializes the listeners in the dispatcher, as well as the listeners in the {@link MapboxNavigation}
   *
   * @param navigationViewOptions that contains all listeners to attach
   */
  void initializeListeners(NavigationViewOptions navigationViewOptions, NavigationViewModel navigationViewModel) {
    assignFeedbackListener(navigationViewOptions.feedbackListener());
    assignNavigationListener(navigationViewOptions.navigationListener(), navigationViewModel);
    assignRouteListener(navigationViewOptions.routeListener());
    assignBottomSheetCallback(navigationViewOptions.bottomSheetCallback());
    MapboxNavigation navigation = navigationViewModel.retrieveNavigation();
    assignRouteProgressChangeObserver(navigationViewOptions, navigation);
    assignLocationObserver(navigationViewOptions, navigation);
    assignInstructionListListener(navigationViewOptions.instructionListListener());
    assignSpeechAnnouncementListener(navigationViewOptions.speechAnnouncementListener());
    assignBannerInstructionsListener(navigationViewOptions.bannerInstructionsListener());
  }

  void onDestroy(@Nullable MapboxNavigation navigation) {
    if (navigation != null) {
      removeRouteProgressChangeObserver(navigation);
      removeLocationObserver(navigation);
    }
  }

  void assignFeedbackListener(@Nullable FeedbackListener feedbackListener) {
    this.feedbackListener = feedbackListener;
  }

  void assignNavigationListener(@Nullable NavigationListener navigationListener,
                                NavigationViewModel navigationViewModel) {
    this.navigationListener = navigationListener;
    if (navigationListener != null && navigationViewModel.isRunning()) {
      navigationListener.onNavigationRunning();
    }
  }

  void assignRouteListener(@Nullable RouteListener routeListener) {
    this.routeListener = routeListener;
  }

  void assignBottomSheetCallback(@Nullable BottomSheetBehavior.BottomSheetCallback bottomSheetCallback) {
    this.bottomSheetCallback = bottomSheetCallback;
  }

  void assignInstructionListListener(@Nullable InstructionListListener instructionListListener) {
    this.instructionListListener = instructionListListener;
  }

  void assignSpeechAnnouncementListener(@Nullable SpeechAnnouncementListener speechAnnouncementListener) {
    this.speechAnnouncementListener = speechAnnouncementListener;
  }

  void assignBannerInstructionsListener(@Nullable BannerInstructionsListener bannerInstructionsListener) {
    this.bannerInstructionsListener = bannerInstructionsListener;
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

  VoiceInstructions onAnnouncement(VoiceInstructions announcement) {
    if (speechAnnouncementListener != null) {
      return speechAnnouncementListener.willVoice(announcement);
    }
    return announcement;
  }

  BannerInstructions onBannerDisplay(BannerInstructions instructions) {
    if (bannerInstructionsListener != null) {
      return bannerInstructionsListener.willDisplay(instructions);
    }
    return instructions;
  }

  private void assignRouteProgressChangeObserver(NavigationViewOptions navigationViewOptions, MapboxNavigation navigation) {
    this.routeProgressObserver = navigationViewOptions.routeProgressObserver();
    if (routeProgressObserver != null) {
      navigation.registerRouteProgressObserver(routeProgressObserver);
    }
  }

  private void assignLocationObserver(NavigationViewOptions navigationViewOptions, MapboxNavigation navigation) {
    this.locationObserver = navigationViewOptions.locationObserver();
    if (locationObserver != null) {
      navigation.registerLocationObserver(locationObserver);
    }
  }

  private void removeRouteProgressChangeObserver(MapboxNavigation navigation) {
    if (routeProgressObserver != null) {
      navigation.unregisterRouteProgressObserver(routeProgressObserver);
    }
  }

  private void removeLocationObserver(MapboxNavigation navigation) {
    if (locationObserver != null) {
      navigation.unregisterLocationObserver(locationObserver);
    }
  }
}
