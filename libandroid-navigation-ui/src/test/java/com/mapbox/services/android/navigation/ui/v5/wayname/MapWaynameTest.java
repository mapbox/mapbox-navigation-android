package com.mapbox.services.android.navigation.ui.v5.wayname;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.support.annotation.NonNull;

import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.services.android.navigation.ui.v5.map.MapFeatureInteractor;
import com.mapbox.services.android.navigation.ui.v5.map.MapLayerInteractor;
import com.mapbox.services.android.navigation.ui.v5.map.MapPaddingAdjustor;
import com.mapbox.services.android.navigation.ui.v5.map.MapWaynameLayoutProvider;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.MAPBOX_WAYNAME_ICON;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.MAPBOX_WAYNAME_LAYER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MapWaynameTest {

  @Test
  public void onUpdateWaynameWithPoint_queryRenderedFeaturesIsCalled() {
    MapWaynameLayoutProvider layoutProvider = mock(MapWaynameLayoutProvider.class);
    MapLayerInteractor layerInteractor = mock(MapLayerInteractor.class);
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    when(waynameLayer.getVisibility()).thenReturn(visibility(Property.VISIBLE));
    when(layerInteractor.retrieveLayerFromId(MAPBOX_WAYNAME_LAYER)).thenReturn(waynameLayer);
    MapFeatureInteractor featureInteractor = mock(MapFeatureInteractor.class);
    MapPaddingAdjustor paddingAdjustor = mock(MapPaddingAdjustor.class);
    String[] layerIds = {"streetsLayer"};
    PointF point = mock(PointF.class);
    MapWayname mapWayname = new MapWayname(layoutProvider, layerInteractor, featureInteractor, paddingAdjustor);
    mapWayname.updateWaynameQueryMap(true);

    mapWayname.updateWaynameWithPoint(point, waynameLayer);

    verify(featureInteractor).queryRenderedFeatures(point, layerIds);
  }

  @Test
  public void onNoRoadsReturnedFromQuery_visibilityIsSetToFalse() {
    PointF point = mock(PointF.class);
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    MapWayname mapWayname = buildMapWayname(point, waynameLayer, Collections.<Feature>emptyList());

    mapWayname.updateWaynameWithPoint(point, waynameLayer);

    verify(waynameLayer).setProperties(any(PropertyValue.class));
  }

  @Test
  public void onRoadsReturnedFromQuery_visibilityIsSetToTrueAndLayoutIconAdded() {
    String roadName = "roadName";
    PointF point = mock(PointF.class);
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    List<Feature> roads = new ArrayList<>();
    Feature road = mock(Feature.class);
    when(road.getStringProperty("name")).thenReturn(roadName);
    roads.add(road);
    MapWayname mapWayname = buildMapWayname(point, waynameLayer, roads);

    mapWayname.updateWaynameWithPoint(point, waynameLayer);

    verify(waynameLayer, times(1)).setProperties(any(PropertyValue.class));
  }

  @Test
  public void onVisibiltySetToFalse_paddingIsAdjusted() {
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    when(waynameLayer.getVisibility()).thenReturn(visibility(Property.VISIBLE));
    MapLayerInteractor layerInteractor = mock(MapLayerInteractor.class);
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
    MapLayerInteractor layerInteractor = mock(MapLayerInteractor.class);
    when(layerInteractor.retrieveLayerFromId(MAPBOX_WAYNAME_LAYER)).thenReturn(waynameLayer);
    MapPaddingAdjustor paddingAdjustor = mock(MapPaddingAdjustor.class);
    MapWayname mapWayname = buildMapWayname(layerInteractor, paddingAdjustor);

    mapWayname.updateWaynameVisibility(true, waynameLayer);

    verify(paddingAdjustor).updateTopPaddingWithWayname();
  }

  @Test
  public void onRoadsReturnedFromQuery_layoutProviderGeneratesBitmap() {
    String roadName = "roadName";
    PointF point = mock(PointF.class);
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    List<Feature> roads = new ArrayList<>();
    Feature road = mock(Feature.class);
    when(road.getStringProperty("name")).thenReturn(roadName);
    roads.add(road);
    MapWaynameLayoutProvider layoutProvider = mock(MapWaynameLayoutProvider.class);
    when(layoutProvider.generateLayoutBitmap(roadName)).thenReturn(mock(Bitmap.class));
    MapWayname mapWayname = buildMapWayname(point, layoutProvider, waynameLayer, roads);

    mapWayname.updateWaynameWithPoint(point, waynameLayer);

    verify(layoutProvider).generateLayoutBitmap(roadName);
  }

  @Test
  public void onUpdateWaynameLayer_layerImageIsAdded() {
    String roadName = "roadName";
    SymbolLayer waynameLayer = mock(SymbolLayer.class);
    when(waynameLayer.getVisibility()).thenReturn(visibility(Property.VISIBLE));
    MapLayerInteractor layerInteractor = mock(MapLayerInteractor.class);
    when(layerInteractor.retrieveLayerFromId(MAPBOX_WAYNAME_LAYER)).thenReturn(waynameLayer);
    Bitmap bitmap = mock(Bitmap.class);
    MapWaynameLayoutProvider layoutProvider = mock(MapWaynameLayoutProvider.class);
    when(layoutProvider.generateLayoutBitmap(roadName)).thenReturn(bitmap);
    MapWayname mapWayname = buildMapWayname(layoutProvider, layerInteractor);

    mapWayname.updateWaynameLayer(roadName, waynameLayer);

    verify(layerInteractor).addLayerImage(MAPBOX_WAYNAME_ICON, bitmap);
  }

  @NonNull
  private MapWayname buildMapWayname(PointF point, MapWaynameLayoutProvider layoutProvider,
                                     SymbolLayer waynameLayer, List<Feature> roads) {
    String[] layerIds = {"streetsLayer"};
    MapLayerInteractor layerInteractor = mock(MapLayerInteractor.class);
    when(waynameLayer.getVisibility()).thenReturn(visibility(Property.VISIBLE));
    when(layerInteractor.retrieveLayerFromId(MAPBOX_WAYNAME_LAYER)).thenReturn(waynameLayer);
    MapFeatureInteractor featureInteractor = mock(MapFeatureInteractor.class);
    when(featureInteractor.queryRenderedFeatures(point, layerIds)).thenReturn(roads);
    MapPaddingAdjustor paddingAdjustor = mock(MapPaddingAdjustor.class);
    MapWayname mapWayname = new MapWayname(layoutProvider, layerInteractor, featureInteractor, paddingAdjustor);
    mapWayname.updateWaynameQueryMap(true);
    return mapWayname;
  }

  @NonNull
  private MapWayname buildMapWayname(MapWaynameLayoutProvider layoutProvider, MapLayerInteractor layerInteractor) {
    MapFeatureInteractor featureInteractor = mock(MapFeatureInteractor.class);
    MapPaddingAdjustor paddingAdjustor = mock(MapPaddingAdjustor.class);
    MapWayname mapWayname = new MapWayname(layoutProvider, layerInteractor, featureInteractor, paddingAdjustor);
    mapWayname.updateWaynameQueryMap(true);
    return mapWayname;
  }

  @NonNull
  private MapWayname buildMapWayname(MapLayerInteractor layerInteractor, MapPaddingAdjustor paddingAdjustor) {
    MapWaynameLayoutProvider layoutProvider = mock(MapWaynameLayoutProvider.class);
    MapFeatureInteractor featureInteractor = mock(MapFeatureInteractor.class);
    MapWayname mapWayname = new MapWayname(layoutProvider, layerInteractor, featureInteractor, paddingAdjustor);
    mapWayname.updateWaynameQueryMap(true);
    return mapWayname;
  }

  @NonNull
  private MapWayname buildMapWayname(PointF point, SymbolLayer waynameLayer, List<Feature> roads) {
    String roadName = "roadName";
    String[] layerIds = {"streetsLayer"};
    MapWaynameLayoutProvider layoutProvider = mock(MapWaynameLayoutProvider.class);
    when(layoutProvider.generateLayoutBitmap(roadName)).thenReturn(mock(Bitmap.class));
    MapLayerInteractor layerInteractor = mock(MapLayerInteractor.class);
    when(waynameLayer.getVisibility()).thenReturn(visibility(Property.VISIBLE));
    when(layerInteractor.retrieveLayerFromId(MAPBOX_WAYNAME_LAYER)).thenReturn(waynameLayer);
    MapFeatureInteractor featureInteractor = mock(MapFeatureInteractor.class);
    when(featureInteractor.queryRenderedFeatures(point, layerIds)).thenReturn(roads);
    MapPaddingAdjustor paddingAdjustor = mock(MapPaddingAdjustor.class);
    MapWayname mapWayname = new MapWayname(layoutProvider, layerInteractor, featureInteractor, paddingAdjustor);
    mapWayname.updateWaynameQueryMap(true);
    return mapWayname;

  }
}