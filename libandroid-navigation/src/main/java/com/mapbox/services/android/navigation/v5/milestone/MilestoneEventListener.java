package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public interface MilestoneEventListener {

  void onMilestoneEvent(RouteProgress routeProgress, String instruction, Milestone milestone);

}
