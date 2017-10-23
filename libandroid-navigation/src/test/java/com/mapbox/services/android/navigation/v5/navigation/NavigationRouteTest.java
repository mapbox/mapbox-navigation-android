package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.directions.v5.DirectionsCriteria;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

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

  @Test
  public void changingDefaultValueToCustomWorksProperly() throws Exception {
    NavigationRoute navigationRoute = NavigationRoute.builder()
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0))
      .destination(Point.fromLngLat(1.0, 5.0))
      .profile(DirectionsCriteria.PROFILE_CYCLING)
      .build();

    assertThat(navigationRoute.getCall().request().url().toString(),
      containsString("/cycling/"));
  }
}