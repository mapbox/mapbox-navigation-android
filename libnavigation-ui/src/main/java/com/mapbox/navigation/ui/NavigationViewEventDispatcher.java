package com.mapbox.navigation.ui;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.arrival.ArrivalObserver;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.ui.feedback.FeedbackItem;
import com.mapbox.navigation.ui.listeners.BannerInstructionsListener;
import com.mapbox.navigation.ui.listeners.FeedbackListener;
import com.mapbox.navigation.ui.listeners.InstructionListListener;
import com.mapbox.navigation.ui.listeners.NavigationListener;
import com.mapbox.navigation.ui.listeners.SpeechAnnouncementListener;

/**
 * In charge of holding any {@link NavigationView} related listeners {@link NavigationListener},
 * or {@link FeedbackListener} and firing listener events when triggered by the {@link NavigationView}.
 */
class NavigationViewEventDispatcher {

  @Nullable
  private RouteProgressObserver routeProgressObserver;
  @Nullable
  private LocationObserver locationObserver;
  @Nullable
  private FeedbackListener feedbackListener;
  @Nullable
  private NavigationListener navigationListener;
  @Nullable
  private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback;
  @Nullable
  private InstructionListListener instructionListListener;
  @Nullable
  private SpeechAnnouncementListener speechAnnouncementListener;
  @Nullable
  private BannerInstructionsListener bannerInstructionsListener;
  @Nullable
  private ArrivalObserver arrivalObserver;
  @Nullable
  private MapboxNavigation navigation;

  /**
   * Initializes the listeners in the dispatcher, as well as the listeners in the {@link MapboxNavigation}
   *
   * @param navigationViewOptions that contains all listeners to attach
   */
  void initializeListeners(
          @NonNull NavigationViewOptions navigationViewOptions,
          @NonNull NavigationViewModel navigationViewModel
  ) {
    assignFeedbackListener(navigationViewOptions.feedbackListener());
    assignNavigationListener(navigationViewOptions.navigationListener(), navigationViewModel);
    assignBottomSheetCallback(navigationViewOptions.bottomSheetCallback());
    assignInstructionListListener(navigationViewOptions.instructionListListener());
    assignSpeechAnnouncementListener(navigationViewOptions.speechAnnouncementListener());
    assignBannerInstructionsListener(navigationViewOptions.bannerInstructionsListener());

    navigation = navigationViewModel.retrieveNavigation();
    if (navigation != null) {
      assignRouteProgressChangeObserver(navigationViewOptions, navigation);
      assignLocationObserver(navigationViewOptions, navigation);
      assignArrivalObserver(navigationViewOptions, navigation);
    }
  }

  /**
   * Call when clearing up the navigation view and view model resources.
   */
  void onDestroy() {
    if (navigation != null) {
      removeRouteProgressChangeObserver(navigation);
      removeLocationObserver(navigation);
      removeArrivalObserver(navigation);
    }
  }

  void assignFeedbackListener(@Nullable FeedbackListener feedbackListener) {
    this.feedbackListener = feedbackListener;
  }

  void assignNavigationListener(@Nullable NavigationListener navigationListener,
                                @NonNull NavigationViewModel navigationViewModel) {
    this.navigationListener = navigationListener;
    if (navigationListener != null && navigationViewModel.isRunning()) {
      navigationListener.onNavigationRunning();
    }
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

  void onBottomSheetStateChanged(@NonNull View bottomSheet, int newState) {
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

  private void assignRouteProgressChangeObserver(
          @NonNull NavigationViewOptions navigationViewOptions,
          @NonNull MapboxNavigation navigation) {
    this.routeProgressObserver = navigationViewOptions.routeProgressObserver();
    if (routeProgressObserver != null) {
      navigation.registerRouteProgressObserver(routeProgressObserver);
    }
  }

  private void assignLocationObserver(@NonNull NavigationViewOptions navigationViewOptions,
                                      @NonNull MapboxNavigation navigation) {
    this.locationObserver = navigationViewOptions.locationObserver();
    if (locationObserver != null) {
      navigation.registerLocationObserver(locationObserver);
    }
  }

  private void assignArrivalObserver(@NonNull NavigationViewOptions navigationViewOptions,
                                     @NonNull MapboxNavigation navigation) {
    arrivalObserver = navigationViewOptions.arrivalObserver();
    if (arrivalObserver != null) {
      navigation.registerArrivalObserver(arrivalObserver);
    }
  }

  private void removeRouteProgressChangeObserver(@NonNull MapboxNavigation navigation) {
    if (routeProgressObserver != null) {
      navigation.unregisterRouteProgressObserver(routeProgressObserver);
    }
  }

  private void removeLocationObserver(@NonNull MapboxNavigation navigation) {
    if (locationObserver != null) {
      navigation.unregisterLocationObserver(locationObserver);
    }
  }

  private void removeArrivalObserver(@NonNull MapboxNavigation navigation) {
    if (arrivalObserver != null) {
      navigation.unregisterArrivalObserver(arrivalObserver);
    }
  }
}
