package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.geojson.Geometry;
import com.mapbox.mapboxsdk.offline.OfflineGeometryRegionDefinition;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class OfflineRegionDefinitionProviderTest {

  private static final double DELTA = 1E-10;

  @Test
  public void buildRegionFor_geometryIsCorrectlySet() {
    String styleUrl = "mapbox://style";
    float pixelRatio = 1f;
    Geometry routeGeometry = mock(Geometry.class);
    OfflineRegionDefinitionProvider provider = new OfflineRegionDefinitionProvider(styleUrl, pixelRatio);

    OfflineGeometryRegionDefinition offlineRegionDefinition = provider.buildRegionFor(routeGeometry);

    assertEquals(routeGeometry, offlineRegionDefinition.getGeometry());
  }

  @Test
  public void buildRegionFor_styleUrlIsCorrectlySet() {
    String styleUrl = "mapbox://style";
    float pixelRatio = 1f;
    Geometry routeGeometry = mock(Geometry.class);
    OfflineRegionDefinitionProvider provider = new OfflineRegionDefinitionProvider(styleUrl, pixelRatio);

    OfflineGeometryRegionDefinition offlineRegionDefinition = provider.buildRegionFor(routeGeometry);

    assertEquals(styleUrl, offlineRegionDefinition.getStyleURL());
  }

  @Test
  public void buildRegionFor_pixelRatioIsCorrectlySet() {
    String styleUrl = "mapbox://style";
    float pixelRatio = 1f;
    Geometry routeGeometry = mock(Geometry.class);
    OfflineRegionDefinitionProvider provider = new OfflineRegionDefinitionProvider(styleUrl, pixelRatio);

    OfflineGeometryRegionDefinition offlineRegionDefinition = provider.buildRegionFor(routeGeometry);

    assertEquals(pixelRatio, offlineRegionDefinition.getPixelRatio(), DELTA);
  }
}