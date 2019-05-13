package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.WalkingOptions;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NavigationWalkingOptionsTest {

  @Test
  public void alleyBias_walkingOptionSet() {
    WalkingOptions.Builder walkingOptionsBuilder = mock(WalkingOptions.Builder.class);
    NavigationWalkingOptions.Builder navigationWalkingOptionsBuilder =
      new NavigationWalkingOptions.Builder(walkingOptionsBuilder);

    navigationWalkingOptionsBuilder.alleyBias(0.7);

    verify(walkingOptionsBuilder).alleyBias(0.7);
  }

  @Test
  public void WalkwayBias_walkingOptionSet() {
    WalkingOptions.Builder walkingOptionsBuilder = mock(WalkingOptions.Builder.class);
    NavigationWalkingOptions.Builder navigationWalkingOptionsBuilder =
      new NavigationWalkingOptions.Builder(walkingOptionsBuilder);

    navigationWalkingOptionsBuilder.walkwayBias(0.8);

    verify(walkingOptionsBuilder).walkwayBias(0.8);
  }

  @Test
  public void walkingSpeed_walkingOptionSet() {
    WalkingOptions.Builder walkingOptionsBuilder = mock(WalkingOptions.Builder.class);
    NavigationWalkingOptions.Builder navigationWalkingOptionsBuilder =
      new NavigationWalkingOptions.Builder(walkingOptionsBuilder);

    navigationWalkingOptionsBuilder.walkingSpeed(1.0);

    verify(walkingOptionsBuilder).walkingSpeed(1.0);
  }
}
