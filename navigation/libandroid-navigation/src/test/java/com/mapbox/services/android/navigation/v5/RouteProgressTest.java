package com.mapbox.services.android.navigation.v5;

import com.google.gson.Gson;
import com.mapbox.services.Constants;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.models.Position;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

public class RouteProgressTest extends BaseTest {

  // Fixtures
  private static final String PRECISION_6 = "directions_v5_precision_6.json";

  private DirectionsRoute route;
  private RouteLeg firstLeg;
  private Position userSnappedPosition;

  @Before
  public void setup() {
    Gson gson = new Gson();
    String body = readPath(PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    route = response.getRoutes().get(0);
    firstLeg = route.getLegs().get(0);
    userSnappedPosition = firstLeg.getSteps().get(4).getManeuver().asPosition();
  }

  @Test
  public void sanityTest() {
    RouteProgress routeProgress = RouteProgress.create(route, userSnappedPosition, 0, 0);
    assertNotNull("should not be null", routeProgress);
  }

  @Test
  public void getRoute_returnsDirectionsRoute() {
    RouteProgress routeProgress
      = RouteProgress.create(route, userSnappedPosition, 0, 0);

    Assert.assertEquals(route, routeProgress.getRoute());
  }

  @Test
  public void getDistanceRemaining_equalsZeroAtEndOfRoute() {
    RouteProgress routeProgress
      = RouteProgress.create(
      route, firstLeg.getSteps().get(firstLeg.getSteps().size() - 1).getManeuver().asPosition(),
      route.getLegs().size() - 1, firstLeg.getSteps().size() - 1);

    Assert.assertEquals(0, routeProgress.getDistanceRemaining(), DELTA);
  }

  @Test
  public void getDistanceRemaining_equalsRouteDistanceAtBeginning() {
    RouteProgress routeProgress
      = RouteProgress.create(
      route, firstLeg.getSteps().get(0).getManeuver().asPosition(),
      0, 0);

    Assert.assertEquals(route.getDistance(), routeProgress.getDistanceRemaining(), LARGE_DELTA);
  }

  @Test
  public void getFractionTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress
      = RouteProgress.create(route, firstLeg.getSteps().get(0).getManeuver().asPosition(),
      0, 0);

    Assert.assertEquals(0, routeProgress.getFractionTraveled(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void getFractionTraveled_equalsCorrectValueAtIntervals() {
    LineString lineString = LineString.fromPolyline(route.getGeometry(), Constants.PRECISION_6);

    double stepSegments = 500; // meters

    // Chop the line in small pieces
    for (double i = 0; i < route.getDistance(); i += stepSegments) {
      Position position = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS).getCoordinates();

      RouteProgress routeProgress = RouteProgress.create(route, position, 0, 0);
      float fractionRemaining = (float) (routeProgress.getDistanceTraveled() / route.getDistance());
      Assert.assertEquals(fractionRemaining, routeProgress.getFractionTraveled(), BaseTest.DELTA);
    }
  }

  @Test
  public void getFractionTraveled_equalsOneAtEndOfRoute() {
    Position lastCoordinate
      = route.getLegs().get(0).getSteps().get(route.getLegs().get(0).getSteps().size() - 1).getManeuver().asPosition();

    RouteProgress routeProgress = RouteProgress.create(route, lastCoordinate,
      route.getLegs().size() - 1, firstLeg.getSteps().size() - 1);

    Assert.assertEquals(1.0, routeProgress.getFractionTraveled(), DELTA);
  }

  @Test
  public void getDurationRemaining_equalsRouteDurationAtBeginning() {
    RouteProgress routeProgress
      = RouteProgress.create(route, firstLeg.getSteps().get(0).getManeuver().asPosition(),
      0, 0);

    Assert.assertEquals(3535.2, routeProgress.getDurationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsZeroAtEndOfRoute() {
    Position lastCoordinate
      = route.getLegs().get(0).getSteps().get(route.getLegs().get(0).getSteps().size() - 1).getManeuver().asPosition();

    RouteProgress routeProgress = RouteProgress.create(route, lastCoordinate,
      route.getLegs().size() - 1, firstLeg.getSteps().size() - 1);

    Assert.assertEquals(0, routeProgress.getDurationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void getDistanceTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress
      = RouteProgress.create(route, firstLeg.getSteps().get(0).getManeuver().asPosition(),
      0, 0);
    Assert.assertEquals(0, routeProgress.getDistanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDistanceTraveled_equalsRouteDistanceAtEndOfRoute() {
    Position lastCoordinate
      = route.getLegs().get(0).getSteps().get(route.getLegs().get(0).getSteps().size() - 1).getManeuver().asPosition();

    RouteProgress routeProgress = RouteProgress.create(route, lastCoordinate,
      route.getLegs().size() - 1, firstLeg.getSteps().size() - 1);

    Assert.assertEquals(route.getDistance(), routeProgress.getDistanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getCurrentLeg_returnsCurrentLeg() {
    RouteProgress routeProgress
      = RouteProgress.create(route, firstLeg.getSteps().get(0).getManeuver().asPosition(),
      0, 0);

    Assert.assertEquals(route.getLegs().get(0), routeProgress.getCurrentLeg());
  }

  @Test
  public void getLegIndex_returnsCurrentLegIndex() {
    RouteProgress routeProgress
      = RouteProgress.create(route, firstLeg.getSteps().get(0).getManeuver().asPosition(),
      0, 0);

    Assert.assertEquals(0, routeProgress.getLegIndex());
  }
}