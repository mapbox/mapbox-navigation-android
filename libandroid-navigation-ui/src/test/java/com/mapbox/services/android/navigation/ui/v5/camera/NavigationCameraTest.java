package com.mapbox.services.android.navigation.ui.v5.camera;

import android.location.Location;

import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.ui.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.camera.SimpleCamera;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;

import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationCameraTest extends BaseTest {

  @Test
  public void sanity() {
    NavigationCamera camera = buildCamera();

    assertNotNull(camera);
  }

  @Test
  public void setTrackingEnabled_trackingIsEnabled() {
    LocationComponent locationComponent = mock(LocationComponent.class);
    NavigationCamera camera = buildCamera(locationComponent);

    verify(locationComponent, times(1)).setCameraMode(CameraMode.TRACKING_GPS);
    verify(locationComponent, times(0)).setCameraMode(CameraMode.NONE);

    camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE);
    verify(locationComponent, times(1)).setCameraMode(CameraMode.NONE);

    camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
    verify(locationComponent, times(2)).setCameraMode(CameraMode.TRACKING_GPS);

    assertTrue(camera.isTrackingEnabled());
  }

  @Test
  public void setTrackingDisabled_trackingIsDisabled() {
    LocationComponent locationComponent = mock(LocationComponent.class);
    NavigationCamera camera = buildCamera(locationComponent);

    verify(locationComponent, times(1)).setCameraMode(CameraMode.TRACKING_GPS);
    verify(locationComponent, times(0)).setCameraMode(CameraMode.NONE);

    camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
    verify(locationComponent, times(2)).setCameraMode(CameraMode.TRACKING_GPS);

    camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE);
    verify(locationComponent, times(1)).setCameraMode(CameraMode.NONE);

    assertFalse(camera.isTrackingEnabled());
  }

  @Test
  public void onResetCamera_trackingIsResumed() {
    NavigationCamera camera = buildCamera();

    camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE);
    camera.resetCameraPositionWith(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);

    assertTrue(camera.isTrackingEnabled());
  }

  @Test
  public void onStartWithNullRoute_progressListenerIsAdded() {
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    ProgressChangeListener listener = mock(ProgressChangeListener.class);
    NavigationCamera camera = buildCamera(navigation, listener);

    camera.start(null);

    verify(navigation, times(1)).addProgressChangeListener(listener);
  }

  @Test
  public void onResumeWithNullLocation_progressListenerIsAdded() {
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    ProgressChangeListener listener = mock(ProgressChangeListener.class);
    NavigationCamera camera = buildCamera(navigation, listener);

    camera.resume(null);

    verify(navigation, times(1)).addProgressChangeListener(listener);
  }

  @Test
  public void resetCameraPositionWith_mapCameraAnimates() throws IOException {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    when(navigation.getCameraEngine()).thenReturn(new SimpleCamera());
    LocationComponent locationComponent = mock(LocationComponent.class);
    Location location = buildMockLocation();
    when(locationComponent.getLastKnownLocation()).thenReturn(location);
    NavigationCamera camera = new NavigationCamera(mapboxMap, navigation, locationComponent);
    camera.start(buildTestDirectionsRoute());

    camera.resetCameraPositionWith(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);

    verify(mapboxMap).animateCamera(any(CameraUpdate.class), anyInt(), any(MapboxMap.CancelableCallback.class));
  }

  @Test
  public void resetCameraPositionWith_nullLocationIsIgnored() throws IOException {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    when(navigation.getCameraEngine()).thenReturn(new SimpleCamera());
    LocationComponent locationComponent = mock(LocationComponent.class);
    when(locationComponent.getLastKnownLocation()).thenReturn(null);
    NavigationCamera camera = new NavigationCamera(mapboxMap, navigation, locationComponent);
    camera.start(buildTestDirectionsRoute());

    camera.resetCameraPositionWith(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);

    verify(mapboxMap, times(0)).animateCamera(any(CameraUpdate.class),
      anyInt(), any(MapboxMap.CancelableCallback.class));
  }

  private Location buildMockLocation() {
    Location location = mock(Location.class);
    when(location.getLatitude()).thenReturn(1.234);
    when(location.getLongitude()).thenReturn(1.234);
    when(location.getBearing()).thenReturn(1f);
    return location;
  }

  private NavigationCamera buildCamera() {
    return new NavigationCamera(mock(MapboxMap.class), mock(MapboxNavigation.class), mock(LocationComponent.class));
  }

  private NavigationCamera buildCamera(MapboxMap map, LocationComponent locationComponent) {
    return new NavigationCamera(map, mock(MapboxNavigation.class), locationComponent);
  }

  private NavigationCamera buildCamera(LocationComponent locationComponent) {
    return new NavigationCamera(mock(MapboxMap.class), mock(MapboxNavigation.class), locationComponent);
  }

  private NavigationCamera buildCamera(MapboxNavigation navigation, ProgressChangeListener listener) {
    return new NavigationCamera(mock(MapboxMap.class), navigation, listener, mock(LocationComponent.class));
  }
}
