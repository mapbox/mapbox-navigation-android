package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.METERS_REMAINING_TILL_ARRIVAL;

class NavigationEventDispatcher {

  private List<NavigationEventListener> navigationEventListeners;
  private ProgressChangeListener internalProgressChangeListener;
  private List<MilestoneEventListener> milestoneEventListeners;
  private List<ProgressChangeListener> progressChangeListeners;
  private List<OffRouteListener> offRouteListeners;

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

  void setInternalProgressChangeListener(ProgressChangeListener internalProgressChangeListener) {
    this.internalProgressChangeListener = internalProgressChangeListener;
  }

  void removeInternalProgressChangeListener() {
    internalProgressChangeListener = null;
  }

  void onMilestoneEvent(RouteProgress routeProgress, String instruction, int identifier) {
    for (MilestoneEventListener milestoneEventListener : milestoneEventListeners) {
      milestoneEventListener.onMilestoneEvent(routeProgress, instruction, identifier);
    }
  }

  void onProgressChange(Location location, RouteProgress routeProgress) {
    // Check if user has arrived and notify internal progress change listener if so.
    if (internalProgressChangeListener != null
      && routeProgress.distanceRemaining() <= METERS_REMAINING_TILL_ARRIVAL) {
      internalProgressChangeListener.onProgressChange(location, routeProgress);
    }

    for (ProgressChangeListener progressChangeListener : progressChangeListeners) {
      progressChangeListener.onProgressChange(location, routeProgress);
    }
  }

  void onUserOffRoute(Location location) {
    for (OffRouteListener offRouteListener : offRouteListeners) {
      offRouteListener.userOffRoute(location);
    }
  }

  void onNavigationEvent(boolean isRunning) {
    for (NavigationEventListener navigationEventListener : navigationEventListeners) {
      navigationEventListener.onRunning(isRunning);
    }
  }
}
