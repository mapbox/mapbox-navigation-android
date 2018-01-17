package com.mapbox.services.android.navigation.v5.navigation.camera;


import com.mapbox.mapboxsdk.geometry.LatLng;

public abstract class Camera {

  /**
   * Direction that the camera is pointing in, in degrees clockwise from north.
   */
  public abstract double bearing(RouteInformation routeInformation);

  /**
   * The location that the camera is pointing at.
   */
  public abstract LatLng target(RouteInformation routeInformation);

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
}
