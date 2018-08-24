package com.mapbox.services.android.navigation.ui.v5.map;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NavigationMapboxMapTest {

  @Test
  public void updateIncidentsVisibility_layerIdentifierIsClosures() {
    MapLayerInteractor mockedMapLayerInteractor = mock(MapLayerInteractor.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mockedMapLayerInteractor);
    boolean anyVisibility = true;

    theNavigationMap.updateIncidentsVisibility(anyVisibility);

    verify(mockedMapLayerInteractor).updateLayerVisibility(eq(anyVisibility), eq("closures"));
  }

  @Test
  public void isIncidentsVisible_layerIdentifierIsClosures() {
    MapLayerInteractor mockedMapLayerInteractor = mock(MapLayerInteractor.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mockedMapLayerInteractor);

    theNavigationMap.isIncidentsVisible();

    verify(mockedMapLayerInteractor).isLayerVisible(eq("closures"));
  }

  @Test
  public void updateTrafficVisibility_layerIdentifierIsTraffic() {
    MapLayerInteractor mockedMapLayerInteractor = mock(MapLayerInteractor.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mockedMapLayerInteractor);
    boolean anyVisibility = false;

    theNavigationMap.updateTrafficVisibility(anyVisibility);

    verify(mockedMapLayerInteractor).updateLayerVisibility(eq(anyVisibility), eq("traffic"));
  }

  @Test
  public void isTrafficVisible_layerIdentifierIsTraffic() {
    MapLayerInteractor mockedMapLayerInteractor = mock(MapLayerInteractor.class);
    NavigationMapboxMap theNavigationMap = new NavigationMapboxMap(mockedMapLayerInteractor);

    theNavigationMap.isTrafficVisible();

    verify(mockedMapLayerInteractor).isLayerVisible(eq("traffic"));
  }
}