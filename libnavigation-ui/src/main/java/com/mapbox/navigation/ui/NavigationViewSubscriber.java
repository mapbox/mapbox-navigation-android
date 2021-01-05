package com.mapbox.navigation.ui;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

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

    navigationViewModel.retrieveOnSpeedLimit().observe(lifecycleOwner, navigationPresenter::onSpeedLimitAvailable);

    navigationViewModel.retrieveFeedbackFlowStatus().observe(lifecycleOwner, feedbackFlowStatus -> {
      if (feedbackFlowStatus != null
              && (feedbackFlowStatus == NavigationViewModel.FEEDBACK_FLOW_SENT
              || feedbackFlowStatus == NavigationViewModel.FEEDBACK_FLOW_CANCEL)) {
        navigationPresenter.onFeedbackFlowStatusChanged(feedbackFlowStatus);
      }
    });

    navigationViewModel
            .retrieveOnFinalDestinationArrival()
            .observe(lifecycleOwner, shouldShowFeedbackDetailsFragment -> {
              if (shouldShowFeedbackDetailsFragment != null && shouldShowFeedbackDetailsFragment) {
                navigationViewModel.retrieveOnFinalDestinationArrival().removeObservers(lifecycleOwner);
                navigationPresenter.onFinalDestinationArrival(
                        navigationViewModel.enableDetailedFeedbackFlowAfterTbt(),
                        navigationViewModel.enableArrivalExperienceFeedback()
                );
              }
            });
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  void unsubscribe() {
    navigationViewModel.retrieveRoute().removeObservers(lifecycleOwner);
    navigationViewModel.retrieveDestination().removeObservers(lifecycleOwner);
    navigationViewModel.retrieveNavigationLocation().removeObservers(lifecycleOwner);
    navigationViewModel.retrieveShouldRecordScreenshot().removeObservers(lifecycleOwner);
    navigationViewModel.retrieveFeedbackFlowStatus().removeObservers(lifecycleOwner);
    navigationViewModel.retrieveOnFinalDestinationArrival().removeObservers(lifecycleOwner);
  }
}
