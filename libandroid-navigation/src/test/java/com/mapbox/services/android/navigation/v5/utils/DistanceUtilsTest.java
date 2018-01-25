package com.mapbox.services.android.navigation.v5.utils;

import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public class DistanceUtilsTest {
  private static final double LARGE_LARGE_UNIT = 18124.65;
  private static final double MEDIUM_LARGE_UNIT = 9812.33;
  private static final double SMALL_SMALL_UNIT = 13.71;
  private static final double LARGE_SMALL_UNIT = 109.73;

  @Test
  public void testFormatDistance_largeMiles() {
    assertOutput("11 mi", LARGE_LARGE_UNIT, NavigationUnitType.TYPE_IMPERIAL, Locale.US);
  }

  @Test
  public void testFormatDistance_largeKilometers() {
    assertOutput("18 km", LARGE_LARGE_UNIT, NavigationUnitType.TYPE_METRIC, Locale.FRANCE);
  }

  @Test
  public void testFormatDistance_mediumMiles() {
    assertOutput("6.1 mi", MEDIUM_LARGE_UNIT, NavigationUnitType.TYPE_IMPERIAL, Locale.US);
  }

  @Test
  public void testFormatDistance_mediumKilometers() {
    assertOutput("9,8 km", MEDIUM_LARGE_UNIT, NavigationUnitType.TYPE_METRIC, Locale.FRANCE);
  }

  @Test
  public void testFormatDistance_smallFeet() {
    assertOutput("50 ft", SMALL_SMALL_UNIT, NavigationUnitType.TYPE_IMPERIAL, Locale.US);
  }

  @Test
  public void testFormatDistance_smallMeters() {
    assertOutput("50 m", SMALL_SMALL_UNIT, NavigationUnitType.TYPE_METRIC, Locale.FRANCE);
  }

  @Test
  public void testFormatDistance_largeFeet() {
    assertOutput("350 ft", LARGE_SMALL_UNIT, NavigationUnitType.TYPE_IMPERIAL, Locale.US);
  }

  @Test
  public void testFormatDistance_largeMeters() {
    assertOutput("100 m", LARGE_SMALL_UNIT, NavigationUnitType.TYPE_METRIC, Locale.FRANCE);
  }

  private void assertOutput(String output, double distance, int unitType, Locale locale) {
    Assert.assertEquals(output,
      DistanceUtils.formatDistance(
        distance,
        locale,
        unitType).toString());
  }
}
