package com.mapbox.services.android.navigation.ui.v5.map;

import android.graphics.Bitmap;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import static com.mapbox.services.android.navigation.ui.v5.map.NavigationSymbolManager.MAPBOX_NAVIGATION_MARKER_NAME;

class SymbolOnStyleLoadedListener implements MapView.OnDidFinishLoadingStyleListener {

  private final MapboxMap mapboxMap;
  private final Bitmap markerBitmap;

  SymbolOnStyleLoadedListener(MapboxMap mapboxMap, Bitmap markerBitmap) {
    this.mapboxMap = mapboxMap;
    this.markerBitmap = markerBitmap;
  }

  @Override
  public void onDidFinishLoadingStyle() {
    mapboxMap.getStyle().addImage(MAPBOX_NAVIGATION_MARKER_NAME, markerBitmap);
  }
}
