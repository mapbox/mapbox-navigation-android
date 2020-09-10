package com.mapbox.navigation.ui;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import static com.mapbox.navigation.ui.feedback.FeedbackBottomSheet.FEEDBACK_FLOW_CANCEL;
import static com.mapbox.navigation.ui.feedback.FeedbackBottomSheet.FEEDBACK_FLOW_SENT;

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
    navigationViewModel.retrieveRoute().observe(lifecycleOwner, directionsRoute -> {
      if (directionsRoute != null) {
        navigationPresenter.onRouteUpdate(directionsRoute);
      }
    });

    navigationViewModel.retrieveDestination().observe(lifecycleOwner, point -> {
      if (point != null) {
        navigationPresenter.onDestinationUpdate(point);
      }
    });

    navigationViewModel.retrieveNavigationLocation().observe(lifecycleOwner, location -> {
      if (location != null) {
        navigationPresenter.onNavigationLocationUpdate(location);
      }
    });

    navigationViewModel.retrieveShouldRecordScreenshot().observe(lifecycleOwner, shouldRecordScreenshot -> {
      if (shouldRecordScreenshot != null && shouldRecordScreenshot) {
        navigationPresenter.onShouldRecordScreenshot();
      }
    });

    navigationViewModel.retrieveIsFeedbackSentSuccess().observe(lifecycleOwner, isFeedbackSentSuccess -> {
      if (isFeedbackSentSuccess != null
              && (isFeedbackSentSuccess == FEEDBACK_FLOW_SENT
              || isFeedbackSentSuccess == FEEDBACK_FLOW_CANCEL)) {
        navigationPresenter.onFeedbackFlowStatusChanged(isFeedbackSentSuccess);
      }
    });

    navigationViewModel.retrieveOnFinalDestinationArrival().observe(lifecycleOwner, shouldShowFeedbackDetailsFragment -> {
      if (shouldShowFeedbackDetailsFragment != null && shouldShowFeedbackDetailsFragment) {
        navigationPresenter.onFinalDestinationArrival();
        navigationViewModel.retrieveOnFinalDestinationArrival().removeObservers(lifecycleOwner);
      }
    });
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  void unsubscribe() {
    navigationViewModel.retrieveRoute().removeObservers(lifecycleOwner);
    navigationViewModel.retrieveDestination().removeObservers(lifecycleOwner);
    navigationViewModel.retrieveNavigationLocation().removeObservers(lifecycleOwner);
    navigationViewModel.retrieveShouldRecordScreenshot().removeObservers(lifecycleOwner);
    navigationViewModel.retrieveIsFeedbackSentSuccess().removeObservers(lifecycleOwner);
    navigationViewModel.retrieveOnFinalDestinationArrival().removeObservers(lifecycleOwner);
  }
}
