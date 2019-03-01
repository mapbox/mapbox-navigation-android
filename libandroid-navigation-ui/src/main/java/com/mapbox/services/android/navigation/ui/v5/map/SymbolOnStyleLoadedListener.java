package com.mapbox.services.android.navigation.ui.v5.map;

import com.mapbox.mapboxsdk.maps.MapView;

class SymbolOnStyleLoadedListener implements MapView.OnDidFinishLoadingStyleListener {

  private final NavigationSymbolManager symbolManager;

  SymbolOnStyleLoadedListener(NavigationSymbolManager symbolManager) {
    this.symbolManager = symbolManager;
  }

  @Override
  public void onDidFinishLoadingStyle() {
    symbolManager.redrawMarkers();
  }
}
