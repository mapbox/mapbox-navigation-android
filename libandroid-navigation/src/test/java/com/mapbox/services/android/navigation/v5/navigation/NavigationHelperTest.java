package com.mapbox.services.android.navigation.v5.navigation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegAnnotation;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.CurrentLegAnnotation;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class NavigationHelperTest extends BaseTest {

  private static final String ANNOTATED_DISTANCE_CONGESTION_ROUTE_FIXTURE = "directions_distance_congestion_annotation.json";

  @Test
  public void createCurrentAnnotation_nullAnnotationReturnsNull() {
    CurrentLegAnnotation currentLegAnnotation = NavigationHelper.createCurrentAnnotation(
      null, mock(RouteLeg.class), 0
    );

    assertEquals(null, currentLegAnnotation);
  }

  @Test
  public void createCurrentAnnotation_emptyDistanceArrayReturnsNull() {
    CurrentLegAnnotation currentLegAnnotation = buildCurrentAnnotation();
    RouteLeg routeLeg = buildRouteLegWithAnnotation();

    CurrentLegAnnotation newLegAnnotation = NavigationHelper.createCurrentAnnotation(
      currentLegAnnotation, routeLeg, 0
    );

    assertEquals(null, newLegAnnotation);
  }

  @Test
  public void createCurrentAnnotation_beginningOfStep_correctAnnotationIsReturned() throws Exception {
    RouteProgress routeProgress = buildDistanceCongestionAnnotationRouteProgress(0, 0, 0, 0, 0);
    Double legDistanceRemaining = routeProgress.currentLeg().distance();

    CurrentLegAnnotation newLegAnnotation = NavigationHelper.createCurrentAnnotation(
      null, routeProgress.currentLeg(), legDistanceRemaining
    );

    assertEquals("moderate", newLegAnnotation.congestion());
  }

  @Test
  public void createCurrentAnnotation_midStep_correctAnnotationIsReturned() throws Exception {
    RouteProgress routeProgress = buildDistanceCongestionAnnotationRouteProgress(0, 0, 0, 0, 0);
    Double legDistanceRemaining = routeProgress.currentLeg().distance() / 2;

    CurrentLegAnnotation newLegAnnotation = NavigationHelper.createCurrentAnnotation(
      null, routeProgress.currentLeg(), legDistanceRemaining
    );

    assertTrue(newLegAnnotation.distanceToAnnotation() < legDistanceRemaining);
    assertEquals("heavy", newLegAnnotation.congestion());
  }

  @Test
  public void createCurrentAnnotation_usesCurrentLegAnnotationForPriorDistanceTraveled() throws Exception {
    RouteProgress routeProgress = buildDistanceCongestionAnnotationRouteProgress(0, 0, 0, 0, 0);
    Double legDistanceRemaining = routeProgress.currentLeg().distance() / 2;
    Double previousAnnotationDistance = routeProgress.currentLeg().distance() / 3;
    CurrentLegAnnotation currentLegAnnotation = CurrentLegAnnotation.builder()
      .distance(100d)
      .distanceToAnnotation(previousAnnotationDistance)
      .index(0)
      .build();

    CurrentLegAnnotation newLegAnnotation = NavigationHelper.createCurrentAnnotation(
      currentLegAnnotation, routeProgress.currentLeg(), legDistanceRemaining
    );

    assertEquals(11, newLegAnnotation.index());
  }

  private RouteProgress buildDistanceCongestionAnnotationRouteProgress(double stepDistanceRemaining, double legDistanceRemaining,
                                                                       double distanceRemaining, int stepIndex, int legIndex) throws Exception {
    DirectionsRoute annotatedRoute = buildDistanceCongestionAnnotationRoute();
    return buildTestRouteProgress(annotatedRoute, stepDistanceRemaining,
      legDistanceRemaining, distanceRemaining, stepIndex, legIndex);
  }

  private DirectionsRoute buildDistanceCongestionAnnotationRoute() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(ANNOTATED_DISTANCE_CONGESTION_ROUTE_FIXTURE);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    return response.routes().get(0);
  }

  private CurrentLegAnnotation buildCurrentAnnotation() {
    return CurrentLegAnnotation.builder()
      .distance(54d)
      .distanceToAnnotation(100)
      .congestion("severe")
      .index(1)
      .build();
  }

  private RouteLeg buildRouteLegWithAnnotation() {
    RouteLeg routeLeg = mock(RouteLeg.class);
    LegAnnotation legAnnotation = LegAnnotation.builder()
      .distance(new ArrayList<Double>())
      .build();
    when(routeLeg.annotation()).thenReturn(legAnnotation);
    return routeLeg;
  }
}