package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;

import com.google.gson.Gson;

import com.google.gson.GsonBuilder;
import com.mapbox.directions.v5.DirectionsAdapterFactory;
import com.mapbox.directions.v5.models.DirectionsResponse;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.constants.Constants;


import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class SnapToRouteTest extends BaseTest {

  // Fixtures
  private static final String MULTI_LEG_ROUTE = "directions_two_leg_route.json";

  private RouteProgress routeProgress;
  private DirectionsRoute route;

  @Before
  public void setUp() throws Exception {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(MULTI_LEG_ROUTE);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    route = response.routes().get(0);

    routeProgress = RouteProgress.builder()
      .stepIndex(0)
      .legIndex(0)
      .legDistanceRemaining(100)
      .stepDistanceRemaining(100)
      .distanceRemaining(100)
      .directionsRoute(route)
      .build();

  }

  @Test
  public void sanity() throws Exception {
    Snap snap = new SnapToRoute();
    assertNotNull(snap);
  }

  @Test
  @Ignore
  public void getSnappedLocation_returnsProviderNameCorrectly() throws Exception {
    Snap snap = new SnapToRoute();
    Location location = new Location("test");
    List<Point> coordinates = PolylineUtils.decode(
      route.legs().get(0).steps().get(1).geometry(), Constants.PRECISION_6);
    Location snappedLocation
      = snap.getSnappedLocation(location, routeProgress, coordinates);
    assertTrue(snappedLocation.getProvider().equals("test-snapped"));
    assertTrue(location.getProvider().equals("test"));
  }
}
