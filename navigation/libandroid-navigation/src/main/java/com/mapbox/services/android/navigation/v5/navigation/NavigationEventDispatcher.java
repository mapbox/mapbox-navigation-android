package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.services.android.navigation.v5.listeners.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;

class NavigationEventDispatcher {

  private List<NavigationEventListener> navigationEventListeners;
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
    if (!milestoneEventListeners.contains(milestoneEventListener)) {
      milestoneEventListeners.add(milestoneEventListener);
    }
  }

  void removeMilestoneEventListener(@Nullable MilestoneEventListener milestoneEventListener) {
    if (milestoneEventListener == null) {
      milestoneEventListeners.clear();
    } else {
      milestoneEventListeners.remove(milestoneEventListener);
    }
  }

  void addProgressChangeListener(@NonNull ProgressChangeListener progressChangeListener) {
    if (!progressChangeListeners.contains(progressChangeListener)) {
      progressChangeListeners.add(progressChangeListener);
    }
  }

  void removeProgressChangeListener(@Nullable ProgressChangeListener progressChangeListener) {
    if (progressChangeListener == null) {
      progressChangeListeners.clear();
    } else {
      progressChangeListeners.remove(progressChangeListener);
    }
  }

  void addOffRouteListener(@NonNull OffRouteListener offRouteListener) {
    if (!offRouteListeners.contains(offRouteListener)) {
      offRouteListeners.add(offRouteListener);
    }
  }

  void removeOffRouteListener(@Nullable OffRouteListener offRouteListener) {
    if (offRouteListener == null) {
      offRouteListeners.clear();
    } else {
      offRouteListeners.remove(offRouteListener);
    }
  }

  void addNavigationEventListener(@NonNull NavigationEventListener navigationEventListener) {
    if (!this.navigationEventListeners.contains(navigationEventListener)) {
      this.navigationEventListeners.add(navigationEventListener);
    }
  }

  void removeNavigationEventListener(@Nullable NavigationEventListener navigationEventListener) {
    if (navigationEventListener == null) {
      navigationEventListeners.clear();
    } else {
      navigationEventListeners.remove(navigationEventListener);
    }
  }

  void onMilestoneEvent(RouteProgress routeProgress, String instruction, int identifier) {
    for (MilestoneEventListener milestoneEventListener : milestoneEventListeners) {
      milestoneEventListener.onMilestoneEvent(routeProgress, instruction, identifier);
    }
  }
}
