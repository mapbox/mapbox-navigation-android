package com.mapbox.services.android.navigation.ui.v5.map;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocationFpsDelegateTest {

  @Test
  public void onCameraIdle_newFpsIsSetZoom16() {
    double zoom = 16d;
    MapboxMap mapboxMap = mock(MapboxMap.class);
    when(mapboxMap.getCameraPosition()).thenReturn(buildCameraPosition(zoom));
    LocationComponent locationComponent = mock(LocationComponent.class);
    LocationFpsDelegate locationFpsDelegate = new LocationFpsDelegate(mapboxMap, locationComponent);

    locationFpsDelegate.onCameraIdle();

    verify(locationComponent).setMaxAnimationFps(eq(25));
  }

  @Test
  public void onCameraIdle_newFpsIsSetZoom14() {
    double zoom = 14d;
    MapboxMap mapboxMap = mock(MapboxMap.class);
    when(mapboxMap.getCameraPosition()).thenReturn(buildCameraPosition(zoom));
    LocationComponent locationComponent = mock(LocationComponent.class);
    LocationFpsDelegate locationFpsDelegate = new LocationFpsDelegate(mapboxMap, locationComponent);

    locationFpsDelegate.onCameraIdle();

    verify(locationComponent).setMaxAnimationFps(eq(15));
  }

  @Test
  public void onCameraIdle_newFpsIsSetZoom10() {
    double zoom = 10d;
    MapboxMap mapboxMap = mock(MapboxMap.class);
    when(mapboxMap.getCameraPosition()).thenReturn(buildCameraPosition(zoom));
    LocationComponent locationComponent = mock(LocationComponent.class);
    LocationFpsDelegate locationFpsDelegate = new LocationFpsDelegate(mapboxMap, locationComponent);

    locationFpsDelegate.onCameraIdle();

    verify(locationComponent).setMaxAnimationFps(eq(10));
  }

  @Test
  public void onCameraIdle_newFpsIsSet5() {
    double zoom = 5d;
    MapboxMap mapboxMap = mock(MapboxMap.class);
    when(mapboxMap.getCameraPosition()).thenReturn(buildCameraPosition(zoom));
    LocationComponent locationComponent = mock(LocationComponent.class);
    LocationFpsDelegate locationFpsDelegate = new LocationFpsDelegate(mapboxMap, locationComponent);

    locationFpsDelegate.onCameraIdle();

    verify(locationComponent).setMaxAnimationFps(eq(5));
  }

  @Test
  public void onStart_idleListenerAdded() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    LocationComponent locationComponent = mock(LocationComponent.class);
    LocationFpsDelegate locationFpsDelegate = new LocationFpsDelegate(mapboxMap, locationComponent);

    locationFpsDelegate.onStart();

    verify(mapboxMap, times(2)).addOnCameraIdleListener(eq(locationFpsDelegate));
  }

  @Test
  public void onStop_idleListenerRemoved() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    LocationComponent locationComponent = mock(LocationComponent.class);
    LocationFpsDelegate locationFpsDelegate = new LocationFpsDelegate(mapboxMap, locationComponent);

    locationFpsDelegate.onStop();

    verify(mapboxMap).removeOnCameraIdleListener(eq(locationFpsDelegate));
  }

  @Test
  public void updateEnabled_falseResetsToMax() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    LocationComponent locationComponent = mock(LocationComponent.class);
    LocationFpsDelegate locationFpsDelegate = new LocationFpsDelegate(mapboxMap, locationComponent);

    locationFpsDelegate.updateEnabled(false);

    verify(locationComponent).setMaxAnimationFps(eq(Integer.MAX_VALUE));
  }

  @Test
  public void isEnabled_returnsFalseWhenSet() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    LocationComponent locationComponent = mock(LocationComponent.class);
    LocationFpsDelegate locationFpsDelegate = new LocationFpsDelegate(mapboxMap, locationComponent);

    locationFpsDelegate.updateEnabled(false);

    assertFalse(locationFpsDelegate.isEnabled());
  }

  private CameraPosition buildCameraPosition(double zoom) {
    return new CameraPosition.Builder().zoom(zoom).build();
  }
}