package com.mapbox.services.android.navigation.v5;

import android.location.Location;

import com.google.gson.Gson;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.Assert.assertNotNull;

public class RouteProgressTest extends BaseTest {

  private DirectionsResponse response;
  private DirectionsRoute route;

  @Before
  public void setup() {
    Gson gson = new Gson();
    String body = readPath("directions_v5.json");
    response = gson.fromJson(body, DirectionsResponse.class);
    route = response.getRoutes().get(0);
  }

  @Test
  public void sanityTest() {
    RouteProgress routeProgress = new RouteProgress(route, Mockito.mock(Location.class), 0, 0, 0);
    assertNotNull("should not be null", routeProgress);
  }

}
