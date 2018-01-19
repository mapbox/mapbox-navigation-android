package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.navigation.metrics.NavigationMetricListeners;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.route.FasterRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

class NavigationEventDispatcher {

  private List<NavigationEventListener> navigationEventListeners;
  private List<MilestoneEventListener> milestoneEventListeners;
  private List<ProgressChangeListener> progressChangeListeners;
  private List<OffRouteListener> offRouteListeners;
  private List<FasterRouteListener> fasterRouteListeners;
  private NavigationMetricListeners.EventListeners metricEventListeners;
  private NavigationMetricListeners.ArrivalListener metricArrivalListener;

  NavigationEventDispatcher() {
    navigationEventListeners = new ArrayList<>();
    milestoneEventListeners = new ArrayList<>();
    progressChangeListeners = new ArrayList<>();
    offRouteListeners = new ArrayList<>();
    fasterRouteListeners = new ArrayList<>();
  }

  void addMilestoneEventListener(@NonNull MilestoneEventListener milestoneEventListener) {
    if (milestoneEventListeners.contains(milestoneEventListener)) {
      Timber.w("The specified MilestoneEventListener has already been added to the stack.");
      return;
    }
    milestoneEventListeners.add(milestoneEventListener);
  }

  void removeMilestoneEventListener(@Nullable MilestoneEventListener milestoneEventListener) {
    if (milestoneEventListener == null) {
      milestoneEventListeners.clear();
    } else if (!milestoneEventListeners.contains(milestoneEventListener)) {
      Timber.w("The specified MilestoneEventListener isn't found in stack, therefore, cannot be removed.");
    } else {
      milestoneEventListeners.remove(milestoneEventListener);
    }
  }

  void addProgressChangeListener(@NonNull ProgressChangeListener progressChangeListener) {
    if (progressChangeListeners.contains(progressChangeListener)) {
      Timber.w("The specified ProgressChangeListener has already been added to the stack.");
      return;
    }
    progressChangeListeners.add(progressChangeListener);
  }

  void removeProgressChangeListener(@Nullable ProgressChangeListener progressChangeListener) {
    if (progressChangeListener == null) {
      progressChangeListeners.clear();
    } else if (!progressChangeListeners.contains(progressChangeListener)) {
      Timber.w("The specified ProgressChangeListener isn't found in stack, therefore, cannot be removed.");
    } else {
      progressChangeListeners.remove(progressChangeListener);
    }
  }

  void addOffRouteListener(@NonNull OffRouteListener offRouteListener) {
    if (offRouteListeners.contains(offRouteListener)) {
      Timber.w("The specified OffRouteListener has already been added to the stack.");
      return;
    }
    offRouteListeners.add(offRouteListener);
  }

  void removeOffRouteListener(@Nullable OffRouteListener offRouteListener) {
    if (offRouteListener == null) {
      offRouteListeners.clear();
    } else if (!offRouteListeners.contains(offRouteListener)) {
      Timber.w("The specified OffRouteListener isn't found in stack, therefore, cannot be removed.");
    } else {
      offRouteListeners.remove(offRouteListener);
    }
  }

  void addNavigationEventListener(@NonNull NavigationEventListener navigationEventListener) {
    if (navigationEventListeners.contains(navigationEventListener)) {
      Timber.w("The specified NavigationEventListener has already been added to the stack.");
      return;
    }
    this.navigationEventListeners.add(navigationEventListener);
  }

  void removeNavigationEventListener(@Nullable NavigationEventListener navigationEventListener) {
    if (navigationEventListener == null) {
      navigationEventListeners.clear();
    } else if (!navigationEventListeners.contains(navigationEventListener)) {
      Timber.w("The specified NavigationEventListener isn't found in stack, therefore, cannot be removed.");
    } else {
      navigationEventListeners.remove(navigationEventListener);
    }
  }

  void addFasterRouteListener(@NonNull FasterRouteListener fasterRouteListener) {
    if (fasterRouteListeners.contains(fasterRouteListener)) {
      Timber.w("The specified FasterRouteListener has already been added to the stack.");
      return;
    }
    fasterRouteListeners.add(fasterRouteListener);
  }

  void removeFasterRouteListener(@Nullable FasterRouteListener fasterRouteListener) {
    if (fasterRouteListener == null) {
      fasterRouteListeners.clear();
    } else if (!fasterRouteListeners.contains(fasterRouteListener)) {
      Timber.w("The specified FasterRouteListener isn't found in stack, therefore, cannot be removed.");
    } else {
      fasterRouteListeners.remove(fasterRouteListener);
    }
  }

  void onMilestoneEvent(RouteProgress routeProgress, String instruction, Milestone milestone) {
    for (MilestoneEventListener milestoneEventListener : milestoneEventListeners) {
      milestoneEventListener.onMilestoneEvent(routeProgress, instruction, milestone);
    }
  }

  void onProgressChange(Location location, RouteProgress routeProgress) {
    if (metricEventListeners != null) {
      // Update RouteProgress
      metricEventListeners.onRouteProgressUpdate(routeProgress);

      // Check if user has arrived and notify metric listener if so
      if (RouteUtils.isArrivalEvent(routeProgress) && metricArrivalListener != null) {
        metricArrivalListener.onArrival(location, routeProgress);
        metricArrivalListener = null;

        // If a this is the last leg, navigation is ending - remove listeners
        if (RouteUtils.isLastLeg(routeProgress)) {
          // Remove off route listeners
          removeOffRouteListener(null);
          // Remove metric listener
          metricEventListeners = null;
        }
      }
    }

    for (ProgressChangeListener progressChangeListener : progressChangeListeners) {
      progressChangeListener.onProgressChange(location, routeProgress);
    }
  }

  void onUserOffRoute(Location location) {
    for (OffRouteListener offRouteListener : offRouteListeners) {
      offRouteListener.userOffRoute(location);
    }
    // Send off route event to metric listener
    if (metricEventListeners != null) {
      metricEventListeners.onOffRouteEvent(location);
    }
  }

  void onNavigationEvent(boolean isRunning) {
    for (NavigationEventListener navigationEventListener : navigationEventListeners) {
      navigationEventListener.onRunning(isRunning);
    }
  }

  void onFasterRouteEvent(DirectionsRoute directionsRoute) {
    for (FasterRouteListener fasterRouteListener : fasterRouteListeners) {
      fasterRouteListener.fasterRouteFound(directionsRoute);
    }
  }

  void addMetricEventListeners(NavigationMetricListeners.EventListeners eventListeners) {
    this.metricEventListeners = eventListeners;
  }

  void addMetricArrivalListener(NavigationMetricListeners.ArrivalListener arrivalListener) {
    this.metricArrivalListener = arrivalListener;
  }
}
