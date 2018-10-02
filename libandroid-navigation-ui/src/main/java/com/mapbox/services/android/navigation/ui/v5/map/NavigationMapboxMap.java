package com.mapbox.services.android.navigation.ui.v5.map;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerOptions;
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
import com.mapbox.services.android.navigation.ui.v5.route.OnRouteSelectionChangeListener;
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
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_MINIMUM_MAP_ZOOM;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.WAYNAME_OFFSET;

/**
 * Wrapper class for {@link MapboxMap}.
 * <p>
 * This class initializes various map-related components and plugins that are
 * useful for providing a navigation-driven map experience.
 * <p>
 * These APIs include drawing a route line, camera animations, and more.
 */
public class NavigationMapboxMap {

  static final String STREETS_LAYER_ID = "streetsLayer";
  private static final String MAPBOX_STREETS_V7 = "mapbox://mapbox.mapbox-streets-v7";
  private static final String STREETS_SOURCE_ID = "streetsSource";
  private static final String ROAD_LABEL = "road_label";
  private static final float DEFAULT_WIDTH = 20f;
  private static final int LAST_INDEX = 0;
  private static final String INCIDENTS_LAYER_ID = "closures";
  private static final String TRAFFIC_LAYER_ID = "traffic";

  private MapboxMap mapboxMap;
  private NavigationCamera mapCamera;
  private NavigationMapRoute mapRoute;
  private LocationLayerPlugin locationLayer;
  private MapPaddingAdjustor mapPaddingAdjustor;
  private MapWayname mapWayname;
  private SymbolLayer waynameLayer;
  private MapLayerInteractor layerInteractor;
  private List<Marker> mapMarkers = new ArrayList<>();

  /**
   * Constructor that can be used once {@link com.mapbox.mapboxsdk.maps.OnMapReadyCallback}
   * has been called via {@link MapView#getMapAsync(OnMapReadyCallback)}.
   *
   * @param mapView   for map size and Context
   * @param mapboxMap for APIs to interact with the map
   */
  public NavigationMapboxMap(@NonNull MapView mapView, @NonNull MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    initializeLocationLayer(mapView, mapboxMap);
    initializeMapPaddingAdjustor(mapView, mapboxMap);
    initializeMapLayerInteractor(mapboxMap);
    initializeWayname(mapView, mapboxMap, layerInteractor, mapPaddingAdjustor);
    initializeRoute(mapView, mapboxMap);
    initializeCamera(mapboxMap);
  }

  // Package private (no modifier) for testing purposes
  NavigationMapboxMap(MapLayerInteractor layerInteractor) {
    this.layerInteractor = layerInteractor;
  }

  // Package private (no modifier) for testing purposes
  NavigationMapboxMap(LocationLayerPlugin locationLayer) {
    this.locationLayer = locationLayer;
  }

  // Package private (no modifier) for testing purposes
  NavigationMapboxMap(NavigationMapRoute mapRoute) {
    this.mapRoute = mapRoute;
  }

  /**
   * Adds a marker icon on the map at the given position.
   * <p>
   * The icon used for this method can be defined in your theme with
   * the attribute <tt>navigationViewDestinationMarker</tt>.
   *
   * @param context  to retrieve the icon drawable from the theme
   * @param position the point at which the marker will be placed
   */
  public void addMarker(Context context, Point position) {
    Marker marker = createMarkerFromIcon(context, position);
    mapMarkers.add(marker);
  }

  /**
   * Clears all markers on the map that have been added by this class.
   * <p>
   * This will not clear all markers from the map entirely.  Does nothing
   * if no markers have been added.
   */
  public void clearMarkers() {
    removeAllMarkers();
  }

  /**
   * Updates the location icon on the map and way name data (if found)
   * for the given {@link Location}.
   *
   * @param location to update the icon and query the map
   */
  public void updateLocation(Location location) {
    locationLayer.forceLocationUpdate(location);
    updateMapWaynameWithLocation(location);
  }

  /**
   * Updates how the user location is shown on the map.
   * <p>
   * <ul>
   * <li>{@link RenderMode#NORMAL}: Shows user location, bearing ignored</li>
   * <li>{@link RenderMode#COMPASS}: Shows user location with bearing considered from compass</li>
   * <li>{@link RenderMode#GPS}: Shows user location with bearing considered from location</li>
   * </ul>
   *
   * @param renderMode GPS, NORMAL, or COMPASS
   */
  public void updateLocationLayerRenderMode(@RenderMode.Mode int renderMode) {
    locationLayer.setRenderMode(renderMode);
  }

  /**
   * Can be used to automatically drive the map camera / route updates and arrow
   * once navigation has started.
   * <p>
   * These will automatically be removed in {@link MapboxNavigation#onDestroy()}.
   *
   * @param navigation to add the progress listeners
   */
  public void addProgressChangeListener(MapboxNavigation navigation) {
    mapRoute.addProgressChangeListener(navigation);
    mapCamera.addProgressChangeListener(navigation);
  }

