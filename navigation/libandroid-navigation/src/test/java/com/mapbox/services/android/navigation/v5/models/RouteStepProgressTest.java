package com.mapbox.services.android.navigation.v5.models;

import com.google.gson.Gson;
import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.api.utils.turf.TurfMisc;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.Assert.assertNotNull;


public class RouteStepProgressTest extends BaseTest {


  private DirectionsResponse response;
  private DirectionsRoute route;
  private LegStep firstStep;
  private RouteLeg firstLeg;

  @Before
  public void setup() {
    Gson gson = new Gson();
    String body = readPath("directions_v5_precision_6.json");
    response = gson.fromJson(body, DirectionsResponse.class);
    route = response.getRoutes().get(0);
    firstStep = route.getLegs().get(0).getSteps().get(0);
    firstLeg = route.getLegs().get(0);
  }

  @Test
  public void sanityTest() {
    RouteStepProgress routeStepProgress
      = new RouteStepProgress(firstLeg, 0, Mockito.mock(Position.class));
    assertNotNull("should not be null", routeStepProgress);
  }

  @Test
  public void getDistanceRemaining_equalsStepDistanceAtBeginning() {
    LineString lineString
      = LineString.fromPolyline(firstLeg.getSteps().get(5).getGeometry(), Constants.PRECISION_6);
    double stepDistance = TurfMeasurement.lineDistance(lineString, TurfConstants.UNIT_METERS);

    RouteStepProgress routeStepProgress
      = new RouteStepProgress(firstLeg, 5, firstLeg.getSteps().get(4).getManeuver().asPosition());

    Assert.assertEquals(stepDistance, routeStepProgress.getDistanceRemaining(), BaseTest.DELTA);
  }

  @Test
  public void getDistanceRemaining_equalsCorrectValueAtIntervals() {
    LineString lineString
      = LineString.fromPolyline(firstStep.getGeometry(), Constants.PRECISION_6);
    double stepDistance = TurfMeasurement.lineDistance(lineString, TurfConstants.UNIT_METERS);

    double stepSegments = 5; // meters

    // Chop the line in small pieces
    for (double i = 0; i < stepDistance; i += stepSegments) {
      Position position = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS).getCoordinates();

      LineString slicedLine = TurfMisc.lineSlice(Point.fromCoordinates(position),
        Point.fromCoordinates(route.getLegs().get(0).getSteps().get(1).getManeuver().asPosition()), lineString);

      double distance = TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);

      RouteStepProgress routeStepProgress = new RouteStepProgress(firstLeg, 0, position);
      Assert.assertEquals(distance, routeStepProgress.getDistanceRemaining(), BaseTest.DELTA);
    }
  }

  @Test
  public void getDistanceRemaining_equalsZeroAtEndOfStep() {
    RouteStepProgress routeStepProgress = new RouteStepProgress(firstLeg, 3,
      firstLeg.getSteps().get(4).getManeuver().asPosition());

    Assert.assertEquals(0, routeStepProgress.getDistanceRemaining(), BaseTest.DELTA);
  }

  @Test
  public void getDistanceTraveled() {

  }

  @Test
  public void getFractionTraveled() {

  }

  @Test
  public void getDurationRemaining() {

  }

}


