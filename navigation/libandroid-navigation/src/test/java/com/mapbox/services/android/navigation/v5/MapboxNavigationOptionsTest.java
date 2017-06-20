package com.mapbox.services.android.navigation.v5;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class MapboxNavigationOptionsTest extends BaseTest {

  @Test
  public void testSanity() {
    assertNotNull("should not be null", new MapboxNavigationOptions());
  }

  @Test
  public void testMaxTurnCompletionOffset() {
    assertEquals(
      new MapboxNavigationOptions().getMaxTurnCompletionOffset(),
      NavigationConstants.MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION,
      DELTA
    );
    assertEquals(
      new MapboxNavigationOptions().setMaxTurnCompletionOffset(100).getMaxTurnCompletionOffset(), 100, DELTA
    );
  }

  @Test
  public void testManeuverZoneRadius() {
    assertEquals(
      new MapboxNavigationOptions().getManeuverZoneRadius(), NavigationConstants.MANEUVER_ZONE_RADIUS, DELTA
    );
    assertEquals(new MapboxNavigationOptions().setManeuverZoneRadius(100).getManeuverZoneRadius(), 100, DELTA);
  }

  @Test
  public void testMaximumDistanceOffRoute() {
    assertEquals(
      new MapboxNavigationOptions().getMaximumDistanceOffRoute(),
      NavigationConstants.MAXIMUM_DISTANCE_BEFORE_OFF_ROUTE,
      DELTA
    );
    assertEquals(
      new MapboxNavigationOptions().setMaximumDistanceOffRoute(100).getMaximumDistanceOffRoute(),
      100,
      DELTA
    );
  }

  @Test
  public void testDeadReckoningTimeInterval() {
    assertEquals(
      new MapboxNavigationOptions().getDeadReckoningTimeInterval(),
      NavigationConstants.DEAD_RECKONING_TIME_INTERVAL,
      DELTA
    );
    assertEquals(
      new MapboxNavigationOptions().setDeadReckoningTimeInterval(100).getDeadReckoningTimeInterval(),
      100,
      DELTA
    );
  }

  @Test
  public void testMaxManipulatedCourseAngle() {
    assertEquals(
      new MapboxNavigationOptions().getMaxManipulatedCourseAngle(),
      NavigationConstants.MAX_MANIPULATED_COURSE_ANGLE,
      DELTA
    );
    assertEquals(
      new MapboxNavigationOptions().setMaxManipulatedCourseAngle(100).getMaxManipulatedCourseAngle(),
      100,
      DELTA
    );
  }
}
