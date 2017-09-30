package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;

import com.google.gson.Gson;
import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.commons.utils.PolylineUtils;

import org.junit.Before;
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
    Gson gson = new Gson();
    String body = readPath(MULTI_LEG_ROUTE);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    route = response.getRoutes().get(0);

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
  public void getSnappedLocation_returnsProviderNameCorrectly() throws Exception {
    Snap snap = new SnapToRoute();
    Location location = new Location("test");
    List<Position> coordinates = PolylineUtils.decode(
      route.getLegs().get(0).getSteps().get(1).getGeometry(), Constants.PRECISION_6);
    Location snappedLocation
      = snap.getSnappedLocation(location, routeProgress, coordinates);
    assertTrue(snappedLocation.getProvider().equals("test-snapped"));
    assertTrue(location.getProvider().equals("test"));
  }
}
