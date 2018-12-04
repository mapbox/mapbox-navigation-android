package com.mapbox.services.android.navigation.ui.v5.route;

import android.app.Activity;
import android.app.Fragment;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.StyleRes;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;

import java.util.ArrayList;
import java.util.List;

/**
 * Provide a route using {@link NavigationMapRoute#addRoutes(List)} and a route will be drawn using
 * runtime styling. The route will automatically be placed below all labels independent of specific
 * style. If the map styles changed when a routes drawn on the map, the route will automatically be
 * redrawn onto the new map style. If during a navigation session, the user gets re-routed, the
 * route line will be redrawn to reflect the new geometry.
 * <p>
 * You are given the option when first constructing an instance of this class to pass in a style
 * resource. This allows for custom colorizing and line scaling of the route. Inside your
 * applications {@code style.xml} file, you extend {@code <style name="NavigationMapRoute">} and
 * change some or all the options currently offered. If no style files provided in the constructor,
 * the default style will be used.
 *
 * @since 0.4.0
 */

public class NavigationMapRoute implements LifecycleObserver {

  @StyleRes
  private final int styleRes;
  private final String belowLayer;
  private final MapboxMap mapboxMap;
  private final MapView mapView;
  private final MapRouteClickListener mapRouteClickListener;
  private final MapRouteProgressChangeListener mapRouteProgressChangeListener;
  private boolean isMapClickListenerAdded = false;
  private MapView.OnDidFinishLoadingStyleListener didFinishLoadingStyleListener;
  private boolean isDidFinishLoadingStyleListenerAdded = false;
  private MapboxNavigation navigation;
  private MapRouteLine routeLine;
  private MapRouteArrow routeArrow;

  /**
   * Construct an instance of {@link NavigationMapRoute}.
   *
   * @param mapView   the MapView to apply the route to
   * @param mapboxMap the MapboxMap to apply route with
   * @since 0.4.0
   */
  public NavigationMapRoute(@NonNull MapView mapView, @NonNull MapboxMap mapboxMap) {
    this(null, mapView, mapboxMap, R.style.NavigationMapRoute);
  }

  /**
   * Construct an instance of {@link NavigationMapRoute}.
   *
   * @param mapView    the MapView to apply the route to
   * @param mapboxMap  the MapboxMap to apply route with
   * @param belowLayer optionally pass in a layer id to place the route line below
   * @since 0.4.0
   */
  public NavigationMapRoute(@NonNull MapView mapView, @NonNull MapboxMap mapboxMap,
                            @Nullable String belowLayer) {
    this(null, mapView, mapboxMap, R.style.NavigationMapRoute, belowLayer);
  }

  /**
   * Construct an instance of {@link NavigationMapRoute}.
   *
   * @param navigation an instance of the {@link MapboxNavigation} object. Passing in null means
   *                   your route won't consider rerouting during a navigation session.
   * @param mapView    the MapView to apply the route to
   * @param mapboxMap  the MapboxMap to apply route with
   * @since 0.4.0
   */
  public NavigationMapRoute(@Nullable MapboxNavigation navigation, @NonNull MapView mapView,
                            @NonNull MapboxMap mapboxMap) {
    this(navigation, mapView, mapboxMap, R.style.NavigationMapRoute);
  }

  /**
   * Construct an instance of {@link NavigationMapRoute}.
   *
   * @param navigation an instance of the {@link MapboxNavigation} object. Passing in null means
   *                   your route won't consider rerouting during a navigation session.
   * @param mapView    the MapView to apply the route to
   * @param mapboxMap  the MapboxMap to apply route with
   * @param belowLayer optionally pass in a layer id to place the route line below
   * @since 0.4.0
   */
  public NavigationMapRoute(@Nullable MapboxNavigation navigation, @NonNull MapView mapView,
                            @NonNull MapboxMap mapboxMap, @Nullable String belowLayer) {
    this(navigation, mapView, mapboxMap, R.style.NavigationMapRoute, belowLayer);
  }

  /**
   * Construct an instance of {@link NavigationMapRoute}.
   *
   * @param navigation an instance of the {@link MapboxNavigation} object. Passing in null means
   *                   your route won't consider rerouting during a navigation session.
   * @param mapView    the MapView to apply the route to
   * @param mapboxMap  the MapboxMap to apply route with
   * @param styleRes   a style resource with custom route colors, scale, etc.
   */
  public NavigationMapRoute(@Nullable MapboxNavigation navigation, @NonNull MapView mapView,
                            @NonNull MapboxMap mapboxMap, @StyleRes int styleRes) {
    this(navigation, mapView, mapboxMap, styleRes, null);
  }

