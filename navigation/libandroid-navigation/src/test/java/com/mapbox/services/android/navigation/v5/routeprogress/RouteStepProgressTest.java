package com.mapbox.services.android.navigation.v5.routeprogress;

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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;


public class RouteStepProgressTest extends BaseTest {

  // Fixtures
  private static final String DCMAPBOX_CHIPOLTLE = "dcmapbox_chipoltle.json";
  private static final String PRECISION_6 = "directions_v5_precision_6.json";

  private DirectionsResponse response;
  private DirectionsRoute route;
  private LegStep firstStep;
  private RouteLeg firstLeg;

  @Before
  public void setup() {
    Gson gson = new Gson();
    String body = readPath(PRECISION_6);
    response = gson.fromJson(body, DirectionsResponse.class);
    route = response.getRoutes().get(0);
    firstStep = route.getLegs().get(0).getSteps().get(0);
    firstLeg = route.getLegs().get(0);
  }

  @Test
  public void sanityTest() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(0).getManeuver().asPosition()))
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();
    assertNotNull("should not be null", routeProgress.currentLegProgress().currentStepProgress());
  }

  @Test
  public void stepDistance_equalsZeroOnOneCoordSteps() {
    Gson gson = new Gson();
    String body = readPath(DCMAPBOX_CHIPOLTLE);
    response = gson.fromJson(body, DirectionsResponse.class);
    DirectionsRoute route = response.getRoutes().get(0);

    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(Mockito.mock(Position.class)))
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(route.getLegs().get(0).getSteps().size() - 1)
      .legIndex(0)
      .build();

    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

    assertNotNull("should not be null", routeStepProgress);
    assertEquals(1, routeStepProgress.fractionTraveled(), DELTA);
    assertEquals(0, routeStepProgress.distanceRemaining(), DELTA);
    assertEquals(0, routeStepProgress.distanceTraveled(), DELTA);
    assertEquals(0, routeStepProgress.durationRemaining(), DELTA);
  }

  @Test
  public void distanceRemaining_equalsStepDistanceAtBeginning() {
    LineString lineString
      = LineString.fromPolyline(firstLeg.getSteps().get(5).getGeometry(), Constants.PRECISION_6);
    double stepDistance = TurfMeasurement.lineDistance(lineString, TurfConstants.UNIT_METERS);

    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(4).getManeuver().asPosition()))
      .stepDistanceRemaining(firstLeg.getSteps().get(5).getDistance())
      .legDistanceRemaining(firstLeg.getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(4)
      .legIndex(0)
      .build();

    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

    Assert.assertEquals(stepDistance, routeStepProgress.distanceRemaining(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void distanceRemaining_equalsCorrectValueAtIntervals() {
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
      RouteProgress routeProgress = RouteProgress.builder()
        .location(buildTestLocation(position))
        .stepDistanceRemaining(distance)
        .legDistanceRemaining(firstLeg.getDistance())
        .distanceRemaining(route.getDistance())
        .directionsRoute(route)
        .stepIndex(0)
        .legIndex(0)
        .build();
      RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
      Assert.assertEquals(distance, routeStepProgress.distanceRemaining(), BaseTest.DELTA);
    }
  }

  @Test
  public void distanceRemaining_equalsZeroAtEndOfStep() {

    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(4).getManeuver().asPosition()))
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(3)
      .legIndex(0)
      .build();

    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

    Assert.assertEquals(0, routeStepProgress.distanceRemaining(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(4).getManeuver().asPosition()))
      .stepDistanceRemaining(firstLeg.getSteps().get(5).getDistance())
      .legDistanceRemaining(firstLeg.getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();

    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    Assert.assertEquals(0, routeStepProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsCorrectValueAtIntervals() {
    LineString lineString = LineString.fromPolyline(firstStep.getGeometry(), Constants.PRECISION_6);

    double stepSegments = 5; // meters

    // Chop the line in small pieces
    for (double i = 0; i < firstStep.getDistance(); i += stepSegments) {
      Position position = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS).getCoordinates();

      LineString slicedLine = TurfMisc.lineSlice(Point.fromCoordinates(position),
        Point.fromCoordinates(route.getLegs().get(0).getSteps().get(1).getManeuver().asPosition()), lineString);

      double distance = TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);
      distance = firstStep.getDistance() - distance;
      if (distance < 0) {
        distance = 0;
      }

      RouteProgress routeProgress = RouteProgress.builder()
        .location(buildTestLocation(position))
        .stepDistanceRemaining(firstLeg.getSteps().get(0).getDistance() - distance)
        .legDistanceRemaining(firstLeg.getDistance())
        .distanceRemaining(route.getDistance())
        .directionsRoute(route)
        .stepIndex(0)
        .legIndex(0)
        .build();

      RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
      Assert.assertEquals(distance, routeStepProgress.distanceTraveled(), BaseTest.DELTA);
    }
  }

  @Test
  public void distanceTraveled_equalsStepDistanceAtEndOfStep() {

    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(4).getManeuver().asPosition()))
      .stepDistanceRemaining(0)
      .legDistanceRemaining(firstLeg.getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(3)
      .legIndex(0)
      .build();
    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    Assert.assertEquals(firstLeg.getSteps().get(3).getDistance(),
      routeStepProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(4).getManeuver().asPosition()))
      .stepDistanceRemaining(firstLeg.getSteps().get(4).getDistance())
      .legDistanceRemaining(firstLeg.getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();

    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    Assert.assertEquals(0, routeStepProgress.fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsCorrectValueAtIntervals() {
    LineString lineString
      = LineString.fromPolyline(firstStep.getGeometry(), Constants.PRECISION_6);

    double stepSegments = 5; // meters

    // Chop the line in small pieces
    for (double i = 0; i < firstStep.getDistance(); i += stepSegments) {
      Position position = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS).getCoordinates();

      LineString slicedLine = TurfMisc.lineSlice(Point.fromCoordinates(position),
        Point.fromCoordinates(route.getLegs().get(0).getSteps().get(1).getManeuver().asPosition()), lineString);

      double distance = TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);

      RouteProgress routeProgress = RouteProgress.builder()
        .location(buildTestLocation(position))
        .stepDistanceRemaining(distance)
        .legDistanceRemaining(firstLeg.getDistance())
        .distanceRemaining(route.getDistance())
        .directionsRoute(route)
        .stepIndex(0)
        .legIndex(0)
        .build();

      RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

      float fractionRemaining = (float) ((firstStep.getDistance() - distance) / firstStep.getDistance());
      if (fractionRemaining < 0) {
        fractionRemaining = 0;
      }
      Assert.assertEquals(fractionRemaining, routeStepProgress.fractionTraveled(), DELTA);
    }
  }

  @Test
  public void fractionTraveled_equalsOneAtEndOfStep() {

    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(4).getManeuver().asPosition()))
      .stepDistanceRemaining(0)
      .legDistanceRemaining(firstLeg.getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(3)
      .legIndex(0)
      .build();

    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    Assert.assertEquals(1.0, routeStepProgress.fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsStepDurationAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(4).getManeuver().asPosition()))
      .stepDistanceRemaining(firstLeg.getSteps().get(5).getDistance())
      .legDistanceRemaining(firstLeg.getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();

    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    Assert.assertEquals(41.5, routeStepProgress.durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsCorrectValueAtIntervals() {
    LineString lineString
      = LineString.fromPolyline(firstStep.getGeometry(), Constants.PRECISION_6);

    double stepSegments = 5; // meters

    // Chop the line in small pieces
    for (double i = 0; i < firstStep.getDistance(); i += stepSegments) {
      Position position = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS).getCoordinates();

      LineString slicedLine = TurfMisc.lineSlice(Point.fromCoordinates(position),
        Point.fromCoordinates(route.getLegs().get(0).getSteps().get(1).getManeuver().asPosition()), lineString);

      double distance = TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);

      RouteProgress routeProgress = RouteProgress.builder()
        .location(buildTestLocation(position))
        .stepDistanceRemaining(distance)
        .legDistanceRemaining(firstLeg.getDistance())
        .distanceRemaining(route.getDistance())
        .directionsRoute(route)
        .stepIndex(0)
        .legIndex(0)
        .build();

      RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
      double fractionRemaining = (firstStep.getDistance() - distance) / firstStep.getDistance();
      Assert.assertEquals((1.0 - fractionRemaining) * firstStep.getDuration(),
        routeStepProgress.durationRemaining(), BaseTest.LARGE_DELTA);
    }
  }

  @Test
  public void getDurationRemaining_equalsZeroAtEndOfStep() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(4).getManeuver().asPosition()))
      .stepDistanceRemaining(0)
      .legDistanceRemaining(firstLeg.getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(3)
      .legIndex(0)
      .build();
    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    Assert.assertEquals(0, routeStepProgress.durationRemaining(), BaseTest.DELTA);
  }
}