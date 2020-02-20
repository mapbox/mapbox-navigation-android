package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.geojson.Geometry;
import com.mapbox.mapboxsdk.offline.OfflineGeometryRegionDefinition;

class OfflineRegionDefinitionProvider {

  private static final int MIN_ZOOM = 11;
  private static final int MAX_ZOOM = 17;
  private final String styleUrl;
  private final float pixelRatio;

  OfflineRegionDefinitionProvider(String styleUrl, float pixelRatio) {
    this.styleUrl = styleUrl;
    this.pixelRatio = pixelRatio;
  }

  OfflineGeometryRegionDefinition buildRegionFor(Geometry routeGeometry) {
    return new OfflineGeometryRegionDefinition(
      styleUrl,
      routeGeometry,
      MIN_ZOOM,
      MAX_ZOOM,
      pixelRatio
    );
  }
}