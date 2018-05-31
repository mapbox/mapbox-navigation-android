package com.mapbox.services.android.navigation.v5.routeprogress;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class RouteStepProgressTest extends BaseTest {

  private static final String DCMAPBOX_CHIPOLTLE_FIXTURE = "dcmapbox_chipoltle.json";

  @Test
  public void sanityTest() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    double stepDistanceRemaining = route.legs().get(0).steps().get(0).distance();
    double legDistanceRemaining = route.legs().get(0).distance();
    double distanceRemaining = route.distance();
    RouteProgress routeProgress = buildTestRouteProgress(route, stepDistanceRemaining, legDistanceRemaining,
      distanceRemaining, 0, 0);

    assertNotNull(routeProgress.currentLegProgress().currentStepProgress());
  }

  @Test
  public void stepDistance_equalsZeroOnOneCoordSteps() throws Exception {
    DirectionsRoute route = loadChipotleTestRoute();
    int stepIndex = route.legs().get(0).steps().size() - 1;
    RouteProgress routeProgress = buildTestRouteProgress(route, 0, 0, 0, stepIndex, 0);
    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

    assertNotNull(routeStepProgress);
    assertEquals(1, routeStepProgress.fractionTraveled(), DELTA);
    assertEquals(0, routeStepProgress.distanceRemaining(), DELTA);
    assertEquals(0, routeStepProgress.distanceTraveled(), DELTA);
    assertEquals(0, routeStepProgress.durationRemaining(), DELTA);
  }

  @Test
  public void distanceRemaining_equalsStepDistanceAtBeginning() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    LineString lineString = LineString.fromPolyline(firstLeg.steps().get(5).geometry(), Constants.PRECISION_6);
    double stepDistance = TurfMeasurement.length(lineString, TurfConstants.UNIT_METERS);

    double stepDistanceRemaining = firstLeg.steps().get(5).distance();
    double legDistanceRemaining = firstLeg.distance();
    double distanceRemaining = route.distance();
    int stepIndex = 4;
    RouteProgress routeProgress = buildTestRouteProgress(route, stepDistanceRemaining,
      legDistanceRemaining, distanceRemaining, stepIndex, 0);
    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

    assertEquals(stepDistance, routeStepProgress.distanceRemaining(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void distanceRemaining_equalsCorrectValueAtIntervals() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    LegStep firstStep = route.legs().get(0).steps().get(0);
    LineString lineString = LineString.fromPolyline(firstStep.geometry(), Constants.PRECISION_6);
    double stepDistance = TurfMeasurement.length(lineString, TurfConstants.UNIT_METERS);
    double stepSegments = 5;

    for (double i = 0; i < stepDistance; i += stepSegments) {
      Point point = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS);

      if (point.equals(route.legs().get(0).steps().get(1).maneuver().location())) {
        return;
      }

      LineString slicedLine = TurfMisc.lineSlice(point,
        route.legs().get(0).steps().get(1).maneuver().location(), lineString);

      double stepDistanceRemaining = TurfMeasurement.length(slicedLine, TurfConstants.UNIT_METERS);
      double legDistanceRemaining = firstLeg.distance();
      double distanceRemaining = route.distance();
      RouteProgress routeProgress = buildTestRouteProgress(route, stepDistanceRemaining,
        legDistanceRemaining, distanceRemaining, 0, 0);
      RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

      assertEquals(stepDistanceRemaining, routeStepProgress.distanceRemaining(), BaseTest.DELTA);
    }
  }

  @Test
  public void distanceRemaining_equalsZeroAtEndOfStep() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteProgress routeProgress = buildTestRouteProgress(route, 0, 0, 0, 3, 0);
    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

    assertEquals(0, routeStepProgress.distanceRemaining(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsZeroAtBeginning() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    int stepIndex = 5;
    int legIndex = 0;
    double stepDistanceRemaining = firstLeg.steps().get(stepIndex).distance();
    double legDistanceRemaining = firstLeg.distance();
    double distanceRemaining = route.distance();
    RouteProgress routeProgress = buildTestRouteProgress(route, stepDistanceRemaining,
      legDistanceRemaining, distanceRemaining, stepIndex, legIndex);
    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

    assertEquals(0, routeStepProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsCorrectValueAtIntervals() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    LegStep firstStep = route.legs().get(0).steps().get(0);
    LineString lineString = LineString.fromPolyline(firstStep.geometry(), Constants.PRECISION_6);
    double stepSegments = 5;
    List<Double> distances = new ArrayList<>();
    List<Double> routeProgressDistancesTraveled = new ArrayList<>();

    for (double i = 0; i < firstStep.distance(); i += stepSegments) {
      Point point = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS);
      LineString slicedLine = TurfMisc.lineSlice(point,
        route.legs().get(0).steps().get(1).maneuver().location(), lineString);
      double distance = TurfMeasurement.length(slicedLine, TurfConstants.UNIT_METERS);
      distance = firstStep.distance() - distance;
      if (distance < 0) {
        distance = 0;
      }
      int stepIndex = 0;
      int legIndex = 0;
      double stepDistanceRemaining = firstLeg.steps().get(0).distance() - distance;
      double legDistanceRemaining = firstLeg.distance();
      double distanceRemaining = route.distance();
      RouteProgress routeProgress = buildTestRouteProgress(route, stepDistanceRemaining,
        legDistanceRemaining, distanceRemaining, stepIndex, legIndex);
      RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

      distances.add(distance);
      routeProgressDistancesTraveled.add(routeStepProgress.distanceTraveled());
    }

    assertTrue(distances.equals(routeProgressDistancesTraveled));
  }

  @Test
  public void distanceTraveled_equalsStepDistanceAtEndOfStep() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    int stepIndex = 3;
    int legIndex = 0;
    double stepDistanceRemaining = 0;
    double legDistanceRemaining = firstLeg.distance();
    double distanceRemaining = route.distance();
    RouteProgress routeProgress = buildTestRouteProgress(route, stepDistanceRemaining,
      legDistanceRemaining, distanceRemaining, stepIndex, legIndex);
    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

    assertEquals(firstLeg.steps().get(3).distance(),
      routeStepProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsZeroAtBeginning() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    int stepIndex = 5;
    int legIndex = 0;
    double stepDistanceRemaining = firstLeg.steps().get(stepIndex).distance();
    double legDistanceRemaining = firstLeg.distance();
    double distanceRemaining = route.distance();
    RouteProgress routeProgress = buildTestRouteProgress(route, stepDistanceRemaining,
      legDistanceRemaining, distanceRemaining, stepIndex, legIndex);

    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    assertEquals(0, routeStepProgress.fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsCorrectValueAtIntervals() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    LegStep firstStep = route.legs().get(0).steps().get(0);
    LineString lineString = LineString.fromPolyline(firstStep.geometry(), Constants.PRECISION_6);
    List<Float> fractionsRemaining = new ArrayList<>();
    List<Float> routeProgressFractionsTraveled = new ArrayList<>();
    double stepSegments = 5;

    for (double i = 0; i < firstStep.distance(); i += stepSegments) {
      Point point = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS);
      LineString slicedLine = TurfMisc.lineSlice(point,
        route.legs().get(0).steps().get(1).maneuver().location(), lineString);
      double stepDistanceRemaining = TurfMeasurement.length(slicedLine, TurfConstants.UNIT_METERS);
      int stepIndex = 0;
      int legIndex = 0;
      double legDistanceRemaining = firstLeg.distance();
      double distanceRemaining = route.distance();
      RouteProgress routeProgress = buildTestRouteProgress(route, stepDistanceRemaining,
        legDistanceRemaining, distanceRemaining, stepIndex, legIndex);
      RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
      float fractionRemaining = (float) ((firstStep.distance() - stepDistanceRemaining) / firstStep.distance());
      if (fractionRemaining < 0) {
        fractionRemaining = 0;
      }
      fractionsRemaining.add(fractionRemaining);
      routeProgressFractionsTraveled.add(routeStepProgress.fractionTraveled());
    }

    assertTrue(fractionsRemaining.equals(routeProgressFractionsTraveled));
  }

  @Test
  public void fractionTraveled_equalsOneAtEndOfStep() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    int stepIndex = 3;
    int legIndex = 0;
    double stepDistanceRemaining = 0;
    double legDistanceRemaining = firstLeg.distance();
    double distanceRemaining = route.distance();
    RouteProgress routeProgress = buildTestRouteProgress(route, stepDistanceRemaining,
      legDistanceRemaining, distanceRemaining, stepIndex, legIndex);
    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

    assertEquals(1.0, routeStepProgress.fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsStepDurationAtBeginning() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    LegStep fourthStep = firstLeg.steps().get(5);
    double stepDuration = fourthStep.duration();
    int stepIndex = 5;
    int legIndex = 0;
    double stepDistanceRemaining = firstLeg.steps().get(stepIndex).distance();
    double legDistanceRemaining = firstLeg.distance();
    double distanceRemaining = route.distance();
    RouteProgress routeProgress = buildTestRouteProgress(route, stepDistanceRemaining,
      legDistanceRemaining, distanceRemaining, stepIndex, legIndex);

    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    double durationRemaining = routeStepProgress.durationRemaining();

    assertEquals(stepDuration, durationRemaining, BaseTest.DELTA);
  }

  @Test
  public void durationRemaining_equalsCorrectValueAtIntervals() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    LegStep firstStep = route.legs().get(0).steps().get(0);
    LineString lineString = LineString.fromPolyline(firstStep.geometry(), Constants.PRECISION_6);
    double stepSegments = 5;
    List<Double> fractionsRemaining = new ArrayList<>();
    List<Double> routeProgressDurationsTraveled = new ArrayList<>();

    for (double i = 0; i < firstStep.distance(); i += stepSegments) {
      Point point = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS);
      LineString slicedLine = TurfMisc.lineSlice(point,
        route.legs().get(0).steps().get(1).maneuver().location(), lineString);
      int stepIndex = 0;
      int legIndex = 0;
      double stepDistanceRemaining = TurfMeasurement.length(slicedLine, TurfConstants.UNIT_METERS);
      double legDistanceRemaining = firstLeg.distance();
      double distanceRemaining = route.distance();
      RouteProgress routeProgress = buildTestRouteProgress(route, stepDistanceRemaining,
        legDistanceRemaining, distanceRemaining, stepIndex, legIndex);
      RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
      double fractionRemaining = (firstStep.distance() - stepDistanceRemaining) / firstStep.distance();

      double expectedFractionRemaining = (1.0 - fractionRemaining) * firstStep.duration();
      fractionsRemaining.add(Math.round(expectedFractionRemaining * 100.0) / 100.0);
      routeProgressDurationsTraveled.add(Math.round(routeStepProgress.durationRemaining() * 100.0) / 100.0);
    }
    double fractionRemaining = fractionsRemaining.get(fractionsRemaining.size() - 1);
    double routeProgressDuration = routeProgressDurationsTraveled.get(routeProgressDurationsTraveled.size() - 1);

    assertEquals(fractionRemaining, routeProgressDuration, BaseTest.DELTA);
  }

  @Test
  public void durationRemaining_equalsZeroAtEndOfStep() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    int stepIndex = 3;
    int legIndex = 0;
    double stepDistanceRemaining = 0;
    double legDistanceRemaining = firstLeg.distance();
    double distanceRemaining = route.distance();
    RouteProgress routeProgress = buildTestRouteProgress(route, stepDistanceRemaining,
      legDistanceRemaining, distanceRemaining, stepIndex, legIndex);
    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

    assertEquals(0, routeStepProgress.durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void stepIntersections_includesAllStepIntersectionsAndNextManeuver() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    int stepIndex = 3;
    int legIndex = 0;
    double stepDistanceRemaining = 0;
    double legDistanceRemaining = firstLeg.distance();
    double distanceRemaining = route.distance();
    RouteProgress routeProgress = buildTestRouteProgress(route, stepDistanceRemaining,
      legDistanceRemaining, distanceRemaining, stepIndex, legIndex);
    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();

    int stepIntersections = route.legs().get(0).steps().get(3).intersections().size();
    int intersectionSize = stepIntersections + 1;

    assertEquals(intersectionSize, routeStepProgress.intersections().size());
  }

  @Test
  public void stepIntersections_handlesNullNextManeuverCorrectly() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    int stepIndex = (route.legs().get(0).steps().size() - 1);
    int legIndex = 0;
    double stepDistanceRemaining = 0;
    double legDistanceRemaining = firstLeg.distance();
    double distanceRemaining = route.distance();
    RouteProgress routeProgress = buildTestRouteProgress(route, stepDistanceRemaining,
      legDistanceRemaining, distanceRemaining, stepIndex, legIndex);
    RouteStepProgress routeStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    int currentStepTotal = route.legs().get(0).steps().get(stepIndex).intersections().size();
    List<Point> lastStepLocation = PolylineUtils.decode(
      route.legs().get(0).steps().get(stepIndex).geometry(), Constants.PRECISION_6);

    assertEquals(currentStepTotal, routeStepProgress.intersections().size());
    assertEquals(routeStepProgress.intersections().get(0).location().latitude(), lastStepLocation.get(0).latitude());
    assertEquals(routeStepProgress.intersections().get(0).location().longitude(), lastStepLocation.get(0).longitude());
  }

  private DirectionsRoute loadChipotleTestRoute() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(DCMAPBOX_CHIPOLTLE_FIXTURE);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    return response.routes().get(0);
  }
}