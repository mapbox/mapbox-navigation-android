package com.mapbox.navigation.ui.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.navigation.core.directions.session.RoutesObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.core.trip.session.TripSessionState;
import com.mapbox.navigation.ui.R;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.mapboxsdk.style.sources.VectorSource;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.ui.internal.ThemeSwitcher;
import com.mapbox.navigation.ui.camera.Camera;
import com.mapbox.navigation.ui.camera.NavigationCamera;
import com.mapbox.navigation.ui.route.IdentifiableRoute;
import com.mapbox.navigation.ui.puck.NavigationPuckPresenter;
import com.mapbox.navigation.ui.puck.PuckDrawableSupplier;
import com.mapbox.navigation.ui.route.NavigationMapRoute;
import com.mapbox.navigation.ui.route.OnRouteSelectionChangeListener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import timber.log.Timber;

import static com.mapbox.navigation.ui.map.NavigationSymbolManager.MAPBOX_NAVIGATION_MARKER_NAME;

/**
 * Wrapper class for {@link MapboxMap}.
 * <p>
 * This class initializes various map-related components and plugins that are
 * useful for providing a navigation-driven map experience.
 * <p>
 * These APIs include drawing a route line, camera animations, and more.
 */
@UiThread
public class NavigationMapboxMap implements LifecycleObserver {

  private static final String STATE_BUNDLE_KEY = "mapbox_navigation_sdk_state_bundle";
  static final String STREETS_LAYER_ID = "streetsLayer";
  private static final String MAPBOX_STREETS_V7_URL = "mapbox.mapbox-streets-v7";
  private static final String MAPBOX_STREETS_V8_URL = "mapbox.mapbox-streets-v8";
  private static final String STREETS_SOURCE_ID = "com.mapbox.services.android.navigation.streets";
  private static final String STREETS_V7_ROAD_LABEL = "road_label";
  private static final String STREETS_V8_ROAD_LABEL = "road";
  private static final String INCIDENTS_LAYER_ID = "closures";
  private static final String TRAFFIC_LAYER_ID = "traffic";
  private static final int[] ZERO_MAP_PADDING = { 0, 0, 0, 0 };
  private static final double NAVIGATION_MAXIMUM_MAP_ZOOM = 18d;
  private final CopyOnWriteArrayList<OnWayNameChangedListener> onWayNameChangedListeners
      = new CopyOnWriteArrayList<>();
  private final MapWayNameChangedListener internalWayNameChangedListener
      = new MapWayNameChangedListener(onWayNameChangedListeners);
  private NavigationMapSettings settings = new NavigationMapSettings();
  @NonNull
  private MapView mapView;
  @NonNull
  private MapboxMap mapboxMap;
  @NonNull
  private LifecycleOwner lifecycleOwner;
  private LocationComponent locationComponent;
  private MapPaddingAdjustor mapPaddingAdjustor;
  @Nullable
  private NavigationSymbolManager navigationSymbolManager;
  private MapLayerInteractor layerInteractor;
  private NavigationMapRoute mapRoute;
  private NavigationCamera mapCamera;
  private NavigationPuckPresenter navigationPuckPresenter;
  @Nullable
  private MapWayName mapWayName;
  @Nullable
  private MapFpsDelegate mapFpsDelegate;
  private LocationFpsDelegate locationFpsDelegate;
  @Nullable
  private MapboxNavigation navigation;
  private boolean vanishRouteLineEnabled;

  /**
   * Constructor that can be used once {@link OnMapReadyCallback}
   * has been called via {@link MapView#getMapAsync(OnMapReadyCallback)}.
   *
   * @param mapView for map size and Context
   * @param mapboxMap for APIs to interact with the map
   * @param lifecycleOwner provides lifecycle for component
   */
  public NavigationMapboxMap(@NonNull MapView mapView,
      @NonNull MapboxMap mapboxMap,
      @NonNull LifecycleOwner lifecycleOwner) {
    this(mapView, mapboxMap, lifecycleOwner, null);
  }

  /**
   * Constructor that can be used once {@link OnMapReadyCallback}
   * has been called via {@link MapView#getMapAsync(OnMapReadyCallback)}.
   *
   * @param mapView for map size and Context
   * @param mapboxMap for APIs to interact with the map
   * @param lifecycleOwner provides lifecycle for component
   * @param vanishRouteLineEnabled determines if the route line should vanish behind the puck during navigation.
   */
  public NavigationMapboxMap(@NonNull MapView mapView,
      @NonNull MapboxMap mapboxMap,
      @NonNull LifecycleOwner lifecycleOwner,
      boolean vanishRouteLineEnabled) {
    this(mapView, mapboxMap, lifecycleOwner, null, vanishRouteLineEnabled, false);
  }

