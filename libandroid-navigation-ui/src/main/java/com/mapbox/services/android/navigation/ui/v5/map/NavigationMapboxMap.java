package com.mapbox.services.android.navigation.ui.v5.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.VectorSource;
import com.mapbox.services.android.navigation.ui.v5.NavigationSnapshotReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.ThemeSwitcher;
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_TOP;
import static com.mapbox.mapboxsdk.style.layers.Property.ICON_ROTATION_ALIGNMENT_VIEWPORT;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconRotationAlignment;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.MAPBOX_LOCATION_SOURCE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.MAPBOX_WAYNAME_LAYER;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.WAYNAME_OFFSET;

public class NavigationMapboxMap {

  static final String STREETS_LAYER_ID = "streetsLayer";
  private static final String STREETS_SOURCE_ID = "streetsSource";
  private static final String MAPBOX_STREETS_V7 = "mapbox://mapbox.mapbox-streets-v7";
  private static final String ROAD_LABEL = "road_label";
  private static final float DEFAULT_WIDTH = 20f;
  private static final int LAST_INDEX = 0;

  private MapboxMap mapboxMap;
  private NavigationCamera mapCamera;
  private NavigationMapRoute mapRoute;
  private LocationLayerPlugin locationLayer;
  private MapPaddingAdjustor mapPaddingAdjustor;
  private MapWayname mapWayname;
  private SymbolLayer waynameLayer;
  private List<Marker> mapMarkers = new ArrayList<>();

  public NavigationMapboxMap(MapView mapView, MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    initializeLocationLayer(mapView, mapboxMap);
    initializeMapPaddingAdjustor(mapView, mapboxMap);
    initializeWayname(mapView, mapboxMap, mapPaddingAdjustor);
    initializeRoute(mapView, mapboxMap);
    initializeCamera(mapboxMap);
  }

  public void addMarker(Context context, Point position) {
    Marker marker = createMarkerFromIcon(context, position);
    mapMarkers.add(marker);
  }

  public void clearMarkers() {
    removeAllMarkers();
  }

  public void updateLocation(Location location) {
    locationLayer.forceLocationUpdate(location);
    updateMapWaynameWithLocation(location);
  }

  public void addProgressChangeListener(MapboxNavigation navigation) {
    mapRoute.addProgressChangeListener(navigation);
    mapCamera.addProgressChangeListener(navigation);
  }

  public void drawRoute(@NonNull DirectionsRoute route) {
    mapRoute.addRoute(route);
  }

  public void removeRoute() {
    mapRoute.removeRoute();
  }

  public void updateCameraTrackingEnabled(boolean isEnabled) {
    mapCamera.updateCameraTrackingLocation(isEnabled);
  }

  public void startCamera(DirectionsRoute directionsRoute) {
    mapCamera.start(directionsRoute);
  }

  public void resumeCamera(Location location) {
    mapCamera.resume(location);
  }

  public void resetCameraPosition() {
    mapCamera.resetCameraPosition();
    resetMapPadding();
  }

  public void showRouteOverview(int[] padding) {
    mapPaddingAdjustor.removeAllPadding();
    mapCamera.showRouteOverview(padding);
  }

  public void updateWaynameView(String wayname) {
    mapWayname.updateWaynameLayer(wayname, waynameLayer);
  }

  public void updateWaynameVisibility(boolean isVisible) {
    mapWayname.updateWaynameVisibility(isVisible, waynameLayer);
  }

  public void updateWaynameQueryMap(boolean isEnabled) {
    mapWayname.updateWaynameQueryMap(isEnabled);
  }

  @SuppressLint("MissingPermission")
  public void onStart() {
    locationLayer.onStart();
    mapCamera.onStart();
    mapRoute.onStart();
  }

  public void onStop() {
    locationLayer.onStop();
    mapCamera.onStop();
    mapRoute.onStop();
  }

  @SuppressLint("MissingPermission")
  public void updateLocationLayerVisibilityTo(boolean isVisible) {
    locationLayer.setLocationLayerEnabled(isVisible);
  }

  public MapboxMap retrieveMap() {
    return mapboxMap;
  }

  public void addOnMoveListener(@NonNull MapboxMap.OnMoveListener onMoveListener) {
    mapboxMap.addOnMoveListener(onMoveListener);
  }

  public void removeOnMoveListener(MapboxMap.OnMoveListener onMoveListener) {
    if (onMoveListener != null) {
      mapboxMap.removeOnMoveListener(onMoveListener);
    }
  }

