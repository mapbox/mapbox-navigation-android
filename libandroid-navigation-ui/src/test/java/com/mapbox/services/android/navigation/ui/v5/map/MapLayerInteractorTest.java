package com.mapbox.services.android.navigation.ui.v5.map;

import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapLayerInteractorTest {

  @Test
  public void updateLayerVisibility_trafficVisibilityIsSet() {
    LineLayer trafficLayer = mock(LineLayer.class);
    when(trafficLayer.getSourceLayer()).thenReturn("traffic");
    List<Layer> layers = buildLayerListWithIncidents(trafficLayer);
    MapboxMap map = mock(MapboxMap.class);
    when(map.getLayers()).thenReturn(layers);
    MapLayerInteractor layerInteractor = new MapLayerInteractor(map);

    layerInteractor.updateLayerVisibility(true, "", "traffic");

    verify(trafficLayer).setProperties(any(PropertyValue.class));
  }

  @Test
  public void updateLayerVisibility_trafficVisibilityIsSetNotSet() {
    LineLayer trafficLayer = mock(LineLayer.class);
    when(trafficLayer.getSourceLayer()).thenReturn("traffic");
    List<Layer> layers = buildLayerListWithIncidents(trafficLayer);
    MapboxMap map = mock(MapboxMap.class);
    when(map.getLayers()).thenReturn(layers);
    MapLayerInteractor layerInteractor = new MapLayerInteractor(map);

    layerInteractor.updateLayerVisibility(true, "", "random");

    verify(trafficLayer, times(0)).setProperties(any(PropertyValue.class));
  }

  @Test
  public void updateLayerVisibility_incidentsAreSetToVisible() {
    SymbolLayer incidentsLayer = mock(SymbolLayer.class);
    when(incidentsLayer.getSourceLayer()).thenReturn("incidents");
    List<Layer> layers = buildLayerListWithTraffic(incidentsLayer);
    MapboxMap map = mock(MapboxMap.class);
    when(map.getLayers()).thenReturn(layers);
    MapLayerInteractor layerInteractor = new MapLayerInteractor(map);

    layerInteractor.updateLayerVisibility(true, "", "incidents");

    verify(incidentsLayer).setProperties(any(PropertyValue.class));
  }

  @Test
  public void updateLayerVisibility_incidentsAreSetToInvisible() {
    SymbolLayer incidentsLayer = mock(SymbolLayer.class);
    when(incidentsLayer.getSourceLayer()).thenReturn("incidents");
    List<Layer> layers = buildLayerListWithTraffic(incidentsLayer);
    MapboxMap map = mock(MapboxMap.class);
    when(map.getLayers()).thenReturn(layers);
    MapLayerInteractor layerInteractor = new MapLayerInteractor(map);

    layerInteractor.updateLayerVisibility(true, "", "random");

    verify(incidentsLayer, times(0)).setProperties(any(PropertyValue.class));
  }

  @Test
  public void isLayerVisible_visibleTrafficReturnsTrue() {
    List<Layer> layers = buildLayerListWithVisibleTraffic();
    MapboxMap map = mock(MapboxMap.class);
    when(map.getLayers()).thenReturn(layers);
    MapLayerInteractor layerInteractor = new MapLayerInteractor(map);

    boolean isVisible = layerInteractor.isLayerVisible("", "traffic");

    assertTrue(isVisible);
  }

  @Test
  public void isLayerVisible_invisibleTrafficReturnsFalse() {
    List<Layer> layers = buildLayerListWithInvisibleTraffic();
    MapboxMap map = mock(MapboxMap.class);
    when(map.getLayers()).thenReturn(layers);
    MapLayerInteractor layerInteractor = new MapLayerInteractor(map);

    boolean isVisible = layerInteractor.isLayerVisible("", "traffic");

    assertFalse(isVisible);
  }

  @Test
  public void isLayerVisible_visibleIncidentsReturnsTrue() {
    List<Layer> layers = buildLayerListWithVisibleIncidents();
    MapboxMap map = mock(MapboxMap.class);
    when(map.getLayers()).thenReturn(layers);
    MapLayerInteractor layerInteractor = new MapLayerInteractor(map);

    boolean isVisible = layerInteractor.isLayerVisible("", "incidents");

    assertTrue(isVisible);
  }

  @Test
  public void isLayerVisible_invisibleIncidentsReturnsFalse() {
    List<Layer> layers = buildLayerListWithInvisibleIncidents();
    MapboxMap map = mock(MapboxMap.class);
    when(map.getLayers()).thenReturn(layers);
    MapLayerInteractor layerInteractor = new MapLayerInteractor(map);

    boolean isVisible = layerInteractor.isLayerVisible("", "incidents");

    assertFalse(isVisible);
  }

  private List<Layer> buildLayerListWithIncidents(Layer layerToAdd) {
    List<Layer> layers = new ArrayList<>();
    layers.add(layerToAdd);
    SymbolLayer incidentsLayer = mock(SymbolLayer.class);
    when(incidentsLayer.getSourceLayer()).thenReturn("incidents");
    layers.add(incidentsLayer);
    SymbolLayer randomLayer = mock(SymbolLayer.class);
    when(randomLayer.getSourceLayer()).thenReturn("random");
    layers.add(randomLayer);
    return layers;
  }

  private List<Layer> buildLayerListWithTraffic(Layer layerToAdd) {
    List<Layer> layers = new ArrayList<>();
    layers.add(layerToAdd);
    LineLayer trafficLayer = mock(LineLayer.class);
    when(trafficLayer.getSourceLayer()).thenReturn("traffic");
    layers.add(trafficLayer);
    SymbolLayer randomLayer = mock(SymbolLayer.class);
    when(randomLayer.getSourceLayer()).thenReturn("random");
    layers.add(randomLayer);
    return layers;
  }

  private List<Layer> buildLayerListWithVisibleIncidents() {
    List<Layer> layers = new ArrayList<>();
    SymbolLayer incidentsLayer = mock(SymbolLayer.class);
    when(incidentsLayer.getSourceLayer()).thenReturn("incidents");
    when(incidentsLayer.getVisibility()).thenReturn(visibility(Property.VISIBLE));
    layers.add(incidentsLayer);
    SymbolLayer randomLayer = mock(SymbolLayer.class);
    when(randomLayer.getSourceLayer()).thenReturn("random");
    layers.add(randomLayer);
    return layers;
  }

  private List<Layer> buildLayerListWithInvisibleIncidents() {
    List<Layer> layers = new ArrayList<>();
    SymbolLayer incidentsLayer = mock(SymbolLayer.class);
    when(incidentsLayer.getSourceLayer()).thenReturn("incidents");
    when(incidentsLayer.getVisibility()).thenReturn(visibility(Property.NONE));
    layers.add(incidentsLayer);
    SymbolLayer randomLayer = mock(SymbolLayer.class);
    when(randomLayer.getSourceLayer()).thenReturn("random");
    layers.add(randomLayer);
    return layers;
  }

  private List<Layer> buildLayerListWithVisibleTraffic() {
    List<Layer> layers = new ArrayList<>();
    SymbolLayer trafficLayer = mock(SymbolLayer.class);
    when(trafficLayer.getSourceLayer()).thenReturn("traffic");
    when(trafficLayer.getVisibility()).thenReturn(visibility(Property.VISIBLE));
    layers.add(trafficLayer);
    SymbolLayer randomLayer = mock(SymbolLayer.class);
    when(randomLayer.getSourceLayer()).thenReturn("random");
    layers.add(randomLayer);
    return layers;
  }

  private List<Layer> buildLayerListWithInvisibleTraffic() {
    List<Layer> layers = new ArrayList<>();
    SymbolLayer trafficLayer = mock(SymbolLayer.class);
    when(trafficLayer.getSourceLayer()).thenReturn("traffic");
    when(trafficLayer.getVisibility()).thenReturn(visibility(Property.NONE));
    layers.add(trafficLayer);
    SymbolLayer randomLayer = mock(SymbolLayer.class);
    when(randomLayer.getSourceLayer()).thenReturn("random");
    layers.add(randomLayer);
    return layers;
  }
}