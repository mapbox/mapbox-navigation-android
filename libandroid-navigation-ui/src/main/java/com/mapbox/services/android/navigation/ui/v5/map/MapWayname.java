package com.mapbox.services.android.navigation.ui.v5.map;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
import static com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap.STREETS_LAYER_ID;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.MAPBOX_WAYNAME_ICON;

class MapWayname {

  private static final String NAME_PROPERTY = "name";
  private final MapWaynameProgressChangeListener progressChangeListener = new MapWaynameProgressChangeListener(this);
  private final Set<OnWayNameChangedListener> onWayNameChangedListeners;
  private WaynameLayoutProvider layoutProvider;
  private MapLayerInteractor layerInteractor;
  private WaynameFeatureFinder featureInteractor;
  private List<Point> currentStepPoints = new ArrayList<>();
  private Location currentLocation = null;
  private MapboxNavigation navigation;
  private boolean isAutoQueryEnabled;
  private boolean isVisible;
  private FeatureFilterTask filterTask;
  private String wayname = "";

  MapWayname(WaynameLayoutProvider layoutProvider, MapLayerInteractor layerInteractor,
             WaynameFeatureFinder featureInteractor, MapPaddingAdjustor paddingAdjustor) {
    this.layoutProvider = layoutProvider;
    this.layerInteractor = layerInteractor;
    this.featureInteractor = featureInteractor;
    paddingAdjustor.updatePaddingWithDefault();
    this.onWayNameChangedListeners = new HashSet<>();
  }

  void updateWaynameWithPoint(PointF point, SymbolLayer waynameLayer) {
    if (!isAutoQueryEnabled) {
      return;
    }
    List<Feature> roadLabelFeatures = findRoadLabelFeatures(point);
    boolean invalidLabelFeatures = roadLabelFeatures.isEmpty();
    if (invalidLabelFeatures) {
      updateVisibility(false, waynameLayer);
      return;
    }
    executeFeatureFilterTask(roadLabelFeatures, waynameLayer);
  }

  void updateWaynameLayer(String wayname, SymbolLayer waynameLayer) {
    if (waynameLayer != null) {
      createWaynameIcon(wayname, waynameLayer);
    }
  }

  void updateProgress(Location currentLocation, List<Point> currentStepPoints) {
    if (!this.currentStepPoints.equals(currentStepPoints)) {
      this.currentStepPoints = currentStepPoints;
    }
    if (this.currentLocation == null || !this.currentLocation.equals(currentLocation)) {
      this.currentLocation = currentLocation;
    }
  }

  void updateWaynameVisibility(boolean isVisible, SymbolLayer waynameLayer) {
    this.isVisible = isVisible;
    updateVisibility(isVisible, waynameLayer);
  }

  void updateWaynameQueryMap(boolean isEnabled) {
    isAutoQueryEnabled = isEnabled;
  }

  boolean isVisible() {
    return isVisible;
  }

  String retrieveWayname() {
    return wayname;
  }

  void addProgressChangeListener(MapboxNavigation navigation) {
    this.navigation = navigation;
    navigation.addProgressChangeListener(progressChangeListener);
  }

  boolean addOnWayNameChangedListener(OnWayNameChangedListener listener) {
    return onWayNameChangedListeners.add(listener);
  }

  boolean removeOnWayNameChangedListener(OnWayNameChangedListener listener) {
    return onWayNameChangedListeners.remove(listener);
  }

  void onStart() {
    if (navigation != null) {
      navigation.addProgressChangeListener(progressChangeListener);
    }
  }

  void onStop() {
    if (isTaskRunning()) {
      filterTask.cancel(true);
    }
    if (navigation != null) {
      navigation.removeProgressChangeListener(progressChangeListener);
    }
  }

  private List<Feature> findRoadLabelFeatures(PointF point) {
    String[] layerIds = {STREETS_LAYER_ID};
    return featureInteractor.queryRenderedFeatures(point, layerIds);
  }

  private void executeFeatureFilterTask(List<Feature> roadFeatures, final SymbolLayer waynameLayer) {
    if (isTaskRunning()) {
      filterTask.cancel(true);
    }

    if (hasValidProgressData()) {
      filterTask = new FeatureFilterTask(roadFeatures, currentLocation, currentStepPoints,
        new OnFeatureFilteredCallback() {
          @Override
          public void onFeatureFiltered(@NonNull Feature feature) {
            updateWaynameLayerWithNameProperty(waynameLayer, feature);
          }
        });
      filterTask.execute();
    }
  }

  private boolean isTaskRunning() {
    return filterTask != null
      && (filterTask.getStatus() == AsyncTask.Status.PENDING
      || filterTask.getStatus() == AsyncTask.Status.RUNNING);
  }

  private boolean hasValidProgressData() {
    return currentLocation != null && !currentStepPoints.isEmpty();
  }

  private void createWaynameIcon(String wayname, Layer waynameLayer) {
    Bitmap waynameLayoutBitmap = layoutProvider.generateLayoutBitmap(wayname);
    if (waynameLayoutBitmap != null) {
      layerInteractor.addLayerImage(MAPBOX_WAYNAME_ICON, waynameLayoutBitmap);
      waynameLayer.setProperties(iconImage(MAPBOX_WAYNAME_ICON));
    }
  }

  private void updateVisibility(boolean isVisible, SymbolLayer waynameLayer) {
    if (checkWaynameVisibility(isVisible, waynameLayer)) {
      return;
    }
    adjustWaynameVisibility(isVisible, waynameLayer);
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
        updateListenersWith(currentWayname);
        wayname = currentWayname;
        if (isVisible) {
          updateVisibility(true, waynameLayer);
        }
        updateWaynameLayer(wayname, waynameLayer);
      }
    } else {
      updateVisibility(false, waynameLayer);
    }
  }

  private void updateListenersWith(String currentWayName) {
    for (OnWayNameChangedListener listener : onWayNameChangedListeners) {
      listener.onWayNameChanged(currentWayName);
    }
  }
}
