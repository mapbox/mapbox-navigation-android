package com.mapbox.services.android.navigation.ui.v5.camera;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.OnLocationCameraTransitionListener;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.ui.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.camera.RouteInformation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    verify(locationComponent, times(1)).setCameraMode(eq(CameraMode.TRACKING_GPS),
      any(OnLocationCameraTransitionListener.class));
    verify(locationComponent, times(0)).setCameraMode(eq(CameraMode.NONE),
      any(OnLocationCameraTransitionListener.class));

    camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE);
    verify(locationComponent, times(1)).setCameraMode(eq(CameraMode.NONE),
      any(OnLocationCameraTransitionListener.class));

    camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
    verify(locationComponent, times(2)).setCameraMode(eq(CameraMode.TRACKING_GPS),
      any(OnLocationCameraTransitionListener.class));

    assertTrue(camera.isTrackingEnabled());
  }

  @Test
  public void setTrackingDisabled_trackingIsDisabled() {
    LocationComponent locationComponent = mock(LocationComponent.class);
    NavigationCamera camera = buildCamera(locationComponent);

    verify(locationComponent, times(1)).setCameraMode(eq(CameraMode.TRACKING_GPS),
      any(OnLocationCameraTransitionListener.class));
    verify(locationComponent, times(0)).setCameraMode(eq(CameraMode.NONE),
      any(OnLocationCameraTransitionListener.class));

    camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
    verify(locationComponent, times(2)).setCameraMode(eq(CameraMode.TRACKING_GPS),
      any(OnLocationCameraTransitionListener.class));

    camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE);
    verify(locationComponent, times(1)).setCameraMode(eq(CameraMode.NONE),
      any(OnLocationCameraTransitionListener.class));

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
  public void onResetCamera_dynamicCameraIsReset() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    when(mapboxMap.getCameraPosition()).thenReturn(mock(CameraPosition.class));
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    DynamicCamera dynamicCamera = mock(DynamicCamera.class);
    when(navigation.getCameraEngine()).thenReturn(dynamicCamera);
    RouteInformation currentRouteInformation = mock(RouteInformation.class);
    NavigationCamera camera = buildCamera(mapboxMap, navigation, currentRouteInformation);

    camera.resetCameraPositionWith(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);

    verify(dynamicCamera).forceResetZoomLevel();
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
  public void update_defaultIsIgnoredWhileTracking() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    LocationComponent locationComponent = mock(LocationComponent.class);
    when(locationComponent.getCameraMode()).thenReturn(CameraMode.TRACKING_GPS);
    when(mapboxMap.getLocationComponent()).thenReturn(locationComponent);
    CameraUpdate cameraUpdate = mock(CameraUpdate.class);
    MapboxMap.CancelableCallback callback = mock(MapboxMap.CancelableCallback.class);
    NavigationCameraUpdate navigationCameraUpdate = new NavigationCameraUpdate(cameraUpdate);
    NavigationCamera camera = buildCamera(mapboxMap);

    camera.update(navigationCameraUpdate, 300, callback);

    verify(mapboxMap, times(0)).animateCamera(cameraUpdate);
  }

  @Test
  public void update_defaultIsAcceptedWithNoTracking() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    LocationComponent locationComponent = mock(LocationComponent.class);
    when(locationComponent.getCameraMode()).thenReturn(CameraMode.NONE);
    when(mapboxMap.getLocationComponent()).thenReturn(locationComponent);
    CameraUpdate cameraUpdate = mock(CameraUpdate.class);
    MapboxMap.CancelableCallback callback = mock(MapboxMap.CancelableCallback.class);
    NavigationCameraUpdate navigationCameraUpdate = new NavigationCameraUpdate(cameraUpdate);
    NavigationCamera camera = buildCamera(mapboxMap);

    camera.update(navigationCameraUpdate, 300, callback);

    verify(mapboxMap).animateCamera(eq(cameraUpdate), eq(300), eq(callback));
  }

  @Test
  public void update_overrideIsAcceptedWhileTracking() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    LocationComponent locationComponent = mock(LocationComponent.class);
    when(locationComponent.getCameraMode()).thenReturn(CameraMode.TRACKING_GPS);
    when(mapboxMap.getLocationComponent()).thenReturn(locationComponent);
    CameraUpdate cameraUpdate = mock(CameraUpdate.class);
    MapboxMap.CancelableCallback callback = mock(MapboxMap.CancelableCallback.class);
    NavigationCameraUpdate navigationCameraUpdate = new NavigationCameraUpdate(cameraUpdate);
    navigationCameraUpdate.setMode(CameraUpdateMode.OVERRIDE);
    NavigationCamera camera = buildCamera(mapboxMap);

    camera.update(navigationCameraUpdate, 300, callback);

    verify(mapboxMap).animateCamera(eq(cameraUpdate), eq(300), eq(callback));
  }

  @Test
  public void update_overrideSetsLocationComponentCameraModeNone() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    LocationComponent locationComponent = mock(LocationComponent.class);
    when(locationComponent.getCameraMode()).thenReturn(CameraMode.TRACKING_GPS);
    when(mapboxMap.getLocationComponent()).thenReturn(locationComponent);
    CameraUpdate cameraUpdate = mock(CameraUpdate.class);
    MapboxMap.CancelableCallback callback = mock(MapboxMap.CancelableCallback.class);
    NavigationCameraUpdate navigationCameraUpdate = new NavigationCameraUpdate(cameraUpdate);
    navigationCameraUpdate.setMode(CameraUpdateMode.OVERRIDE);
    NavigationCamera camera = buildCamera(mapboxMap);

    camera.update(navigationCameraUpdate, 300, callback);

    verify(locationComponent).setCameraMode(eq(CameraMode.NONE));
  }

  private NavigationCamera buildCamera() {
    return new NavigationCamera(mock(MapboxMap.class), mock(MapboxNavigation.class), mock(LocationComponent.class));
  }

  private NavigationCamera buildCamera(MapboxMap mapboxMap) {
    return new NavigationCamera(mapboxMap, mock(MapboxNavigation.class), mock(LocationComponent.class));
  }

  private NavigationCamera buildCamera(LocationComponent locationComponent) {
    return new NavigationCamera(mock(MapboxMap.class), mock(MapboxNavigation.class), locationComponent);
  }

  private NavigationCamera buildCamera(MapboxNavigation navigation, ProgressChangeListener listener) {
    return new NavigationCamera(mock(MapboxMap.class), navigation, listener,
      mock(LocationComponent.class), mock(RouteInformation.class));
  }

  private NavigationCamera buildCamera(MapboxMap mapboxMap, MapboxNavigation navigation,
                                       RouteInformation routeInformation) {
    return new NavigationCamera(mapboxMap, navigation, mock(ProgressChangeListener.class),
      mock(LocationComponent.class), routeInformation);
  }
}
