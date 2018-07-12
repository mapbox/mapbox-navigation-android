package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.route.RouteFetcher;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.buildInstructionString;

class RouteProcessorThreadListener implements RouteProcessorBackgroundThread.Listener {

  private final NavigationEventDispatcher eventDispatcher;
  private final RouteFetcher routeFetcher;
  private final NavigationNotificationProvider notificationProvider;

  RouteProcessorThreadListener(NavigationEventDispatcher eventDispatcher, RouteFetcher routeFetcher,
                               NavigationNotificationProvider notificationProvider) {
    this.eventDispatcher = eventDispatcher;
    this.routeFetcher = routeFetcher;
    this.notificationProvider = notificationProvider;
  }

  /**
   * Corresponds to ProgressChangeListener object, updating the notification and passing information
   * to the navigation event dispatcher.
   */
  @Override
  public void onNewRouteProgress(Location location, RouteProgress routeProgress) {
    notificationProvider.updateNavigationNotification(routeProgress);
    eventDispatcher.onProgressChange(location, routeProgress);
  }

  /**
   * With each valid and successful rawLocation update, this will get called once the work on the
   * navigation engine thread has finished. Depending on whether or not a milestone gets triggered
   * or not, the navigation event dispatcher will be called to notify the developer.
   */
  @Override
  public void onMilestoneTrigger(List<Milestone> triggeredMilestones, RouteProgress routeProgress) {
    for (Milestone milestone : triggeredMilestones) {
      String instruction = buildInstructionString(routeProgress, milestone);
      eventDispatcher.onMilestoneEvent(routeProgress, instruction, milestone);
    }
  }

  /**
   * With each valid and successful rawLocation update, this callback gets invoked and depending on
   * whether or not the user is off route, the event dispatcher gets called.
   */
  @Override
  public void onUserOffRoute(Location location, boolean userOffRoute) {
    if (userOffRoute) {
      eventDispatcher.onUserOffRoute(location);
    }
  }

  /**
   * RouteListener from the {@link RouteProcessorBackgroundThread} - if fired with checkFasterRoute set
   * to true, a new {@link DirectionsRoute} should be fetched with {@link RouteFetcher}.
   *
   * @param location         to create a new origin
   * @param routeProgress    for various {@link com.mapbox.api.directions.v5.models.LegStep} data
   * @param checkFasterRoute true if should check for faster route, false otherwise
   */
  @Override
  public void onCheckFasterRoute(Location location, RouteProgress routeProgress, boolean checkFasterRoute) {
    if (checkFasterRoute) {
      routeFetcher.findRouteFromRouteProgress(location, routeProgress);
    }
  }
}
