package com.mapbox.services.android.navigation.ui.v5.route;

import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.BaseTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.mapbox.services.android.navigation.ui.v5.route.RouteConstants.ROUTE_LAYER_ID;
import static org.mockito.ArgumentMatchers.any;
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
    List<Layer> routeLayers = new ArrayList<>();
    routeLayers.add(mock(Layer.class));
    routeLayers.add(mock(Layer.class));
    DirectionsRoute route = buildTestDirectionsRoute();
    MapRouteLine routeLine = new MapRouteLine(routeLineSource, wayPointSource, routeLayers);

    routeLine.draw(route);

    // two times because reset at beginning
    verify(routeLineSource, times(2)).setGeoJson(any(FeatureCollection.class));
  }

  @Test
  public void onDraw_wayPointSourceIsSet() throws IOException {
    GeoJsonSource routeLineSource = mock(GeoJsonSource.class);
    GeoJsonSource wayPointSource = mock(GeoJsonSource.class);
    List<Layer> routeLayers = new ArrayList<>();
    routeLayers.add(mock(Layer.class));
    routeLayers.add(mock(Layer.class));
    DirectionsRoute route = buildTestDirectionsRoute();
    MapRouteLine routeLine = new MapRouteLine(routeLineSource, wayPointSource, routeLayers);

    routeLine.draw(route);

    // two times because reset at beginning
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

    // two times because reset at beginning
    verify(routeLineSource, times(2)).setGeoJson(any(FeatureCollection.class));
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

    // two times because reset at beginning
    verify(wayPointSource, times(2)).setGeoJson(any(FeatureCollection.class));
  }

  @Test
  public void updatePrimaryIndex_routeLineSourceIsSet() throws IOException {
    GeoJsonSource routeLineSource = mock(GeoJsonSource.class);
    GeoJsonSource wayPointSource = mock(GeoJsonSource.class);
    List<Layer> routeLayers = new ArrayList<>();
    routeLayers.add(mock(Layer.class));
    routeLayers.add(mock(Layer.class));
    List<DirectionsRoute> routes = new ArrayList<>();
    routes.add(buildTestDirectionsRoute());
    routes.add(buildTestDirectionsRoute());
    MapRouteLine routeLine = new MapRouteLine(routeLineSource, wayPointSource, routeLayers);
    routeLine.draw(routes);

    routeLine.updatePrimaryRouteIndex(1);

    verify(routeLineSource, times(3)).setGeoJson(any(FeatureCollection.class));
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
}