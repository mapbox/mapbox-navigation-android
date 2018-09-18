package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.navigator.NavigationStatus;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@Ignore
public class NavigationRouteProcessorTest extends BaseTest {

  @Test
  public void buildNewRouteProgress_routeProgressReturned() throws IOException {
    NavigationRouteProcessor processor = new NavigationRouteProcessor();

    // TODO mock final status
    RouteProgress progress = processor.buildNewRouteProgress(mock(NavigationStatus.class), buildTestDirectionsRoute());

    assertNotNull(progress);
  }
}
