package com.mapbox.services.android.navigation.ui.v5.map;

import android.graphics.Bitmap;
import android.graphics.PointF;

import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;

import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
import static com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap.STREETS_LAYER_ID;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.MAPBOX_WAYNAME_ICON;

class MapWayname {

  private static final String NAME_PROPERTY = "name";
  private static final int FIRST_ROAD_FEATURE = 0;

  private WaynameLayoutProvider layoutProvider;
  private WaynameLayerInteractor layerInteractor;
  private WaynameFeatureFinder featureInteractor;
  private MapPaddingAdjustor paddingAdjustor;
  private boolean isAutoQueryEnabled;
  private boolean isVisible;
  private String wayname = "";

  MapWayname(WaynameLayoutProvider layoutProvider, WaynameLayerInteractor layerInteractor,
             WaynameFeatureFinder featureInteractor, MapPaddingAdjustor paddingAdjustor) {
    this.layoutProvider = layoutProvider;
    this.layerInteractor = layerInteractor;
    this.featureInteractor = featureInteractor;
    this.paddingAdjustor = paddingAdjustor;
  }

  void updateWaynameWithPoint(PointF point, SymbolLayer waynameLayer) {
    if (!isAutoQueryEnabled || !isVisible) {
      return;
    }
    List<Feature> roads = findRoadLabelFeatures(point);
    boolean shouldBeVisible = !roads.isEmpty();
    adjustWaynameVisibility(shouldBeVisible, waynameLayer);
    adjustMapPadding(shouldBeVisible);
    if (!shouldBeVisible) {
      return;
    }
    updateLayerWithRoadLabelFeatures(roads, waynameLayer);
  }

  void updateWaynameLayer(String wayname, SymbolLayer waynameLayer) {
    if (waynameLayer != null) {
      createWaynameIcon(wayname, waynameLayer);
    }
  }

  void updateWaynameVisibility(boolean isVisible, SymbolLayer waynameLayer) {
    this.isVisible = isVisible;
    adjustMapPadding(isVisible);
    if (checkWaynameVisibility(isVisible, waynameLayer)) {
      return;
    }
    adjustWaynameVisibility(isVisible, waynameLayer);
  }

  void updateWaynameQueryMap(boolean isEnabled) {
    isAutoQueryEnabled = isEnabled;
  }

  boolean isVisible() {
    return isVisible;
  }

  private List<Feature> findRoadLabelFeatures(PointF point) {
    String[] layerIds = {STREETS_LAYER_ID};
    return featureInteractor.queryRenderedFeatures(point, layerIds);
  }

  private void updateLayerWithRoadLabelFeatures(List<Feature> roads, SymbolLayer waynameLayer) {
    boolean isValidFeatureList = !roads.isEmpty();
    if (isValidFeatureList) {
      Feature roadFeature = roads.get(FIRST_ROAD_FEATURE);
      updateWaynameLayerWithNameProperty(waynameLayer, roadFeature);
    } else {
      updateWaynameVisibility(false, waynameLayer);
    }
  }

  private void createWaynameIcon(String wayname, Layer waynameLayer) {
    boolean isVisible = waynameLayer.getVisibility().getValue().contentEquals(Property.VISIBLE);
    if (isVisible) {
      Bitmap waynameLayoutBitmap = layoutProvider.generateLayoutBitmap(wayname);
      if (waynameLayoutBitmap != null) {
        layerInteractor.addLayerImage(MAPBOX_WAYNAME_ICON, waynameLayoutBitmap);
        waynameLayer.setProperties(iconImage(MAPBOX_WAYNAME_ICON));
      }
    }
  }

  private boolean checkWaynameVisibility(boolean isVisible, Layer waynameLayer) {
    return (isVisible && isWaynameVisible(waynameLayer)) || !isVisible && !isWaynameVisible(waynameLayer);
  }

  private boolean isWaynameVisible(Layer waynameLayer) {
    return waynameLayer != null && waynameLayer.getVisibility().getValue().contentEquals(Property.VISIBLE);
  }

  private void adjustWaynameVisibility(boolean isVisible, Layer waynameLayer) {
    if (waynameLayer != null) {
      waynameLayer.setProperties(visibility(isVisible ? Property.VISIBLE : Property.NONE));
    }
  }

  private void updateWaynameLayerWithNameProperty(SymbolLayer waynameLayer, Feature roadFeature) {
    boolean hasValidNameProperty = roadFeature.hasNonNullValueForProperty(NAME_PROPERTY);
    if (hasValidNameProperty) {
      String currentWayname = roadFeature.getStringProperty(NAME_PROPERTY);
      boolean newWayname = !wayname.contentEquals(currentWayname);
      if (newWayname) {
        wayname = currentWayname;
        updateWaynameVisibility(true, waynameLayer);
        updateWaynameLayer(wayname, waynameLayer);
      }
    }
  }

  private void adjustMapPadding(boolean isVisible) {
    if (isVisible) {
      paddingAdjustor.updateTopPaddingWithWayname();
    } else {
      paddingAdjustor.updateTopPaddingWithDefault();
    }
  }
}