  /**
   * Constructor that can be used once {@link OnMapReadyCallback}
   * has been called via {@link MapView#getMapAsync(OnMapReadyCallback)}.
   *
   * @param mapView for map size and Context
   * @param mapboxMap for APIs to interact with the map
   * @param lifecycleOwner provides lifecycle for component
   * @param routeBelowLayerId optionally pass in a layer id to place the route line below
   */
  public NavigationMapboxMap(@NonNull MapView mapView,
      @NonNull MapboxMap mapboxMap,
      @NonNull LifecycleOwner lifecycleOwner,
      @Nullable String routeBelowLayerId) {
    this(mapView, mapboxMap, lifecycleOwner, routeBelowLayerId, false, false);
  }

  /**
   * Constructor that can be used once {@link OnMapReadyCallback}
   * has been called via {@link MapView#getMapAsync(OnMapReadyCallback)}.
   *
   * @param mapView for map size and Context
   * @param mapboxMap for APIs to interact with the map
   * @param lifecycleOwner provides lifecycle for component
   * @param routeBelowLayerId optionally pass in a layer id to place the route line below
   * @param vanishRouteLineEnabled determines if the route line should vanish behind the puck during navigation.
   * @param useSpecializedLocationLayer determines if the location puck should use a specialized render layer.
   */
  public NavigationMapboxMap(@NonNull MapView mapView,
      @NonNull MapboxMap mapboxMap,
      @NonNull LifecycleOwner lifecycleOwner,
      @Nullable String routeBelowLayerId,
      boolean vanishRouteLineEnabled,
      boolean useSpecializedLocationLayer) {
    this.mapView = mapView;
    this.mapboxMap = mapboxMap;
    this.vanishRouteLineEnabled = vanishRouteLineEnabled;
    this.lifecycleOwner = lifecycleOwner;
    initializeMapPaddingAdjustor(mapView, mapboxMap);
    initializeNavigationSymbolManager(mapView, mapboxMap);
    initializeMapLayerInteractor(mapboxMap);
    initializeRoute(mapView, mapboxMap, routeBelowLayerId);
    initializeCamera(mapboxMap);
    initializeLocationComponent(useSpecializedLocationLayer);
    registerLifecycleOwnerObserver();
  }

  private void initializeLocationComponent(boolean useSpecializedLocationLayer) {
    setupLocationComponent(mapView, mapboxMap, useSpecializedLocationLayer);
    initializeLocationFpsDelegate(mapboxMap, locationComponent);
  }

  @TestOnly
  NavigationMapboxMap(MapLayerInteractor layerInteractor) {
    this.layerInteractor = layerInteractor;
  }

  @TestOnly
  NavigationMapboxMap(LocationComponent locationComponent) {
    this.locationComponent = locationComponent;
  }

  @TestOnly
  NavigationMapboxMap(NavigationMapRoute mapRoute) {
    this.mapRoute = mapRoute;
  }

  @TestOnly
  NavigationMapboxMap(NavigationSymbolManager navigationSymbolManager) {
    this.navigationSymbolManager = navigationSymbolManager;
  }

  @TestOnly
  NavigationMapboxMap(@NonNull MapWayName mapWayName, @NonNull MapFpsDelegate mapFpsDelegate) {
    this.mapWayName = mapWayName;
    this.mapFpsDelegate = mapFpsDelegate;
  }

  @TestOnly
  NavigationMapboxMap(@NonNull MapWayName mapWayName, @NonNull MapFpsDelegate mapFpsDelegate,
      NavigationMapRoute mapRoute, NavigationCamera mapCamera,
      LocationFpsDelegate locationFpsDelegate) {
    this.mapWayName = mapWayName;
    this.mapFpsDelegate = mapFpsDelegate;
    this.mapRoute = mapRoute;
    this.mapCamera = mapCamera;
    this.locationFpsDelegate = locationFpsDelegate;
  }

  @TestOnly
  NavigationMapboxMap(
          @NonNull MapboxMap mapboxMap,
          MapLayerInteractor layerInteractor,
          @NonNull MapPaddingAdjustor adjustor
  ) {
    this.layerInteractor = layerInteractor;
    this.mapboxMap = mapboxMap;
    initializeWayName(mapboxMap, adjustor);
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
    if (navigationSymbolManager != null) {
      navigationSymbolManager.addDestinationMarkerFor(position);
    }
  }

