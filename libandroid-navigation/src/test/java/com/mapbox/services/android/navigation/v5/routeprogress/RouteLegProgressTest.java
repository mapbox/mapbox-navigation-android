package com.mapbox.services.android.navigation.v5.routeprogress;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.directions.v5.DirectionsAdapterFactory;
import com.mapbox.directions.v5.models.DirectionsResponse;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.directions.v5.models.RouteLeg;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.constants.Constants;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.io.IOException;

public class RouteLegProgressTest extends BaseTest {

  // Fixtures
  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";

  private RouteProgress routeProgress;
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

    routeProgress = RouteProgress.builder()
      .currentStepCoordinates(
        PolylineUtils.decode(route.legs().get(0).steps().get(0).geometry(), Constants.PRECISION_6))
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();
  }

  @Test
  public void sanityTest() {
    assertNotNull("should not be null", routeProgress.currentLegProgress());
  }

  @Test
  public void upComingStep_returnsNextStepInLeg() {
    RouteProgress newRouteProgress = routeProgress.toBuilder().stepIndex(5).build();
    assertTrue(newRouteProgress.currentLegProgress().upComingStep().geometry()
      .startsWith("so{gfA~}xpgFzOyNnRoOdVqXzLmQbDiGhKqQ|Vie@`X{g@dkAw{B~NcXhPoWlRmXfSeW|U"));
  }

  @Test
  public void upComingStep_returnsNull() {
    int lastStepIndex = firstLeg.steps().size() - 1;
    RouteProgress newRouteProgress = routeProgress.toBuilder().stepIndex(lastStepIndex).build();
    assertNull(newRouteProgress.currentLegProgress().upComingStep());
  }

  @Test
  public void currentStep_returnsCurrentStep() {
    RouteProgress newRouteProgress = routeProgress.toBuilder().stepIndex(5).build();
    assertEquals(
      firstLeg.steps().get(5).geometry(),
      newRouteProgress.currentLegProgress().currentStep().geometry()
    );
    assertNotSame(
      firstLeg.steps().get(6).geometry(),
      newRouteProgress.currentLegProgress().currentStep().geometry()
    );
  }

  @Test
  public void previousStep_returnsPreviousStep() {
    RouteProgress newRouteProgress = routeProgress.toBuilder().stepIndex(5).build();
    assertTrue(firstLeg.steps().get(4).geometry()
      .equals(newRouteProgress.currentLegProgress().previousStep().geometry()));
    assertFalse(firstLeg.steps().get(5).geometry()
      .equals(newRouteProgress.currentLegProgress().previousStep().geometry()));
  }

  @Test
  public void stepIndex_returnsCurrentStepIndex() {
    RouteProgress newRouteProgress = routeProgress.toBuilder().stepIndex(3).build();
    assertEquals(3, newRouteProgress.currentLegProgress().stepIndex(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsZeroAtBeginning() {
    assertEquals(0.0, routeProgress.currentLegProgress().fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsCorrectValueAtIntervals() {
    double stepSegments = 5000; // meters

    // Chop the line in small pieces
    for (double i = 0; i < firstLeg.distance(); i += stepSegments) {
      float fractionRemaining = (float) (routeProgress.currentLegProgress().distanceTraveled()
        / firstLeg.distance());
      assertEquals(fractionRemaining, routeProgress.currentLegProgress().fractionTraveled(), BaseTest.DELTA);
    }
  }

  @Test
  public void fractionTraveled_equalsOneAtEndOfLeg() {
    RouteProgress newRouteProgress = routeProgress.toBuilder()
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .stepIndex(firstLeg.steps().size() - 1)
      .build();

    assertEquals(1.0, newRouteProgress.currentLegProgress().fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void distanceRemaining_equalsLegDistanceAtBeginning() {
    assertEquals(firstLeg.distance(), routeProgress.currentLegProgress().distanceRemaining(),
      BaseTest.LARGE_DELTA);
  }

  @Test
  public void distanceRemaining_equalsZeroAtEndOfLeg() {
    RouteProgress newRouteProgress = routeProgress.toBuilder()
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(firstLeg.steps().size() - 1)
      .build();

    assertEquals(0, newRouteProgress.currentLegProgress().distanceRemaining(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsZeroAtBeginning() {
    assertEquals(0, routeProgress.currentLegProgress().distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDistanceTraveled_equalsLegDistanceAtEndOfLeg() {
    RouteProgress newRouteProgress = routeProgress.toBuilder()
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(firstLeg.steps().size() - 1)
      .build();
    assertEquals(firstLeg.distance(), newRouteProgress.currentLegProgress().distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsLegDurationAtBeginning() {
    assertEquals(3535.2, routeProgress.currentLegProgress().durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsZeroAtEndOfLeg() {
    RouteProgress newRouteProgress = routeProgress.toBuilder()
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(firstLeg.steps().size() - 1)
      .build();
    assertEquals(0, newRouteProgress.currentLegProgress().durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void followOnStep_doesReturnTwoStepsAheadOfCurrent() throws Exception {
    RouteProgress newRouteProgress = routeProgress.toBuilder().stepIndex(5).build();
    assertTrue(newRouteProgress.currentLegProgress().followOnStep().geometry()
      .startsWith("un`ffAz_dogFz`Aq^hF{B|GaB|HcAxKKlIp@lOzC|Dh@hKtAzFh@`FDvHy@bG{AjKaEfF"));
  }

  @Test
  public void followOnStep_returnsNull() {
    int lastStepIndex = firstLeg.steps().size() - 1;
    RouteProgress newRouteProgress = routeProgress.toBuilder().stepIndex(lastStepIndex).build();
    assertNull(newRouteProgress.currentLegProgress().followOnStep());
  }
}
