package com.mapbox.services.android.navigation.ui.v5.map;

import android.graphics.Bitmap;

import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.mapboxsdk.style.sources.VectorSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

class MapLayerInteractor {

  private MapboxMap mapboxMap;

  MapLayerInteractor(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
  }

  void addLayer(Layer layer) {
    mapboxMap.addLayer(layer);
  }

  Layer retrieveLayerFromId(String layerId) {
    return mapboxMap.getLayerAs(layerId);
  }

  void addLayerImage(String imageName, Bitmap image) {
    mapboxMap.addImage(imageName, image);
  }

  void updateLayerVisibility(boolean isVisible, String tileSetIdentifier, String layerIdentifier) {
    // TODO find sourceIdentifier for Layer https://github.com/mapbox/mapbox-gl-native/issues/12691
    List<String> sourceIdentifiers = Collections.emptyList();
    List<Layer> layers = mapboxMap.getLayers();
    updateLayerWithVisibility(layerIdentifier, sourceIdentifiers, layers, isVisible);
  }

  boolean isLayerVisible(String tileSetIdentifier, String layerIdentifier) {
    // TODO find sourceIdentifier for Layer https://github.com/mapbox/mapbox-gl-native/issues/12691
    List<String> sourceIdentifiers = Collections.emptyList();
    List<Layer> layers = mapboxMap.getLayers();
    return findLayerVisibility(layerIdentifier, sourceIdentifiers, layers);
  }

  // TODO find sourceIdentifier for Layer https://github.com/mapbox/mapbox-gl-native/issues/12691
  private List<String> findSourceIdentifiersFor(String tileSetIdentifier) {
    List<String> sourceIdentifiers = new ArrayList<>();
    List<Source> sources = mapboxMap.getSources();
    for (Source source : sources) {
      boolean isVectorSource = source instanceof VectorSource;
      boolean isValidSource = isVectorSource
        && ((VectorSource) source).getUrl() != null
        && ((VectorSource) source).getUrl().contains(tileSetIdentifier);
      if (isValidSource) {
        sourceIdentifiers.add(source.getId());
      }
    }
    return sourceIdentifiers;
  }

  private void updateLayerWithVisibility(String layerIdentifier, List<String> sourceIdentifiers,
                                         List<Layer> layers, boolean isVisible) {
    for (Layer layer : layers) {
      if (isInvalid(layer)) {
        continue;
      }
      String sourceLayerId = retrieveSourceLayerId(layer);
      if (sourceLayerId.equals(layerIdentifier)) {
        layer.setProperties(visibility(isVisible ? VISIBLE : NONE));
      }
    }
  }

  private boolean findLayerVisibility(String layerIdentifier, List<String> sourceIdentifiers, List<Layer> layers) {
    for (Layer layer : layers) {
      if (isInvalid(layer)) {
        continue;
      }
      String sourceLayerId = retrieveSourceLayerId(layer);
      if (sourceLayerId.equals(layerIdentifier)) {
        return layer.getVisibility().value.equals(VISIBLE);
      }
    }
    return false;
  }

  private boolean isInvalid(Layer layer) {
    return !(layer instanceof LineLayer || layer instanceof SymbolLayer);
  }

  private String retrieveSourceLayerId(Layer layer) {
    String sourceIdentifier;
    if (layer instanceof LineLayer) {
      sourceIdentifier = ((LineLayer) layer).getSourceLayer();
    } else {
      sourceIdentifier = ((SymbolLayer) layer).getSourceLayer();
    }
    return sourceIdentifier;
  }
}
