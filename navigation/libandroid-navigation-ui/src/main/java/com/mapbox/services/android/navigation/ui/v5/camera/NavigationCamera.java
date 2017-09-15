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

import timber.log.Timber;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

public class NavigationCamera implements ProgressChangeListener {

  private static final int CAMERA_TILT = 50;
  private static int CAMERA_ZOOM = 17;

  private MapboxMap mapboxMap;
  private MapboxNavigation navigation;

  private Location location;
  private double targetDistance;
  private boolean trackingEnabled = true;

  public NavigationCamera(@NonNull Context context, @NonNull MapboxMap mapboxMap,
                          @NonNull MapboxNavigation navigation) {
    this.mapboxMap = mapboxMap;
    this.navigation = navigation;
    initialize(context);
  }

  public void start(DirectionsRoute route) {
    if (route != null) {
      CameraPosition cameraPosition = buildCameraPositionFromRoute(route);
      animateCameraToPosition(cameraPosition);
    } else {
      navigation.addProgressChangeListener(NavigationCamera.this);
    }
  }

  public void resume(Location location) {
    if (location != null) {
      CameraPosition position = buildCameraPositionFromLocation(location);
      animateCameraToPosition(position);
    } else {
      navigation.addProgressChangeListener(NavigationCamera.this);
    }
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    this.location = location;
    easeCameraToLocation(location);
  }

  public void setCameraTrackingLocation(boolean trackingEnabled) {
    this.trackingEnabled = trackingEnabled;
  }

  public boolean isTrackingEnabled() {
    return trackingEnabled;
  }

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

  private void initialize(Context context) {
    initializeTargetDistance(context);
    initializeScreenOrientation(context);
  }

  private void initializeTargetDistance(Context context) {
    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
    ScreenDensityMap densityMap = new ScreenDensityMap();
    targetDistance = densityMap.getTargetDistance(metrics.densityDpi);
    Timber.d("Screen Density: " + metrics.densityDpi + " Target Distance: " + targetDistance);
  }

  private void initializeScreenOrientation(Context context) {
    CAMERA_ZOOM = new OrientationMap().get(context.getResources().getConfiguration().orientation);
  }

  private class OrientationMap extends SparseArray<Integer> {

    OrientationMap() {
      put(ORIENTATION_PORTRAIT, 17);
      put(ORIENTATION_LANDSCAPE, 16);
    }
  }
}
