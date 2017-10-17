package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class NavigationRouteTest extends BaseTest {

  @Test
  public void sanityTest() throws Exception {
    NavigationRoute navigationRoute = NavigationRoute.builder()
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .build();
    assertNotNull(navigationRoute);
  }
}