package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;


public class NavigationMapRouteTest {

  @Mock
  MapView mapView;

  @Mock
  MapboxMap mapboxMap;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

//  @Test
//  public void testSanity() {
//    new NavigationMapRoute(null, mapView, mapboxMap);
//  }


}
