package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.directions.v5.DirectionsCriteria;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
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

  @Test
  public void addingPointAndBearingKeepsCorrectOrder() throws Exception {
    NavigationRoute navigationRoute = NavigationRoute.builder()
      .accessToken(ACCESS_TOKEN)
      .origin(Point.fromLngLat(1.0, 2.0), 90d, 90d)
      .addBearing(2.0, 3.0)
      .destination(Point.fromLngLat(1.0, 5.0))
      .build();

    assertThat(navigationRoute.getCall().request().url().toString(),
      containsString("bearings=90,90;2,3;"));
  }

  @Test
  @Ignore
  public void reverseOriginDestinationDoesntMessUpBearings() throws Exception {
    NavigationRoute navigationRoute = NavigationRoute.builder()
      .accessToken(ACCESS_TOKEN)
      .destination(Point.fromLngLat(1.0, 5.0),1d,5d)
      .origin(Point.fromLngLat(1.0, 2.0), 90d, 90d)
      .build();

    assertThat(navigationRoute.getCall().request().url().toString(),
      containsString("bearings=90,90;1,5"));
  }
}
