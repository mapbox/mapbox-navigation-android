package com.mapbox.services.android.navigation.ui.v5.wayname;

import android.graphics.Bitmap;
import android.graphics.PointF;

import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.services.android.navigation.ui.v5.map.MapFeatureInteractor;
import com.mapbox.services.android.navigation.ui.v5.map.MapLayerInteractor;
import com.mapbox.services.android.navigation.ui.v5.map.MapPaddingAdjustor;
import com.mapbox.services.android.navigation.ui.v5.map.MapWaynameLayoutProvider;

import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
import static com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap.STREETS_LAYER_ID;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.MAPBOX_WAYNAME_ICON;

public class MapWayname {

  private static final String NAME_PROPERTY = "name";
  private static final int FIRST_ROAD_FEATURE = 0;

  private MapWaynameLayoutProvider layoutProvider;
  private MapLayerInteractor layerInteractor;
  private MapFeatureInteractor featureInteractor;
  private MapPaddingAdjustor paddingAdjustor;
  private boolean autoQueryIsEnabled;
  private String wayname = "";

  public MapWayname(MapWaynameLayoutProvider layoutProvider, MapLayerInteractor layerInteractor,
                    MapFeatureInteractor featureInteractor, MapPaddingAdjustor paddingAdjustor) {
    this.layoutProvider = layoutProvider;
    this.layerInteractor = layerInteractor;
    this.featureInteractor = featureInteractor;
    this.paddingAdjustor = paddingAdjustor;
  }

  public void updateWaynameWithPoint(PointF point, SymbolLayer waynameLayer) {
    if (!autoQueryIsEnabled) {
      return;
    }
    List<Feature> roads = findRoadLabelFeatures(point);
    updateLayerWithRoadLabelFeatures(roads, waynameLayer);
  }

  public void updateWaynameLayer(String wayname, SymbolLayer waynameLayer) {
    if (waynameLayer != null) {
      createWaynameIcon(wayname, waynameLayer);
    }
  }

  public void updateWaynameVisibility(boolean isVisible, SymbolLayer waynameLayer) {
    if (checkWaynameVisibility(isVisible, waynameLayer)) {
      return;
    }
    adjustWaynameVisibility(isVisible, waynameLayer);
  }

  public void updateWaynameQueryMap(boolean isEnabled) {
    autoQueryIsEnabled = isEnabled;
  }

  public void updateDefaultMapTopPadding(int topPadding) {
    paddingAdjustor.calculatePaddingValues(topPadding, layoutProvider.retrieveHeight());
  }

  private List<Feature> findRoadLabelFeatures(PointF point) {
    String[] layerIds = {STREETS_LAYER_ID};
    return featureInteractor.queryRenderedFeatures(point, layerIds);
  }

  private void updateLayerWithRoadLabelFeatures(List<Feature> roads, SymbolLayer waynameLayer) {
    if (!roads.isEmpty()) {
      String currentWayname = roads.get(FIRST_ROAD_FEATURE).getStringProperty(NAME_PROPERTY);
      boolean newWayname = !wayname.contentEquals(currentWayname);
      if (newWayname) {
        wayname = currentWayname;
        updateWaynameVisibility(true, waynameLayer);
        updateWaynameLayer(wayname, waynameLayer);
      }
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
      adjustMapPadding(isVisible);
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