  /**
   * Adds a custom marker to the map based on the options provided.
   * <p>
   * Please note, the map will manage all markers added.  Calling {@link NavigationMapboxMap#clearMarkers()}
   * will clear all destination / custom markers that have been added to the map.
   *
   * @param options for the custom {@link com.mapbox.mapboxsdk.plugins.annotation.Symbol}
   *
   * @return the {@link com.mapbox.mapboxsdk.plugins.annotation.Symbol} that added to the map or null if adding fails
   */
  public Symbol addCustomMarker(SymbolOptions options) {
    if (navigationSymbolManager != null) {
      return navigationSymbolManager.addCustomSymbolFor(options);
    } else {
      return null;
    }
  }

  /**
   * Clears marker with the specified marker id.
   * <p>
   * Please note, this only clears the marker added by {@link NavigationMapboxMap#addCustomMarker(SymbolOptions)}
   *
   * @param markerId of the {@link com.mapbox.mapboxsdk.plugins.annotation.Symbol}
   */
  public void clearMarkerWithId(long markerId) {
    if (navigationSymbolManager != null) {
      navigationSymbolManager.clearSymbolWithId(markerId);
    }
  }

  /**
   * Clears all markers with the specified icon image.
   * <p>
   * Please note, this only clears the markers added by {@link NavigationMapboxMap#addCustomMarker(SymbolOptions)}
   *
   * @param markerIconImageProperty of the {@link com.mapbox.mapboxsdk.plugins.annotation.Symbol}
   */
  public void clearMarkersWithIconImageProperty(String markerIconImageProperty) {
    if (navigationSymbolManager != null) {
      navigationSymbolManager.clearSymbolsWithIconImageProperty(markerIconImageProperty);
    }
  }

  /**
   * Clears all markers on the map that have been added by this class.
   * <p>
   * This will not clear all markers from the map entirely.  Does nothing
   * if no markers have been added.
   */
  public void clearMarkers() {
    if (navigationSymbolManager != null) {
      navigationSymbolManager.clearAllMarkerSymbols();
    }
  }

  /**
   * Updates the location icon on the map and way name data (if found)
   * for the given {@link Location}.
   * <p>
   * {@link NavigationMapboxMap} automatically listens to
   * {@link LocationObserver#onEnhancedLocationChanged(Location, List)} when a progress observer
   * is subscribed with {@link #addProgressChangeListener(MapboxNavigation)}
   * and invoking this method in that scenario will lead to competing updates.
   *
   * @param location to update the icon and query the map
   */
  public void updateLocation(@Nullable Location location) {
    if (location != null) {
      List<Location> locations = new ArrayList<>(1);
      locations.add(location);
      updateLocation(locations);
    }
  }

