package com.mapbox.services.android.navigation.v5.utils;

import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public class DistanceUtilsTest {

  @Test
  public void testFormatDistance_largeMiles() {
    assertOutput("11 mi", 18124.65, NavigationUnitType.TYPE_IMPERIAL, Locale.US);
  }

  @Test
  public void testFormatDistance_mediumMiles() {
    assertOutput("5.6 mi", 9012.33, NavigationUnitType.TYPE_IMPERIAL, Locale.US);
  }

  @Test
  public void testFormatDistance_smallFeet() {
    assertOutput("50 ft", 13.71, NavigationUnitType.TYPE_IMPERIAL, Locale.US);
  }

  @Test
  public void testFormatDistance_largeFeet() {
    assertOutput("350 ft", 109.73, NavigationUnitType.TYPE_IMPERIAL, Locale.US);
  }

  @Test
  public void testFormatDistance_largeKilometers() {
    assertOutput("18 km", 18124.65, NavigationUnitType.TYPE_METRIC, Locale.FRANCE);
  }

  @Test
  public void testFormatDistance_mediumKilometers() {
    assertOutput("9,8 km", 9812.33, NavigationUnitType.TYPE_METRIC, Locale.FRANCE);
  }

  @Test
  public void testFormatDistance_smallMeters() {
    assertOutput("50 m", 48.72, NavigationUnitType.TYPE_METRIC, Locale.FRANCE);
  }

  @Test
  public void testFormatDistance_largeMeters() {
    assertOutput("100 m", 109.73, NavigationUnitType.TYPE_METRIC, Locale.FRANCE);
  }

  private void assertOutput(String output, double distance, int unitType, Locale locale) {
    Assert.assertEquals(output,
      DistanceUtils.formatDistance(
        distance,
        locale,
        unitType).toString());
  }
}
