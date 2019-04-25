package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.navigator.NavigationStatus;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class NavigationRouteProcessorTest extends BaseTest {

  @Test
  public void buildNewRouteProgress_routeProgressReturned() throws IOException {
    MapboxNavigator navigator = mock(MapboxNavigator.class);
    NavigationStatus status = mock(NavigationStatus.class);
    NavigationRouteProcessor processor = new NavigationRouteProcessor();

    RouteProgress progress = processor.buildNewRouteProgress(navigator, status, buildTestDirectionsRoute());

    assertNotNull(progress);
  }

  @Test
  public void buildNewRouteProgress_previousStatusIsReturned() throws IOException {
    MapboxNavigator navigator = mock(MapboxNavigator.class);
    NavigationStatus status = mock(NavigationStatus.class);
    NavigationRouteProcessor processor = new NavigationRouteProcessor();

    processor.buildNewRouteProgress(navigator, status, buildTestDirectionsRoute());

    assertEquals(status, processor.retrievePreviousStatus());
  }
}