  /**
   * This method can be used to provide the list of locations where the last one is the target
   * and the rest are intermediate points used as the animation path.
   * The puck and the camera will be animated between each of the points linearly until reaching the target.
   * <p>
   * {@link NavigationMapboxMap} automatically listens to
   * {@link LocationObserver#onEnhancedLocationChanged(Location, List)} when a progress observer
   * is subscribed with {@link #addProgressChangeListener(MapboxNavigation)}
   * and invoking this method in that scenario will lead to competing updates.
   *
   * @param locations the path to update the location icon
   */
  public void updateLocation(@NonNull List<Location> locations) {
    if (locations.size() > 0) {
      Location targetLocation = locations.get(0);
      locationComponent.forceLocationUpdate(locations, false);
      updateMapWayNameWithLocation(targetLocation);
    }
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
   * Can be used to automatically drive the map camera, puck and route updates
   * when {@link TripSessionState#STARTED}.
   * <p>
   * Use {@link #removeProgressChangeListener()} when the session is finished to avoid leaks.
   *
   * @param navigation to add the progress listeners
   * @see MapboxNavigation#startTripSession()
   */
  public void addProgressChangeListener(@NonNull MapboxNavigation navigation) {
    this.navigation = navigation;
    initializeFpsDelegate(mapView);

    mapRoute.setVanishRouteLineEnabled(vanishRouteLineEnabled);
    mapRoute.addProgressChangeListener(navigation);
    mapCamera.addProgressChangeListener(navigation);
    mapFpsDelegate.addProgressChangeListener(navigation);
    navigation.registerLocationObserver(locationObserver);

    if (navigationPuckPresenter != null) {
      navigationPuckPresenter.addProgressChangeListener(navigation);
    }
  }

  /**
   * Can be used to automatically drive the map camera, puck and route updates
   * when {@link TripSessionState#STARTED}.
   * <p>
   * Use {@link #removeProgressChangeListener()} when the session is finished to avoid leaks.
   *
   * @param navigation to add the progress listeners
   * @param enableVanishingRouteLine determines if the route line should vanish behind the puck.
   * @see MapboxNavigation#startTripSession()
   */
  public void addProgressChangeListener(@NonNull MapboxNavigation navigation, boolean enableVanishingRouteLine) {
    this.vanishRouteLineEnabled = enableVanishingRouteLine;
    addProgressChangeListener(navigation);
  }

  /**
   * Removes the previously registered progress change listener.
   */
  public void removeProgressChangeListener() {
    if (navigation != null) {
      if (mapRoute != null) {
        mapRoute.removeProgressChangeListener(navigation);
      }

      if (mapCamera != null) {
        mapCamera.removeProgressChangeListener();
      }

      if (mapWayName != null) {
        mapWayName.removeProgressChangeListener();
        mapWayName.removeOnWayNameChangedListener(internalWayNameChangedListener);
        mapWayName.updateWayNameQueryMap(!settings.isMapWayNameEnabled());
        mapWayName = null;
      }

      if (mapFpsDelegate != null) {
        mapFpsDelegate.removeProgressChangeListener();
        removeFpsListenersFromCamera();
        mapFpsDelegate = null;
      }

      if (navigation != null) {
        navigation.unregisterLocationObserver(locationObserver);
      }

      if (navigationPuckPresenter != null) {
        navigationPuckPresenter.removeProgressChangeListener();
      }
    }
  }

  /**
   * Can be used to store the current state of the map in
   * {@link androidx.fragment.app.Fragment#onSaveInstanceState(Bundle)}.
   *
   * @param outState to store state variables
   * @see #restoreStateFrom(Bundle)
   */
  public void saveStateWith(@NonNull Bundle outState) {
    settings.updateCurrentPadding(mapPaddingAdjustor.retrieveCurrentPadding());
    settings.updateShouldUseDefaultPadding(mapPaddingAdjustor.isUsingDefault());
    settings.updateCameraTrackingMode(mapCamera.getCameraTrackingMode());
    settings.updateLocationFpsEnabled(locationFpsDelegate.isEnabled());
    settings.updateVanishingRouteLineEnabled(vanishRouteLineEnabled);
    NavigationMapboxMapInstanceState instanceState = new NavigationMapboxMapInstanceState(settings);
    outState.putParcelable(STATE_BUNDLE_KEY, instanceState);
  }

  /**
   * Can be used to restore a {@link NavigationMapboxMap} after it has been initialized.
   * <p>
   * This cannot be called in {@link androidx.fragment.app.Fragment#onViewStateRestored(Bundle)}
   * because we cannot guarantee the map is re-initialized at that point.
   *
   * @param inState to store state variables
   * @see #saveStateWith(Bundle)
   */
  public void restoreStateFrom(@NonNull Bundle inState) {
    Parcelable parcelable = inState.getParcelable(STATE_BUNDLE_KEY);
    if (parcelable instanceof NavigationMapboxMapInstanceState) {
      NavigationMapboxMapInstanceState instanceState = (NavigationMapboxMapInstanceState) parcelable;
      settings = instanceState.retrieveSettings();
      restoreMapWith(settings);
    } else {
      Timber.d("no instance state to restore");
    }
  }

  /**
   * Will draw the given {@link DirectionsRoute} on the map using the colors defined
   * in your given style.
   * <p>
   * In most of the scenarios, this call can be wired to the {@link RoutesObserver#onRoutesChanged(List)}
   * to display the route that {@link MapboxNavigation} uses to compute the route progress.
   *
   * @param route to be drawn
   * @see MapboxNavigation#registerRoutesObserver(RoutesObserver)
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
   * <p>
   * In most of the scenarios, this call can be wired to the {@link RoutesObserver#onRoutesChanged(List)}
   * to display the route that {@link MapboxNavigation} uses to compute the route progress.
   *
   * @param routes to be drawn
   * @see MapboxNavigation#registerRoutesObserver(RoutesObserver)
   */
  public void drawRoutes(@NonNull List<? extends DirectionsRoute> routes) {
    mapRoute.addRoutes(routes);
  }

  public void drawIdentifiableRoute(@NonNull IdentifiableRoute route) {
    mapRoute.addIdentifiableRoute(route);
  }

  public void drawIdentifiableRoutes(@NonNull List<IdentifiableRoute> routes) {
    mapRoute.addIdentifiableRoutes(routes);
  }

  /**
   * Can be used to manually update the route progress.
   * <p>
   * {@link NavigationMapboxMap} automatically listens to
   * {@link RouteProgressObserver#onRouteProgressChanged(RouteProgress)} when a progress observer
   * is subscribed with {@link #addProgressChangeListener(MapboxNavigation)}
   * and invoking this method in that scenario will lead to competing updates.
   * @param routeProgress current progress
   */
  public void onNewRouteProgress(RouteProgress routeProgress) {
    mapRoute.onNewRouteProgress(routeProgress);
  }

  /**
   * Set a {@link OnRouteSelectionChangeListener} to know which route the user has currently
   * selected as their primary route.
   *
   * @param listener a listener which lets you know when the user has changed
   * the primary route and provides the current direction
   * route which the user has selected
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
   * else false
   */
  public void showAlternativeRoutes(boolean alternativesVisible) {
    mapRoute.showAlternativeRoutes(alternativesVisible);
  }

  /**
   * Will hide the drawn route displayed on the map. Does nothing
   * if no route is drawn.
   * @see #showRoute()
   */
  public void hideRoute() {
    mapRoute.updateRouteVisibilityTo(false);
    mapRoute.updateRouteArrowVisibilityTo(false);
  }

  /**
   * Will show the drawn route displayed on the map.
   * @see #hideRoute()
   */
  public void showRoute() {
    mapRoute.updateRouteVisibilityTo(true);
    mapRoute.updateRouteArrowVisibilityTo(true);
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
   * Provides the object being used to draw the route line.
   */
  public NavigationMapRoute retrieveMapRoute() {
    return mapRoute;
  }

  /**
   * Updates the {@link NavigationCamera.TrackingMode} that will be used when camera tracking is enabled.
   *
   * @param trackingMode the tracking mode
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
    initializeWayName(mapboxMap, mapPaddingAdjustor);
    mapWayName.addProgressChangeListener(navigation);
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
   * the custom padding that was last passed via {@link #adjustLocationIconWith(int[])}.
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
   * Hide or show the location icon on the map.
   *
   * @param isVisible true to show, false to hide
   */
  @SuppressLint("MissingPermission")
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
  @NonNull
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
  public void addOnCameraTrackingChangedListener(@NonNull OnCameraTrackingChangedListener listener) {
    locationComponent.addOnCameraTrackingChangedListener(listener);
  }

  /**
   * Remove a {@link OnCameraTrackingChangedListener} from the {@link LocationComponent} that is
   * wrapped within this class.
   *
   * @param listener to be removed
   */
  public void removeOnCameraTrackingChangedListener(@NonNull OnCameraTrackingChangedListener listener) {
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
  public void adjustLocationIconWith(@NonNull int[] customPadding) {
    mapPaddingAdjustor.adjustLocationIconWith(customPadding);
  }

  /**
   * Set {@link Camera} which allows you to perform operations like zoom, tilt etc.
   *
   * @param camera Camera
   */
  public void setCamera(Camera camera) {
    mapCamera.setCamera(camera);
  }

  /**
   * Allows to pass in a custom implementation of {@link PuckDrawableSupplier}
   *
   * @param supplier PuckDrawableSupplier
   */
  public void setPuckDrawableSupplier(@NonNull PuckDrawableSupplier supplier) {
    this.navigationPuckPresenter = new NavigationPuckPresenter(mapboxMap, supplier);
  }

  /**
   * Used to take the snapshot of the current state of the map.
   */
  public void takeScreenshot(@NonNull MapboxMap.SnapshotReadyCallback snapshotReadyCallback) {
    mapboxMap.snapshot(snapshotReadyCallback);
  }

  /**
   * Called during the onStart event of the Lifecycle owner to initialize resources.
   */
  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  protected void onStart() {
    mapCamera.onStart();
    handleWayNameOnStart();
    handleFpsOnStart();
    locationFpsDelegate.onStart();

    if (navigation != null) {
      navigation.registerLocationObserver(locationObserver);
    }

    if (navigationPuckPresenter != null) {
      navigationPuckPresenter.onStart();
    }
  }

  /**
   * Called during the onStop event of the Lifecycle owner to clean up resources.
   */
  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  protected void onStop() {
    mapCamera.onStop();
    handleWayNameOnStop();
    handleFpsOnStop();
    locationFpsDelegate.onStop();

    if (navigation != null) {
      navigation.unregisterLocationObserver(locationObserver);
    }

    if (navigationPuckPresenter != null) {
      navigationPuckPresenter.onStop();
    }
  }

  @SuppressLint("MissingPermission")
  private void setupLocationComponent(
          @NonNull MapView mapView,
          @NonNull MapboxMap map, boolean useSpecializedLocationLayer) {
    locationComponent = map.getLocationComponent();
    map.setMaxZoomPreference(NAVIGATION_MAXIMUM_MAP_ZOOM);
    Context context = mapView.getContext();
    Style style = map.getStyle();
    int locationLayerStyleRes = ThemeSwitcher.retrieveAttrResourceId(context,
        R.attr.navigationViewLocationLayerStyle, R.style.MapboxStyleNavigationLocationLayerStyle);
    LocationComponentOptions options = LocationComponentOptions.createFromAttributes(context, locationLayerStyleRes);
    LocationComponentActivationOptions activationOptions = LocationComponentActivationOptions.builder(context, style)
      .locationComponentOptions(options)
      .useDefaultLocationEngine(false)
      .useSpecializedLocationLayer(useSpecializedLocationLayer)
      .build();
    locationComponent.activateLocationComponent(activationOptions);
    locationComponent.setLocationComponentEnabled(true);
    locationComponent.setCameraMode(CameraMode.TRACKING);
    locationComponent.setRenderMode(RenderMode.COMPASS);
  }

  private void initializeMapPaddingAdjustor(@NonNull MapView mapView, MapboxMap mapboxMap) {
    mapPaddingAdjustor = new MapPaddingAdjustor(mapView, mapboxMap);
  }

  private void initializeNavigationSymbolManager(@NonNull MapView mapView, @NonNull MapboxMap mapboxMap) {
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

  private void initializeRoute(@NonNull MapView mapView, @NonNull MapboxMap map, String routeBelowLayerId) {
    Context context = mapView.getContext();
    int routeStyleRes = ThemeSwitcher.retrieveAttrResourceId(
        context, R.attr.navigationViewRouteStyle, R.style.MapboxStyleNavigationMapRoute
    );
    mapRoute = new NavigationMapRoute.Builder(mapView, map, lifecycleOwner)
        .withStyle(routeStyleRes)
        .withBelowLayer(routeBelowLayerId)
        .withVanishRouteLineEnabled(vanishRouteLineEnabled)
        .build();
  }

  private void initializeCamera(@NonNull MapboxMap map) {
    mapCamera = new NavigationCamera(map);
  }

  private void initializeLocationFpsDelegate(@NonNull MapboxMap map, @NonNull LocationComponent locationComponent) {
    locationFpsDelegate = new LocationFpsDelegate(map, locationComponent);
  }

  private void initializeWayName(@NonNull MapboxMap mapboxMap, @NonNull MapPaddingAdjustor paddingAdjustor) {
    if (mapWayName != null) {
      return;
    }
    initializeStreetsSource(mapboxMap);
    WaynameFeatureFinder featureFinder = new WaynameFeatureFinder(mapboxMap);
    mapWayName = new MapWayName(featureFinder, paddingAdjustor);
    mapWayName.updateWayNameQueryMap(settings.isMapWayNameEnabled());
    mapWayName.addOnWayNameChangedListener(internalWayNameChangedListener);
  }

  private void initializeStreetsSource(@NonNull MapboxMap mapboxMap) {
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

  private void registerLifecycleOwnerObserver() {
    lifecycleOwner.getLifecycle().addObserver(this);
  }

  @Nullable
  private Source findSourceByUrl(@NonNull List<Source> sources, @NonNull String streetsUrl) {
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

  private void restoreMapWith(@NonNull NavigationMapSettings settings) {
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

    vanishRouteLineEnabled = settings.retrieveVanishingRouteLineEnabled();
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

  @NonNull
  private LocationObserver locationObserver = new LocationObserver() {
    @Override
    public void onRawLocationChanged(@NotNull Location rawLocation) {
      Timber.d("raw location %s", rawLocation.toString());
    }

    @Override
    public void onEnhancedLocationChanged(
        @NotNull Location enhancedLocation,
        @NotNull List<? extends Location> keyPoints
    ) {
      if (keyPoints.isEmpty()) {
        updateLocation(enhancedLocation);
      } else {
        updateLocation((List<Location>) keyPoints);
      }
    }
  };
}
