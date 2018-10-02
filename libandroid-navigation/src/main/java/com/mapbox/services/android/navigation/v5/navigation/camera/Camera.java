package com.mapbox.services.android.navigation.v5.navigation.camera;

import com.mapbox.geojson.Point;

import java.util.List;

/**
 * This class handles calculating camera's zoom and tilt properties while routing.
 * The {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation} uses
 * a {@link SimpleCamera} by default. If you would like to customize the camera properties, create a
 * concrete implementation of this class or subclass {@link SimpleCamera} and update
 * {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation#setCameraEngine(Camera)}.
 *
 * @since 0.10.0
 */
public abstract class Camera {

  /**
   * The angle, in degrees, of the camera angle from the nadir (directly facing the Earth).
   * See tilt(float) for details of restrictions on the range of values.
   */
  public abstract double tilt(RouteInformation routeInformation);

  /**
   * Zoom level near the center of the screen. See zoom(float) for the definition of the camera's
   * zoom level.
   */
  public abstract double zoom(RouteInformation routeInformation);

  /**
   * Return a list of route coordinates that should be visible when creating the route's overview.
   */
  public abstract List<Point> overview(RouteInformation routeInformation);
}
