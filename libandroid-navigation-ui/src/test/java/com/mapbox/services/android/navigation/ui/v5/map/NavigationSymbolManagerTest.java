package com.mapbox.services.android.navigation.ui.v5.map;

import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationSymbolManagerTest {

  @Test
  public void addMarkerFor_symbolManagerAddsOptions() {
    SymbolManager symbolManager = mock(SymbolManager.class);
    Symbol symbol = mock(Symbol.class);
    when(symbolManager.create(any(SymbolOptions.class))).thenReturn(symbol);
    NavigationSymbolManager navigationSymbolManager = new NavigationSymbolManager(symbolManager);
    Point position = Point.fromLngLat(1.2345, 1.3456);

    navigationSymbolManager.addMarkerFor(position);

    verify(symbolManager).create(any(SymbolOptions.class));
  }

  @Test
  public void removeAllMarkerSymbols_previouslyAddedMarkersRemoved() {
    SymbolManager symbolManager = mock(SymbolManager.class);
    Symbol symbol = mock(Symbol.class);
    when(symbolManager.create(any(SymbolOptions.class))).thenReturn(symbol);
    NavigationSymbolManager navigationSymbolManager = new NavigationSymbolManager(symbolManager);
    Point position = Point.fromLngLat(1.2345, 1.3456);
    navigationSymbolManager.addMarkerFor(position);

    navigationSymbolManager.removeAllMarkerSymbols();

    verify(symbolManager).delete(symbol);
  }
}