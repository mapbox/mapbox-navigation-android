package com.mapbox.navigation.base.route;

import static com.mapbox.navigation.testing.factories.NavigationRouteFactoryKt.createNavigationRoute;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static java.util.stream.Collectors.groupingBy;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.navigation.base.extensions.RouteOptionsExtensions;
import com.mapbox.navigation.testing.FileUtils;
import com.mapbox.navigation.testing.NativeRouteParserRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class RouteExclusionsJavaTest {

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
        NavigationRoute navigationRoute = createNavigationRoute(
                directionsRoute
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
        NavigationRoute navigationRoute = createNavigationRoute(
                directionsRoute
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
        NavigationRoute navigationRoute = createNavigationRoute(
                directionsRoute
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
