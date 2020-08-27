package com.mapbox.navigation.ui.map;

import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationSymbolManagerTest {

  private static final long DEFAULT_SYMBOL_ID = 0L;
  private static final String DEFAULT_SYMBOL_ICON_IMAGE = "defaultSymbolIconImage";

  @Test
  public void addDestinationMarkerFor_symbolManagerAddsOptions() {
    SymbolManager symbolManager = mock(SymbolManager.class);
    Symbol symbol = buildSymbolWith(DEFAULT_SYMBOL_ID, DEFAULT_SYMBOL_ICON_IMAGE);
    when(symbolManager.create(any(SymbolOptions.class))).thenReturn(symbol);
    NavigationSymbolManager navigationSymbolManager = new NavigationSymbolManager(symbolManager);
    Point position = Point.fromLngLat(1.2345, 1.3456);

    navigationSymbolManager.addDestinationMarkerFor(position);

    verify(symbolManager).create(any(SymbolOptions.class));
  }

  @Test
  public void addDestinationMarkerFor_destinationSymbolRemovedIfPreviouslyAdded() {
    SymbolManager symbolManager = mock(SymbolManager.class);
    Symbol oldDestinationSymbol = mock(Symbol.class);
    Symbol currentDestinationSymbol = mock(Symbol.class);
    when(symbolManager.create(any(SymbolOptions.class))).thenReturn(oldDestinationSymbol, currentDestinationSymbol);
    NavigationSymbolManager navigationSymbolManager = new NavigationSymbolManager(symbolManager);
    Point position = Point.fromLngLat(1.2345, 1.3456);

    navigationSymbolManager.addDestinationMarkerFor(position);
    navigationSymbolManager.addDestinationMarkerFor(position);

    verify(symbolManager, times(2)).create(any(SymbolOptions.class));
    verify(symbolManager, times(1)).delete(eq(oldDestinationSymbol));
  }

  @Test
  public void clearAllMarkerSymbols_previouslyAddedMarkersCleared() {
    SymbolManager symbolManager = mock(SymbolManager.class);
    Symbol symbol = mock(Symbol.class);
    when(symbolManager.create(any(SymbolOptions.class))).thenReturn(symbol);
    NavigationSymbolManager navigationSymbolManager = new NavigationSymbolManager(symbolManager);
    Point position = Point.fromLngLat(1.2345, 1.3456);
    navigationSymbolManager.addDestinationMarkerFor(position);

    navigationSymbolManager.clearAllMarkerSymbols();

    verify(symbolManager).delete(symbol);
  }

  @Test
  public void addCustomSymbolFor_symbolManagerCreatesSymbol() {
    Symbol symbol = buildSymbolWith(DEFAULT_SYMBOL_ID, DEFAULT_SYMBOL_ICON_IMAGE);
    SymbolOptions symbolOptions = mock(SymbolOptions.class);
    SymbolManager symbolManager = buildSymbolManager(symbolOptions, symbol);
    NavigationSymbolManager navigationSymbolManager = new NavigationSymbolManager(symbolManager);

    navigationSymbolManager.addCustomSymbolFor(symbolOptions);

    verify(symbolManager).create(symbolOptions);
  }

  @Test
  public void clearSymbolWithId_previouslyAddedMarkersCleared() {
    long symbolId = 911L;
    Symbol symbol = buildSymbolWith(symbolId, DEFAULT_SYMBOL_ICON_IMAGE);
    SymbolOptions symbolOptions = mock(SymbolOptions.class);
    SymbolManager symbolManager = buildSymbolManager(symbolOptions, symbol);
    NavigationSymbolManager navigationSymbolManager = new NavigationSymbolManager(symbolManager);
    navigationSymbolManager.addCustomSymbolFor(symbolOptions);

    navigationSymbolManager.clearSymbolWithId(symbolId);

    verify(symbolManager).delete(symbol);
  }

  @Test
  public void clearSymbolWithId_symbolBeClearedOnlyOnce() {
    long symbolId = 911L;
    Symbol symbol = buildSymbolWith(symbolId, DEFAULT_SYMBOL_ICON_IMAGE);
    SymbolOptions symbolOptions = mock(SymbolOptions.class);
    SymbolManager symbolManager = buildSymbolManager(symbolOptions, symbol);
    NavigationSymbolManager navigationSymbolManager = new NavigationSymbolManager(symbolManager);
    navigationSymbolManager.addCustomSymbolFor(symbolOptions);

    navigationSymbolManager.clearSymbolWithId(symbolId);
    navigationSymbolManager.clearSymbolWithId(symbolId);

    verify(symbolManager, times(1)).delete(symbol);
  }

  @Test
  public void clearSymbolsWithIconImageProperty_sameIconImageSymbolsCleared() {
    Symbol defaultSymbol = buildSymbolWith(DEFAULT_SYMBOL_ID, DEFAULT_SYMBOL_ICON_IMAGE);
    String iconImageFeedback = "feedback";
    Symbol firstFeedbackSymbol = buildSymbolWith(DEFAULT_SYMBOL_ID + 1, iconImageFeedback);
    Symbol secondFeedbackSymbol = buildSymbolWith(DEFAULT_SYMBOL_ID + 2, iconImageFeedback);
    SymbolOptions symbolOptions = mock(SymbolOptions.class);
    SymbolManager symbolManager = buildSymbolManager(symbolOptions, defaultSymbol, firstFeedbackSymbol, secondFeedbackSymbol);
    NavigationSymbolManager navigationSymbolManager = new NavigationSymbolManager(symbolManager);
    navigationSymbolManager.addCustomSymbolFor(symbolOptions);
    navigationSymbolManager.addCustomSymbolFor(symbolOptions);
    navigationSymbolManager.addCustomSymbolFor(symbolOptions);

    navigationSymbolManager.clearSymbolsWithIconImageProperty(iconImageFeedback);

    verify(symbolManager).delete(firstFeedbackSymbol);
    verify(symbolManager).delete(secondFeedbackSymbol);
    verify(symbolManager, times(0)).delete(defaultSymbol);
  }

  private Symbol buildSymbolWith(long symbolId, String iconImage) {
    Symbol symbol = mock(Symbol.class);
    when(symbol.getId()).thenReturn(symbolId);
    when(symbol.getIconImage()).thenReturn(iconImage);
    return symbol;
  }

  private SymbolManager buildSymbolManager(SymbolOptions symbolOptions, Symbol symbol, Symbol... symbols) {
    SymbolManager symbolManager = mock(SymbolManager.class);
    when(symbolManager.create(symbolOptions)).thenReturn(symbol, symbols);
    return symbolManager;
  }
}