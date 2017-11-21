package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.services.android.navigation.v5.navigation.metrics.NavigationMetricListener;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.METERS_REMAINING_TILL_ARRIVAL;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_DEPART;

class NavigationEventDispatcher {

  private List<NavigationEventListener> navigationEventListeners;
  private List<MilestoneEventListener> milestoneEventListeners;
  private List<ProgressChangeListener> progressChangeListeners;
  private List<OffRouteListener> offRouteListeners;
  private NavigationMetricListener navigationMetricListener;

  NavigationEventDispatcher() {
    navigationEventListeners = new ArrayList<>();
    milestoneEventListeners = new ArrayList<>();
    progressChangeListeners = new ArrayList<>();
    offRouteListeners = new ArrayList<>();
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

  void setNavigationMetricListener(NavigationMetricListener navigationMetricListener) {
    this.navigationMetricListener = navigationMetricListener;
  }

  void onMilestoneEvent(RouteProgress routeProgress, String instruction, int identifier) {
    for (MilestoneEventListener milestoneEventListener : milestoneEventListeners) {
      milestoneEventListener.onMilestoneEvent(routeProgress, instruction, identifier);
    }
  }

  void onProgressChange(Location location, RouteProgress routeProgress) {
    if (navigationMetricListener != null) {
      // Update RouteProgress
      navigationMetricListener.onRouteProgressUpdate(routeProgress);
      // Check if user has departed and notify metric listener if so
      boolean isDepartureEvent = routeProgress.currentLegProgress().currentStep().maneuver() != null
        && routeProgress.currentLegProgress().currentStep().maneuver().type().contains(STEP_MANEUVER_TYPE_DEPART);
      if (isDepartureEvent) {
        navigationMetricListener.onDeparture(location, routeProgress);
      }
      // Check if user has arrived and notify metric listener if so
      boolean isArrivalEvent = routeProgress.distanceRemaining() <= METERS_REMAINING_TILL_ARRIVAL;
      if (isArrivalEvent) {
        navigationMetricListener.onArrival(location, routeProgress);
        // Remove off route listeners
        removeOffRouteListener(null);
        // Remove metric listener
        navigationMetricListener = null;
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
    if (navigationMetricListener != null) {
      navigationMetricListener.onOffRouteEvent(location);
    }
  }

  void onNavigationEvent(boolean isRunning) {
    for (NavigationEventListener navigationEventListener : navigationEventListeners) {
      navigationEventListener.onRunning(isRunning);
    }
  }
}
