package com.mapbox.services.android.navigation.v5.navigation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NavigationWalkingOptionsTest {

  @Test
  public void alleyBias_walkingOptionSet() {
    NavigationWalkingOptions options = NavigationWalkingOptions.builder().alleyBias(0.7).build();

    assertEquals(Double.valueOf(0.7), options.getWalkingOptions().alleyBias());
  }

  @Test
  public void walkwayBias_walkingOptionSet() {
    NavigationWalkingOptions options = NavigationWalkingOptions.builder().walkwayBias(0.8).build();

    assertEquals(Double.valueOf(0.8), options.getWalkingOptions().walkwayBias());
  }

  @Test
  public void walkingSpeed_walkingOptionSet() {
    NavigationWalkingOptions options = NavigationWalkingOptions.builder().walkingSpeed(2.0).build();

    assertEquals(Double.valueOf(2.0), options.getWalkingOptions().walkingSpeed());
  }
}
