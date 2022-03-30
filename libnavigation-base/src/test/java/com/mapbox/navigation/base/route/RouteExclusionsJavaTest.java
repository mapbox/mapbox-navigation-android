package com.mapbox.navigation.base.route;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static java.util.stream.Collectors.groupingBy;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.bindgen.ExpectedFactory;
import com.mapbox.geojson.Point;
import com.mapbox.navigation.base.extensions.RouteOptionsExtensions;
import com.mapbox.navigation.base.internal.SDKRouteParser;
import com.mapbox.navigation.base.internal.route.NavigationRouteEx;
import com.mapbox.navigation.testing.FileUtils;
import com.mapbox.navigator.RouteInterface;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class RouteExclusionsJavaTest {

  @Mock
  private SDKRouteParser parser;

  @Before
  public void setup() {
    List<RouteInterface> nativeRoutes = new ArrayList<>();
    RouteInterface nativeRoute = Mockito.mock(RouteInterface.class);
    Mockito.doReturn("route_id").when(nativeRoute).getRouteId();
    nativeRoutes.add(nativeRoute);
    Mockito.doReturn(
        ExpectedFactory.createValue(nativeRoutes)
    ).when(parser).parseDirectionsResponse(
        ArgumentMatchers.anyString(), ArgumentMatchers.anyString()
    );
  }

  @Test
  public void emptyExclusionViolationsIfNoExcludeRouteOptionsAdded() {
    Point origin = Point.fromLngLat(14.75513115258181, 55.19464648744247);
    Point destination = Point.fromLngLat(12.54071010365584, 55.68521471271404);
    List<Point> coordinates = new ArrayList<>();
    coordinates.add(origin);
    coordinates.add(destination);
    RouteOptions routeOptionsWithoutExclusions =
        RouteOptionsExtensions.applyDefaultNavigationOptions(
            RouteOptions.builder()
                .coordinatesList(coordinates)
        ).build();
    DirectionsRoute directionsRoute = DirectionsRoute.builder()
        .routeIndex("0")
        .routeOptions(routeOptionsWithoutExclusions)
        .distance(183888.609)
        .duration(10697.573)
        .build();
    NavigationRoute navigationRoute = NavigationRouteEx.createNavigationRoute(
        directionsRoute, parser
    );

    List<ExclusionViolation> exclusionViolations =
        RouteExclusions.exclusionViolations(navigationRoute);

    assertEquals(0, exclusionViolations.size());
  }

  @Test
  public void tollAndFerryExclusionViolationsSize() {
    DirectionsRoute directionsRoute = DirectionsRoute.fromJson(
        FileUtils.INSTANCE.loadJsonFixture("toll_and_ferry_directions_route.json")
    );
    NavigationRoute navigationRoute = NavigationRouteEx.createNavigationRoute(
        directionsRoute, parser
    );

    List<ExclusionViolation> exclusionViolations =
        RouteExclusions.exclusionViolations(navigationRoute);

    assertEquals(77, exclusionViolations.size());
  }

  @Test
  public void tollAndFerryExclusionViolationsType() {
    DirectionsRoute directionsRoute = DirectionsRoute.fromJson(
        FileUtils.INSTANCE.loadJsonFixture("toll_and_ferry_directions_route.json")
    );
    NavigationRoute navigationRoute = NavigationRouteEx.createNavigationRoute(
        directionsRoute, parser
    );

    Map<String, List<ExclusionViolation>> tollAndFerryExclusionViolations =
        RouteExclusions.exclusionViolations(navigationRoute)
            .stream()
            .collect(groupingBy(ExclusionViolation::getType));

    assertEquals(2, tollAndFerryExclusionViolations.size());
    assertTrue(tollAndFerryExclusionViolations.containsKey("toll"));
    assertTrue(tollAndFerryExclusionViolations.containsKey("ferry"));
  }
}
