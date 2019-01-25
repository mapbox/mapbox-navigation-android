package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.exception.NavigationException;

import org.junit.Test;

import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

import static com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_DRIVING;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RouteRetrievalInfoTest extends BaseTest {

  private final double DISTANCE = 100;
  private final int STEP_COUNT = 5;
  private final int COORDINATE_COUNT = 2;
  private final String PROFILE = PROFILE_DRIVING;
  private final int NUMBER_OF_ROUTES = 1;
  private final boolean IS_OFFLINE = false;
  private final long ELAPSED_TIME = 1000;

  @Test(expected = NavigationException.class)
  public void errorThrownIfEndCalledBeforeStart() {
    RouteRetrievalInfo.builder().end();
  }

  @Test
  public void infoIsRetrievedFromRoute() {
    RouteRetrievalInfo routeRetrievalInfo = RouteRetrievalInfo.builder()
      .route(getMockedDirectionsRoute())
      .numberOfRoutes(NUMBER_OF_ROUTES)
      .isOffline(IS_OFFLINE)
      .elapsedTime(ELAPSED_TIME)
      .build();

    assertEquals(routeRetrievalInfo.distance(), DISTANCE, .1);
    assertEquals(routeRetrievalInfo.stepCount(), STEP_COUNT);
    assertEquals(routeRetrievalInfo.coordinateCount(), COORDINATE_COUNT);
    assertEquals(routeRetrievalInfo.profile(), PROFILE);
    assertEquals(routeRetrievalInfo.numberOfRoutes(), NUMBER_OF_ROUTES);
    assertEquals(routeRetrievalInfo.isOffline(), IS_OFFLINE);
    assertEquals(routeRetrievalInfo.elapsedTime(), ELAPSED_TIME);
  }

  private DirectionsRoute getMockedDirectionsRoute() {
    DirectionsRoute directionsRoute = mock(DirectionsRoute.class);
    when(directionsRoute.distance()).thenReturn(DISTANCE);
    RouteLeg routeLeg = mock(RouteLeg.class);
    List stepList = mock(List.class);
    when(stepList.size()).thenReturn(STEP_COUNT);
    when(routeLeg.steps()).thenReturn(stepList);
    when(directionsRoute.legs()).thenReturn(Collections.singletonList(routeLeg));
    RouteOptions routeOptions = mock(RouteOptions.class);
    List coordinatesList = mock(List.class);
    when(coordinatesList.size()).thenReturn(COORDINATE_COUNT);
    when(routeOptions.coordinates()).thenReturn(coordinatesList);
    when(routeOptions.profile()).thenReturn(PROFILE);
    when(directionsRoute.routeOptions()).thenReturn(routeOptions);
    return directionsRoute;
  }
}
