package com.mapbox.services.android.navigation.ui.v5;


import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback;
import android.view.View;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackItem;
import com.mapbox.services.android.navigation.ui.v5.listeners.BannerInstructionsListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.FeedbackListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.InstructionListListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.SpeechAnnouncementListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;

/**
 * In charge of holding any {@link NavigationView} related listeners {@link NavigationListener},
 * {@link RouteListener}, or {@link FeedbackListener} and firing listener events when
 * triggered by the {@link NavigationView}.
 */
class NavigationViewEventDispatcher {

  private ProgressChangeListener progressChangeListener;
  private MilestoneEventListener milestoneEventListener;
  private FeedbackListener feedbackListener;
  private NavigationListener navigationListener;
  private RouteListener routeListener;
  private BottomSheetCallback bottomSheetCallback;
  private InstructionListListener instructionListListener;
  private SpeechAnnouncementListener speechAnnouncementListener;
  private BannerInstructionsListener bannerInstructionsListener;

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
    assignSpeechAnnouncementListener(navigationViewOptions.speechAnnouncementListener());
    assignBannerInstructionsListener(navigationViewOptions.bannerInstructionsListener());
  }

  void onDestroy(@Nullable MapboxNavigation navigation) {
    if (navigation != null) {
      removeProgressChangeListener(navigation);
      removeMilestoneEventListener(navigation);
    }
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

  SpeechAnnouncement onAnnouncement(SpeechAnnouncement announcement) {
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

  private void assignProgressChangeListener(NavigationViewOptions navigationViewOptions, MapboxNavigation navigation) {
    this.progressChangeListener = navigationViewOptions.progressChangeListener();
    if (progressChangeListener != null) {
      navigation.addProgressChangeListener(progressChangeListener);
    }
  }

  private void assignMilestoneEventListener(NavigationViewOptions navigationViewOptions, MapboxNavigation navigation) {
    this.milestoneEventListener = navigationViewOptions.milestoneEventListener();
    if (milestoneEventListener != null) {
      navigation.addMilestoneEventListener(milestoneEventListener);
    }
  }

  private void removeMilestoneEventListener(MapboxNavigation navigation) {
    if (milestoneEventListener != null) {
      navigation.removeMilestoneEventListener(milestoneEventListener);
    }
  }

  private void removeProgressChangeListener(MapboxNavigation navigation) {
    if (progressChangeListener != null) {
      navigation.removeProgressChangeListener(progressChangeListener);
    }
  }
}
