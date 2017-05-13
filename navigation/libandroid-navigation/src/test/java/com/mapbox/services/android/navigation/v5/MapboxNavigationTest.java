package com.mapbox.services.android.navigation.v5;

import android.content.Context;

import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.Assert.assertNotNull;

public class MapboxNavigationTest extends BaseTest{

  @Test
  public void sanityTest() {
    MapboxNavigation navigation = new MapboxNavigation(Mockito.mock(Context.class), "pk.XXX");
    assertNotNull("should not be null", navigation);
  }
}