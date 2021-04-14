package com.mapbox.navigation.ui.maps;

import androidx.annotation.NonNull;

import com.mapbox.common.TileStore;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import static org.mockito.Mockito.mock;

@Implements(TileStore.class)
public class ShadowTileStore {

  @Implementation
  public static TileStore getInstance(@NonNull String path) {
    return mock(TileStore.class);
  }

  @Implementation
  public static TileStore getInstance() {
    return mock(TileStore.class);
  }
}
