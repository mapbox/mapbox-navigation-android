package com.mapbox.services.android.navigation.ui.v5.listeners;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

/**
 * A listener that can be implemented and
 * added to {@link com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions} to
 * hook into route related events occurring in {@link com.mapbox.services.android.navigation.ui.v5.NavigationView}.
 */
public interface RouteListener {

  /**
   * Will trigger in an off route scenario.  Given the {@link Point} the user has gone
   * off route, this listener can return true or false.
   * <p>
   * Returning true will allow the SDK to proceed with the re-route process and fetch a new route
   * with this given off route {@link Point}.
   * <p>
   * Returning false will stop the re-route process and the user will continue without a
   * new route in the direction they are traveling.
   *
   * @param offRoutePoint the given point the user has gone off route
   * @return true if the reroute should be allowed, false if not
   * @since 0.8.0
   */
  boolean allowRerouteFrom(Point offRoutePoint);

  /**
   * Will triggered only if {@link RouteListener#allowRerouteFrom(Point)} returns true.
   * <p>
   * This serves as the official off route event and will continue the process to fetch a new route
   * with the given off route {@link Point}.
   *
   * @param offRoutePoint the given point the user has gone off route
   * @since 0.8.0
   */
  void onOffRoute(Point offRoutePoint);

  /**
   * Will trigger when a new {@link DirectionsRoute} has been retrieved post off route.
   * <p>
   * This is the new route the user will be following until another off route event is triggered.
   *
   * @param directionsRoute the new directions route
   * @since 0.8.0
   */
  void onRerouteAlong(DirectionsRoute directionsRoute);

  /**
   * Will trigger if the request for a new {@link DirectionsRoute} fails.
   *
   * @since 0.8.0
   */
  void onFailedReroute(String errorMessage);

  /**
   * Will trigger when a user has arrived at a given waypoint along a {@link DirectionsRoute}.
   * <p>
   * For example, if there are two {@link com.mapbox.api.directions.v5.models.LegStep}s, this callback
   * will be triggered twice, once for each waypoint.
   *
   * @since 0.14.0
   */
  void onArrival();
}