  public void takeScreenshot(NavigationSnapshotReadyCallback navigationSnapshotReadyCallback) {
    mapboxMap.snapshot(navigationSnapshotReadyCallback);
  }

  private void initializeLocationLayer(MapView mapView, MapboxMap map) {
    Context context = mapView.getContext();
    int locationLayerStyleRes = ThemeSwitcher.retrieveNavigationViewStyle(context,
      R.attr.navigationViewLocationLayerStyle);
    locationLayer = new LocationLayerPlugin(mapView, map, null, locationLayerStyleRes);
    locationLayer.setRenderMode(RenderMode.GPS);
  }

  private void initializeMapPaddingAdjustor(MapView mapView, MapboxMap mapboxMap) {
    mapPaddingAdjustor = new MapPaddingAdjustor(mapView, mapboxMap);
  }

  private void initializeCamera(MapboxMap map) {
    mapCamera = new NavigationCamera(map);
  }

  private void initializeWayname(MapView mapView, MapboxMap mapboxMap, MapPaddingAdjustor paddingAdjustor) {
    initializeStreetsSource(mapboxMap);
    WaynameLayoutProvider layoutProvider = new WaynameLayoutProvider(mapView.getContext());
    WaynameLayerInteractor layerInteractor = new WaynameLayerInteractor(mapboxMap);
    WaynameFeatureFinder featureInteractor = new WaynameFeatureFinder(mapboxMap);
    initializeWaynameLayer(layerInteractor);
    mapWayname = new MapWayname(layoutProvider, layerInteractor, featureInteractor, paddingAdjustor);
  }

  private void initializeWaynameLayer(WaynameLayerInteractor layerInteractor) {
    waynameLayer = createWaynameLayer();
    layerInteractor.addLayer(waynameLayer);
  }

  private SymbolLayer createWaynameLayer() {
    return new SymbolLayer(MAPBOX_WAYNAME_LAYER, MAPBOX_LOCATION_SOURCE)
      .withProperties(
        iconAllowOverlap(true),
        iconIgnorePlacement(true),
        iconSize(
          interpolate(exponential(1f), zoom(),
            stop(0f, 0.6f),
            stop(18f, 1.2f)
          )
        ),
        iconAnchor(ICON_ANCHOR_TOP),
        iconOffset(WAYNAME_OFFSET),
        iconRotationAlignment(ICON_ROTATION_ALIGNMENT_VIEWPORT)
      );
  }

  private void initializeStreetsSource(MapboxMap mapboxMap) {
    VectorSource streetSource = new VectorSource(STREETS_SOURCE_ID, MAPBOX_STREETS_V7);
    mapboxMap.addSource(streetSource);
    LineLayer streetsLayer = new LineLayer(STREETS_LAYER_ID, STREETS_SOURCE_ID)
      .withProperties(
        lineWidth(DEFAULT_WIDTH),
        lineColor(Color.WHITE)
      )
      .withSourceLayer(ROAD_LABEL);
    mapboxMap.addLayerAt(streetsLayer, LAST_INDEX);
  }

  private void resetMapPadding() {
    if (mapWayname.isVisible()) {
      mapPaddingAdjustor.updateTopPaddingWithWayname();
    } else {
      mapPaddingAdjustor.updateTopPaddingWithDefault();
    }
  }

  private void initializeRoute(MapView mapView, MapboxMap map) {
    Context context = mapView.getContext();
    int routeStyleRes = ThemeSwitcher.retrieveNavigationViewStyle(context, R.attr.navigationViewRouteStyle);
    mapRoute = new NavigationMapRoute(null, mapView, map, routeStyleRes);
  }

  @NonNull
  private Marker createMarkerFromIcon(Context context, Point position) {
    LatLng markerPosition = new LatLng(position.latitude(),
      position.longitude());
    Icon markerIcon = ThemeSwitcher.retrieveThemeMapMarker(context);
    return mapboxMap.addMarker(new MarkerOptions()
      .position(markerPosition)
      .icon(markerIcon));
  }

  private void removeAllMarkers() {
    for (Marker marker : mapMarkers) {
      mapboxMap.removeMarker(marker);
    }
  }

  private void updateMapWaynameWithLocation(Location location) {
    LatLng latLng = new LatLng(location);
    PointF mapPoint = mapboxMap.getProjection().toScreenLocation(latLng);
    mapWayname.updateWaynameWithPoint(mapPoint, waynameLayer);
  }
}
