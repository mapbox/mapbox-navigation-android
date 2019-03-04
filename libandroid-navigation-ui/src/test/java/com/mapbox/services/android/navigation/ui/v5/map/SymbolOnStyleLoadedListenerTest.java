package com.mapbox.services.android.navigation.ui.v5.map;

import android.graphics.Bitmap;

import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;

import org.junit.Test;

import static com.mapbox.services.android.navigation.ui.v5.map.NavigationSymbolManager.MAPBOX_NAVIGATION_MARKER_NAME;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SymbolOnStyleLoadedListenerTest {

  @Test
  public void onDidFinishLoadingStyle_markerIsAdded() {
    MapboxMap mapboxMap = mock(MapboxMap.class);
    Style style = mock(Style.class);
    when(mapboxMap.getStyle()).thenReturn(style);
    Bitmap markerBitmap = mock(Bitmap.class);
    SymbolOnStyleLoadedListener listener = new SymbolOnStyleLoadedListener(mapboxMap, markerBitmap);

    listener.onDidFinishLoadingStyle();

    verify(style).addImage(eq(MAPBOX_NAVIGATION_MARKER_NAME), eq(markerBitmap));
  }
}