package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;
import android.location.Location;

import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.DEFAULT_MANIFEST_NAME)
public class NavigationRouteProcessorTest extends BaseTest {

  private NavigationRouteProcessor routeProcessor;
  private MapboxNavigation navigation;

  @Before
  public void setup() throws Exception {
    routeProcessor = new NavigationRouteProcessor();
    MapboxNavigationOptions options = MapboxNavigationOptions.builder().build();
    navigation = new MapboxNavigation(mock(Context.class), ACCESS_TOKEN, options, mock(NavigationTelemetry.class),
      mock(LocationEngine.class));
    navigation.startNavigation(buildDirectionsRoute());
  }

  @Test
  public void sanity() throws Exception {
    assertNotNull(routeProcessor);
  }

  @Test
  public void onFirstRouteProgressBuilt_NewRouteIsDecoded() throws Exception {
    RouteProgress progress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    assertEquals(0, progress.legIndex());
    assertEquals(0, progress.currentLegProgress().stepIndex());
  }

  @Test
  public void onShouldIncreaseStepIndex_IndexIsIncreased() throws Exception {
    RouteProgress progress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    int currentStepIndex = progress.currentLegProgress().stepIndex();
    routeProcessor.onShouldIncreaseIndex();
    routeProcessor.checkIncreaseStepIndex(navigation);
    RouteProgress secondProgress = routeProcessor.buildNewRouteProgress(navigation, mock(Location.class));
    int secondStepIndex = secondProgress.currentLegProgress().stepIndex();
    assertTrue(currentStepIndex != secondStepIndex);
  }

  // TODO snapped location and bearing match booleans
}
