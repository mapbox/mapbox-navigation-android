package com.mapbox.services.android.navigation.v5.utils;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepManeuver;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static junit.framework.Assert.assertEquals;

public class MeasurementUtilsTest extends BaseTest {

  @Test
  public void userTrueDistanceFromStep_returnsZeroWhenCurrentStepAndPointEqualSame() {
    Point futurePoint = Point.fromLngLat(-95.367697, 29.758938);

    List<Point> geometryPoints = new ArrayList<>();
    geometryPoints.add(futurePoint);
    LegStep step = createTestStep(geometryPoints);

    double distance = MeasurementUtils.userTrueDistanceFromStep(futurePoint, step);
    assertEquals(0d, distance, DELTA);
  }

  @Test
  public void userTrueDistanceFromStep_onlyOnePointInLineStringStillMeasuresDistanceCorrectly() {
    Point futurePoint = Point.fromLngLat(-95.3676974, 29.7589382);

    List<Point> geometryPoints = new ArrayList<>();
    geometryPoints.add(Point.fromLngLat(-95.8427, 29.7757));
    LegStep step = createTestStep(geometryPoints);

    double distance = MeasurementUtils.userTrueDistanceFromStep(futurePoint, step);
    assertEquals(45900.73617999494, distance, DELTA);
  }

  @Test
  public void userTrueDistanceFromStep_onePointStepGeometryWithDifferentRawPoint() {
    Point futurePoint = Point.fromLngLat(-95.3676974, 29.7589382);

    List<Point> geometryPoints = new ArrayList<>();
    geometryPoints.add(Point.fromLngLat(-95.8427, 29.7757));
    geometryPoints.add(futurePoint);
    LegStep step = createTestStep(geometryPoints);

    double distance = MeasurementUtils.userTrueDistanceFromStep(futurePoint, step);
    assertEquals(0.04457271773629306d, distance, DELTA);
  }

  private LegStep createTestStep(List<Point> geometryPoints) {
    double[] location = {0d, 0d};
    StepManeuver maneuver = StepManeuver.builder()
      .rawLocation(location)
      .build();

    return LegStep.builder()
      .geometry(PolylineUtils.encode(geometryPoints, PRECISION_6))
      .mode("driving")
      .distance(2000d)
      .duration(1000d)
      .maneuver(maneuver)
      .weight(0d)
      .build();
  }
}