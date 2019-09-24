package com.mapbox.services.android.navigation.ui.v5.map;

import android.graphics.PointF;
import android.location.Location;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap.STREETS_LAYER_ID;

class MapWayName {

  private static final String NAME_PROPERTY = "name";
  private static final String EMPTY_CURRENT_WAY_NAME = "";
  private final MapWaynameProgressChangeListener progressChangeListener = new MapWaynameProgressChangeListener(this);
  private final Set<OnWayNameChangedListener> onWayNameChangedListeners;
  private WaynameFeatureFinder featureInteractor;
  private List<Point> currentStepPoints = new ArrayList<>();
  private Location currentLocation = null;
  private MapboxNavigation navigation;
  private boolean isAutoQueryEnabled;
  private FeatureFilterTask filterTask;
  private String wayName = EMPTY_CURRENT_WAY_NAME;

  MapWayName(WaynameFeatureFinder featureInteractor, MapPaddingAdjustor paddingAdjustor) {
    this.featureInteractor = featureInteractor;
    paddingAdjustor.updatePaddingWithDefault();
    this.onWayNameChangedListeners = new HashSet<>();
  }

  void updateWayNameWithPoint(PointF point) {
    if (!isAutoQueryEnabled) {
      return;
    }
    List<Feature> roadLabelFeatures = findRoadLabelFeatures(point);
    boolean invalidLabelFeatures = roadLabelFeatures.isEmpty();
    if (invalidLabelFeatures) {
      return;
    }
    executeFeatureFilterTask(roadLabelFeatures);
  }

  void updateProgress(Location currentLocation, List<Point> currentStepPoints) {
    if (!this.currentStepPoints.equals(currentStepPoints)) {
      this.currentStepPoints = currentStepPoints;
    }
    if (this.currentLocation == null || !this.currentLocation.equals(currentLocation)) {
      this.currentLocation = currentLocation;
    }
  }

  void updateWayNameQueryMap(boolean isEnabled) {
    isAutoQueryEnabled = isEnabled;
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

  private void executeFeatureFilterTask(List<Feature> roadFeatures) {
    if (isTaskRunning()) {
      filterTask.cancel(true);
    }

    if (hasValidProgressData()) {
      filterTask = new FeatureFilterTask(roadFeatures, currentLocation, currentStepPoints,
        new OnFeatureFilteredCallback() {
          @Override
          public void onFeatureFiltered(@NonNull Feature feature) {
            updateWayNameLayerWithNameProperty(feature);
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

  private void updateWayNameLayerWithNameProperty(Feature roadFeature) {
    boolean hasValidNameProperty = roadFeature.hasNonNullValueForProperty(NAME_PROPERTY);
    if (hasValidNameProperty) {
      String currentWayName = roadFeature.getStringProperty(NAME_PROPERTY);
      boolean newWayName = !wayName.contentEquals(currentWayName);
      if (newWayName) {
        updateListenersWith(currentWayName);
        wayName = currentWayName;
      }
    } else {
      updateListenersWith(EMPTY_CURRENT_WAY_NAME);
    }
  }

  private void updateListenersWith(String currentWayName) {
    for (OnWayNameChangedListener listener : onWayNameChangedListeners) {
      listener.onWayNameChanged(currentWayName);
    }
  }
}
