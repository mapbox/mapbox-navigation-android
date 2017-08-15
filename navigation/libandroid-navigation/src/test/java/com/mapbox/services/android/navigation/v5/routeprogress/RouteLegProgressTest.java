package com.mapbox.services.android.navigation.v5.routeprogress;

import com.google.gson.Gson;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.RouteLeg;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

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
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();
    assertNotNull("should not be null", routeProgress.currentLegProgress());
  }

  @Test
  public void upComingStep_returnsNextStepInLeg() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();
    assertTrue(routeProgress.currentLegProgress().upComingStep().getGeometry()
      .startsWith("so{gfA~}xpgFzOyNnRoOdVqXzLmQbDiGhKqQ|Vie@`X{g@dkAw{B~NcXhPoWlRmXfSeW|U"));
  }

  @Test
  public void upComingStep_returnsNull() {
    int lastStepIndex = firstLeg.getSteps().size() - 1;
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(lastStepIndex)
      .legIndex(0)
      .build();
    assertNull(routeProgress.currentLegProgress().upComingStep());
  }

  @Test
  public void currentStep_returnsCurrentStep() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();
    assertEquals(
      firstLeg.getSteps().get(5).getGeometry(), routeProgress.currentLegProgress().currentStep().getGeometry()
    );
    assertNotSame(
      firstLeg.getSteps().get(6).getGeometry(), routeProgress.currentLegProgress().currentStep().getGeometry()
    );
  }

  @Test
  public void previousStep_returnsPreviousStep() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();

    // TODO replace with equalsTo once https://github.com/mapbox/mapbox-java/pull/450 merged
    assertEquals(
      firstLeg.getSteps().get(4).getGeometry(), routeProgress.currentLegProgress().previousStep().getGeometry()
    );
    assertNotSame(
      firstLeg.getSteps().get(5).getGeometry(), routeProgress.currentLegProgress().previousStep().getGeometry()
    );
  }

  @Test
  public void stepIndex_returnsCurrentStepIndex() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(3)
      .legIndex(0)
      .build();

    assertEquals(3, routeProgress.currentLegProgress().stepIndex(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();

    assertEquals(0.0, routeProgress.currentLegProgress().fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsCorrectValueAtIntervals() {
    double stepSegments = 5000; // meters

    // Chop the line in small pieces
    for (double i = 0; i < firstLeg.getDistance(); i += stepSegments) {
      RouteProgress routeProgress = RouteProgress.builder()
        .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
        .legDistanceRemaining(route.getLegs().get(0).getDistance())
        .distanceRemaining(route.getDistance())
        .directionsRoute(route)
        .stepIndex(0)
        .legIndex(0)
        .build();

      float fractionRemaining = (float) (routeProgress.currentLegProgress().distanceTraveled()
        / firstLeg.getDistance());
      assertEquals(fractionRemaining, routeProgress.currentLegProgress().fractionTraveled(), BaseTest.DELTA);
    }
  }

  @Test
  public void fractionTraveled_equalsOneAtEndOfLeg() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(firstLeg.getSteps().size() - 1)
      .legIndex(0)
      .build();

    assertEquals(1.0, routeProgress.currentLegProgress().fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void distanceRemaining_equalsLegDistanceAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();

    assertEquals(firstLeg.getDistance(), routeProgress.currentLegProgress().distanceRemaining(),
      BaseTest.LARGE_DELTA);
  }

  @Test
  public void distanceRemaining_equalsZeroAtEndOfLeg() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(firstLeg.getSteps().size() - 1)
      .legIndex(0)
      .build();

    assertEquals(0, routeProgress.currentLegProgress().distanceRemaining(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();
    assertEquals(0, routeProgress.currentLegProgress().distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDistanceTraveled_equalsLegDistanceAtEndOfLeg() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(firstLeg.getSteps().size() - 1)
      .legIndex(0)
      .build();
    assertEquals(firstLeg.getDistance(), routeProgress.currentLegProgress().distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsLegDurationAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();
    assertEquals(3535.2, routeProgress.currentLegProgress().durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsZeroAtEndOfLeg() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(firstLeg.getSteps().size() - 1)
      .legIndex(0)
      .build();
    assertEquals(0, routeProgress.currentLegProgress().durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void followOnStep_doesReturnTwoStepsAheadOfCurrent() throws Exception {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();
    assertTrue(routeProgress.currentLegProgress().followOnStep().getGeometry()
      .startsWith("un`ffAz_dogFz`Aq^hF{B|GaB|HcAxKKlIp@lOzC|Dh@hKtAzFh@`FDvHy@bG{AjKaEfF"));
  }

  @Test
  public void followOnStep_returnsNull() {
    int lastStepIndex = firstLeg.getSteps().size() - 1;
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(lastStepIndex)
      .legIndex(0)
      .build();
    assertNull(routeProgress.currentLegProgress().followOnStep());
  }
}