  /**
   * Construct an instance of {@link NavigationMapRoute}.
   *
   * @param navigation an instance of the {@link MapboxNavigation} object. Passing in null means
   *                   your route won't consider rerouting during a navigation session.
   * @param mapView    the MapView to apply the route to
   * @param mapboxMap  the MapboxMap to apply route with
   * @param styleRes   a style resource with custom route colors, scale, etc.
   * @param belowLayer optionally pass in a layer id to place the route line below
   */
  public NavigationMapRoute(@Nullable MapboxNavigation navigation, @NonNull MapView mapView,
                            @NonNull MapboxMap mapboxMap, @StyleRes int styleRes,
                            @Nullable String belowLayer) {
    this.styleRes = styleRes;
    this.belowLayer = belowLayer;
    this.mapView = mapView;
    this.mapboxMap = mapboxMap;
    this.navigation = navigation;
    this.routeLine = new MapRouteLine(mapView.getContext(), mapboxMap, styleRes, belowLayer);
    this.routeArrow = new MapRouteArrow(mapView, mapboxMap, styleRes);
    this.mapRouteClickListener = new MapRouteClickListener(routeLine);
    this.mapRouteProgressChangeListener = new MapRouteProgressChangeListener(routeLine, routeArrow);
    initializeDidFinishLoadingStyleListener();
    addListeners();
  }

  // For testing only
  NavigationMapRoute(@Nullable MapboxNavigation navigation, @NonNull MapView mapView,
                     @NonNull MapboxMap mapboxMap, @StyleRes int styleRes, @Nullable String belowLayer,
                     MapRouteClickListener mapClickListener,
                     MapView.OnDidFinishLoadingStyleListener didFinishLoadingStyleListener,
                     MapRouteProgressChangeListener progressChangeListener) {
    this.styleRes = styleRes;
    this.belowLayer = belowLayer;
    this.mapView = mapView;
    this.mapboxMap = mapboxMap;
    this.navigation = navigation;
    this.mapRouteClickListener = mapClickListener;
    this.didFinishLoadingStyleListener = didFinishLoadingStyleListener;
    this.mapRouteProgressChangeListener = progressChangeListener;
    addListeners();
  }

  // For testing only
  NavigationMapRoute(@Nullable MapboxNavigation navigation, @NonNull MapView mapView,
                     @NonNull MapboxMap mapboxMap, @StyleRes int styleRes, @Nullable String belowLayer,
                     MapRouteClickListener mapClickListener,
                     MapView.OnDidFinishLoadingStyleListener didFinishLoadingStyleListener,
                     MapRouteProgressChangeListener progressChangeListener,
                     MapRouteLine routeLine,
                     MapRouteArrow routeArrow) {
    this.styleRes = styleRes;
    this.belowLayer = belowLayer;
    this.mapView = mapView;
    this.mapboxMap = mapboxMap;
    this.navigation = navigation;
    this.mapRouteClickListener = mapClickListener;
    this.didFinishLoadingStyleListener = didFinishLoadingStyleListener;
    this.mapRouteProgressChangeListener = progressChangeListener;
    this.routeLine = routeLine;
    this.routeArrow = routeArrow;
  }

  /**
   * Allows adding a single primary route for the user to traverse along. No alternative routes will
   * be drawn on top of the map.
   *
   * @param directionsRoute the directions route which you'd like to display on the map
   * @since 0.4.0
   */
  public void addRoute(DirectionsRoute directionsRoute) {
    List<DirectionsRoute> routes = new ArrayList<>();
    routes.add(directionsRoute);
    addRoutes(routes);
  }

  /**
   * Provide a list of {@link DirectionsRoute}s, the primary route will default to the first route
   * in the directions route list. All other routes in the list will be drawn on the map using the
   * alternative route style.
   *
   * @param directionsRoutes a list of direction routes, first one being the primary and the rest of
   *                         the routes are considered alternatives.
   * @since 0.8.0
   */
  public void addRoutes(@NonNull @Size(min = 1) List<DirectionsRoute> directionsRoutes) {
    routeLine.draw(directionsRoutes);
  }

  /**
   * Hides all routes on the map drawn by this class.
   *
   * @param isVisible true to show routes, false to hide
   */
  public void updateRouteVisibilityTo(boolean isVisible) {
    routeLine.updateVisibilityTo(isVisible);
    mapRouteProgressChangeListener.updateVisibility(isVisible);
  }


  /**
   * Hides the progress arrow on the map drawn by this class.
   *
   * @param isVisible true to show routes, false to hide
   */
  public void updateRouteArrowVisibilityTo(boolean isVisible) {
    routeArrow.updateVisibilityTo(isVisible);
  }

