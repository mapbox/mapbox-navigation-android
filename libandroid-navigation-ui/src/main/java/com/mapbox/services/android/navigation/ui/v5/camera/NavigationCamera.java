package com.mapbox.services.android.navigation.ui.v5.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.location.Location;
import android.support.annotation.NonNull;
import android.view.View;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.camera.Camera;
import com.mapbox.services.android.navigation.v5.navigation.camera.RouteInformation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

/**
 * Updates the map camera while navigating.
 * <p>
 * This class listens to the progress of {@link MapboxNavigation} and moves
 * the {@link MapboxMap} camera based on the location updates.
 *
 * @since 0.6.0
 */
public class NavigationCamera implements ProgressChangeListener {

  private MapboxMap mapboxMap;
  private MapboxNavigation navigation;
  private CameraPosition currentCameraPosition;
  private double targetDistance;
  private boolean trackingEnabled = true;
  private Configuration configuration;

  /**
   * Creates an instance of {@link NavigationCamera}.
   *
   * @param view       for determining percentage of total screen
   * @param mapboxMap  for moving the camera
   * @param navigation for listening to location updates
   * @since 0.6.0
   */
  public NavigationCamera(@NonNull View view, @NonNull MapboxMap mapboxMap,
                          @NonNull MapboxNavigation navigation) {
    this.mapboxMap = mapboxMap;
    this.navigation = navigation;
    initialize(view);
  }

  /**
   * Called when beginning navigation with a route.
   * <p>
   * Creates a {@link CameraPosition} based on the {@link DirectionsRoute}.
   * If the route is null, the {@link ProgressChangeListener} is still added so future updates aren't ignored.
   *
   * @param route used to create the camera position
   * @since 0.6.0
   */
  public void start(DirectionsRoute route) {
    if (route != null) {
      currentCameraPosition = buildCameraPositionFromRoute(route);
      animateCameraToPosition(currentCameraPosition);
    } else {
      navigation.addProgressChangeListener(NavigationCamera.this);
    }
  }

  /**
   * Called during rotation.
   * The camera should resume from the last location update, not the beginning of the route.
   * <p>
   * Creates a {@link CameraPosition} based on the {@link Location}.
   * If the route is null, the {@link ProgressChangeListener} is still added so future updates aren't ignored.
   *
   * @param location used to create the camera position
   * @since 0.6.0
   */
  public void resume(Location location) {
    if (location != null) {
      currentCameraPosition = buildCameraPositionFromLocation(location, null);
      animateCameraToPosition(currentCameraPosition);
    } else {
      navigation.addProgressChangeListener(NavigationCamera.this);
    }
  }

  /**
   * Used to update the camera position.
   * <p>
   * {@link Location} is also stored in case the user scrolls the map and the camera
   * will eventually need to return to that last location update.
   *
   * @param location      used to update the camera position
   * @param routeProgress ignored in this scenario
   * @since 0.6.0
   */
  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    if (location.getLongitude() != 0 && location.getLatitude() != 0) {
      easeCameraToLocation(location, routeProgress);
    }
  }

  /**
   * Setter for whether or not the camera should follow the location.
   *
   * @param trackingEnabled true if should track, false if should not
   * @since 0.6.0
   */
  public void setCameraTrackingLocation(boolean trackingEnabled) {
    this.trackingEnabled = trackingEnabled;
  }

  /**
   * Getter for current state of tracking.
   *
   * @return true if tracking, false if not
   * @since 0.6.0
   */
  public boolean isTrackingEnabled() {
    return trackingEnabled;
  }

  /**
   * Enables tracking and moves the camera to the last known location update
   * from the {@link ProgressChangeListener}.
   *
   * @since 0.6.0
   */
  public void resetCameraPosition() {
    this.trackingEnabled = true;
    if (currentCameraPosition != null) {
      mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(currentCameraPosition), 750, true);
    }
  }

  /**
   * Will animate the {@link MapboxMap} to the given {@link CameraPosition}
   * with a 2 second duration.
   * <p>
   * If a user interacts with the {@link MapboxMap} while the animation is in progress,
   * the animation will be cancelled.  So it's important to add the {@link ProgressChangeListener}
   * in both onCancel() and onFinish() scenarios.
   *
   * @param position to which the camera should animate
   */
  private void animateCameraToPosition(CameraPosition position) {
    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 2000,
      new MapboxMap.CancelableCallback() {
        @Override
        public void onCancel() {
          navigation.addProgressChangeListener(NavigationCamera.this);
        }

        @Override
        public void onFinish() {
          navigation.addProgressChangeListener(NavigationCamera.this);
        }
      });
  }

  private void easeCameraToLocation(Location location, RouteProgress routeProgress) {
    currentCameraPosition = buildCameraPositionFromLocation(location, routeProgress);
    if (trackingEnabled) {
      mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(currentCameraPosition), 1000, false);
    }
  }

  /**
   * Creates a camera position based on the given route.
   * <p>
   * From the {@link DirectionsRoute}, an initial bearing and target position are created.
   * Then using a preset tilt and zoom (based on screen orientation), a {@link CameraPosition} is built.
   *
   * @param route used to build the camera position
   * @return camera position to be animated to
   */
  @NonNull
  private CameraPosition buildCameraPositionFromRoute(DirectionsRoute route) {
    RouteInformation routeInformation = RouteInformation.create(configuration, targetDistance,
            route, null, null);
    return buildCameraPositionFromRouteInformation(routeInformation);
  }

  /**
   * Creates a camera position based on the given location.
   * <p>
   * From the {@link Location}, a target position is created.
   * Then using a preset tilt and zoom (based on screen orientation), a {@link CameraPosition} is built.
   *
   * @param location used to build the camera position
   * @return camera position to be animated to
   */
  @NonNull
  private CameraPosition buildCameraPositionFromLocation(Location location, RouteProgress routeProgress) {
    RouteInformation routeInformation = RouteInformation.create(configuration, targetDistance,
            null, location, routeProgress);
    return buildCameraPositionFromRouteInformation(routeInformation);
  }

  private CameraPosition buildCameraPositionFromRouteInformation(RouteInformation routeInformation) {
    Camera cameraEngine = navigation.getCameraEngine();
    double tilt = cameraEngine.tilt(routeInformation);
    double zoom = cameraEngine.zoom(routeInformation);
    Point targetPoint = cameraEngine.target(routeInformation);
    LatLng target = new LatLng(
            targetPoint.latitude(),
            targetPoint.longitude()
    );
    double initialBearing = cameraEngine.bearing(routeInformation);

    return new CameraPosition.Builder()
            .tilt(tilt)
            .zoom(zoom)
            .target(target)
            .bearing(initialBearing)
            .build();
  }

  /**
   * Initializes both the target distance and zoom level for the camera.
   *
   * @param view used for setting target distance / zoom level
   */
  private void initialize(View view) {
    initializeTargetDistance(view);
    initializeScreenOrientation(view.getContext());
  }

  /**
   * Defines the camera target distance given the percentage of the
   * total phone screen the view uses.
   * <p>
   * If the view takes up a smaller portion of the screen, the target distance needs
   * to be adjusted to accommodate.
   *
   * @param view used for calculating target distance
   */
  private void initializeTargetDistance(View view) {
    double viewHeight = (double) view.getHeight();
    double screenHeight = (double) view.getContext().getResources().getDisplayMetrics().heightPixels;
    targetDistance = (viewHeight / screenHeight) * 100;
  }

  private void initializeScreenOrientation(Context context) {
    configuration = context.getResources().getConfiguration();
  }
}
