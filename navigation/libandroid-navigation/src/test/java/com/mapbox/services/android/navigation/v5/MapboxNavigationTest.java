package com.mapbox.services.android.navigation.v5;

import android.content.Context;

import com.mapbox.services.android.location.LostLocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.Assert.assertNotNull;

public class MapboxNavigationTest extends BaseTest {

  @Test
  public void sanityTest() {
    MapboxNavigation navigation = new MapboxNavigation(Mockito.mock(Context.class), "pk.XXX");
    assertNotNull("should not be null", navigation);
  }

  @Test
  public void getMapboxNavigationOptions_currentOptionDoesReturn() {
    MapboxNavigationOptions options = new MapboxNavigationOptions();
    options.setManeuverZoneRadius(19.9999);
    MapboxNavigation navigation = new MapboxNavigation(Mockito.mock(Context.class), "pk.XXX", options);
    double actualValue = navigation.getMapboxNavigationOptions().getManeuverZoneRadius();
    Assert.assertEquals(19.9999, actualValue, BaseTest.DELTA);
  }

  @Test
  public void setMapboxNavigationOptions_optionsChangedAfterServiceCreated() {
    MapboxNavigationOptions options = new MapboxNavigationOptions();
    options.setManeuverZoneRadius(19.9999);
    MapboxNavigation navigation = new MapboxNavigation(Mockito.mock(Context.class), "pk.XXX", options);
    navigation.startNavigation(Mockito.mock(DirectionsRoute.class));
    options.setManeuverZoneRadius(100.9);
    Assert.assertEquals(100.9, navigation.getMapboxNavigationOptions().getManeuverZoneRadius(), DELTA);
  }


}