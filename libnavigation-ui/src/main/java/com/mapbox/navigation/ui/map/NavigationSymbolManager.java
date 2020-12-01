package com.mapbox.navigation.ui.map;

import androidx.annotation.NonNull;

import androidx.collection.LongSparseArray;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;

import java.util.ArrayList;
import java.util.List;

class NavigationSymbolManager {

  static final String MAPBOX_NAVIGATION_DESTINATION_MARKER_NAME = "mapbox-navigation-destination-marker";
  private final LongSparseArray<Symbol> markersSymbols = new LongSparseArray<>();
  private Symbol destinationSymbol = null;
  @NonNull
  private final SymbolManager symbolManager;

  NavigationSymbolManager(@NonNull SymbolManager symbolManager) {
    this.symbolManager = symbolManager;
    symbolManager.setIconAllowOverlap(true);
    symbolManager.setIconIgnorePlacement(true);
  }

  void addDestinationMarkerFor(@NonNull Point position) {
    if (destinationSymbol != null) {
      symbolManager.delete(destinationSymbol);
      markersSymbols.remove(destinationSymbol.getId());
    }

    SymbolOptions options = createSymbolOptionsFor(position);
    destinationSymbol = createSymbolFrom(options);
  }

  Symbol addCustomSymbolFor(@NonNull SymbolOptions options) {
    return createSymbolFrom(options);
  }

  void clearSymbolWithId(long symbolId) {
    Symbol symbol = markersSymbols.get(symbolId);
    if (symbol != null) {
      symbolManager.delete(symbol);
      markersSymbols.remove(symbolId);
    }
  }

  void clearSymbolsWithIconImageProperty(@NonNull String symbolIconImageProperty) {
    List<Symbol> toDelete = new ArrayList<>();
    for (int i = 0; i < markersSymbols.size(); i++) {
      Symbol symbol = markersSymbols.valueAt(i);
      if (symbolIconImageProperty.equals(symbol.getIconImage())) {
        symbolManager.delete(symbol);
        toDelete.add(symbol);
      }
    }

    for (Symbol symbol : toDelete) {
      markersSymbols.remove(symbol.getId());
    }
  }

  void clearAllMarkerSymbols() {
    for (int i = 0; i < markersSymbols.size(); i++) {
      Symbol symbol = markersSymbols.valueAt(i);
      symbolManager.delete(symbol);
    }
    markersSymbols.clear();
    destinationSymbol = null;
  }

  @NonNull
  private SymbolOptions createSymbolOptionsFor(@NonNull Point position) {
    LatLng markerPosition = new LatLng(position.latitude(),
        position.longitude());
    return new SymbolOptions()
        .withLatLng(markerPosition)
        .withIconImage(MAPBOX_NAVIGATION_DESTINATION_MARKER_NAME);
  }

  private Symbol createSymbolFrom(@NonNull SymbolOptions options) {
    Symbol symbol = symbolManager.create(options);
    markersSymbols.put(symbol.getId(), symbol);
    return symbol;
  }
}