  /**
   * Can be used to store the current state of the map in
   * {@link android.support.v4.app.FragmentActivity#onSaveInstanceState(Bundle, PersistableBundle)}.
   * <p>
   * This method uses {@link NavigationMapboxMapInstanceState}, stored with the provided key.  This key
   * can also later be used to extract the {@link NavigationMapboxMapInstanceState}.
   *
   * @param key      used to store the state
   * @param outState to store state variables
   */
  public void saveStateWith(String key, Bundle outState) {
    boolean isVisible = mapWayname.isVisible();
    String waynameText = mapWayname.retrieveWayname();
    boolean isCameraTracking = mapCamera.isTrackingEnabled();
    NavigationMapboxMapInstanceState instanceState = new NavigationMapboxMapInstanceState(
      isVisible, waynameText, isCameraTracking
    );
    outState.putParcelable(key, instanceState);
  }

  /**
   * Can be used to restore a {@link NavigationMapboxMap} after it has been initialized.
   * <p>
   * This cannot be called in {@link android.support.v4.app.FragmentActivity#onRestoreInstanceState(Bundle)}
   * because we cannot guarantee the map is re-initialized at that point.
   * <p>
   * You can extract the {@link NavigationMapboxMapInstanceState} in <tt>onRestoreInstanceState</tt> and then
   * restore the map once it's ready.
   *
   * @param instanceState to extract state variables
   */
  public void restoreFrom(NavigationMapboxMapInstanceState instanceState) {
    boolean isVisible = instanceState.isWaynameVisible();
    updateWaynameVisibility(isVisible);
    if (isVisible) {
      updateWaynameView(instanceState.retrieveWayname());
    }
    boolean cameraTracking = instanceState.isCameraTracking();
    updateCameraTrackingEnabled(cameraTracking);
  }

  /**
   * Will draw the given {@link DirectionsRoute} on the map using the colors defined
   * in your given style.
   *
   * @param route to be drawn
   */
  public void drawRoute(@NonNull DirectionsRoute route) {
    mapRoute.addRoute(route);
  }

  /**
   * Will draw the given list of {@link DirectionsRoute} on the map using the colors defined
   * in your given style.
   * <p>
   * The primary route will default to the first route in the directions route list.
   * All other routes in the list will be drawn on the map using the alternative route style.
   *
   * @param routes to be drawn
   */
  public void drawRoutes(@NonNull List<DirectionsRoute> routes) {
    mapRoute.addRoutes(routes);
  }

  /**
   * Set a {@link OnRouteSelectionChangeListener} to know which route the user has currently
   * selected as their primary route.
   *
   * @param listener a listener which lets you know when the user has changed
   *                 the primary route and provides the current direction
   *                 route which the user has selected
   */
  public void setOnRouteSelectionChangeListener(@NonNull OnRouteSelectionChangeListener listener) {
    mapRoute.setOnRouteSelectionChangeListener(listener);
  }

  /**
   * Toggle whether or not you'd like the map to display the alternative routes. This option can be used
   * for when the user actually begins the navigation session and alternative routes aren't needed
   * anymore.
   *
   * @param alternativesVisible true if you'd like alternative routes to be displayed on the map,
   *                            else false
   */
  public void showAlternativeRoutes(boolean alternativesVisible) {
    mapRoute.showAlternativeRoutes(alternativesVisible);
  }

  /**
   * Will remove the drawn route displayed on the map.  Does nothing
   * if no route is drawn.
   */
  public void removeRoute() {
    mapRoute.removeRoute();
  }

  /**
   * Provides the camera being used to animate the map camera positions
   * along the route, driven by the progress change listener.
   *
   * @return camera used to animate map
   */
  public NavigationCamera retrieveCamera() {
    return mapCamera;
  }

  /**
   * Will enable or disable the camera tracking the location updates provided
   * by {@link MapboxNavigation}.  The camera will only be
   * tracking if {@link NavigationMapboxMap#addProgressChangeListener(MapboxNavigation)}
   * has been called.
   *
   * @param isEnabled true to track, false to not track
   */
  public void updateCameraTrackingEnabled(boolean isEnabled) {
    mapCamera.updateCameraTrackingLocation(isEnabled);
  }

  /**
   * Centers the map camera to the beginning of the provided {@link DirectionsRoute}.
   *
   * @param directionsRoute to update the camera position
   */
  public void startCamera(@NonNull DirectionsRoute directionsRoute) {
    mapCamera.start(directionsRoute);
  }

  /**
   * Centers the map camera around the provided {@link Location}.
   *
   * @param location to update the camera position
   */
  public void resumeCamera(@NonNull Location location) {
    mapCamera.resume(location);
  }

  /**
   * Resets the map camera / padding to the last known camera position.
   * <p>
   * Tracking is also re-enabled.
   */
  public void resetCameraPosition() {
    mapCamera.resetCameraPosition();
    resetMapPadding();
  }

  /**
   * Adjusts the map camera to {@link DirectionsRoute} being traveled along.
   * <p>
   * Also includes the given padding.
   *
   * @param padding for creating the overview camera position
   */
  public void showRouteOverview(int[] padding) {
    mapPaddingAdjustor.removeAllPadding();
    mapCamera.showRouteOverview(padding);
  }

