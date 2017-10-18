package com.mapbox.services.android.navigation.v5.routeprogress;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.directions.v5.DirectionsAdapterFactory;
import com.mapbox.directions.v5.models.DirectionsResponse;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.directions.v5.models.RouteLeg;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.constants.Constants;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

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
  public void setup() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(PRECISION_6);
    response = gson.fromJson(body, DirectionsResponse.class);
    route = response.routes().get(0);
    firstStep = route.legs().get(0).steps().get(0);
    firstLeg = route.legs().get(0);
  }

  @Test
  public void sanityTest() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();
    assertNotNull("should not be null", routeProgress.currentLegProgress().currentStepProgress());
  }

  @Test
  public void stepDistance_equalsZeroOnOneCoordSteps() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(DCMAPBOX_CHIPOLTLE);
    response = gson.fromJson(body, DirectionsResponse.class);
    DirectionsRoute route = response.routes().get(0);

    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(route.legs().get(0).steps().size() - 1)
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
      = LineString.fromPolyline(firstLeg.steps().get(5).geometry(), Constants.PRECISION_6);
    double stepDistance = TurfMeasurement.lineDistance(lineString, TurfConstants.UNIT_METERS);

    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(firstLeg.steps().get(5).distance())
      .legDistanceRemaining(firstLeg.distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(4)
      .legIndex(0)
      .build();
    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

    assertEquals(stepDistance, routeStepProgress.distanceRemaining(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void distanceRemaining_equalsCorrectValueAtIntervals() {
    LineString lineString
      = LineString.fromPolyline(firstStep.geometry(), Constants.PRECISION_6);
    double stepDistance = TurfMeasurement.lineDistance(lineString, TurfConstants.UNIT_METERS);

    double stepSegments = 5; // meters

    // Chop the line in small pieces
    for (double i = 0; i < stepDistance; i += stepSegments) {
      Point point = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS);

      if (point.equals(route.legs().get(0).steps().get(1).maneuver().location())) {
        return;
      }

      LineString slicedLine = TurfMisc.lineSlice(point,
        route.legs().get(0).steps().get(1).maneuver().location(), lineString);

      double distance = TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);
      RouteProgress routeProgress = RouteProgress.builder()
        .stepDistanceRemaining(distance)
        .legDistanceRemaining(firstLeg.distance())
        .distanceRemaining(route.distance())
        .directionsRoute(route)
        .stepIndex(0)
        .legIndex(0)
        .build();
      RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
      assertEquals(distance, routeStepProgress.distanceRemaining(), BaseTest.DELTA);
    }
  }

  @Test
  public void distanceRemaining_equalsZeroAtEndOfStep() {

    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(3)
      .legIndex(0)
      .build();
    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

    assertEquals(0, routeStepProgress.distanceRemaining(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(firstLeg.steps().get(5).distance())
      .legDistanceRemaining(firstLeg.distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();

    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    assertEquals(0, routeStepProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsCorrectValueAtIntervals() {
    LineString lineString = LineString.fromPolyline(firstStep.geometry(), Constants.PRECISION_6);

    double stepSegments = 5; // meters

    // Chop the line in small pieces
    for (double i = 0; i < firstStep.distance(); i += stepSegments) {
      Point point = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS);

      LineString slicedLine = TurfMisc.lineSlice(point,
        route.legs().get(0).steps().get(1).maneuver().location(), lineString);

      double distance = TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);
      distance = firstStep.distance() - distance;
      if (distance < 0) {
        distance = 0;
      }

      RouteProgress routeProgress = RouteProgress.builder()
        .stepDistanceRemaining(firstLeg.steps().get(0).distance() - distance)
        .legDistanceRemaining(firstLeg.distance())
        .distanceRemaining(route.distance())
        .directionsRoute(route)
        .stepIndex(0)
        .legIndex(0)
        .build();

      RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
      assertEquals(distance, routeStepProgress.distanceTraveled(), BaseTest.DELTA);
    }
  }

  @Test
  public void distanceTraveled_equalsStepDistanceAtEndOfStep() {

    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(0)
      .legDistanceRemaining(firstLeg.distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(3)
      .legIndex(0)
      .build();
    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    assertEquals(firstLeg.steps().get(3).distance(),
      routeStepProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(firstLeg.steps().get(4).distance())
      .legDistanceRemaining(firstLeg.distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();

    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    assertEquals(0, routeStepProgress.fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsCorrectValueAtIntervals() {
    LineString lineString
      = LineString.fromPolyline(firstStep.geometry(), Constants.PRECISION_6);

    double stepSegments = 5; // meters

    // Chop the line in small pieces
    for (double i = 0; i < firstStep.distance(); i += stepSegments) {
      Point point = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS);

      LineString slicedLine = TurfMisc.lineSlice(point,
        route.legs().get(0).steps().get(1).maneuver().location(), lineString);

      double distance = TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);

      RouteProgress routeProgress = RouteProgress.builder()
        .stepDistanceRemaining(distance)
        .legDistanceRemaining(firstLeg.distance())
        .distanceRemaining(route.distance())
        .directionsRoute(route)
        .stepIndex(0)
        .legIndex(0)
        .build();

      RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

      float fractionRemaining = (float) ((firstStep.distance() - distance) / firstStep.distance());
      if (fractionRemaining < 0) {
        fractionRemaining = 0;
      }
      assertEquals(fractionRemaining, routeStepProgress.fractionTraveled(), DELTA);
    }
  }

  @Test
  public void fractionTraveled_equalsOneAtEndOfStep() {

    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(0)
      .legDistanceRemaining(firstLeg.distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(3)
      .legIndex(0)
      .build();

    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    assertEquals(1.0, routeStepProgress.fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsStepDurationAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(firstLeg.steps().get(5).distance())
      .legDistanceRemaining(firstLeg.distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();

    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    assertEquals(41.5, routeStepProgress.durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsCorrectValueAtIntervals() {
    LineString lineString
      = LineString.fromPolyline(firstStep.geometry(), Constants.PRECISION_6);

    double stepSegments = 5; // meters

    // Chop the line in small pieces
    for (double i = 0; i < firstStep.distance(); i += stepSegments) {
      Point point = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS);

      LineString slicedLine = TurfMisc.lineSlice(point,
        route.legs().get(0).steps().get(1).maneuver().location(), lineString);

      double distance = TurfMeasurement.lineDistance(slicedLine, TurfConstants.UNIT_METERS);

      RouteProgress routeProgress = RouteProgress.builder()
        .stepDistanceRemaining(distance)
        .legDistanceRemaining(firstLeg.distance())
        .distanceRemaining(route.distance())
        .directionsRoute(route)
        .stepIndex(0)
        .legIndex(0)
        .build();

      RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
      double fractionRemaining = (firstStep.distance() - distance) / firstStep.distance();
      Assert.assertEquals((1.0 - fractionRemaining) * firstStep.duration(),
        routeStepProgress.durationRemaining(), BaseTest.LARGE_DELTA);
    }

  }

  @Test
  public void getDurationRemaining_equalsZeroAtEndOfStep() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(0)
      .legDistanceRemaining(firstLeg.distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(3)
      .legIndex(0)
      .build();
    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    assertEquals(0, routeStepProgress.durationRemaining(), BaseTest.DELTA);
  }
}