package com.mapbox.services.android.navigation.ui.v5.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.mapboxsdk.style.sources.VectorSource;
import com.mapbox.services.android.navigation.ui.v5.NavigationSnapshotReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.ThemeSwitcher;
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.ui.v5.route.OnRouteSelectionChangeListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.mapbox.services.android.navigation.ui.v5.map.NavigationSymbolManager.MAPBOX_NAVIGATION_MARKER_NAME;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_MINIMUM_MAP_ZOOM;

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
  private static final String MAPBOX_STREETS_V7_URL = "mapbox.mapbox-streets-v7";
  private static final String MAPBOX_STREETS_V8_URL = "mapbox.mapbox-streets-v8";
  private static final String STREETS_SOURCE_ID = "com.mapbox.services.android.navigation.streets";
  private static final String STREETS_V7_ROAD_LABEL = "road_label";
  private static final String STREETS_V8_ROAD_LABEL = "road";
  private static final String INCIDENTS_LAYER_ID = "closures";
  private static final String TRAFFIC_LAYER_ID = "traffic";
  private static final int[] ZERO_MAP_PADDING = {0, 0, 0, 0};
  private static final double NAVIGATION_MAXIMUM_MAP_ZOOM = 18d;
  private final CopyOnWriteArrayList<OnWayNameChangedListener> onWayNameChangedListeners
    = new CopyOnWriteArrayList<>();
  private final MapWayNameChangedListener internalWayNameChangedListener
    = new MapWayNameChangedListener(onWayNameChangedListeners);
  private NavigationMapSettings settings = new NavigationMapSettings();
  private MapView mapView;
  private MapboxMap mapboxMap;
  private LocationComponent locationComponent;
  private MapPaddingAdjustor mapPaddingAdjustor;
  private NavigationSymbolManager navigationSymbolManager;
  private MapLayerInteractor layerInteractor;
  private NavigationMapRoute mapRoute;
  private NavigationCamera mapCamera;
  @Nullable
  private MapWayName mapWayName;
  @Nullable
  private MapFpsDelegate mapFpsDelegate;
  private LocationFpsDelegate locationFpsDelegate;

  /**
   * Constructor that can be used once {@link com.mapbox.mapboxsdk.maps.OnMapReadyCallback}
   * has been called via {@link MapView#getMapAsync(OnMapReadyCallback)}.
   *
   * @param mapView   for map size and Context
   * @param mapboxMap for APIs to interact with the map
   */
  public NavigationMapboxMap(@NonNull MapView mapView, @NonNull MapboxMap mapboxMap) {
    this.mapView = mapView;
    this.mapboxMap = mapboxMap;
    initializeLocationComponent(mapView, mapboxMap);
    initializeMapPaddingAdjustor(mapView, mapboxMap);
    initializeNavigationSymbolManager(mapView, mapboxMap);
    initializeMapLayerInteractor(mapboxMap);
    initializeRoute(mapView, mapboxMap);
    initializeCamera(mapboxMap, locationComponent);
    initializeLocationFpsDelegate(mapboxMap, locationComponent);
  }

  // Package private (no modifier) for testing purposes
  NavigationMapboxMap(MapLayerInteractor layerInteractor) {
    this.layerInteractor = layerInteractor;
  }

  // Package private (no modifier) for testing purposes
  NavigationMapboxMap(LocationComponent locationComponent) {
    this.locationComponent = locationComponent;
  }

  // Package private (no modifier) for testing purposes
  NavigationMapboxMap(NavigationMapRoute mapRoute) {
    this.mapRoute = mapRoute;
  }

  // Package private (no modifier) for testing purposes
  NavigationMapboxMap(NavigationSymbolManager navigationSymbolManager) {
    this.navigationSymbolManager = navigationSymbolManager;
  }

  // Package private (no modifier) for testing purposes
  NavigationMapboxMap(@NonNull MapWayName mapWayName, @NonNull MapFpsDelegate mapFpsDelegate) {
    this.mapWayName = mapWayName;
    this.mapFpsDelegate = mapFpsDelegate;
  }

  // Package private (no modifier) for testing purposes
  NavigationMapboxMap(@NonNull MapWayName mapWayName, @NonNull MapFpsDelegate mapFpsDelegate,
                      NavigationMapRoute mapRoute, NavigationCamera mapCamera,
                      LocationFpsDelegate locationFpsDelegate) {
    this.mapWayName = mapWayName;
    this.mapFpsDelegate = mapFpsDelegate;
    this.mapRoute = mapRoute;
    this.mapCamera = mapCamera;
    this.locationFpsDelegate = locationFpsDelegate;
  }

  // Package private (no modifier) for testing purposes
  NavigationMapboxMap(MapboxMap mapboxMap, MapLayerInteractor layerInteractor, MapPaddingAdjustor adjustor) {
    this.layerInteractor = layerInteractor;
    initializeWayName(mapboxMap, adjustor);
  }

  /**
   * Adds a marker icon on the map at the given position.
   * <p>
   * The icon used for this method can be defined in your theme with
   * the attribute <tt>navigationViewDestinationMarker</tt>.
   *
   * @param context  to retrieve the icon drawable from the theme
   * @param position the point at which the marker will be placed
   * @deprecated Use {@link NavigationMapboxMap#addDestinationMarker(Point)} instead.
   * A {@link Context} is no longer needed.
   */
  @Deprecated
  public void addMarker(Context context, Point position) {
    navigationSymbolManager.addDestinationMarkerFor(position);
  }

  /**
   * Adds a marker icon on the map at the given position.
   * <p>
   * The icon used for this method can be defined in your theme with
   * the attribute <tt>navigationViewDestinationMarker</tt>.
   *
   * @param position the point at which the marker will be placed
   */
  public void addDestinationMarker(Point position) {
    navigationSymbolManager.addDestinationMarkerFor(position);
  }

  /**
   * Adds a custom marker to the map based on the options provided.
   * <p>
   * Please note, the map will manage all markers added.  Calling {@link NavigationMapboxMap#clearMarkers()}
   * will clear all destination / custom markers that have been added to the map.
   *
   * @param options for the custom {@link com.mapbox.mapboxsdk.plugins.annotation.Symbol}
   */
  public void addCustomMarker(SymbolOptions options) {
    navigationSymbolManager.addCustomSymbolFor(options);
  }

  /**
   * Clears all markers on the map that have been added by this class.
   * <p>
   * This will not clear all markers from the map entirely.  Does nothing
   * if no markers have been added.
   */
  public void clearMarkers() {
    navigationSymbolManager.removeAllMarkerSymbols();
  }

  /**
   * Updates the location icon on the map and way name data (if found)
   * for the given {@link Location}.
   *
   * @param location to update the icon and query the map
   */
  public void updateLocation(Location location) {
    locationComponent.forceLocationUpdate(location);
    updateMapWayNameWithLocation(location);
  }

  /**
   * The maximum preferred frames per second at which to render the map.
   * <p>
   * This property only takes effect when the application has limited resources, such as when
   * the device is running on battery power. By default, this is set to 20fps.
   * <p>
   * Throttling will also only take effect when the camera is currently tracking
   * the user location.
   *
   * @param maxFpsThreshold to be used to limit map frames per second
   */
  public void updateMapFpsThrottle(int maxFpsThreshold) {
    if (mapFpsDelegate != null) {
      mapFpsDelegate.updateMaxFpsThreshold(maxFpsThreshold);
    } else {
      settings.updateMaxFps(maxFpsThreshold);
    }
  }

  /**
   * Enabled by default, the navigation map will throttle frames per second when the application has
   * limited resources, such as when the device is running on battery power.
   * <p>
   * Throttling will also only take effect when the camera is currently tracking
   * the user location.
   *
   * @param isEnabled true to enable (default), false to render at device ability
   */
  public void updateMapFpsThrottleEnabled(boolean isEnabled) {
    if (mapFpsDelegate != null) {
      mapFpsDelegate.updateEnabled(isEnabled);
    } else {
      settings.updateMaxFpsEnabled(isEnabled);
    }
  }

  /**
   * Enabled by default, the navigation map will throttle frames per second of the location icon
   * based on the map zoom level.
   *
   * @param isEnabled true to enable (default), false to render at device ability
   */
  public void updateLocationFpsThrottleEnabled(boolean isEnabled) {
    locationFpsDelegate.updateEnabled(isEnabled);
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
    locationComponent.setRenderMode(renderMode);
  }

  /**
   * Can be used to automatically drive the map camera / route updates and arrow
   * once navigation has started.
   * <p>
   * These will automatically be removed in {@link MapboxNavigation#onDestroy()}.
   *
   * @param navigation to add the progress listeners
   */
  public void addProgressChangeListener(@NonNull MapboxNavigation navigation) {
    initializeWayName(mapboxMap, mapPaddingAdjustor);
    initializeFpsDelegate(mapView);
    mapRoute.addProgressChangeListener(navigation);
    mapCamera.addProgressChangeListener(navigation);
    mapWayName.addProgressChangeListener(navigation);
    mapFpsDelegate.addProgressChangeListener(navigation);
  }

  /**
   * Can be used to store the current state of the map in
   * {@link androidx.fragment.app.Fragment#onSaveInstanceState(Bundle)}.
   * <p>
   * This method uses {@link NavigationMapboxMapInstanceState}, stored with the provided key.  This key
   * can also later be used to extract the {@link NavigationMapboxMapInstanceState}.
   *
   * @param key      used to store the state
   * @param outState to store state variables
   */
  public void saveStateWith(String key, Bundle outState) {
    settings.updateCurrentPadding(mapPaddingAdjustor.retrieveCurrentPadding());
    settings.updateShouldUseDefaultPadding(mapPaddingAdjustor.isUsingDefault());
    settings.updateCameraTrackingMode(mapCamera.getCameraTrackingMode());
    settings.updateLocationFpsEnabled(locationFpsDelegate.isEnabled());
    NavigationMapboxMapInstanceState instanceState = new NavigationMapboxMapInstanceState(settings);
    outState.putParcelable(key, instanceState);
  }

  /**
   * Can be used to restore a {@link NavigationMapboxMap} after it has been initialized.
   * <p>
   * This cannot be called in {@link androidx.fragment.app.Fragment#onViewStateRestored(Bundle)}
   * because we cannot guarantee the map is re-initialized at that point.
   * <p>
   * You can extract the {@link NavigationMapboxMapInstanceState} in <tt>onRestoreInstanceState</tt> and then
   * restore the map once it's ready.
   *
   * @param instanceState to extract state variables
   */
  public void restoreFrom(NavigationMapboxMapInstanceState instanceState) {
    settings = instanceState.retrieveSettings();
    restoreMapWith(settings);
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
   * Updates the {@link NavigationCamera.TrackingMode} that will be used when camera tracking is enabled.
   *
   * @param trackingMode the tracking mode
   * @since 0.21.0
   */
  public void updateCameraTrackingMode(@NavigationCamera.TrackingMode int trackingMode) {
    mapCamera.updateCameraTrackingMode(trackingMode);
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
   * You can also specify a tracking mode to reset with.  For example if you would like
   * to reset the camera and continue tracking, you would use {@link NavigationCamera#NAVIGATION_TRACKING_MODE_GPS}.
   *
   * @param trackingCameraMode the tracking mode
   */
  public void resetCameraPositionWith(@NavigationCamera.TrackingMode int trackingCameraMode) {
    mapCamera.resetCameraPositionWith(trackingCameraMode);
  }

  /**
   * This method resets the map padding to the default padding that is
   * generated when navigation begins (location icon moved to lower half of the screen) or
   * the custom padding that was last passed via {@link MapPaddingAdjustor#adjustLocationIconWith(int[])}.
   * <p>
   * The custom padding will be used if it exists, otherwise the default will be used.
   */
  public void resetPadding() {
    mapPaddingAdjustor.resetPadding();
  }

  /**
   * Adjusts the map camera to {@link DirectionsRoute} being traveled along.
   * <p>
   * Also includes the given padding.
   *
   * @param padding for creating the overview camera position
   */
  public void showRouteOverview(int[] padding) {
    mapPaddingAdjustor.updatePaddingWith(ZERO_MAP_PADDING);
    mapCamera.showRouteOverview(padding);
  }

  /**
   * Enables or disables the way name chip underneath the location icon.
   *
   * @param isEnabled true to enable, false to disable
   */
  public void updateWaynameQueryMap(boolean isEnabled) {
    if (mapWayName != null) {
      mapWayName.updateWayNameQueryMap(isEnabled);
    } else {
      settings.updateWayNameEnabled(isEnabled);
    }
  }

  /**
   * Should be used in {@link FragmentActivity#onStart()} to ensure proper
   * accounting for the lifecycle.
   */
  public void onStart() {
    mapCamera.onStart();
    mapRoute.onStart();
    handleWayNameOnStart();
    handleFpsOnStart();
    locationFpsDelegate.onStart();
  }

  /**
   * Should be used in {@link FragmentActivity#onStop()} to ensure proper
   * accounting for the lifecycle.
   */
  public void onStop() {
    mapCamera.onStop();
    mapRoute.onStop();
    handleWayNameOnStop();
    handleFpsOnStop();
    locationFpsDelegate.onStop();
  }

  /**
   * Hide or show the location icon on the map.
   *
   * @param isVisible true to show, false to hide
   */
  public void updateLocationVisibilityTo(boolean isVisible) {
    locationComponent.setLocationComponentEnabled(isVisible);
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

  /**
   * Add a {@link OnCameraTrackingChangedListener} to the {@link LocationComponent} that is
   * wrapped within this class.
   * <p>
   * This listener will fire any time camera tracking is dismissed or the camera mode is updated.
   *
   * @param listener to be added
   */
  public void addOnCameraTrackingChangedListener(OnCameraTrackingChangedListener listener) {
    locationComponent.addOnCameraTrackingChangedListener(listener);
  }

  /**
   * Remove a {@link OnCameraTrackingChangedListener} from the {@link LocationComponent} that is
   * wrapped within this class.
   *
   * @param listener to be removed
   */
  public void removeOnCameraTrackingChangedListener(OnCameraTrackingChangedListener listener) {
    locationComponent.removeOnCameraTrackingChangedListener(listener);
  }

  /**
   * Add a {@link OnWayNameChangedListener} for listening to updates
   * to the way name shown on the map below the location icon.
   *
   * @param listener to be added
   * @return true if added, false if listener was not found
   */
  public boolean addOnWayNameChangedListener(OnWayNameChangedListener listener) {
    return onWayNameChangedListeners.add(listener);
  }

  /**
   * Remove a {@link OnWayNameChangedListener} for listening to updates
   * to the way name shown on the map below the location icon.
   *
   * @param listener to be removed
   * @return true if removed, false if listener was not found
   */
  public boolean removeOnWayNameChangedListener(OnWayNameChangedListener listener) {
    return onWayNameChangedListeners.remove(listener);
  }

  /**
   * Use this method to position the location icon on the map.
   * <p>
   * For example, to position the icon in the center of the map, you can pass {0, 0, 0, 0} which
   * eliminates the default padding we provide when navigation begins.
   *
   * @param customPadding true if should be centered on the map, false to position above the bottom view
   */
  public void adjustLocationIconWith(int[] customPadding) {
    mapPaddingAdjustor.adjustLocationIconWith(customPadding);
  }

  public void takeScreenshot(NavigationSnapshotReadyCallback navigationSnapshotReadyCallback) {
    mapboxMap.snapshot(navigationSnapshotReadyCallback);
  }

  @SuppressLint("MissingPermission")
  private void initializeLocationComponent(MapView mapView, MapboxMap map) {
    locationComponent = map.getLocationComponent();
    map.setMinZoomPreference(NAVIGATION_MINIMUM_MAP_ZOOM);
    map.setMaxZoomPreference(NAVIGATION_MAXIMUM_MAP_ZOOM);
    Context context = mapView.getContext();
    Style style = map.getStyle();
    int locationLayerStyleRes = findLayerStyleRes(context);
    LocationComponentOptions options = LocationComponentOptions.createFromAttributes(context, locationLayerStyleRes);
    LocationComponentActivationOptions activationOptions = LocationComponentActivationOptions.builder(context, style)
      .locationComponentOptions(options)
      .useDefaultLocationEngine(false)
      .build();
    locationComponent.activateLocationComponent(activationOptions);
    locationComponent.setLocationComponentEnabled(true);
  }

  private int findLayerStyleRes(Context context) {
    int locationLayerStyleRes = ThemeSwitcher.retrieveNavigationViewStyle(context,
      R.attr.navigationViewLocationLayerStyle);
    if (!isValid(locationLayerStyleRes)) {
      locationLayerStyleRes = R.style.NavigationLocationLayerStyle;
    }
    return locationLayerStyleRes;
  }

  private boolean isValid(@AnyRes int resId) {
    return resId != -1 && (resId & 0xff000000) != 0 && (resId & 0x00ff0000) != 0;
  }

  private void initializeMapPaddingAdjustor(MapView mapView, MapboxMap mapboxMap) {
    mapPaddingAdjustor = new MapPaddingAdjustor(mapView, mapboxMap);
  }

  private void initializeNavigationSymbolManager(MapView mapView, MapboxMap mapboxMap) {
    Bitmap markerBitmap = ThemeSwitcher.retrieveThemeMapMarker(mapView.getContext());
    mapboxMap.getStyle().addImage(MAPBOX_NAVIGATION_MARKER_NAME, markerBitmap);
    SymbolManager symbolManager = new SymbolManager(mapView, mapboxMap, mapboxMap.getStyle());
    navigationSymbolManager = new NavigationSymbolManager(symbolManager);
    SymbolOnStyleLoadedListener onStyleLoadedListener = new SymbolOnStyleLoadedListener(mapboxMap, markerBitmap);
    mapView.addOnDidFinishLoadingStyleListener(onStyleLoadedListener);
  }

  private void initializeMapLayerInteractor(MapboxMap mapboxMap) {
    layerInteractor = new MapLayerInteractor(mapboxMap);
  }

  private void initializeRoute(MapView mapView, MapboxMap map) {
    Context context = mapView.getContext();
    int routeStyleRes = ThemeSwitcher.retrieveNavigationViewStyle(context, R.attr.navigationViewRouteStyle);
    mapRoute = new NavigationMapRoute(null, mapView, map, routeStyleRes);
  }

  private void initializeCamera(MapboxMap map, LocationComponent locationComponent) {
    mapCamera = new NavigationCamera(map, locationComponent);
  }

  private void initializeLocationFpsDelegate(MapboxMap map, LocationComponent locationComponent) {
    locationFpsDelegate = new LocationFpsDelegate(map, locationComponent);
  }

  private void initializeWayName(MapboxMap mapboxMap, MapPaddingAdjustor paddingAdjustor) {
    if (mapWayName != null) {
      return;
    }
    initializeStreetsSource(mapboxMap);
    WaynameFeatureFinder featureFinder = new WaynameFeatureFinder(mapboxMap);
    mapWayName = new MapWayName(featureFinder, paddingAdjustor);
    mapWayName.updateWayNameQueryMap(settings.isMapWayNameEnabled());
    mapWayName.addOnWayNameChangedListener(internalWayNameChangedListener);
  }

  private void initializeStreetsSource(MapboxMap mapboxMap) {
    List<Source> sources = mapboxMap.getStyle().getSources();
    Source sourceV7 = findSourceByUrl(sources, MAPBOX_STREETS_V7_URL);
    Source sourceV8 = findSourceByUrl(sources, MAPBOX_STREETS_V8_URL);

    if (sourceV7 != null) {
      layerInteractor.addStreetsLayer(sourceV7.getId(), STREETS_V7_ROAD_LABEL);
    } else if (sourceV8 != null) {
      layerInteractor.addStreetsLayer(sourceV8.getId(), STREETS_V8_ROAD_LABEL);
    } else {
      VectorSource streetSource = new VectorSource(STREETS_SOURCE_ID, MAPBOX_STREETS_V8_URL);
      mapboxMap.getStyle().addSource(streetSource);
      layerInteractor.addStreetsLayer(STREETS_SOURCE_ID, STREETS_V8_ROAD_LABEL);
    }
  }

  @Nullable
  private Source findSourceByUrl(List<Source> sources, String streetsUrl) {
    for (Source source : sources) {
      if (source instanceof VectorSource) {
        VectorSource vectorSource = (VectorSource) source;
        String url = vectorSource.getUrl();
        if (url != null && url.contains(streetsUrl)) {
          return vectorSource;
        }
      }
    }
    return null;
  }

  private void initializeFpsDelegate(MapView mapView) {
    if (mapFpsDelegate != null) {
      return;
    }
    MapBatteryMonitor batteryMonitor = new MapBatteryMonitor();
    mapFpsDelegate = new MapFpsDelegate(mapView, batteryMonitor);
    mapFpsDelegate.updateEnabled(settings.isMaxFpsEnabled());
    mapFpsDelegate.updateMaxFpsThreshold(settings.retrieveMaxFps());
    addFpsListenersToCamera();
  }

  private void addFpsListenersToCamera() {
    mapCamera.addOnTrackingModeTransitionListener(mapFpsDelegate);
    mapCamera.addOnTrackingModeChangedListener(mapFpsDelegate);
  }

  private void removeFpsListenersFromCamera() {
    mapCamera.removeOnTrackingModeTransitionListener(mapFpsDelegate);
    mapCamera.removeOnTrackingModeChangedListener(mapFpsDelegate);
  }

  private void updateMapWayNameWithLocation(Location location) {
    if (mapWayName == null) {
      return;
    }
    LatLng latLng = new LatLng(location);
    PointF mapPoint = mapboxMap.getProjection().toScreenLocation(latLng);
    mapWayName.updateWayNameWithPoint(mapPoint);
  }

  private void restoreMapWith(NavigationMapSettings settings) {
    updateCameraTrackingMode(settings.retrieveCameraTrackingMode());
    updateLocationFpsThrottleEnabled(settings.isLocationFpsEnabled());
    if (settings.shouldUseDefaultPadding()) {
      mapPaddingAdjustor.updatePaddingWithDefault();
    } else {
      adjustLocationIconWith(settings.retrieveCurrentPadding());
    }
    if (mapWayName != null) {
      mapWayName.updateWayNameQueryMap(settings.isMapWayNameEnabled());
    }
    if (mapFpsDelegate != null) {
      mapFpsDelegate.updateMaxFpsThreshold(settings.retrieveMaxFps());
      mapFpsDelegate.updateEnabled(settings.isMaxFpsEnabled());
    }
  }

  private void handleWayNameOnStart() {
    if (mapWayName != null) {
      mapWayName.onStart();
      mapWayName.addOnWayNameChangedListener(internalWayNameChangedListener);
    }
  }

  private void handleFpsOnStart() {
    if (mapFpsDelegate != null) {
      mapFpsDelegate.onStart();
      addFpsListenersToCamera();
    }
  }

  private void handleWayNameOnStop() {
    if (mapWayName != null) {
      mapWayName.onStop();
      mapWayName.removeOnWayNameChangedListener(internalWayNameChangedListener);
    }
  }

  private void handleFpsOnStop() {
    if (mapFpsDelegate != null) {
      mapFpsDelegate.onStop();
      removeFpsListenersFromCamera();
    }
  }
}
