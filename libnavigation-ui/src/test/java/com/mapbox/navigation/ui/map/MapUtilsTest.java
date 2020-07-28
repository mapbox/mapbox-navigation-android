package com.mapbox.navigation.ui.map;

import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.navigation.ui.internal.utils.MapUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MapUtilsTest {

  @Test
  public void addLayerToMapAbove_layerListSizeAmountCorrect() {
    SymbolLayer layerOne = mock(SymbolLayer.class);
    LineLayer layerTwo = mock(LineLayer.class);
    LineLayer layerThree = mock(LineLayer.class);
    LineLayer layerToAddViaMapUtils = mock(LineLayer.class);

    List<Layer> layers = new ArrayList<>();
    layers.add(layerOne);
    layers.add(layerTwo);
    layers.add(layerThree);
    layers.add(layerToAddViaMapUtils);

    Style mockedStyle = mock(Style.class);
    mockedStyle.addLayer(layerOne);
    mockedStyle.addLayer(layerTwo);
    mockedStyle.addLayer(layerThree);
    MapUtils.addLayerToMapAbove(mockedStyle, layerToAddViaMapUtils, null);

    when(mockedStyle.getLayers()).thenReturn(layers);
    assertEquals(mockedStyle.getLayers().size(), layers.size());
  }

  @Test
  public void addLayerToMapAbove_declaredAboveLayer() {
    SymbolLayer layerOne = mock(SymbolLayer.class);
    LineLayer layerTwo = mock(LineLayer.class);
    when(layerTwo.getId()).thenReturn("idForLayerTwo");
    LineLayer layerThree = mock(LineLayer.class);
    LineLayer layerToAddViaMapUtils = mock(LineLayer.class);
    when(layerThree.getId()).thenReturn("idForLayerToAddViaMapUtils");

    List<Layer> layers = new ArrayList<>();
    layers.add(layerOne);
    layers.add(layerTwo);
    layers.add(layerThree);
    layers.add(layerToAddViaMapUtils);

    Style mockedStyle = mock(Style.class);
    mockedStyle.addLayer(layerOne);
    mockedStyle.addLayer(layerTwo);
    mockedStyle.addLayer(layerThree);
    MapUtils.addLayerToMapAbove(mockedStyle, layerToAddViaMapUtils, layerTwo.getId());

    when(mockedStyle.getLayers()).thenReturn(layers);
    assertEquals(mockedStyle.getLayers().get(2).getId(), "idForLayerToAddViaMapUtils");
  }
}