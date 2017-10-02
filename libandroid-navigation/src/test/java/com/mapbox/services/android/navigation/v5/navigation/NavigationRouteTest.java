package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.commons.models.Position;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class NavigationRouteTest extends BaseTest {

  private static final String accessToken = "pk.XXX";

  @Test
  public void sanityTest() throws Exception {
    NavigationRoute navigationRoute = NavigationRoute.builder()
      .accessToken(accessToken)
      .origin(Position.fromCoordinates(1.0, 2.0))
      .destination(Position.fromCoordinates(1.0, 5.0))
      .build();
    assertNotNull(navigationRoute);
  }

  @Test
  public void originDestination_doGetAddedToNullCoordinateList() throws Exception {
    Position origin = Position.fromCoordinates(1.0, 2.0);
    Position destination = Position.fromCoordinates(1.0, 5.0);

    NavigationRoute navigationRoute = NavigationRoute.builder()
      .accessToken(accessToken)
      .origin(origin)
      .destination(destination)
      .build();
    assertEquals(2, navigationRoute.coordinates().size());
    assertTrue(origin.equals(navigationRoute.coordinates().get(0)));
    assertTrue(destination.equals(navigationRoute.coordinates().get(1)));
  }

  @Test
  public void originDestination_doGetAddedToFullCoordinateList() throws Exception {
    Position origin = Position.fromCoordinates(1.0, 2.0);
    Position destination = Position.fromCoordinates(1.0, 5.0);
    Position waypointOne = Position.fromCoordinates(10.0, 4.0);
    Position waypointTwo = Position.fromCoordinates(5.0, 3.0);
    Position waypointThree = Position.fromCoordinates(9.0, 7.0);

    NavigationRoute navigationRoute = NavigationRoute.builder()
      .accessToken(accessToken)
      .origin(origin)
      .destination(destination)
      .profile(DirectionsCriteria.PROFILE_DRIVING)
      .addWaypoint(waypointOne)
      .addWaypoint(waypointTwo)
      .addWaypoint(waypointThree)
      .build();
    assertEquals(5, navigationRoute.coordinates().size());
    assertTrue(origin.equals(navigationRoute.coordinates().get(0)));
    assertTrue(waypointOne.equals(navigationRoute.coordinates().get(1)));
    assertTrue(destination.equals(navigationRoute.coordinates().get(4)));
  }

  @Test
  public void requestDoesNotAttachRadiusIfOnesNotProvided() throws Exception {
    NavigationRoute navigationRoute = NavigationRoute.builder()
      .accessToken(accessToken)
      .origin(Position.fromCoordinates(1.0, 2.0))
      .destination(Position.fromCoordinates(1.0, 5.0))
      .addBearing(90, 100)
      .build();

    String url = navigationRoute.getDirectionsRequest().cloneCall().request().url().toString();
    assertFalse(url.contains("radiuses="));
  }
}