  /**
   * Add a {@link OnRouteSelectionChangeListener} to know which route the user has currently
   * selected as their primary route.
   *
   * @param onRouteSelectionChangeListener a listener which lets you know when the user has changed
   *                                       the primary route and provides the current direction
   *                                       route which the user has selected
   * @since 0.8.0
   */
  public void setOnRouteSelectionChangeListener(
    @Nullable OnRouteSelectionChangeListener onRouteSelectionChangeListener) {
    mapRouteClickListener.setOnRouteSelectionChangeListener(onRouteSelectionChangeListener);
  }

  /**
   * Toggle whether or not you'd like the map to display the alternative routes. This options great
   * for when the user actually begins the navigation session and alternative routes aren't needed
   * anymore.
   *
   * @param alternativesVisible true if you'd like alternative routes to be displayed on the map,
   *                            else false
   * @since 0.8.0
   */
  public void showAlternativeRoutes(boolean alternativesVisible) {
    mapRouteClickListener.updateAlternativesVisible(alternativesVisible);
    routeLine.toggleAlternativeVisibilityWith(alternativesVisible);
  }

  /**
   * This method will allow this class to listen to new routes based on
   * the progress updates from {@link MapboxNavigation}.
   * <p>
   * If a new route is given to {@link MapboxNavigation#startNavigation(DirectionsRoute)}, this
   * class will automatically draw the new route.
   *
   * @param navigation to add the progress change listener
   */
  public void addProgressChangeListener(MapboxNavigation navigation) {
    this.navigation = navigation;
    navigation.addProgressChangeListener(mapRouteProgressChangeListener);
  }


  /**
   * Should be called if {@link NavigationMapRoute#addProgressChangeListener(MapboxNavigation)} was
   * called to prevent leaking.
   *
   * @param navigation to remove the progress change listener
   */
  public void removeProgressChangeListener(MapboxNavigation navigation) {
    if (navigation != null) {
      navigation.removeProgressChangeListener(mapRouteProgressChangeListener);
    }
  }

  /**
   * This method should be added in your {@link Activity#onStart()} or {@link Fragment#onStart()}
   * to handle adding and removing of listeners, preventing memory leaks.
   */
  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  public void onStart() {
    addListeners();
  }

  /**
   * This method should be added in your {@link Activity#onStop()} or {@link Fragment#onStop()}
   * to handle adding and removing of listeners, preventing memory leaks.
   */
  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  public void onStop() {
    removeListeners();
  }

  /**
   * This method should be added in your {@link Activity#onDestroy()} or {@link Fragment#onDestroyView()}
   * to handle removing resources that were added to the map.
   */
  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  public void onDestroy() {
    routeLine.onDestroy();
    routeArrow.onDestroy();
  }

  private void initializeDidFinishLoadingStyleListener() {
    didFinishLoadingStyleListener = new MapView.OnDidFinishLoadingStyleListener() {
      @Override
      public void onDidFinishLoadingStyle() {
        redraw();
      }
    };
  }

  private void addListeners() {
    if (!isMapClickListenerAdded) {
      mapboxMap.addOnMapClickListener(mapRouteClickListener);
      isMapClickListenerAdded = true;
    }
    if (navigation != null) {
      navigation.addProgressChangeListener(mapRouteProgressChangeListener);
    }
    if (!isDidFinishLoadingStyleListenerAdded) {
      mapView.addOnDidFinishLoadingStyleListener(didFinishLoadingStyleListener);
      isDidFinishLoadingStyleListenerAdded = true;
    }
  }

  private void removeListeners() {
    if (isMapClickListenerAdded) {
      mapboxMap.removeOnMapClickListener(mapRouteClickListener);
      isMapClickListenerAdded = false;
    }
    if (navigation != null) {
      navigation.removeProgressChangeListener(mapRouteProgressChangeListener);
    }
    if (isDidFinishLoadingStyleListenerAdded) {
      mapView.removeOnDidFinishLoadingStyleListener(didFinishLoadingStyleListener);
      isDidFinishLoadingStyleListenerAdded = false;
    }
  }

  private void redraw() {
    routeArrow = new MapRouteArrow(mapView, mapboxMap, styleRes);
    List<DirectionsRoute> routes = routeLine.retrieveDirectionsRoutes();
    boolean alternativesVisible = routeLine.retrieveAlternativesVisible();
    int primaryRouteIndex = routeLine.retrievePrimaryRouteIndex();
    boolean isVisible = routeLine.retrieveVisibilty();
    routeLine = new MapRouteLine(mapView.getContext(), mapboxMap, styleRes, belowLayer);
    routeLine.redraw(routes, alternativesVisible, primaryRouteIndex, isVisible);
  }
}