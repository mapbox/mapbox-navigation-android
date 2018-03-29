package com.mapbox.services.android.navigation.v5.routeprogress;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class RouteLegProgressTest extends BaseTest {

  private DirectionsRoute route;
  private RouteProgress routeProgress;
  private RouteLeg firstLeg;

  @Before
  public void setup() throws Exception {
    route = buildDirectionsRoute();
    firstLeg = route.legs().get(0);
    routeProgress = buildDefaultRouteProgress();
  }

  @Test
  public void sanityTest() {
    assertNotNull("should not be null", routeProgress.currentLegProgress());
  }

  @Test
  public void upComingStep_returnsNextStepInLeg() {
    routeProgress = routeProgress.toBuilder().stepIndex(5).build();
    assertTrue(routeProgress.currentLegProgress().upComingStep().geometry()
      .startsWith("so{gfA~}xpgFzOyNnRoOdVqXzLmQbDiGhKqQ|Vie@`X{g@dkAw{B~NcXhPoWlRmXfSeW|U"));
  }

  @Test
  public void upComingStep_returnsNull() {
    int lastStepIndex = firstLeg.steps().size() - 1;
    routeProgress = routeProgress.toBuilder().stepIndex(lastStepIndex).build();
    assertNull(routeProgress.currentLegProgress().upComingStep());
  }

  @Test
  public void currentStep_returnsCurrentStep() {
    routeProgress = routeProgress.toBuilder().stepIndex(5).build();
    assertEquals(
      firstLeg.steps().get(5).geometry(), routeProgress.currentLegProgress().currentStep().geometry()
    );
    assertNotSame(
      firstLeg.steps().get(6).geometry(), routeProgress.currentLegProgress().currentStep().geometry()
    );
  }

  @Test
  public void previousStep_returnsPreviousStep() {
    routeProgress = routeProgress.toBuilder().stepIndex(5).build();
    assertEquals(
      firstLeg.steps().get(4).geometry(), routeProgress.currentLegProgress().previousStep().geometry()
    );
    assertNotSame(
      firstLeg.steps().get(5).geometry(), routeProgress.currentLegProgress().previousStep().geometry()
    );
  }

  @Test
  public void stepIndex_returnsCurrentStepIndex() {
    routeProgress = routeProgress.toBuilder().stepIndex(3).build();

    assertEquals(3, routeProgress.currentLegProgress().stepIndex(), BaseTest.DELTA);
  }

  @Test
  public void fractionTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress();

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
    RouteProgress routeProgress = buildEndOfLegRouteProgress();

    assertEquals(1.0, routeProgress.currentLegProgress().fractionTraveled(), BaseTest.DELTA);
  }

  @Test
  public void distanceRemaining_equalsLegDistanceAtBeginning() {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress();

    assertEquals(firstLeg.distance(), routeProgress.currentLegProgress().distanceRemaining(),
      BaseTest.LARGE_DELTA);
  }

  @Test
  public void distanceRemaining_equalsZeroAtEndOfLeg() {
    RouteProgress routeProgress = buildEndOfLegRouteProgress();

    assertEquals(0, routeProgress.currentLegProgress().distanceRemaining(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress();

    assertEquals(0, routeProgress.currentLegProgress().distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDistanceTraveled_equalsLegDistanceAtEndOfLeg() {
    RouteProgress routeProgress = buildEndOfLegRouteProgress();

    assertEquals(firstLeg.distance(), routeProgress.currentLegProgress().distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsLegDurationAtBeginning() {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress();

    assertEquals(3535.2, routeProgress.currentLegProgress().durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void getDurationRemaining_equalsZeroAtEndOfLeg() {
    int lastStepIndex = firstLeg.steps().size() - 1;
    routeProgress = routeProgress.toBuilder().stepIndex(lastStepIndex).build();

    assertEquals(0, routeProgress.currentLegProgress().durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void followOnStep_doesReturnTwoStepsAheadOfCurrent() throws Exception {
    routeProgress = routeProgress.toBuilder().stepIndex(5).build();

    assertTrue(routeProgress.currentLegProgress().followOnStep().geometry()
      .startsWith("un`ffAz_dogFz`Aq^hF{B|GaB|HcAxKKlIp@lOzC|Dh@hKtAzFh@`FDvHy@bG{AjKaEfF"));
  }

  @Test
  public void followOnStep_returnsNull() {
    int lastStepIndex = firstLeg.steps().size() - 1;
    routeProgress = routeProgress.toBuilder().stepIndex(lastStepIndex).build();

    assertNull(routeProgress.currentLegProgress().followOnStep());
  }

  private RouteProgress buildBeginningOfLegRouteProgress() {
    List<Point> currentStepPoints = PolylineUtils.decode(
      route.legs().get(0).steps().get(0).geometry(), PRECISION_6
    );
    return RouteProgress.builder()
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .currentStepPoints(currentStepPoints)
      .stepIndex(0)
      .legIndex(0)
      .build();
  }

  private RouteProgress buildEndOfLegRouteProgress() {
    int index = firstLeg.steps().size() - 1;
    List<Point> currentStepPoints = PolylineUtils.decode(
      route.legs().get(0).steps().get(index).geometry(), PRECISION_6
    );
    return RouteProgress.builder()
      .stepDistanceRemaining(0)
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .directionsRoute(route)
      .currentStepPoints(currentStepPoints)
      .stepIndex(index)
      .legIndex(0)
      .build();
  }
}
