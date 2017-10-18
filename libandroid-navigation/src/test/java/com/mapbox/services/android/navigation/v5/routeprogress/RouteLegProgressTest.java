package com.mapbox.services.android.navigation.v5.routeprogress;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.directions.v5.DirectionsAdapterFactory;
import com.mapbox.directions.v5.models.DirectionsResponse;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.io.IOException;

public class RouteLegProgressTest extends BaseTest {

  // Fixtures
  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";

  private DirectionsRoute route;
  private RouteLeg firstLeg;

  @Before
  public void setup() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(DIRECTIONS_PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    route = response.routes().get(0);
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
    assertNotNull("should not be null", routeProgress.currentLegProgress());
  }

  @Test
  public void upComingStep_returnsNextStepInLeg() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();
    assertTrue(routeProgress.currentLegProgress().upComingStep().geometry()
      .startsWith("so{gfA~}xpgFzOyNnRoOdVqXzLmQbDiGhKqQ|Vie@`X{g@dkAw{B~NcXhPoWlRmXfSeW|U"));
  }

  @Test
  public void upComingStep_returnsNull() {
    int lastStepIndex = firstLeg.steps().size() - 1;
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(lastStepIndex)
      .legIndex(0)
      .build();
    assertNull(routeProgress.currentLegProgress().upComingStep());
  }

  @Test
  public void currentStep_returnsCurrentStep() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();
    assertEquals(
      firstLeg.steps().get(5).geometry(), routeProgress.currentLegProgress().currentStep().geometry()
    );
    assertNotSame(
      firstLeg.steps().get(6).geometry(), routeProgress.currentLegProgress().currentStep().geometry()
    );
  }

  @Test
  public void previousStep_returnsPreviousStep() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();

    // TODO replace with equalsTo once https://github.com/mapbox/mapbox-java/pull/450 merged
    assertEquals(
      firstLeg.steps().get(4).geometry(), routeProgress.currentLegProgress().previousStep().geometry()
    );
    assertNotSame(
      firstLeg.steps().get(5).geometry(), routeProgress.currentLegProgress().previousStep().geometry()
    );
  }

  @Test
  public void stepIndex_returnsCurrentStepIndex() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(3)
      .legIndex(0)
      .build();

    assertEquals(3, routeProgress.currentLegProgress().stepIndex(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
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
    for (double i = 0; i < firstLeg.distance(); i += stepSegments) {
      RouteProgress routeProgress = RouteProgress.builder()
        .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
        .legDistanceRemaining(route.legs().get(0).distance())
        .distanceRemaining(route.distance())
        .directionsRoute(route)
        .stepIndex(0)
        .legIndex(0)
        .build();

      float fractionRemaining = (float) (routeProgress.currentLegProgress().distanceTraveled()
        / firstLeg.distance());
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
      .stepIndex(firstLeg.steps().size() - 1)
      .legIndex(0)
      .build();

    assertEquals(1.0, routeProgress.currentLegProgress().fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void distanceRemaining_equalsLegDistanceAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();

    assertEquals(firstLeg.distance(), routeProgress.currentLegProgress().distanceRemaining(),
      BaseTest.LARGE_DELTA);
  }

  @Test
  public void distanceRemaining_equalsZeroAtEndOfLeg() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(firstLeg.steps().size() - 1)
      .legIndex(0)
      .build();

    assertEquals(0, routeProgress.currentLegProgress().distanceRemaining(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
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
      .stepIndex(firstLeg.steps().size() - 1)
      .legIndex(0)
      .build();
    assertEquals(firstLeg.distance(), routeProgress.currentLegProgress().distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsLegDurationAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
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
      .stepIndex(firstLeg.steps().size() - 1)
      .legIndex(0)
      .build();
    assertEquals(0, routeProgress.currentLegProgress().durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void followOnStep_doesReturnTwoStepsAheadOfCurrent() throws Exception {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(5)
      .legIndex(0)
      .build();
    assertTrue(routeProgress.currentLegProgress().followOnStep().geometry()
      .startsWith("un`ffAz_dogFz`Aq^hF{B|GaB|HcAxKKlIp@lOzC|Dh@hKtAzFh@`FDvHy@bG{AjKaEfF"));
  }

  @Test
  public void followOnStep_returnsNull() {
    int lastStepIndex = firstLeg.steps().size() - 1;
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(lastStepIndex)
      .legIndex(0)
      .build();
    assertNull(routeProgress.currentLegProgress().followOnStep());
  }
}
