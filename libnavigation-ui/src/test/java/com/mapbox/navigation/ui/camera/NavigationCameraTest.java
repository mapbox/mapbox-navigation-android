package com.mapbox.navigation.ui.camera;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.OnLocationCameraTransitionListener;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.ui.BaseTest;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

    verify(locationComponent, times(0)).setCameraMode(eq(CameraMode.TRACKING_GPS),
      any(OnLocationCameraTransitionListener.class));
    verify(locationComponent, times(0)).setCameraMode(eq(CameraMode.NONE),
      any(OnLocationCameraTransitionListener.class));

    camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE);
    verify(locationComponent, times(1)).setCameraMode(eq(CameraMode.NONE),
      any(OnLocationCameraTransitionListener.class));

    camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
    verify(locationComponent, times(1)).setCameraMode(eq(CameraMode.TRACKING_GPS),
      any(OnLocationCameraTransitionListener.class));

    assertTrue(camera.isTrackingEnabled());
  }

  @Test
  public void setTrackingDisabled_trackingIsDisabled() {
    LocationComponent locationComponent = mock(LocationComponent.class);
    NavigationCamera camera = buildCamera(locationComponent);

    verify(locationComponent, times(0)).setCameraMode(eq(CameraMode.TRACKING_GPS),
      any(OnLocationCameraTransitionListener.class));
    verify(locationComponent, times(0)).setCameraMode(eq(CameraMode.NONE),
      any(OnLocationCameraTransitionListener.class));

    camera.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
    verify(locationComponent, times(1)).setCameraMode(eq(CameraMode.TRACKING_GPS),
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
    DynamicCamera dynamicCamera = mock(DynamicCamera.class);
    NavigationCamera camera = buildCamera(mapboxMap);
    camera.setCamera(dynamicCamera);

    camera.resetCameraPositionWith(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);

    verify(dynamicCamera).forceResetZoomLevel();
  }

  @Test
  public void onStartWithNullRoute_progressListenerIsAdded() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    NavigationCamera camera = buildCamera(mapboxMap, navigation);

    camera.start(null);

    verify(navigation, times(1)).registerRouteProgressObserver(any(RouteProgressObserver.class));
  }

  @Test
  public void onResumeWithNullLocation_progressListenerIsAdded() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    NavigationCamera camera = buildCamera(mapboxMap, navigation);

    camera.resume(null);

    verify(navigation, times(1)).registerRouteProgressObserver(any(RouteProgressObserver.class));
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

  @Test
  public void onShowRouteOverview_whenRouteProgressAndRouteAreNull() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    NavigationCamera camera = buildCamera(mapboxMap, navigation);

    camera.start(null);
    boolean result = camera.showRouteGeometryOverview(new int[4]);

    assertFalse(result);
    // verify no crash
  }

  @Test
  public void onShowRouteOverview_animateCamera_whenRouteProgressIsNull() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    DirectionsRoute directionsRoute = mock(DirectionsRoute.class);
    DynamicCamera dynamicCamera = mock(DynamicCamera.class);
    NavigationCamera camera = buildCamera(mapboxMap, navigation);
    camera.setCamera(dynamicCamera);
    List<Point> points = new ArrayList<>();
    points.add(mock(Point.class));
    points.add(mock(Point.class));
    points.add(mock(Point.class));
    when(dynamicCamera.overview(any())).thenReturn(points);

    camera.start(directionsRoute);
    boolean result = camera.showRouteGeometryOverview(new int[4]);

    assertTrue(result);
    verify(mapboxMap).animateCamera(any(), anyInt(), any());
  }

  @Test
  public void onShowRouteOverview_animateCamera_whenRouteInfoIsNull() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    when(mapboxMap.getCameraPosition()).thenReturn(CameraPosition.DEFAULT);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    DirectionsRoute directionsRoute = mock(DirectionsRoute.class);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.getRoute()).thenReturn(directionsRoute);
    DynamicCamera dynamicCamera = mock(DynamicCamera.class);
    NavigationCamera camera = buildCamera(mapboxMap, navigation);
    camera.setCamera(dynamicCamera);
    List<Point> points = new ArrayList<>();
    points.add(mock(Point.class));
    points.add(mock(Point.class));
    points.add(mock(Point.class));
    when(dynamicCamera.overview(any())).thenReturn(points);

    camera.start(null);
    camera.routeProgressObserver.onRouteProgressChanged(routeProgress);
    boolean result = camera.showRouteGeometryOverview(new int[4]);

    assertTrue(result);
    verify(mapboxMap).animateCamera(any(), anyInt(), any());
  }

  @Test
  public void onShowRouteOverview_emptyRoute() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    when(mapboxMap.getCameraPosition()).thenReturn(CameraPosition.DEFAULT);
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    DirectionsRoute directionsRoute = mock(DirectionsRoute.class);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.getRoute()).thenReturn(directionsRoute);
    DynamicCamera dynamicCamera = mock(DynamicCamera.class);
    NavigationCamera camera = buildCamera(mapboxMap, navigation);
    camera.setCamera(dynamicCamera);
    List<Point> points = new ArrayList<>();
    when(dynamicCamera.overview(any())).thenReturn(points);

    camera.start(null);
    camera.routeProgressObserver.onRouteProgressChanged(routeProgress);
    boolean result = camera.showRouteGeometryOverview(new int[4]);

    assertFalse(result);
    // verify no crash
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

  private NavigationCamera buildCamera(MapboxMap mapboxMap, MapboxNavigation mapboxNavigation) {
    return new NavigationCamera(mapboxMap, mapboxNavigation, mock(LocationComponent.class));
  }
}
