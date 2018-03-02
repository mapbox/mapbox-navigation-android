package com.mapbox.services.android.navigation.ui.v5.camera;

import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.DEFAULT_MANIFEST_NAME)
public class NavigationCameraTest {

  @Mock
  private MapboxNavigation navigation;

  @Mock
  private MapboxMap mapboxMap;

  private NavigationCamera camera;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    camera = new NavigationCamera(mapboxMap, navigation);
  }

  @Test
  public void sanity() throws Exception {
    assertNotNull(camera);
  }

  @Test
  public void setTrackingEnabled_trackingIsEnabled() throws Exception {
    camera.setCameraTrackingLocation(false);
    camera.setCameraTrackingLocation(true);
    assertTrue(camera.isTrackingEnabled());
  }

  @Test
  public void setTrackingDisabled_trackingIsDisabled() throws Exception {
    camera.setCameraTrackingLocation(true);
    camera.setCameraTrackingLocation(false);
    assertFalse(camera.isTrackingEnabled());
  }

  @Test
  public void onResetCamera_trackingIsResumed() throws Exception {
    camera.setCameraTrackingLocation(false);
    camera.resetCameraPosition();
    assertTrue(camera.isTrackingEnabled());
  }

  @Test
  public void onStartWithNullRoute_progressListenerIsAdded() throws Exception {
    camera.start(null);
    verify(navigation, times(1)).addProgressChangeListener(camera);
  }

  @Test
  public void onResumeWithNullLocation_progressListenerIsAdded() throws Exception {
    camera.resume(null);
    verify(navigation, times(1)).addProgressChangeListener(camera);
  }
}
