package com.mapbox.services.android.navigation.ui.v5.map;

import android.graphics.PointF;

import com.mapbox.mapboxsdk.maps.MapboxMap;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WaynameFeatureFinderTest {

  @Test
  public void queryRenderedFeatures_mapboxMapIsCalled() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    WaynameFeatureFinder featureFinder = new WaynameFeatureFinder(mapboxMap);
    PointF point = mock(PointF.class);
    String[] layerIds = {"id", "id"};

    featureFinder.queryRenderedFeatures(point, layerIds);

    verify(mapboxMap).queryRenderedFeatures(point, layerIds);
  }
}