  /**
   * Set the text of the way name chip underneath the location icon.
   * <p>
   * The text will only be set if the way name is visible / enabled.
   *
   * @param wayname text to be set
   */
  public void updateWaynameView(String wayname) {
    mapWayname.updateWaynameLayer(wayname, waynameLayer);
  }

  /**
   * Hide or show the way name chip underneath the location icon.
   *
   * @param isVisible true to show, false to hide
   */
  public void updateWaynameVisibility(boolean isVisible) {
    mapWayname.updateWaynameVisibility(isVisible, waynameLayer);
  }

  /**
   * Provides current visibility of the map way name.
   *
   * @return true if visible, false if not
   */
  public boolean isWaynameVisible() {
    return mapWayname.isVisible();
  }

  /**
   * Enables or disables the way name chip underneath the location icon.
   *
   * @param isEnabled true to enable, false to disable
   */
  public void updateWaynameQueryMap(boolean isEnabled) {
    mapWayname.updateWaynameQueryMap(isEnabled);
  }

  /**
   * Should be used in {@link FragmentActivity#onStart()} to ensure proper
   * accounting for the lifecycle.
   */
  public void onStart() {
    locationLayer.onStart();
    mapCamera.onStart();
    mapRoute.onStart();
  }

  /**
   * Should be used in {@link FragmentActivity#onStop()} to ensure proper
   * accounting for the lifecycle.
   */
  public void onStop() {
    locationLayer.onStop();
    mapCamera.onStop();
    mapRoute.onStop();
  }

  /**
   * Hide or show the location icon on the map.
   *
   * @param isVisible true to show, false to hide
   */
  public void updateLocationLayerVisibilityTo(boolean isVisible) {
    locationLayer.setLocationLayerEnabled(isVisible);
  }

  /**
   * Provides the {@link MapboxMap} originally given in the constructor.
   * <p>
   * This method gives access to all map-related APIs.
   *
   * @return map provided in the constructor
   */
  public MapboxMap retrieveMap() {
    return mapboxMap;
  }

  /**
   * Updates the visibility of incidents layers on the map (if any exist).
   *
   * @param isVisible true if incidents should be visible, false otherwise
   */
  public void updateIncidentsVisibility(boolean isVisible) {
    layerInteractor.updateLayerVisibility(isVisible, INCIDENTS_LAYER_ID);
  }

  /**
   * Returns true if the map has incidents layers and they are visible and
   * will return false otherwise.
   *
   * @return true if the map has incidents layers and they are visible, false otherwise
   */
  public boolean isIncidentsVisible() {
    return layerInteractor.isLayerVisible(INCIDENTS_LAYER_ID);
  }

  /**
   * Updates the visibility of traffic layers on the map (if any exist).
   *
   * @param isVisible true if traffic should be visible, false otherwise
   */
  public void updateTrafficVisibility(boolean isVisible) {
    layerInteractor.updateLayerVisibility(isVisible, TRAFFIC_LAYER_ID);
  }

  /**
   * Returns true if the map has traffic layers and they are visible and
   * will return false otherwise.
   *
   * @return true if the map has traffic layers and they are visible, false otherwise
   */
  public boolean isTrafficVisible() {
    return layerInteractor.isLayerVisible(TRAFFIC_LAYER_ID);
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

    LocationLayerOptions locationLayerOptions =
      LocationLayerOptions.createFromAttributes(context, locationLayerStyleRes);
    locationLayerOptions = locationLayerOptions.toBuilder().minZoom(NAVIGATION_MINIMUM_MAP_ZOOM).build();

    locationLayer = new LocationLayerPlugin(mapView, map, null, locationLayerOptions);
    locationLayer.setRenderMode(RenderMode.GPS);
  }

  private void initializeMapPaddingAdjustor(MapView mapView, MapboxMap mapboxMap) {
    mapPaddingAdjustor = new MapPaddingAdjustor(mapView, mapboxMap);
  }

  private void initializeCamera(MapboxMap map) {
    mapCamera = new NavigationCamera(map, locationLayer);
  }

  private void initializeWayname(MapView mapView, MapboxMap mapboxMap,
                                 MapLayerInteractor layerInteractor, MapPaddingAdjustor paddingAdjustor) {
    initializeStreetsSource(mapboxMap);
    WaynameLayoutProvider layoutProvider = new WaynameLayoutProvider(mapView.getContext());
    WaynameFeatureFinder featureInteractor = new WaynameFeatureFinder(mapboxMap);
    initializeWaynameLayer(layerInteractor);
    mapWayname = new MapWayname(layoutProvider, layerInteractor, featureInteractor, paddingAdjustor);
  }

  private void initializeWaynameLayer(MapLayerInteractor layerInteractor) {
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

  private void initializeMapLayerInteractor(MapboxMap mapboxMap) {
    layerInteractor = new MapLayerInteractor(mapboxMap);
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
