package com.mapbox.navigation.base.route;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.navigation.base.extensions.RouteOptionsExtensions;
import com.mapbox.navigation.testing.FileUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RouteExclusionsJavaTest {

  @Test
  public void routeOptionsBuilderExcludeParsing() {
    Point origin = Point.fromLngLat(14.75513115258181, 55.19464648744247);
    Point destination = Point.fromLngLat(12.54071010365584, 55.68521471271404);
    List<Point> coordinates = new ArrayList<>();
    coordinates.add(origin);
    coordinates.add(destination);
    RouteOptions.Builder routeOptionsBuilder =
        RouteOptionsExtensions.applyDefaultNavigationOptions(
            RouteOptions.builder()
                .coordinatesList(coordinates)
                .accessToken("pk.123")
        );

    RouteOptions routeOptionsWithExclusions = RouteExclusions.exclude(
        routeOptionsBuilder,
        DirectionsCriteria.EXCLUDE_TOLL,
        DirectionsCriteria.EXCLUDE_FERRY
    ).build();

    assertEquals("toll,ferry", routeOptionsWithExclusions.exclude());
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
                .accessToken("pk.123")
        ).build();
    DirectionsRoute directionsRoute = DirectionsRoute.builder()
        .routeOptions(routeOptionsWithoutExclusions)
        .distance(183888.609)
        .duration(10697.573)
        .build();

    List<ExclusionViolation> exclusionViolations =
        RouteExclusions.exclusionViolations(directionsRoute);

    assertEquals(0, exclusionViolations.size());
  }

  @Test
  public void tollAndFerryExclusionViolationsSize() {
    DirectionsRoute directionsRoute = DirectionsRoute.fromJson(
        FileUtils.INSTANCE.loadJsonFixture("toll_and_ferry_directions_route.json")
    );

    List<ExclusionViolation> exclusionViolations =
        RouteExclusions.exclusionViolations(directionsRoute);

    assertEquals(77, exclusionViolations.size());
  }

  @Test
  public void tollAndFerryExclusionViolationsType() {
    DirectionsRoute directionsRoute = DirectionsRoute.fromJson(
        FileUtils.INSTANCE.loadJsonFixture("toll_and_ferry_directions_route.json")
    );

    Map<String, List<ExclusionViolation>> tollAndFerryExclusionViolations =
        RouteExclusions.exclusionViolations(directionsRoute)
            .stream()
            .collect(groupingBy(ExclusionViolation::getType));

    assertEquals(2, tollAndFerryExclusionViolations.size());
    assertTrue(tollAndFerryExclusionViolations.containsKey("toll"));
    assertTrue(tollAndFerryExclusionViolations.containsKey("ferry"));
  }
}
