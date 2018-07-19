package com.mapbox.services.android.navigation.ui.v5.camera;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.location.Location;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.camera.Camera;
import com.mapbox.services.android.navigation.v5.navigation.camera.RouteInformation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;

/**
 * Updates the map camera while navigating.
 * <p>
 * This class listens to the progress of {@link MapboxNavigation} and moves
 * the {@link MapboxMap} camera based on the location updates.
 *
 * @since 0.6.0
 */
public class NavigationCamera implements LifecycleObserver {

  private static final long MAX_ANIMATION_DURATION_MS = 1500;
  private static final int ONE_POINT = 1;

  private MapboxMap mapboxMap;
  private MapboxNavigation navigation;
  private RouteInformation currentRouteInformation;
  private RouteProgress currentRouteProgress;
  private boolean trackingEnabled = true;
  private long locationUpdateTimestamp;
  private ProgressChangeListener progressChangeListener = new ProgressChangeListener() {
    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
      currentRouteProgress = routeProgress;
      if (trackingEnabled) {
        currentRouteInformation = buildRouteInformationFromLocation(location, routeProgress);
        animateCameraFromLocation(currentRouteInformation);
      }
    }
  };

  /**
   * Creates an instance of {@link NavigationCamera}.
   *
   * @param mapboxMap  for moving the camera
   * @param navigation for listening to location updates
   * @since 0.6.0
   */
  public NavigationCamera(@NonNull MapboxMap mapboxMap, @NonNull MapboxNavigation navigation) {
    this.mapboxMap = mapboxMap;
    this.navigation = navigation;
    initialize();
  }

  /**
   * Creates an instance of {@link NavigationCamera}.
   * <p>
   * The camera won't do anything until {@link NavigationCamera#addProgressChangeListener} is called.
   *
   * @param mapboxMap for moving the camera
   * @since 0.15.0
   */
  public NavigationCamera(@NonNull MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    mapboxMap.setMinZoomPreference(7d);
  }

  /**
   * Used for testing only.
   */
  NavigationCamera(MapboxMap mapboxMap, MapboxNavigation navigation, ProgressChangeListener progressChangeListener) {
    this.mapboxMap = mapboxMap;
    this.navigation = navigation;
    this.progressChangeListener = progressChangeListener;
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
      navigation.addProgressChangeListener(progressChangeListener);
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
    }
    navigation.addProgressChangeListener(progressChangeListener);
  }

  /**
   * Setter for whether or not the camera should follow the location.
   *
   * @param trackingEnabled true if should track, false if should not
   * @since 0.6.0
   */
  public void updateCameraTrackingLocation(boolean trackingEnabled) {
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
    trackingEnabled = true;
    if (currentRouteInformation != null) {
      if (navigation.getCameraEngine() instanceof DynamicCamera) {
        ((DynamicCamera) navigation.getCameraEngine()).forceResetZoomLevel();
      }
      animateCameraFromLocation(currentRouteInformation);
    }
  }

  public void showRouteOverview(int[] padding) {
    trackingEnabled = false;
    RouteInformation routeInformation = buildRouteInformationFromProgress(currentRouteProgress);
    animateCameraForRouteOverview(routeInformation, padding);
  }

  /**
   * Call in {@link FragmentActivity#onStart()} to properly add the {@link ProgressChangeListener}
   * for the camera and prevent any leaks or further updates.
   *
   * @since 0.15.0
   */
  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  public void onStart() {
    if (navigation != null) {
      navigation.addProgressChangeListener(progressChangeListener);
    }
  }

  /**
   * Call in {@link FragmentActivity#onStop()} to properly remove the {@link ProgressChangeListener}
   * for the camera and prevent any leaks or further updates.
   *
   * @since 0.15.0
   */
  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  public void onStop() {
    if (navigation != null) {
      navigation.removeProgressChangeListener(progressChangeListener);
    }
  }

  public void addProgressChangeListener(MapboxNavigation navigation) {
    this.navigation = navigation;
    navigation.setCameraEngine(new DynamicCamera(mapboxMap));
    navigation.addProgressChangeListener(progressChangeListener);
  }

  private void initialize() {
    mapboxMap.setMinZoomPreference(7d);
    navigation.setCameraEngine(new DynamicCamera(mapboxMap));
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
    return RouteInformation.create(route, null, null);
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
    return RouteInformation.create(null, location, routeProgress);
  }

  @NonNull
  private RouteInformation buildRouteInformationFromProgress(RouteProgress routeProgress) {
    if (routeProgress == null) {
      return RouteInformation.create(null, null, null);
    }
    return RouteInformation.create(routeProgress.directionsRoute(), null, null);
  }

  /**
   * Will animate the {@link MapboxMap} to the given {@link CameraPosition} with the given duration.
   *
   * @param position to which the camera should animate
   * @param callback that will fire if the animation is cancelled or finished
   */
  private void updateMapCameraPosition(CameraPosition position, MapboxMap.CancelableCallback callback) {
    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000, callback);
  }

  /**
   * Will ease the {@link MapboxMap} to the given {@link CameraPosition} with the given duration.
   *
   * @param position to which the camera should animate
   */
  private void easeMapCameraPosition(CameraPosition position) {
    mapboxMap.easeCamera(CameraUpdateFactory.newCameraPosition(position),
      obtainLocationUpdateDuration(), false, null);
  }

  /**
   * Creates an initial animation with the given {@link RouteInformation#route()}.
   * <p>
   * This is the first animation that fires prior to receiving progress updates.
   * <p>
   * If a user interacts with the {@link MapboxMap} while the animation is in progress,
   * the animation will be cancelled.  So it's important to add the {@link ProgressChangeListener}
   * in both onCancel() and onFinish() scenarios.
   *
   * @param routeInformation with route data
   */
  private void animateCameraFromRoute(RouteInformation routeInformation) {

    Camera cameraEngine = navigation.getCameraEngine();

    Point targetPoint = cameraEngine.target(routeInformation);
    LatLng targetLatLng = new LatLng(targetPoint.latitude(), targetPoint.longitude());
    double bearing = cameraEngine.bearing(routeInformation);
    double zoom = cameraEngine.zoom(routeInformation);

    CameraPosition position = new CameraPosition.Builder()
      .target(targetLatLng)
      .bearing(bearing)
      .zoom(zoom)
      .build();

    updateMapCameraPosition(position, new AddProgressListenerCancelableCallback(navigation, progressChangeListener));
  }

  private void animateCameraForRouteOverview(RouteInformation routeInformation, int[] padding) {
    Camera cameraEngine = navigation.getCameraEngine();
    List<Point> routePoints = cameraEngine.overview(routeInformation);
    boolean invalidPoints = routePoints.isEmpty();
    if (invalidPoints) {
      return;
    }
    animateMapboxMapForRouteOverview(padding, routePoints);
  }

  private void animateMapboxMapForRouteOverview(int[] padding, List<Point> routePoints) {
    if (routePoints.size() <= ONE_POINT) {
      return;
    }
    CameraUpdate resetUpdate = buildResetCameraUpdate();
    final CameraUpdate overviewUpdate = buildOverviewCameraUpdate(padding, routePoints);
    mapboxMap.animateCamera(resetUpdate, 150,
      new CameraOverviewCancelableCallback(overviewUpdate, mapboxMap)
    );
  }

  @NonNull
  private CameraUpdate buildResetCameraUpdate() {
    CameraPosition resetPosition = new CameraPosition.Builder().tilt(0).bearing(0).build();
    return CameraUpdateFactory.newCameraPosition(resetPosition);
  }

  @NonNull
  private CameraUpdate buildOverviewCameraUpdate(int[] padding, List<Point> routePoints) {
    final LatLngBounds routeBounds = convertRoutePointsToLatLngBounds(routePoints);
    return CameraUpdateFactory.newLatLngBounds(
      routeBounds, padding[0], padding[1], padding[2], padding[3]
    );
  }

  private LatLngBounds convertRoutePointsToLatLngBounds(List<Point> routePoints) {
    List<LatLng> latLngs = new ArrayList<>();
    for (Point routePoint : routePoints) {
      latLngs.add(new LatLng(routePoint.latitude(), routePoint.longitude()));
    }
    return new LatLngBounds.Builder()
      .includes(latLngs)
      .build();
  }

  /**
   * Creates an animation with the given {@link RouteInformation#location()}.
   * <p>
   * This animation that fires for new progress update.
   *
   * @param routeInformation with location data
   */
  private void animateCameraFromLocation(RouteInformation routeInformation) {

    Camera cameraEngine = navigation.getCameraEngine();

    Point targetPoint = cameraEngine.target(routeInformation);
    LatLng target = new LatLng(targetPoint.latitude(), targetPoint.longitude());
    double bearing = cameraEngine.bearing(routeInformation);
    float tilt = (float) cameraEngine.tilt(routeInformation);
    double zoom = cameraEngine.zoom(routeInformation);

    CameraPosition position = new CameraPosition.Builder()
      .target(target)
      .bearing(bearing)
      .tilt(tilt)
      .zoom(zoom)
      .build();

    easeMapCameraPosition(position);
  }

  private int obtainLocationUpdateDuration() {
    long previousUpdateTimeStamp = locationUpdateTimestamp;
    locationUpdateTimestamp = SystemClock.elapsedRealtime();
    long duration = locationUpdateTimestamp - previousUpdateTimeStamp;
    return (int) (duration < MAX_ANIMATION_DURATION_MS ? duration : MAX_ANIMATION_DURATION_MS);
  }
}
