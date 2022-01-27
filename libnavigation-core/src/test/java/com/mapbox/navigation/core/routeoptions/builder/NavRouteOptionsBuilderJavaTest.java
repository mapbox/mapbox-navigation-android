package com.mapbox.navigation.core.routeoptions.builder;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import kotlin.Unit;
import org.junit.Test;

import java.util.ArrayList;

import static com.mapbox.navigation.core.routeoptions.builder.NavRouteOptionsBuilderTestKt.testRouteOptionsSetup;
import static org.junit.Assert.assertEquals;

public class NavRouteOptionsBuilderJavaTest {
  @Test
  public void useOptionsBuilderFromJava() {
    RouteOptions options = testRouteOptionsSetup((builder) -> builder
        .fromCurrentLocation()
        .addIntermediateWaypoint(
            Point.fromLngLat(3.0, 3.0),
            "test name",
            null,
            null,
            null
        )
        .toDestination(
            Point.fromLngLat(4.0, 4.0),
            null,
            null,
            null,
            null
        )
        .profileDrivingTraffic((drivingSpecificSetup) -> {
          drivingSpecificSetup.exclude((drivingSpecificExclude) -> {
            drivingSpecificExclude.cashOnlyTolls();
            return Unit.INSTANCE;
          });
          return Unit.INSTANCE;
        })
    );

    assertEquals(3, options.coordinatesList().size());
    assertEquals(
        new ArrayList<String>() {{
          add(DirectionsCriteria.EXCLUDE_CASH_ONLY_TOLLS);
        }},
        options.excludeList()
    );
  }
}
