package com.mapbox.services.android.navigation.ui.v5.map;

import android.graphics.PointF;
import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class MapWayNameTest {

  @Test
  public void onUpdateWaynameWithPoint_queryRenderedFeaturesIsCalled() {
    WaynameFeatureFinder featureFinder = mock(WaynameFeatureFinder.class);
    MapPaddingAdjustor paddingAdjustor = mock(MapPaddingAdjustor.class);
    String[] layerIds = {"streetsLayer"};
    PointF point = mock(PointF.class);
    MapWayName mapWayName = new MapWayName(featureFinder, paddingAdjustor);
    mapWayName.updateWayNameQueryMap(true);

    mapWayName.updateWayNameWithPoint(point);

    verify(featureFinder).queryRenderedFeatures(point, layerIds);
  }

  @Test
  public void addOnWayNameChangedListener_duplicateListenerIgnored() {
    String roadName = "roadName";
    PointF point = mock(PointF.class);
    List<Feature> roads = buildRoadFeatureList(roadName);
    MapWayName mapWayName = buildMapWayname(point, roads);
    List<Point> stepPoints = new ArrayList<>();
    stepPoints.add(mock(Point.class));
    mapWayName.updateProgress(mock(Location.class), stepPoints);
    OnWayNameChangedListener listener = mock(OnWayNameChangedListener.class);

    mapWayName.addOnWayNameChangedListener(listener);
    boolean wasAdded = mapWayName.addOnWayNameChangedListener(listener);

    assertFalse(wasAdded);
  }

  @Test
  public void removeOnWayNameChangedListener_duplicateListenerIgnored() {
    String roadName = "roadName";
    PointF point = mock(PointF.class);
    List<Feature> roads = buildRoadFeatureList(roadName);
    MapWayName mapWayName = buildMapWayname(point, roads);
    List<Point> stepPoints = new ArrayList<>();
    stepPoints.add(mock(Point.class));
    mapWayName.updateProgress(mock(Location.class), stepPoints);
    OnWayNameChangedListener listener = mock(OnWayNameChangedListener.class);

    mapWayName.removeOnWayNameChangedListener(listener);
    boolean wasRemoved = mapWayName.removeOnWayNameChangedListener(listener);

    assertFalse(wasRemoved);
  }

  @Test
  public void onFeatureWithoutNamePropertyReturned_updateIsIgnored() {
    PointF point = mock(PointF.class);
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    List<Feature> roads = new ArrayList<>();
    Feature road = mock(Feature.class);
    roads.add(road);
    MapWayName mapWayName = buildMapWayname(point, roads);

    mapWayName.updateWayNameWithPoint(point);

    verify(waynameLayer, times(0)).setProperties(any(PropertyValue.class));
  }

  @NonNull
  private MapWayName buildMapWayname(PointF point, List<Feature> roads) {
    String[] layerIds = {"streetsLayer"};
    WaynameFeatureFinder featureInteractor = mock(WaynameFeatureFinder.class);
    when(featureInteractor.queryRenderedFeatures(point, layerIds)).thenReturn(roads);
    MapPaddingAdjustor paddingAdjustor = mock(MapPaddingAdjustor.class);
    MapWayName mapWayName = new MapWayName(featureInteractor, paddingAdjustor);
    mapWayName.updateWayNameQueryMap(true);
    return mapWayName;
  }

  @NonNull
  private List<Feature> buildRoadFeatureList(String roadName) {
    List<Feature> roads = new ArrayList<>();
    Feature road = mock(Feature.class);
    when(road.hasNonNullValueForProperty("name")).thenReturn(true);
    when(road.getStringProperty("name")).thenReturn(roadName);
    roads.add(road);
    return roads;
  }
}