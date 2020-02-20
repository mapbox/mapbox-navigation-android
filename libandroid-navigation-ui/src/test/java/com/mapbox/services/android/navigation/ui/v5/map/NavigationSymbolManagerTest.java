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
  public void addDestinationMarkerFor_symbolManagerAddsOptions() {
    SymbolManager symbolManager = mock(SymbolManager.class);
    Symbol symbol = mock(Symbol.class);
    when(symbolManager.create(any(SymbolOptions.class))).thenReturn(symbol);
    NavigationSymbolManager navigationSymbolManager = new NavigationSymbolManager(symbolManager);
    Point position = Point.fromLngLat(1.2345, 1.3456);

    navigationSymbolManager.addDestinationMarkerFor(position);

    verify(symbolManager).create(any(SymbolOptions.class));
  }

  @Test
  public void removeAllMarkerSymbols_previouslyAddedMarkersRemoved() {
    SymbolManager symbolManager = mock(SymbolManager.class);
    Symbol symbol = mock(Symbol.class);
    when(symbolManager.create(any(SymbolOptions.class))).thenReturn(symbol);
    NavigationSymbolManager navigationSymbolManager = new NavigationSymbolManager(symbolManager);
    Point position = Point.fromLngLat(1.2345, 1.3456);
    navigationSymbolManager.addDestinationMarkerFor(position);

    navigationSymbolManager.removeAllMarkerSymbols();

    verify(symbolManager).delete(symbol);
  }

  @Test
  public void addDestinationMarkerFor_previouslyAddedMarkersRemoved() {
    SymbolManager symbolManager = mock(SymbolManager.class);
    Symbol symbol = mock(Symbol.class);
    SymbolOptions symbolOptions = mock(SymbolOptions.class);
    when(symbolManager.create(symbolOptions)).thenReturn(symbol);
    NavigationSymbolManager navigationSymbolManager = new NavigationSymbolManager(symbolManager);

    navigationSymbolManager.addCustomSymbolFor(symbolOptions);

    verify(symbolManager).create(symbolOptions);
  }
}