package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.navigator.Navigator;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

import static junit.framework.Assert.assertNotNull;

@Ignore
public class NavigationRouteProcessorTest extends BaseTest {

  @Test
  public void buildNewRouteProgress_routeProgressReturned() throws IOException {
    Navigator navigator = new Navigator();
    NavigationRouteProcessor processor = new NavigationRouteProcessor(navigator);
    Date date = new Date();

    RouteProgress progress = processor.buildNewRouteProgress(date, buildTestDirectionsRoute());

    assertNotNull(progress);
  }
}
