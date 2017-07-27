package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.Nullable;

import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;

class NavigationEventDispatcher {

  List<MilestoneEventListener> milestoneEventListeners;

  NavigationEventDispatcher() {
    milestoneEventListeners = new ArrayList<>();
  }

  void addMilestoneEventListener(MilestoneEventListener milestoneEventListener) {
    milestoneEventListeners.add(milestoneEventListener);
  }

  void removeMilestoneEventListener(@Nullable MilestoneEventListener milestoneEventListener) {
    if (milestoneEventListener == null) {
      milestoneEventListeners.clear();
    } else {
      milestoneEventListeners.remove(milestoneEventListener);
    }
  }

  void onMilestoneEvent(RouteProgress routeProgress, String instruction, int identifier) {
    for (MilestoneEventListener milestoneEventListener : milestoneEventListeners) {
      milestoneEventListener.onMilestoneEvent(routeProgress, instruction, identifier);
    }
  }


}
