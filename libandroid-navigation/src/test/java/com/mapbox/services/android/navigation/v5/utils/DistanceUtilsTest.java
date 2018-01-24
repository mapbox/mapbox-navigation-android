package com.mapbox.services.android.navigation.v5.utils;

import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.text.DecimalFormat;

@RunWith(RobolectricTestRunner.class)
public class DistanceUtilsTest {
  private static final double largeMiles = 18024.65;

  @Test
  public void testDistanceFormatter() {
    Assert.assertEquals("11 mi", DistanceUtils.formatDistance(
      largeMiles,
      new DecimalFormat(NavigationConstants.DECIMAL_FORMAT),
      false,
      NavigationUnitType.TYPE_IMPERIAL).toString().toString());
  }
}
