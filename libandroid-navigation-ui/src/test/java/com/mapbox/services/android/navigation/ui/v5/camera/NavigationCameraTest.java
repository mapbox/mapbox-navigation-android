package com.mapbox.services.android.navigation.ui.v5.camera;

import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NavigationCameraTest {

  @Test
  public void sanity() throws Exception {
    NavigationCamera camera = buildCamera();

    assertNotNull(camera);
  }

  @Test
  public void setTrackingEnabled_trackingIsEnabled() throws Exception {
    NavigationCamera camera = buildCamera();

    camera.updateCameraTrackingLocation(false);
    camera.updateCameraTrackingLocation(true);

    assertTrue(camera.isTrackingEnabled());
  }

  @Test
  public void setTrackingDisabled_trackingIsDisabled() throws Exception {
    NavigationCamera camera = buildCamera();

    camera.updateCameraTrackingLocation(true);
    camera.updateCameraTrackingLocation(false);

    assertFalse(camera.isTrackingEnabled());
  }

  @Test
  public void onResetCamera_trackingIsResumed() throws Exception {
    NavigationCamera camera = buildCamera();

    camera.updateCameraTrackingLocation(false);
    camera.resetCameraPosition();

    assertTrue(camera.isTrackingEnabled());
  }

  @Test
  public void onStartWithNullRoute_progressListenerIsAdded() throws Exception {
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    ProgressChangeListener listener = mock(ProgressChangeListener.class);
    NavigationCamera camera = buildCamera(navigation, listener);

    camera.start(null);

    verify(navigation, times(1)).addProgressChangeListener(listener);
  }

  @Test
  public void onResumeWithNullLocation_progressListenerIsAdded() throws Exception {
    MapboxNavigation navigation = mock(MapboxNavigation.class);
    ProgressChangeListener listener = mock(ProgressChangeListener.class);
    NavigationCamera camera = buildCamera(navigation, listener);

    camera.resume(null);

    verify(navigation, times(1)).addProgressChangeListener(listener);
  }

  private NavigationCamera buildCamera() {
    return new NavigationCamera(mock(MapboxMap.class), mock(MapboxNavigation.class));
  }

  private NavigationCamera buildCamera(MapboxNavigation navigation, ProgressChangeListener listener) {
    return new NavigationCamera(mock(MapboxMap.class), navigation, listener);
  }
}
