package com.mapbox.services.android.navigation.v5.navigation.camera;

import com.mapbox.geojson.Point;

import java.util.List;

/**
 * This class handles calculating all properties necessary to configure the camera position while
 * routing. The {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation} uses
 * a {@link SimpleCamera} by default. If you would like to customize the camera position, create a
 * concrete implementation of this class or subclass {@link SimpleCamera} and update
 * {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation#setCameraEngine(Camera)}.
 *
 * @since 0.10.0
 */
public abstract class Camera {

  /**
   * Direction that the camera is pointing in, in degrees clockwise from north.
   */
  public abstract double bearing(RouteInformation routeInformation);

  /**
   * The location that the camera is pointing at.
   */
  public abstract Point target(RouteInformation routeInformation);

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


  public abstract List<Point> overview(RouteInformation routeInformation);
}
