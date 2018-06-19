package com.mapbox.services.android.navigation.ui.v5.map;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.support.annotation.NonNull;

import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.MAPBOX_WAYNAME_ICON;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.MAPBOX_WAYNAME_LAYER;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapWaynameTest {

  @Test
  public void onUpdateWaynameWithPoint_queryRenderedFeaturesIsCalled() {
    WaynameLayoutProvider layoutProvider = mock(WaynameLayoutProvider.class);
    WaynameLayerInteractor layerInteractor = mock(WaynameLayerInteractor.class);
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    when(waynameLayer.getVisibility()).thenReturn(visibility(Property.VISIBLE));
    when(layerInteractor.retrieveLayerFromId(MAPBOX_WAYNAME_LAYER)).thenReturn(waynameLayer);
    WaynameFeatureFinder featureInteractor = mock(WaynameFeatureFinder.class);
    MapPaddingAdjustor paddingAdjustor = mock(MapPaddingAdjustor.class);
    String[] layerIds = {"streetsLayer"};
    PointF point = mock(PointF.class);
    MapWayname mapWayname = new MapWayname(layoutProvider, layerInteractor, featureInteractor, paddingAdjustor);
    mapWayname.updateWaynameVisibility(true, waynameLayer);
    mapWayname.updateWaynameQueryMap(true);

    mapWayname.updateWaynameWithPoint(point, waynameLayer);

    verify(featureInteractor).queryRenderedFeatures(point, layerIds);
  }

  @Test
  public void onRoadsReturnedFromQuery_layoutIconAdded() {
    String roadName = "roadName";
    PointF point = mock(PointF.class);
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    List<Feature> roads = new ArrayList<>();
    Feature road = mock(Feature.class);
    when(road.hasNonNullValueForProperty("name")).thenReturn(true);
    when(road.getStringProperty("name")).thenReturn(roadName);
    roads.add(road);
    WaynameLayoutProvider layoutProvider = mock(WaynameLayoutProvider.class);
    when(layoutProvider.generateLayoutBitmap(roadName)).thenReturn(mock(Bitmap.class));
    MapWayname mapWayname = buildMapWayname(point, layoutProvider, waynameLayer, roads);
    mapWayname.updateWaynameVisibility(true, waynameLayer);

    mapWayname.updateWaynameWithPoint(point, waynameLayer);

    verify(layoutProvider, times(1)).generateLayoutBitmap(roadName);
  }

  @Test
  public void onFeatureWithoutNamePropertyReturned_updateIsIgnored() {
    PointF point = mock(PointF.class);
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    List<Feature> roads = new ArrayList<>();
    Feature road = mock(Feature.class);
    roads.add(road);
    MapWayname mapWayname = buildMapWayname(point, waynameLayer, roads);

    mapWayname.updateWaynameWithPoint(point, waynameLayer);

    verify(waynameLayer, times(0)).setProperties(any(PropertyValue.class));
  }

  @Test
  public void onVisibiltySetToFalse_paddingIsAdjusted() {
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    when(waynameLayer.getVisibility()).thenReturn(visibility(Property.VISIBLE));
    WaynameLayerInteractor layerInteractor = mock(WaynameLayerInteractor.class);
    when(layerInteractor.retrieveLayerFromId(MAPBOX_WAYNAME_LAYER)).thenReturn(waynameLayer);
    MapPaddingAdjustor paddingAdjustor = mock(MapPaddingAdjustor.class);
    MapWayname mapWayname = buildMapWayname(layerInteractor, paddingAdjustor);

    mapWayname.updateWaynameVisibility(false, waynameLayer);

    verify(paddingAdjustor).updateTopPaddingWithDefault();
  }

  @Test
  public void onVisibiltySetToTrue_paddingIsAdjusted() {
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    when(waynameLayer.getVisibility()).thenReturn(visibility(Property.NONE));
    WaynameLayerInteractor layerInteractor = mock(WaynameLayerInteractor.class);
    when(layerInteractor.retrieveLayerFromId(MAPBOX_WAYNAME_LAYER)).thenReturn(waynameLayer);
    MapPaddingAdjustor paddingAdjustor = mock(MapPaddingAdjustor.class);
    MapWayname mapWayname = buildMapWayname(layerInteractor, paddingAdjustor);

    mapWayname.updateWaynameVisibility(true, waynameLayer);

    verify(paddingAdjustor).updateTopPaddingWithWayname();
  }

  @Test
  public void onVisibiltySetToFalse_isVisibleReturnsFalse() {
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    when(waynameLayer.getVisibility()).thenReturn(visibility(Property.VISIBLE));
    WaynameLayerInteractor layerInteractor = mock(WaynameLayerInteractor.class);
    when(layerInteractor.retrieveLayerFromId(MAPBOX_WAYNAME_LAYER)).thenReturn(waynameLayer);
    MapPaddingAdjustor paddingAdjustor = mock(MapPaddingAdjustor.class);
    MapWayname mapWayname = buildMapWayname(layerInteractor, paddingAdjustor);

    mapWayname.updateWaynameVisibility(false, waynameLayer);

    assertFalse(mapWayname.isVisible());
  }

  @Test
  public void onVisibiltySetToTrue_isVisibleReturnsTrue() {
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    when(waynameLayer.getVisibility()).thenReturn(visibility(Property.NONE));
    WaynameLayerInteractor layerInteractor = mock(WaynameLayerInteractor.class);
    when(layerInteractor.retrieveLayerFromId(MAPBOX_WAYNAME_LAYER)).thenReturn(waynameLayer);
    MapPaddingAdjustor paddingAdjustor = mock(MapPaddingAdjustor.class);
    MapWayname mapWayname = buildMapWayname(layerInteractor, paddingAdjustor);

    mapWayname.updateWaynameVisibility(true, waynameLayer);

    assertTrue(mapWayname.isVisible());
  }

  @Test
  public void onRoadsReturnedFromQuery_layoutProviderGeneratesBitmap() {
    String roadName = "roadName";
    PointF point = mock(PointF.class);
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    List<Feature> roads = new ArrayList<>();
    Feature road = mock(Feature.class);
    when(road.hasNonNullValueForProperty("name")).thenReturn(true);
    when(road.getStringProperty("name")).thenReturn(roadName);
    roads.add(road);
    WaynameLayoutProvider layoutProvider = mock(WaynameLayoutProvider.class);
    when(layoutProvider.generateLayoutBitmap(roadName)).thenReturn(mock(Bitmap.class));
    MapWayname mapWayname = buildMapWayname(point, layoutProvider, waynameLayer, roads);
    mapWayname.updateWaynameVisibility(true, waynameLayer);

    mapWayname.updateWaynameWithPoint(point, waynameLayer);

    verify(layoutProvider).generateLayoutBitmap(roadName);
  }

  @Test
  public void onUpdateWaynameLayer_layerImageIsAdded() {
    String roadName = "roadName";
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    when(waynameLayer.getVisibility()).thenReturn(visibility(Property.VISIBLE));
    WaynameLayerInteractor layerInteractor = mock(WaynameLayerInteractor.class);
    when(layerInteractor.retrieveLayerFromId(MAPBOX_WAYNAME_LAYER)).thenReturn(waynameLayer);
    Bitmap bitmap = mock(Bitmap.class);
    WaynameLayoutProvider layoutProvider = mock(WaynameLayoutProvider.class);
    when(layoutProvider.generateLayoutBitmap(roadName)).thenReturn(bitmap);
    MapWayname mapWayname = buildMapWayname(layoutProvider, layerInteractor);

    mapWayname.updateWaynameLayer(roadName, waynameLayer);

    verify(layerInteractor).addLayerImage(MAPBOX_WAYNAME_ICON, bitmap);
  }

  @NonNull
  private MapWayname buildMapWayname(PointF point, WaynameLayoutProvider layoutProvider,
                                     SymbolLayer waynameLayer, List<Feature> roads) {
    String[] layerIds = {"streetsLayer"};
    WaynameLayerInteractor layerInteractor = mock(WaynameLayerInteractor.class);
    when(waynameLayer.getVisibility()).thenReturn(visibility(Property.VISIBLE));
    when(layerInteractor.retrieveLayerFromId(MAPBOX_WAYNAME_LAYER)).thenReturn(waynameLayer);
    WaynameFeatureFinder featureInteractor = mock(WaynameFeatureFinder.class);
    when(featureInteractor.queryRenderedFeatures(point, layerIds)).thenReturn(roads);
    MapPaddingAdjustor paddingAdjustor = mock(MapPaddingAdjustor.class);
    MapWayname mapWayname = new MapWayname(layoutProvider, layerInteractor, featureInteractor, paddingAdjustor);
    mapWayname.updateWaynameQueryMap(true);
    return mapWayname;
  }

  @NonNull
  private MapWayname buildMapWayname(WaynameLayoutProvider layoutProvider, WaynameLayerInteractor layerInteractor) {
    WaynameFeatureFinder featureInteractor = mock(WaynameFeatureFinder.class);
    MapPaddingAdjustor paddingAdjustor = mock(MapPaddingAdjustor.class);
    MapWayname mapWayname = new MapWayname(layoutProvider, layerInteractor, featureInteractor, paddingAdjustor);
    mapWayname.updateWaynameQueryMap(true);
    return mapWayname;
  }

  @NonNull
  private MapWayname buildMapWayname(WaynameLayerInteractor layerInteractor, MapPaddingAdjustor paddingAdjustor) {
    WaynameLayoutProvider layoutProvider = mock(WaynameLayoutProvider.class);
    WaynameFeatureFinder featureInteractor = mock(WaynameFeatureFinder.class);
    MapWayname mapWayname = new MapWayname(layoutProvider, layerInteractor, featureInteractor, paddingAdjustor);
    mapWayname.updateWaynameQueryMap(true);
    return mapWayname;
  }

  @NonNull
  private MapWayname buildMapWayname(PointF point, SymbolLayer waynameLayer, List<Feature> roads) {
    String roadName = "roadName";
    String[] layerIds = {"streetsLayer"};
    WaynameLayoutProvider layoutProvider = mock(WaynameLayoutProvider.class);
    when(layoutProvider.generateLayoutBitmap(roadName)).thenReturn(mock(Bitmap.class));
    WaynameLayerInteractor layerInteractor = mock(WaynameLayerInteractor.class);
    when(waynameLayer.getVisibility()).thenReturn(visibility(Property.VISIBLE));
    when(layerInteractor.retrieveLayerFromId(MAPBOX_WAYNAME_LAYER)).thenReturn(waynameLayer);
    WaynameFeatureFinder featureInteractor = mock(WaynameFeatureFinder.class);
    when(featureInteractor.queryRenderedFeatures(point, layerIds)).thenReturn(roads);
    MapPaddingAdjustor paddingAdjustor = mock(MapPaddingAdjustor.class);
    MapWayname mapWayname = new MapWayname(layoutProvider, layerInteractor, featureInteractor, paddingAdjustor);
    mapWayname.updateWaynameQueryMap(true);
    return mapWayname;

  }
}