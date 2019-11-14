package com.mapbox.services.android.navigation.ui.v5.route;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.BaseTest;
import com.mapbox.services.android.navigation.ui.v5.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ROUTE_LAYER_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ROUTE_SHIELD_LAYER_ID;
import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.WAYPOINT_LAYER_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class MapRouteLineTest extends BaseTest {

  private Style style;

  @Before
  public void setUp() {
    style = mock(Style.class);
    when(style.getLayers()).thenReturn(Collections.emptyList());
  }

  @Test
  public void onDraw_routeLineSourceIsSet() throws IOException {
    GeoJsonSource routeLineSource = mock(GeoJsonSource.class);
    GeoJsonSource wayPointSource = mock(GeoJsonSource.class);
    List<Layer> routeLayers = buildMockLayers();
    DirectionsRoute route = buildTestDirectionsRoute();
    MapRouteLine routeLine = new MapRouteLine(routeLineSource, wayPointSource, routeLayers);

    routeLine.draw(route);

    verify(routeLineSource, times(3)).setGeoJson(any(FeatureCollection.class));
  }

  @Test
  public void onDraw_wayPointSourceIsSet() throws IOException {
    GeoJsonSource routeLineSource = mock(GeoJsonSource.class);
    GeoJsonSource wayPointSource = mock(GeoJsonSource.class);
    List<Layer> routeLayers = buildMockLayers();
    DirectionsRoute route = buildTestDirectionsRoute();
    MapRouteLine routeLine = new MapRouteLine(routeLineSource, wayPointSource, routeLayers);

    routeLine.draw(route);

    verify(wayPointSource, times(2)).setGeoJson(any(FeatureCollection.class));
  }

  @Test
  public void onStyleLoaded_recreateRouteLine() {
    TypedArray typedArray = mock(TypedArray.class);
    Context context = mock(Context.class);
    when(context.obtainStyledAttributes(anyInt(), any(int[].class))).thenReturn(typedArray);

    LineLayer routeShieldLayer = mock(LineLayer.class);
    when(routeShieldLayer.getId()).thenReturn(ROUTE_SHIELD_LAYER_ID);
    LineLayer routeLayer = mock(LineLayer.class);
    when(routeLayer.getId()).thenReturn(ROUTE_LAYER_ID);
    SymbolLayer wayPointLayer = mock(SymbolLayer.class);
    when(wayPointLayer.getId()).thenReturn(WAYPOINT_LAYER_ID);
    MapRouteLayerProvider mapRouteLayerProvider = mock(MapRouteLayerProvider.class);
    when(mapRouteLayerProvider.initializeRouteLayer(
      eq(style), anyBoolean(), anyFloat(), anyFloat(),
      anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
      .thenReturn(routeLayer);
    when(mapRouteLayerProvider.initializeRouteShieldLayer(
      eq(style), anyFloat(), anyFloat(), anyInt(), anyInt()
    )).thenReturn(routeShieldLayer);
    when(mapRouteLayerProvider.initializeWayPointLayer(
      eq(style), any(Drawable.class), any(Drawable.class)
    )).thenReturn(wayPointLayer);

    FeatureCollection routesFeatureCollection = mock(FeatureCollection.class);
    FeatureCollection waypointsFeatureCollection = mock(FeatureCollection.class);
    GeoJsonSource routeLineSource = mock(GeoJsonSource.class);
    GeoJsonSource wayPointSource = mock(GeoJsonSource.class);
    MapRouteSourceProvider mapRouteSourceProvider = mock(MapRouteSourceProvider.class);
    when(mapRouteSourceProvider.build(
      eq(RouteConstants.ROUTE_SOURCE_ID), eq(routesFeatureCollection), any(GeoJsonOptions.class)
    )).thenReturn(routeLineSource);
    when(mapRouteSourceProvider.build(
      eq(RouteConstants.WAYPOINT_SOURCE_ID), eq(waypointsFeatureCollection), any(GeoJsonOptions.class)
    )).thenReturn(wayPointSource);

    new MapRouteLine(
      context,
      style,
      10,
      null,
      buildDrawableProvider(),
      mapRouteSourceProvider,
      mapRouteLayerProvider,
      routesFeatureCollection,
      waypointsFeatureCollection,
      new ArrayList<DirectionsRoute>(),
      new ArrayList<FeatureCollection>(),
      new HashMap<LineString, DirectionsRoute>(),
      0,
      true,
      true
    );

    verify(style).addLayer(routeLayer);
    verify(style).addLayer(routeShieldLayer);
    verify(style).addLayer(wayPointLayer);
    verify(style).addSource(routeLineSource);
    verify(style).addSource(wayPointSource);

    verify(routeLineSource, times(0)).setGeoJson(any(FeatureCollection.class));
    verify(wayPointSource, times(0)).setGeoJson(any(FeatureCollection.class));
  }

  @Test
  public void updatePrimaryIndex_routeLineSourceIsSet() throws IOException {
    GeoJsonSource routeLineSource = mock(GeoJsonSource.class);
    GeoJsonSource wayPointSource = mock(GeoJsonSource.class);
    List<Layer> routeLayers = buildMockLayers();
    List<DirectionsRoute> routes = new ArrayList<>();
    routes.add(buildTestDirectionsRoute());
    routes.add(buildTestDirectionsRoute());
    MapRouteLine routeLine = new MapRouteLine(routeLineSource, wayPointSource, routeLayers);
    routeLine.draw(routes);

    routeLine.updatePrimaryRouteIndex(1);

    verify(routeLineSource, times(4)).setGeoJson(any(FeatureCollection.class));
  }

  @Test
  public void updatePrimaryIndex_newPrimaryRouteIndexUpdated() throws IOException {
    GeoJsonSource mockedRouteLineSource = mock(GeoJsonSource.class);
    GeoJsonSource mockedWayPointSource = mock(GeoJsonSource.class);
    List<Layer> anyRouteLayers = buildMockLayers();
    List<DirectionsRoute> routes = new ArrayList<>();
    routes.add(buildTestDirectionsRoute());
    routes.add(buildTestDirectionsRoute());
    routes.add(buildTestDirectionsRoute());
    routes.add(buildTestDirectionsRoute());
    MapRouteLine routeLine = new MapRouteLine(mockedRouteLineSource, mockedWayPointSource, anyRouteLayers);
    routeLine.draw(routes);

    boolean isNewIndex = routeLine.updatePrimaryRouteIndex(3);

    assertTrue(isNewIndex);
    assertEquals(3, routeLine.retrievePrimaryRouteIndex());
  }

  @Test
  public void updatePrimaryIndex_newPrimaryRouteIndexIsNotUpdated() throws IOException {
    GeoJsonSource mockedRouteLineSource = mock(GeoJsonSource.class);
    GeoJsonSource mockedWayPointSource = mock(GeoJsonSource.class);
    List<Layer> anyRouteLayers = buildMockLayers();
    List<DirectionsRoute> routes = new ArrayList<>();
    routes.add(buildTestDirectionsRoute());
    routes.add(buildTestDirectionsRoute());
    routes.add(buildTestDirectionsRoute());
    routes.add(buildTestDirectionsRoute());
    MapRouteLine routeLine = new MapRouteLine(mockedRouteLineSource, mockedWayPointSource, anyRouteLayers);
    routeLine.draw(routes);

    boolean isNewIndex = routeLine.updatePrimaryRouteIndex(-1);

    assertFalse(isNewIndex);
    assertEquals(0, routeLine.retrievePrimaryRouteIndex());
  }

  @Test
  public void routeLineCap_defaultIsSet() {
    Context context = mock(Context.class);
    TypedArray typedArray = mock(TypedArray.class);
    when(context.obtainStyledAttributes(anyInt(), eq(R.styleable.NavigationMapRoute))).thenReturn(typedArray);
    when(typedArray.getBoolean(R.styleable.NavigationMapRoute_roundedLineCap, true)).thenReturn(true);
    MapRouteLayerProvider layerProvider = buildLayerProvider();

    new MapRouteLine(context, style, R.style.NavigationMapRoute, "",
      buildDrawableProvider(), mock(MapRouteSourceProvider.class), layerProvider
    );

    verify(layerProvider).initializeRouteLayer(eq(style), eq(true), anyFloat(), anyFloat(),
      anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()
    );
  }

  @Test
  public void routeLineCap_isSetFromAttributes() {
    Context context = mock(Context.class);
    TypedArray typedArray = mock(TypedArray.class);
    when(context.obtainStyledAttributes(anyInt(), eq(R.styleable.NavigationMapRoute))).thenReturn(typedArray);
    when(typedArray.getBoolean(R.styleable.NavigationMapRoute_roundedLineCap, true)).thenReturn(false);
    MapRouteLayerProvider layerProvider = buildLayerProvider();

    new MapRouteLine(context, style, R.style.NavigationMapRoute, "",
      buildDrawableProvider(), mock(MapRouteSourceProvider.class), layerProvider
    );

    verify(layerProvider).initializeRouteLayer(eq(style), eq(false), anyFloat(), anyFloat(),
      anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()
    );
  }

  @NonNull
  private List<Layer> buildMockLayers() {
    List<Layer> routeLayers = new ArrayList<>();
    LineLayer lineLayerOne = mock(LineLayer.class);
    when(lineLayerOne.getId()).thenReturn(ROUTE_LAYER_ID);
    routeLayers.add(lineLayerOne);
    LineLayer lineLayerTwo = mock(LineLayer.class);
    when(lineLayerTwo.getId()).thenReturn(ROUTE_LAYER_ID);
    routeLayers.add(lineLayerTwo);
    return routeLayers;
  }

  private MapRouteLayerProvider buildLayerProvider() {
    LineLayer routeShieldLayer = mock(LineLayer.class);
    when(routeShieldLayer.getId()).thenReturn(ROUTE_SHIELD_LAYER_ID);
    LineLayer routeLayer = mock(LineLayer.class);
    when(routeLayer.getId()).thenReturn(ROUTE_LAYER_ID);
    SymbolLayer wayPointLayer = mock(SymbolLayer.class);
    when(wayPointLayer.getId()).thenReturn(WAYPOINT_LAYER_ID);
    MapRouteLayerProvider mapRouteLayerProvider = mock(MapRouteLayerProvider.class);
    when(mapRouteLayerProvider.initializeRouteLayer(
      eq(style), anyBoolean(), anyFloat(), anyFloat(),
      anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
      .thenReturn(routeLayer);
    when(mapRouteLayerProvider.initializeRouteShieldLayer(
      eq(style), anyFloat(), anyFloat(), anyInt(), anyInt()
    )).thenReturn(routeShieldLayer);
    when(mapRouteLayerProvider.initializeWayPointLayer(
      eq(style), any(Drawable.class), any(Drawable.class)
    )).thenReturn(wayPointLayer);

    return mapRouteLayerProvider;
  }

  private MapRouteDrawableProvider buildDrawableProvider() {
    Drawable drawable = mock(Drawable.class);
    MapRouteDrawableProvider mapRouteDrawableProvider = mock(MapRouteDrawableProvider.class);
    when(mapRouteDrawableProvider.retrieveDrawable(anyInt())).thenReturn(drawable);
    return mapRouteDrawableProvider;
  }
}