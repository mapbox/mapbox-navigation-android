package com.mapbox.services.android.navigation.ui.v5.camera;

import android.animation.Animator;
import android.content.Context;
import android.content.res.Configuration;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.camera.Camera;
import com.mapbox.services.android.navigation.v5.navigation.camera.RouteInformation;
import com.mapbox.services.android.navigation.v5.navigation.camera.SimpleCamera;
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
public class NavigationCamera extends SimpleCamera implements ProgressChangeListener {

  private MapboxMap mapboxMap;
  private MapboxNavigation navigation;
  private RouteInformation currentRouteInformation;
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
      currentRouteInformation = buildRouteInformationFromRoute(route);
      animateCameraFromRoute(currentRouteInformation);
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
      currentRouteInformation = buildRouteInformationFromLocation(location, null);
      animateCameraFromLocation(currentRouteInformation);
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
      animateCameraToLocation(location, routeProgress);
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
    if (currentRouteInformation != null) {
      animateCameraFromLocation(currentRouteInformation);
    }
  }

  private void animateCameraToLocation(Location location, RouteProgress routeProgress) {
    currentRouteInformation = buildRouteInformationFromLocation(location, routeProgress);
    if (trackingEnabled) {
      animateCameraFromLocation(currentRouteInformation);
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
  private RouteInformation buildRouteInformationFromRoute(DirectionsRoute route) {
    return RouteInformation.create(configuration, targetDistance,
            route, null, null);
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
  private RouteInformation buildRouteInformationFromLocation(Location location, RouteProgress routeProgress) {
    return RouteInformation.create(configuration, targetDistance,
            null, location, routeProgress);
  }

  private void animateCameraFromLocation(RouteInformation routeInformation) {

    Camera cameraEngine = navigation.getCameraEngine();

    Point targetPoint = cameraEngine.target(routeInformation);
    LatLng target = new LatLng(
            targetPoint.latitude(),
            targetPoint.longitude()
    );
    LatLngAnimator latLngAnimator = new LatLngAnimator(target, 1000);
    latLngAnimator.setInterpolator(new LinearInterpolator());

    double bearing = cameraEngine.bearing(routeInformation);
    BearingAnimator bearingAnimator = new BearingAnimator(bearing, 1000);
    bearingAnimator.setInterpolator(new DecelerateInterpolator());

    float tilt = (float) cameraEngine.tilt(routeInformation);
    TiltAnimator tiltAnimator = new TiltAnimator(tilt, 1000);
    tiltAnimator.setInterpolator(new LinearInterpolator());

    double zoom = cameraEngine.zoom(routeInformation);
    ZoomAnimator zoomAnimator = new ZoomAnimator(zoom, 1000);
    zoomAnimator.setInterpolator(new LinearInterpolator());

    MapAnimator.builder(mapboxMap)
      .addLatLngAnimator(latLngAnimator)
      .addBearingAnimator(bearingAnimator)
      .addTiltAnimator(tiltAnimator)
      .addZoomAnimator(zoomAnimator)
      .build()
      .playTogether();
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
  private void animateCameraToPosition(CameraPosition position, MapboxMap.CancelableCallback callback) {
    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 2000, callback);
  }

  private void animateCameraFromRoute(RouteInformation routeInformation) {

    Camera cameraEngine = navigation.getCameraEngine();

    Point targetPoint = cameraEngine.target(routeInformation);
    LatLng targetLatLng = new LatLng(
      targetPoint.latitude(),
      targetPoint.longitude()
    );

    double zoom = cameraEngine.zoom(routeInformation);

    CameraPosition position = new CameraPosition.Builder()
      .target(targetLatLng)
      .zoom(zoom)
      .build();

    double bearing = cameraEngine.bearing(routeInformation);
    final BearingAnimator bearingAnimator = new BearingAnimator(bearing, 1500);
    bearingAnimator.setInterpolator(new FastOutSlowInInterpolator());

    float tilt = (float) cameraEngine.tilt(routeInformation);
    final TiltAnimator tiltAnimator = new TiltAnimator(tilt, 1500);
    tiltAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

    final LatLngAnimator latLngAnimator = new LatLngAnimator(targetLatLng, 1500);
    latLngAnimator.setInterpolator(new FastOutSlowInInterpolator());

    animateCameraToPosition(position, new MapboxMap.CancelableCallback() {
      @Override
      public void onCancel() {
        navigation.addProgressChangeListener(NavigationCamera.this);
      }

      @Override
      public void onFinish() {
        MapAnimator.builder(mapboxMap)
          .addBearingAnimator(bearingAnimator)
          .addTiltAnimator(tiltAnimator)
          .addLatLngAnimator(latLngAnimator)
          .build()
          .playTogether(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
              // No-op
            }

            @Override
            public void onAnimationEnd(Animator animation) {
              navigation.addProgressChangeListener(NavigationCamera.this);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
              navigation.addProgressChangeListener(NavigationCamera.this);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
              // No-op
            }
          });
      }
    });
  }

  /**
   * Initializes both the target distance and zoom level for the camera.
   *
   * @param view used for setting target distance / zoom level
   */
  private void initialize(View view) {
    initializeTargetDistance(view);
    initializeScreenOrientation(view.getContext());
    navigation.setCameraEngine(new DynamicCamera(mapboxMap));
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
    targetDistance = ((viewHeight / screenHeight) * 100) * 2;
  }

  private void initializeScreenOrientation(Context context) {
    configuration = context.getResources().getConfiguration();
  }
}
