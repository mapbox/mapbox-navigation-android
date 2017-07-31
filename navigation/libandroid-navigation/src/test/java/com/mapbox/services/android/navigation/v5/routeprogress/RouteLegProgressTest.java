package com.mapbox.services.android.navigation.v5.routeprogress;

import com.google.gson.Gson;
import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.v5.BaseTest;
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

public class RouteLegProgressTest extends BaseTest {

  // Fixtures
  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";

  private DirectionsRoute route;
  private RouteLeg firstLeg;

  @Before
  public void setup() {
    Gson gson = new Gson();
    String body = readPath(DIRECTIONS_PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    route = response.getRoutes().get(0);
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
    Assert.assertNotNull("should not be null", routeProgress.currentLegProgress());
  }

  @Test
  public void upComingStep_returnsNextStepInLeg() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(4).getManeuver().asPosition()))
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();
    Assert.assertTrue(routeProgress.currentLegProgress().upComingStep().getGeometry()
      .startsWith("so{gfA~}xpgFzOyNnRoOdVqXzLmQbDiGhKqQ|Vie@`X{g@dkAw{B~NcXhPoWlRmXfSeW|U"));
  }

  @Test
  public void upComingStep_returnsNull() {
    int lastStepIndex = firstLeg.getSteps().size() - 1;
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(lastStepIndex - 2).getManeuver().asPosition()))
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(lastStepIndex)
      .legIndex(0)
      .build();
    Assert.assertNull(routeProgress.currentLegProgress().upComingStep());
  }

  @Test
  public void currentStep_returnsCurrentStep() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(4).getManeuver().asPosition()))
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();
    Assert.assertEquals(
      firstLeg.getSteps().get(5).getGeometry(), routeProgress.currentLegProgress().currentStep().getGeometry()
    );
    Assert.assertNotSame(
      firstLeg.getSteps().get(6).getGeometry(), routeProgress.currentLegProgress().currentStep().getGeometry()
    );
  }

  @Test
  public void previousStep_returnsPreviousStep() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(4).getManeuver().asPosition()))
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();

    // TODO replace with equalsTo once https://github.com/mapbox/mapbox-java/pull/450 merged
    Assert.assertEquals(
      firstLeg.getSteps().get(4).getGeometry(), routeProgress.currentLegProgress().previousStep().getGeometry()
    );
    Assert.assertNotSame(
      firstLeg.getSteps().get(5).getGeometry(), routeProgress.currentLegProgress().previousStep().getGeometry()
    );
  }

  @Test
  public void stepIndex_returnsCurrentStepIndex() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(4).getManeuver().asPosition()))
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(3)
      .legIndex(0)
      .build();

    Assert.assertEquals(3, routeProgress.currentLegProgress().stepIndex(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(0).getManeuver().asPosition()))
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();

    Assert.assertEquals(0.0, routeProgress.currentLegProgress().fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsCorrectValueAtIntervals() {
    double stepSegments = 5000; // meters

    // Chop the line in small pieces
    LineString lineString = LineString.fromPolyline(route.getGeometry(), Constants.PRECISION_6);
    for (double i = 0; i < firstLeg.getDistance(); i += stepSegments) {
      Position position = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_METERS).getCoordinates();

      RouteProgress routeProgress = RouteProgress.builder()
        .location(buildTestLocation(position))
        .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
        .legDistanceRemaining(route.getLegs().get(0).getDistance())
        .distanceRemaining(route.getDistance())
        .directionsRoute(route)
        .stepIndex(0)
        .legIndex(0)
        .build();

      float fractionRemaining = (float) (routeProgress.currentLegProgress().distanceTraveled() / firstLeg.getDistance());
      Assert.assertEquals(fractionRemaining, routeProgress.currentLegProgress().fractionTraveled(), BaseTest.DELTA);
    }
  }

  @Test
  public void fractionTraveled_equalsOneAtEndOfLeg() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(firstLeg.getSteps().size() - 1).getManeuver().asPosition()))
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(firstLeg.getSteps().size() - 1)
      .legIndex(0)
      .build();

    Assert.assertEquals(1.0, routeProgress.currentLegProgress().fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void distanceRemaining_equalsLegDistanceAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(0).getManeuver().asPosition()))
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();

    Assert.assertEquals(firstLeg.getDistance(), routeProgress.currentLegProgress().distanceRemaining(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void distanceRemaining_equalsZeroAtEndOfLeg() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(firstLeg.getSteps().size() - 1).getManeuver().asPosition()))
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(firstLeg.getSteps().size() - 1)
      .legIndex(0)
      .build();

    Assert.assertEquals(0, routeProgress.currentLegProgress().distanceRemaining(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(0).getManeuver().asPosition()))
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();
    Assert.assertEquals(0, routeProgress.currentLegProgress().distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDistanceTraveled_equalsLegDistanceAtEndOfLeg() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(firstLeg.getSteps().size() - 1).getManeuver().asPosition()))
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(firstLeg.getSteps().size() - 1)
      .legIndex(0)
      .build();
    Assert.assertEquals(firstLeg.getDistance(), routeProgress.currentLegProgress().distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsLegDurationAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(0).getManeuver().asPosition()))
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();
    Assert.assertEquals(3535.2, routeProgress.currentLegProgress().durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsZeroAtEndOfLeg() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstLeg.getSteps().get(firstLeg.getSteps().size() - 1).getManeuver().asPosition()))
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(firstLeg.getSteps().size() - 1)
      .legIndex(0)
      .build();
    Assert.assertEquals(0, routeProgress.currentLegProgress().durationRemaining(), BaseTest.DELTA);
  }
}
