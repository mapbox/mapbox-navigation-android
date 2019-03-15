package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.junit.Test;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RouteHandlerTest extends BaseTest {

  @Test
  public void updateRoute_newSetsRouteJson() throws IOException {
    DirectionsRoute newRoute = buildTestDirectionsRoute();
    MapboxNavigator mapboxNavigator = mock(MapboxNavigator.class);
    RouteHandler routeHandler = new RouteHandler(mapboxNavigator);

    routeHandler.updateRoute(newRoute, DirectionsRouteType.NEW_ROUTE);

    verify(mapboxNavigator).setRoute(eq(newRoute.toJson()), eq(0), eq(0));
  }

  @Test
  public void updateRoute_freshRouteUpdatesAnnotationJson() throws IOException {
    DirectionsRoute freshRoute = buildTestDirectionsRoute("directions_two_leg_route.json");
    MapboxNavigator mapboxNavigator = mock(MapboxNavigator.class);
    RouteHandler routeHandler = new RouteHandler(mapboxNavigator);

    routeHandler.updateRoute(freshRoute, DirectionsRouteType.FRESH_ROUTE);

    String firstLegAnnotationJson = freshRoute.legs().get(0).annotation().toJson();
    verify(mapboxNavigator).updateAnnotations(eq(firstLegAnnotationJson), eq(0), eq(0));
    String secondLegAnnotationJson = freshRoute.legs().get(1).annotation().toJson();
    verify(mapboxNavigator).updateAnnotations(eq(secondLegAnnotationJson), eq(0), eq(1));
  }
}