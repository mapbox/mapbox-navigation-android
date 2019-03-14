package com.mapbox.services.android.navigation.ui.v5.route;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.BaseTest;
import com.mapbox.services.android.navigation.ui.v5.R;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ROUTE_LAYER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class MapRouteLineTest extends BaseTest {

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
  public void onRedraw_routeLineSourceIsSet() throws IOException {
    GeoJsonSource routeLineSource = mock(GeoJsonSource.class);
    GeoJsonSource wayPointSource = mock(GeoJsonSource.class);
    List<Layer> routeLayers = buildMockLayers();
    List<DirectionsRoute> routes = new ArrayList<>();
    routes.add(buildTestDirectionsRoute());
    routes.add(buildTestDirectionsRoute());
    MapRouteLine routeLine = new MapRouteLine(routeLineSource, wayPointSource, routeLayers);

    routeLine.redraw(routes, false, 0, true);

    verify(routeLineSource, times(3)).setGeoJson(any(FeatureCollection.class));
  }

  @Test
  public void onRedraw_wayPointSourceIsSet() throws IOException {
    GeoJsonSource routeLineSource = mock(GeoJsonSource.class);
    GeoJsonSource wayPointSource = mock(GeoJsonSource.class);
    List<Layer> routeLayers = buildMockLayers();
    List<DirectionsRoute> routes = new ArrayList<>();
    routes.add(buildTestDirectionsRoute());
    routes.add(buildTestDirectionsRoute());
    MapRouteLine routeLine = new MapRouteLine(routeLineSource, wayPointSource, routeLayers);

    routeLine.redraw(routes, false, 0, true);

    verify(wayPointSource, times(2)).setGeoJson(any(FeatureCollection.class));
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
  public void routeLineCap_defaultIsSet() {
    Context context = mock(Context.class);
    TypedArray typedArray = mock(TypedArray.class);
    when(context.obtainStyledAttributes(anyInt(), eq(R.styleable.NavigationMapRoute))).thenReturn(typedArray);
    when(typedArray.getBoolean(R.styleable.NavigationMapRoute_roundedLineCap, true)).thenReturn(true);
    MapboxMap mapboxMap = buildMockMap();
    MapRouteLayerProvider layerProvider = mock(MapRouteLayerProvider.class);

    new MapRouteLine(context, mapboxMap, R.style.NavigationMapRoute, "",
      mock(MapRouteDrawableProvider.class), mock(MapRouteSourceProvider.class), layerProvider
    );

    verify(layerProvider).initializeRouteLayer(eq(mapboxMap), eq(true), anyFloat(), anyFloat(),
      anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()
    );
  }

  @Test
  public void routeLineCap_isSetFromAttributes() {
    Context context = mock(Context.class);
    TypedArray typedArray = mock(TypedArray.class);
    when(context.obtainStyledAttributes(anyInt(), eq(R.styleable.NavigationMapRoute))).thenReturn(typedArray);
    when(typedArray.getBoolean(R.styleable.NavigationMapRoute_roundedLineCap, true)).thenReturn(false);
    MapboxMap mapboxMap = buildMockMap();
    MapRouteLayerProvider layerProvider = mock(MapRouteLayerProvider.class);

    new MapRouteLine(context, mapboxMap, R.style.NavigationMapRoute, "",
      mock(MapRouteDrawableProvider.class), mock(MapRouteSourceProvider.class), layerProvider
    );

    verify(layerProvider).initializeRouteLayer(eq(mapboxMap), eq(false), anyFloat(), anyFloat(),
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

  @NotNull
  private MapboxMap buildMockMap() {
    Style style = mock(Style.class);
    when(style.getLayers()).thenReturn(Collections.emptyList());
    MapboxMap mapboxMap = mock(MapboxMap.class);
    when(mapboxMap.getStyle()).thenReturn(style);
    return mapboxMap;
  }
}