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
  public void testMediumAlertInterval() {
    assertEquals(
      new MapboxNavigationOptions().getMediumAlertInterval(), NavigationConstants.MEDIUM_ALERT_INTERVAL, DELTA
    );
    assertEquals(new MapboxNavigationOptions().setMediumAlertInterval(100).getMediumAlertInterval(), 100, DELTA);
  }

  @Test
  public void testHighAlertInterval() {
    assertEquals(
      new MapboxNavigationOptions().getHighAlertInterval(),
      NavigationConstants.HIGH_ALERT_INTERVAL,
      DELTA
    );
    assertEquals(
      new MapboxNavigationOptions().setHighAlertInterval(100).getHighAlertInterval(),
      100,
      DELTA
    );
  }

  @Test
  public void testMinimumMediumAlertDistanceDriving() {
    assertEquals(
      new MapboxNavigationOptions().getMinimumMediumAlertDistanceDriving(),
      NavigationConstants.MINIMUM_DISTANCE_FOR_MEDIUM_ALERT_DRIVING,
      DELTA
    );
    assertEquals(
      new MapboxNavigationOptions().setMinimumMediumAlertDistanceDriving(100).getMinimumMediumAlertDistanceDriving(),
      100,
      DELTA
    );
  }

  @Test
  public void testMinimumMediumAlertDistanceCycling() {
    assertEquals(
      new MapboxNavigationOptions().getMinimumMediumAlertDistanceCycling(),
      NavigationConstants.MINIMUM_DISTANCE_FOR_MEDIUM_ALERT_CYCLING,
      DELTA
    );
    assertEquals(
      new MapboxNavigationOptions().setMinimumMediumAlertDistanceCycling(100).getMinimumMediumAlertDistanceCycling(),
      100,
      DELTA
    );
  }

  @Test
  public void testMinimumMediumAlertDistanceWalking() {
    assertEquals(
      new MapboxNavigationOptions().getMinimumMediumAlertDistanceWalking(),
      NavigationConstants.MINIMUM_DISTANCE_FOR_MEDIUM_ALERT_WALKING,
      DELTA
    );
    assertEquals(
      new MapboxNavigationOptions().setMinimumMediumAlertDistanceWalking(100).getMinimumMediumAlertDistanceWalking(),
      100,
      DELTA
    );
  }

  @Test
  public void testMinimumHighAlertDistanceDriving() {
    assertEquals(
      new MapboxNavigationOptions().getMinimumHighAlertDistanceDriving(),
      NavigationConstants.MINIMUM_DISTANCE_FOR_HIGH_ALERT_DRIVING,
      DELTA
    );
    assertEquals(
      new MapboxNavigationOptions().setMinimumHighAlertDistanceDriving(100).getMinimumHighAlertDistanceDriving(),
      100,
      DELTA
    );
  }

  @Test
  public void testMinimumHighAlertDistanceCycling() {
    assertEquals(
      new MapboxNavigationOptions().getMinimumHighAlertDistanceCycling(),
      NavigationConstants.MINIMUM_DISTANCE_FOR_HIGH_ALERT_CYCLING,
      DELTA
    );
    assertEquals(
      new MapboxNavigationOptions().setMinimumHighAlertDistanceCycling(100).getMinimumHighAlertDistanceCycling(),
      100,
      DELTA
    );
  }

  @Test
  public void testMinimumHighAlertDistanceWalking() {
    assertEquals(
      new MapboxNavigationOptions().getMinimumHighAlertDistanceWalking(),
      NavigationConstants.MINIMUM_DISTANCE_FOR_HIGH_ALERT_WALKING,
      DELTA
    );
    assertEquals(
      new MapboxNavigationOptions().setMinimumHighAlertDistanceWalking(100).getMinimumHighAlertDistanceWalking(),
      100,
      DELTA
    );
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
