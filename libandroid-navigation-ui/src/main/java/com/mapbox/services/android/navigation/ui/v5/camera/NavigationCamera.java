package com.mapbox.services.android.navigation.ui.v5.camera;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.location.Location;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.OnLocationCameraTransitionListener;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.camera.Camera;
import com.mapbox.services.android.navigation.v5.navigation.camera.RouteInformation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.MathUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import timber.log.Timber;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_MAX_CAMERA_ADJUSTMENT_ANIMATION_DURATION;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_MIN_CAMERA_TILT_ADJUSTMENT_ANIMATION_DURATION;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_MIN_CAMERA_ZOOM_ADJUSTMENT_ANIMATION_DURATION;

/**
 * Updates the map camera while navigating.
 * <p>
 * This class listens to the progress of {@link MapboxNavigation} and moves
 * the {@link MapboxMap} camera based on the location updates.
 *
 * @since 0.6.0
 */
public class NavigationCamera implements LifecycleObserver {

  /**
   * Camera tracks the user location, with bearing provided by the location update.
   * <p>
   * Equivalent of the {@link CameraMode#TRACKING_GPS}.
   */
  public static final int NAVIGATION_TRACKING_MODE_GPS = 0;
  /**
   * Camera tracks the user location, with bearing always set to north (0).
   * <p>
   * Equivalent of the {@link CameraMode#TRACKING_GPS_NORTH}.
   */
  public static final int NAVIGATION_TRACKING_MODE_NORTH = 1;
  /**
   * Camera does not tack the user location.
   * <p>
   * Equivalent of the {@link CameraMode#NONE}.
   */
  public static final int NAVIGATION_TRACKING_MODE_NONE = 2;
  private static final int ONE_POINT = 1;
  private final CopyOnWriteArrayList<OnTrackingModeTransitionListener> onTrackingModeTransitionListeners
    = new CopyOnWriteArrayList<>();
  private final CopyOnWriteArrayList<OnTrackingModeChangedListener> onTrackingModeChangedListeners
    = new CopyOnWriteArrayList<>();
  private final OnLocationCameraTransitionListener cameraTransitionListener
    = new NavigationCameraTransitionListener(this);
  private MapboxMap mapboxMap;
  private LocationComponent locationComponent;
  private MapboxNavigation navigation;
  private RouteInformation currentRouteInformation;
  private RouteProgress currentRouteProgress;
  @TrackingMode
  private int trackingCameraMode = NAVIGATION_TRACKING_MODE_GPS;
  private ProgressChangeListener progressChangeListener = new ProgressChangeListener() {
    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
      currentRouteProgress = routeProgress;
      if (isTrackingEnabled()) {
        currentRouteInformation = buildRouteInformationFromLocation(location, routeProgress);
        adjustCameraFromLocation(currentRouteInformation);
      }
    }
  };

  /**
   * Creates an instance of {@link NavigationCamera}.
   *
   * @param mapboxMap         for moving the camera
   * @param navigation        for listening to location updates
   * @param locationComponent for managing camera mode
   */
  public NavigationCamera(@NonNull MapboxMap mapboxMap, @NonNull MapboxNavigation navigation,
                          @NonNull LocationComponent locationComponent) {
    this.mapboxMap = mapboxMap;
    this.navigation = navigation;
    this.locationComponent = locationComponent;
    initializeWith(navigation);
  }

  /**
   * Creates an instance of {@link NavigationCamera}.
   * <p>
   * Camera will start tracking current user location by default.
   *
   * @param mapboxMap         for moving the camera
   * @param locationComponent for managing camera mode
   */
  public NavigationCamera(@NonNull MapboxMap mapboxMap, LocationComponent locationComponent) {
    this.mapboxMap = mapboxMap;
    this.locationComponent = locationComponent;
    updateCameraTrackingMode(trackingCameraMode);
  }

  /**
   * Used for testing only.
   */
  NavigationCamera(MapboxMap mapboxMap, MapboxNavigation navigation, ProgressChangeListener progressChangeListener,
                   LocationComponent locationComponent) {
    this.mapboxMap = mapboxMap;
    this.locationComponent = locationComponent;
    this.navigation = navigation;
    this.progressChangeListener = progressChangeListener;
  }

  /**
   * Called when beginning navigation with a route.
   *
   * @param route used to update route information
   */
  public void start(DirectionsRoute route) {
    if (route != null) {
      currentRouteInformation = buildRouteInformationFromRoute(route);
    }
    navigation.addProgressChangeListener(progressChangeListener);
  }

  /**
   * Called during rotation to update route information.
   *
   * @param location used to update route information
   */
  public void resume(Location location) {
    if (location != null) {
      currentRouteInformation = buildRouteInformationFromLocation(location, null);
    }
    navigation.addProgressChangeListener(progressChangeListener);
  }

  /**
   * Updates the {@link TrackingMode} that's going to be used when camera tracking is enabled.
   *
   * @param trackingMode the tracking mode
   */
  public void updateCameraTrackingMode(@TrackingMode int trackingMode) {
    setCameraMode(trackingMode);
  }

  /**
   * Getter for current state of tracking.
   *
   * @return true if tracking, false if not
   */
  public boolean isTrackingEnabled() {
    return trackingCameraMode != NAVIGATION_TRACKING_MODE_NONE;
  }

  /**
   * Getter for {@link TrackingMode} that's being used when tracking is enabled.
   *
   * @return tracking mode
   */
  @TrackingMode
  public int getCameraTrackingMode() {
    return trackingCameraMode;
  }

  /**
   * Resets the map camera / padding to the last known camera position.
   * <p>
   * You can also specify a tracking mode to reset with.  For example if you would like
   * to reset the camera and continue tracking, you would use {@link NavigationCamera#NAVIGATION_TRACKING_MODE_GPS}.
   *
   * @param trackingMode the tracking mode
   */
  public void resetCameraPositionWith(@TrackingMode int trackingMode) {
    resetWith(trackingMode);
  }

  /**
   * This method stops the map camera from tracking the current location, and then zooms
   * out to an overview of the current route being traveled.
   *
   * @param padding in pixels around the bounding box of the overview (left, top, right, bottom)
   */
  public void showRouteOverview(int[] padding) {
    updateCameraTrackingMode(NAVIGATION_TRACKING_MODE_NONE);
    RouteInformation routeInformation = buildRouteInformationFromProgress(currentRouteProgress);
    animateCameraForRouteOverview(routeInformation, padding);
  }

  /**
   * Call in {@link FragmentActivity#onStart()} to properly add the {@link ProgressChangeListener}
   * for the camera and prevent any leaks or further updates.
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
   */
  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  public void onStop() {
    if (navigation != null) {
      navigation.removeProgressChangeListener(progressChangeListener);
    }
  }

  /**
   * This method can be called if you did not pass an instance of {@link MapboxNavigation}
   * to the constructor.
   * <p>
   * The camera will begin listening to progress updates and update the route accordingly.
   *
   * @param navigation to add the camera progress change listener
   */
  public void addProgressChangeListener(MapboxNavigation navigation) {
    this.navigation = navigation;
    navigation.setCameraEngine(new DynamicCamera(mapboxMap));
    navigation.addProgressChangeListener(progressChangeListener);
  }

  /**
   * Adds given tracking mode transition listener for receiving notification of camera
   * transition updates.
   *
   * @param listener to be added
   */
  public void addOnTrackingModeTransitionListener(@NonNull OnTrackingModeTransitionListener listener) {
    onTrackingModeTransitionListeners.add(listener);
  }

  /**
   * Removes given tracking mode transition listener for receiving notification of camera
   * transition updates.
   *
   * @param listener to be removed
   */
  public void removeOnTrackingModeTransitionListener(@NonNull OnTrackingModeTransitionListener listener) {
    onTrackingModeTransitionListeners.remove(listener);
  }

  /**
   * Adds given tracking mode changed listener for receiving notification of camera
   * mode changes.
   *
   * @param listener to be added
   */
  public void addOnTrackingModeChangedListener(@NonNull OnTrackingModeChangedListener listener) {
    onTrackingModeChangedListeners.add(listener);
  }

  /**
   * Removes given tracking mode transition listener for receiving notification of camera
   * mode changes.
   *
   * @param listener to be removed
   */
  public void removeOnTrackingModeChangedListener(@NonNull OnTrackingModeChangedListener listener) {
    onTrackingModeChangedListeners.remove(listener);
  }

  void updateTransitionListenersFinished(@CameraMode.Mode int cameraMode) {
    Integer trackingCameraMode = findTrackingModeFor(cameraMode);
    if (trackingCameraMode == null) {
      return;
    }
    for (OnTrackingModeTransitionListener listener : onTrackingModeTransitionListeners) {
      listener.onTransitionFinished(trackingCameraMode);
    }
  }

  void updateTransitionListenersCancelled(@CameraMode.Mode int cameraMode) {
    Integer trackingCameraMode = findTrackingModeFor(cameraMode);
    if (trackingCameraMode == null) {
      return;
    }
    for (OnTrackingModeTransitionListener listener : onTrackingModeTransitionListeners) {
      listener.onTransitionCancelled(trackingCameraMode);
    }
  }

  private void initializeWith(MapboxNavigation navigation) {
    navigation.setCameraEngine(new DynamicCamera(mapboxMap));
    updateCameraTrackingMode(trackingCameraMode);
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

  private void animateCameraForRouteOverview(RouteInformation routeInformation, int[] padding) {
    Camera cameraEngine = navigation.getCameraEngine();
    List<Point> routePoints = cameraEngine.overview(routeInformation);
    if (!routePoints.isEmpty()) {
      animateMapboxMapForRouteOverview(padding, routePoints);
    }
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

  private void setCameraMode(@TrackingMode int trackingCameraMode) {
    @CameraMode.Mode Integer cameraMode = findCameraModeFor(trackingCameraMode);
    if (cameraMode != null) {
      this.trackingCameraMode = trackingCameraMode;
      updateTrackingModeListenersWith(this.trackingCameraMode);
      if (cameraMode != locationComponent.getCameraMode()) {
        locationComponent.setCameraMode(cameraMode, cameraTransitionListener);
      }
    } else {
      Timber.e("Using unsupported camera tracking mode - %d.", trackingCameraMode);
    }
  }

  @Nullable
  private Integer findCameraModeFor(@TrackingMode int trackingCameraMode) {
    if (trackingCameraMode == NAVIGATION_TRACKING_MODE_GPS) {
      return CameraMode.TRACKING_GPS;
    } else if (trackingCameraMode == NAVIGATION_TRACKING_MODE_NORTH) {
      return CameraMode.TRACKING_GPS_NORTH;
    } else if (trackingCameraMode == NAVIGATION_TRACKING_MODE_NONE) {
      return CameraMode.NONE;
    } else {
      return null;
    }
  }

  @Nullable
  private Integer findTrackingModeFor(@CameraMode.Mode int cameraMode) {
    if (cameraMode == CameraMode.TRACKING_GPS) {
      return NAVIGATION_TRACKING_MODE_GPS;
    } else if (cameraMode == CameraMode.TRACKING_GPS_NORTH) {
      return NAVIGATION_TRACKING_MODE_NORTH;
    } else if (cameraMode == CameraMode.NONE) {
      return NAVIGATION_TRACKING_MODE_NONE;
    } else {
      return null;
    }
  }

  private void updateTrackingModeListenersWith(@TrackingMode int trackingMode) {
    for (OnTrackingModeChangedListener listener : onTrackingModeChangedListeners) {
      listener.onTrackingModeChanged(trackingMode);
    }
  }

  private void resetWith(@TrackingMode int trackingMode) {
    updateCameraTrackingMode(trackingMode);
    if (currentRouteInformation != null) {
      Camera camera = navigation.getCameraEngine();
      if (camera instanceof DynamicCamera) {
        ((DynamicCamera) camera).forceResetZoomLevel();
      }
      adjustCameraFromLocation(currentRouteInformation);
    }
  }

  private void adjustCameraFromLocation(RouteInformation routeInformation) {
    Camera cameraEngine = navigation.getCameraEngine();

    float tilt = (float) cameraEngine.tilt(routeInformation);
    double zoom = cameraEngine.zoom(routeInformation);

    locationComponent.zoomWhileTracking(zoom, getZoomAnimationDuration(zoom));
    locationComponent.tiltWhileTracking(tilt, getTiltAnimationDuration(tilt));
  }

  private long getZoomAnimationDuration(double zoom) {
    double zoomDiff = Math.abs(mapboxMap.getCameraPosition().zoom - zoom);
    return (long) MathUtils.clamp(
      500 * zoomDiff,
      NAVIGATION_MIN_CAMERA_ZOOM_ADJUSTMENT_ANIMATION_DURATION,
      NAVIGATION_MAX_CAMERA_ADJUSTMENT_ANIMATION_DURATION);
  }

  private long getTiltAnimationDuration(double tilt) {
    double tiltDiff = Math.abs(mapboxMap.getCameraPosition().tilt - tilt);
    return (long) MathUtils.clamp(
      500 * tiltDiff,
      NAVIGATION_MIN_CAMERA_TILT_ADJUSTMENT_ANIMATION_DURATION,
      NAVIGATION_MAX_CAMERA_ADJUSTMENT_ANIMATION_DURATION);
  }

  @Retention(RetentionPolicy.SOURCE)
  @IntDef( {NAVIGATION_TRACKING_MODE_GPS,
    NAVIGATION_TRACKING_MODE_NORTH,
    NAVIGATION_TRACKING_MODE_NONE
  })
  public @interface TrackingMode {
  }
}
