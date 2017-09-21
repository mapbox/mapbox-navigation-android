package com.mapbox.services.android.navigation.ui.v5.camera;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.SparseArray;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.models.Position;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

/**
 * Updates the map camera while navigating.
 * <p>
 * This class listens to the progress of {@link MapboxNavigation} and moves
 * the {@link MapboxMap} camera based on the location updates.
 *
 * @since 0.6.0
 */
public class NavigationCamera implements ProgressChangeListener {

  private static final int CAMERA_TILT = 45;
  private static int CAMERA_ZOOM = 17;

  private MapboxMap mapboxMap;
  private MapboxNavigation navigation;

  private Location location;
  private double targetDistance;
  private boolean trackingEnabled = true;

  /**
   * Creates an instance of {@link NavigationCamera}.
   *
   * @param context    for determining screen density
   * @param mapboxMap  for moving the camera
   * @param navigation for listening to location updates
   * @since 0.6.0
   */
  public NavigationCamera(@NonNull Context context, @NonNull MapboxMap mapboxMap,
                          @NonNull MapboxNavigation navigation) {
    this.mapboxMap = mapboxMap;
    this.navigation = navigation;
    initialize(context);
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
      CameraPosition cameraPosition = buildCameraPositionFromRoute(route);
      animateCameraToPosition(cameraPosition);
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
      CameraPosition position = buildCameraPositionFromLocation(location);
      animateCameraToPosition(position);
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
      this.location = location;
      easeCameraToLocation(location);
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
    if (location != null) {
      Position targetPosition = TurfMeasurement.destination(
        Position.fromCoordinates(location.getLongitude(), location.getLatitude()),
        targetDistance, location.getBearing(), TurfConstants.UNIT_METERS
      );

      LatLng target = new LatLng(
        targetPosition.getLatitude(),
        targetPosition.getLongitude()
      );

      CameraPosition cameraPosition = new CameraPosition.Builder()
        .tilt(CAMERA_TILT)
        .zoom(CAMERA_ZOOM)
        .target(target)
        .bearing(location.getBearing())
        .build();
      mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 750, true);
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

  private void easeCameraToLocation(Location location) {
    CameraPosition cameraPosition = buildCameraPositionFromLocation(location);

    if (trackingEnabled) {
      mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, false);
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
    LineString lineString = LineString.fromPolyline(route.getGeometry(), Constants.PRECISION_6);

    double initialBearing = TurfMeasurement.bearing(
      Position.fromLngLat(
        lineString.getCoordinates().get(0).getLongitude(), lineString.getCoordinates().get(0).getLatitude()
      ),
      Position.fromLngLat(
        lineString.getCoordinates().get(1).getLongitude(), lineString.getCoordinates().get(1).getLatitude()
      )
    );

    Position targetPosition = TurfMeasurement.destination(
      Position.fromCoordinates(
        lineString.getCoordinates().get(0).getLongitude(), lineString.getCoordinates().get(0).getLatitude()
      ),
      120, initialBearing, TurfConstants.UNIT_METERS
    );

    LatLng target = new LatLng(
      targetPosition.getLatitude(),
      targetPosition.getLongitude()
    );

    return new CameraPosition.Builder()
      .tilt(CAMERA_TILT)
      .zoom(CAMERA_ZOOM)
      .target(target)
      .bearing(initialBearing)
      .build();
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
  private CameraPosition buildCameraPositionFromLocation(Location location) {
    Position targetPosition = TurfMeasurement.destination(
      Position.fromCoordinates(location.getLongitude(), location.getLatitude()),
      targetDistance, location.getBearing(), TurfConstants.UNIT_METERS
    );

    LatLng target = new LatLng(
      targetPosition.getLatitude(),
      targetPosition.getLongitude()
    );

    return new CameraPosition.Builder()
      .tilt(CAMERA_TILT)
      .zoom(CAMERA_ZOOM)
      .target(target)
      .bearing(location.getBearing())
      .build();
  }

  /**
   * Initializes both the target distance and zoom level for the camera.
   *
   * @param context used for getting current orientation
   */
  private void initialize(Context context) {
    initializeTargetDistance(context);
    initializeScreenOrientation(context);
  }

  /**
   * Defines the camera target distance given the phone's screen density.
   * <p>
   * If the camera has a higher screen density, the target distance needs
   * to be increased to accommodate more pixels.
   *
   * @param context used for getting current orientation
   */
  private void initializeTargetDistance(Context context) {
    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
    ScreenDensityMap densityMap = new ScreenDensityMap();
    targetDistance = densityMap.getTargetDistance(metrics.densityDpi);
  }

  /**
   * Defines the camera zoom level given the screen orientation.
   *
   * @param context used for getting current orientation
   */
  private void initializeScreenOrientation(Context context) {
    CAMERA_ZOOM = new OrientationMap().get(context.getResources().getConfiguration().orientation);
  }

  /**
   * Holds the two different screen orientations
   * and their corresponding zoom levels.
   */
  private class OrientationMap extends SparseArray<Integer> {

    OrientationMap() {
      put(ORIENTATION_PORTRAIT, 17);
      put(ORIENTATION_LANDSCAPE, 16);
    }
  }
}
