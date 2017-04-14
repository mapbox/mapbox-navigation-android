package com.mapbox.services.android.navigation.v5.listeners;

import com.mapbox.services.Experimental;
import com.mapbox.services.android.navigation.v5.RouteProgress;

/**
 * Listening in to the alertLevelChange is useful for correctly getting the timing of user notifications while the user
 * is traversing along the route. The listener's invoked only when the user's reached a specific point along the
 * current step they are on. The alert thresholds can be adjusted within the constants file while developing and are
 * based on time (in seconds) till the user reaches the next maneuver.
 * <p>
 * This is an experimental API. Experimental APIs are quickly evolving and might change or be removed in minor versions.
 *
 * @see <a href="https://www.mapbox.com/mapbox-java/#alertlevelchange">AlertLevelChange documentation</a>
 * @since 2.0.0
 */
@Experimental
public interface AlertLevelChangeListener {

  /**
   * Method's invoked when the alert level has changed during the current navigation session.
   *
   * @param alertLevel    One of the alert level constants found in {@link com.mapbox.services.android.Constants}.
   * @param routeProgress Provides a {@link RouteProgress} object which will contain information about the users
   *                      current position along the route.
   * @see <a href="https://www.mapbox.com/mapbox-java/#alertlevelchange">AlertLevelChange documentation</a>
   * @since 2.0.0
   */
  void onAlertLevelChange(int alertLevel, RouteProgress routeProgress);
}
