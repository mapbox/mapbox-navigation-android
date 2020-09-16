package com.mapbox.navigation.ui.route;

import android.content.Context;
import androidx.annotation.*;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.trip.session.TripSessionState;
import com.mapbox.navigation.ui.internal.route.RouteLayerProvider;
import com.mapbox.navigation.ui.internal.utils.CompareUtils;
import com.mapbox.navigation.ui.internal.utils.RouteLineValueAnimator;
import org.jetbrains.annotations.NotNull;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mapbox.navigation.ui.internal.route.RouteConstants.LAYER_ABOVE_UPCOMING_MANEUVER_ARROW;
import static com.mapbox.navigation.ui.route.MapboxRouteLayerProviderFactory.getLayerProvider;

/**
 * Provide a route using {@link NavigationMapRoute#addRoutes(List)} and a route will be drawn using
 * runtime styling. The route will automatically be placed below all labels independent of specific
 * style. If the map styles changed when a routes drawn on the map, the route will automatically be
 * redrawn onto the new map style.
 * <p>
 * You are given the option when first constructing an instance of this class to pass in a style
 * resource. This allows for custom colorizing and line scaling of the route. Inside your
 * applications {@code style.xml} file, you extend {@code <style name="NavigationMapRoute">} and
 * change some or all the options currently offered. If no style files provided in the constructor,
 * the default style will be used.
 */
@UiThread
public class NavigationMapRoute implements LifecycleObserver {

  @StyleRes
  private final int styleRes;
  @Nullable
  private final String belowLayer;
  @NonNull
  private final MapboxMap mapboxMap;
  @NonNull
  private final MapView mapView;
  private MapRouteLine routeLine;
  private MapRouteArrow routeArrow;
  private boolean vanishRouteLineEnabled;
  //private MapView.OnDidFinishLoadingStyleListener didFinishLoadingStyleListener;
  @Nullable
  private MapboxNavigation navigation;

  @NonNull
  private final LifecycleOwner lifecycleOwner;
  @Nullable
  private MapRouteProgressChangeListener mapRouteProgressChangeListener;

  @Nullable
  private RouteLineValueAnimator vanishingRouteLineAnimator;

  @Nullable
  private MapRouteLineInitializedCallback routeLineInitializedCallback;

  /**
   * Construct an instance of {@link NavigationMapRoute}.
   *
   * @param navigation an instance of the {@link MapboxNavigation} object. Passing in null means
   * your route won't consider rerouting during a navigation session.
   * @param mapView the MapView to apply the route to
   * @param mapboxMap the MapboxMap to apply route with
   * @param lifecycleOwner provides lifecycle for the component
   * @param styleRes a style resource with custom route colors, scale, etc.
   * @param belowLayer optionally pass in a layer id to place the route line below
   * @param vanishRouteLineEnabled determines if the route line should vanish behind the puck during navigation.
   * @param routeLineInitializedCallback indicates that the route line layer has been added to the current style
   * @param routeStyleDescriptors optionally describes the styling of the route lines
   */
  private NavigationMapRoute(@Nullable MapboxNavigation navigation,
      @NonNull MapView mapView,
      @NonNull MapboxMap mapboxMap,
      @NonNull LifecycleOwner lifecycleOwner,
      @StyleRes int styleRes,
      @Nullable String belowLayer,
      boolean vanishRouteLineEnabled,
      @Nullable MapRouteLineInitializedCallback routeLineInitializedCallback,
      @Nullable List<RouteStyleDescriptor> routeStyleDescriptors) {
    //this.routeStyleDescriptors = routeStyleDescriptors;
    this.vanishRouteLineEnabled = vanishRouteLineEnabled;
    this.styleRes = styleRes;
    this.belowLayer = belowLayer;
    this.mapView = mapView;
    this.mapboxMap = mapboxMap;
    this.navigation = navigation;
    buildMapRouteLine(
        mapView,
        mapboxMap,
        styleRes,
        belowLayer,
        routeStyleDescriptors,
        routeLineInitializedCallback
    );
    //this.routeArrow = new MapRouteArrow(mapView, mapboxMap, styleRes, LAYER_ABOVE_UPCOMING_MANEUVER_ARROW);
    //this.mapRouteClickListener = new MapRouteClickListener(this.routeLine);
    this.mapRouteProgressChangeListener = buildMapRouteProgressChangeListener();
    this.routeLineInitializedCallback = routeLineInitializedCallback;
    this.lifecycleOwner = lifecycleOwner;
    //initializeDidFinishLoadingStyleListener();
    registerLifecycleObserver();
  }

  /**
   * Allows adding a single primary route for the user to traverse along. No alternative routes will
   * be drawn on top of the map.
   *
   * @param directionsRoute the directions route which you'd like to display on the map
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
   * the routes are considered alternatives.
   */
  public void addRoutes(@NonNull @Size(min = 1) List<? extends DirectionsRoute> directionsRoutes) {
    //cancelVanishingRouteLineAnimator();
    if (directionsRoutes.isEmpty()) {
      routeLine.clearRouteData();
    } else if (CompareUtils.areEqualContentsIgnoreOrder(
        routeLine.retrieveDirectionsRoutes(),
        directionsRoutes
    )) {
      routeLine.updatePrimaryRouteIndex(directionsRoutes.get(0));
    } else {
      routeLine.draw(directionsRoutes);
    }
  }

  @Nullable
  private void buildMapRouteLine(
      @NonNull final MapView mapView,
      @NonNull final MapboxMap mapboxMap,
      @StyleRes final int styleRes,
      @Nullable final String belowLayer,
      @Nullable final List<RouteStyleDescriptor> routeStyleDescriptors,
      final MapRouteLineInitializedCallback routeLineInitializedCallback
  ) {
    final Context context = mapView.getContext();
    final List<RouteStyleDescriptor> routeStyleDescriptorsToUse = routeStyleDescriptors == null
        ? Collections.emptyList() : routeStyleDescriptors;
    final RouteLayerProvider layerProvider = getLayerProvider(routeStyleDescriptorsToUse);

    mapboxMap.getStyle(style ->
        routeLine = new MapRouteLine(
          context,
          style,
          styleRes,
          belowLayer,
          layerProvider,
          routeLineInitializedCallback
      ));
  }

  private void shutdownVanishingRouteLineAnimator() {
    if (this.vanishingRouteLineAnimator != null) {
      this.vanishingRouteLineAnimator.setValueAnimatorHandler(null);
      cancelVanishingRouteLineAnimator();
    }
  }

  private void cancelVanishingRouteLineAnimator() {
    if (this.vanishingRouteLineAnimator != null) {
      this.vanishingRouteLineAnimator.cancelAnimationCallbacks();
    }
  }

  /**
   * Called during the onStart event of the Lifecycle owner to initialize resources.
   */
  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  protected void onStart() {
    addListeners();
    updateProgressChangeListener();
  }

  /**
   * Called during the onStop event of the Lifecycle owner to clean up resources.
   */
  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  protected void onStop() {
    shutdownVanishingRouteLineAnimator();
    removeListeners();
  }

  private void removeListeners() {
    //if (isMapClickListenerAdded) {
    //  mapboxMap.removeOnMapClickListener(mapRouteClickListener);
    //  isMapClickListenerAdded = false;
    //}
    if (navigation != null) {
      navigation.unregisterRouteProgressObserver(mapRouteProgressChangeListener);
    }
    //if (isDidFinishLoadingStyleListenerAdded) {
    //  mapView.removeOnDidFinishLoadingStyleListener(didFinishLoadingStyleListener);
    //  isDidFinishLoadingStyleListenerAdded = false;
    //}
  }

  private void updateProgressChangeListener() {
    if (navigation != null) {
      navigation.unregisterRouteProgressObserver(mapRouteProgressChangeListener);
    }
    mapRouteProgressChangeListener = buildMapRouteProgressChangeListener();
    if (navigation != null) {
      navigation.registerRouteProgressObserver(mapRouteProgressChangeListener);
    }
  }

  private void addListeners() {
    //if (!isMapClickListenerAdded) {
    //  mapboxMap.addOnMapClickListener(mapRouteClickListener);
    //  isMapClickListenerAdded = true;
    //}
    if (navigation != null) {
      navigation.registerRouteProgressObserver(mapRouteProgressChangeListener);
    }
    //if (!isDidFinishLoadingStyleListenerAdded) {
    //  mapView.addOnDidFinishLoadingStyleListener(didFinishLoadingStyleListener);
    //  isDidFinishLoadingStyleListenerAdded = true;
    //}
  }

  @Nullable
  private MapRouteProgressChangeListener buildMapRouteProgressChangeListener() {
    shutdownVanishingRouteLineAnimator();
    if (vanishRouteLineEnabled) {
      vanishingRouteLineAnimator = new RouteLineValueAnimator();
    } else {
      vanishingRouteLineAnimator = null;
    }

    float vanishingPoint = (this.mapRouteProgressChangeListener != null)
        ? this.mapRouteProgressChangeListener.getPercentDistanceTraveled() : 0f;
    MapRouteProgressChangeListener newListener = new MapRouteProgressChangeListener(
        this.routeLine,
        routeArrow,
        vanishingRouteLineAnimator
    );
    newListener.updatePercentDistanceTraveled(vanishingPoint);
    return newListener;
  }

  private void registerLifecycleObserver() {
    lifecycleOwner.getLifecycle().addObserver(this);
  }

  /**
   * Hides all routes on the map drawn by this class.
   *
   * @param isVisible true to show routes, false to hide
   */
  public void updateRouteVisibilityTo(boolean isVisible) {
    routeLine.updateVisibilityTo(isVisible);
  }

  /**
   * The Builder of {@link NavigationMapRoute}.
   */
  public static class Builder {
    @NonNull private MapView mapView;
    @NonNull private MapboxMap mapboxMap;
    @NonNull private LifecycleOwner lifecycleOwner;
    @Nullable private MapboxNavigation navigation;
    @StyleRes private int styleRes = com.mapbox.navigation.ui.R.style.MapboxStyleNavigationMapRoute;
    @Nullable private String belowLayer;
    private boolean vanishRouteLineEnabled = false;
    @Nullable private MapRouteLineInitializedCallback routeLineInitializedCallback;
    @Nullable private List<RouteStyleDescriptor> routeStyleDescriptors;

    /**
     * Instantiates a new Builder.
     *
     * @param mapView the MapView to apply the route to
     * @param mapboxMap the MapboxMap to apply route with
     * @param lifecycleOwner provides lifecycle for the component
     */
    public Builder(@NonNull MapView mapView, @NonNull MapboxMap mapboxMap, @NonNull LifecycleOwner lifecycleOwner) {
      this.mapView = mapView;
      this.mapboxMap = mapboxMap;
      this.lifecycleOwner = lifecycleOwner;
    }

    /**
     * An instance of the {@link MapboxNavigation} object. Default is null that means
     * your route won't consider rerouting during a navigation session.
     *
     * @return the builder
     */
    @NonNull
    public Builder withMapboxNavigation(@Nullable MapboxNavigation navigation) {
      this.navigation = navigation;
      return this;
    }

    /**
     * @param enabled determines if the route line should vanish behind the puck
     * during navigation. By default is `false`
     */
    @NonNull
    public Builder withVanishRouteLineEnabled(boolean enabled) {
      this.vanishRouteLineEnabled = enabled;
      return this;
    }

    /**
     * Style resource with custom route colors, scale, etc. Default value is R.style.NavigationMapRoute
     *
     * @return the builder
     */
    @NonNull
    public Builder withStyle(@StyleRes int styleRes) {
      this.styleRes = styleRes;
      return this;
    }

    /**
     * BelowLayer optionally pass in a layer id to place the route line below
     *
     * @return the builder
     */
    @NonNull
    public Builder withBelowLayer(@Nullable String belowLayer) {
      this.belowLayer = belowLayer;
      return this;
    }

    /**
     * RouteStyleDescriptors can be used for programmatically styling the route lines.
     *
     * @return the builder
     */
    @NonNull
    public Builder withRouteStyleDescriptors(List<RouteStyleDescriptor> routeStyleDescriptors) {
      this.routeStyleDescriptors = routeStyleDescriptors;
      return this;
    }

    /**
     * Indicate that the route line layer has been added to the current style
     *
     * @return the builder
     */
    @NonNull
    public Builder withRouteLineInitializedCallback(
        @Nullable MapRouteLineInitializedCallback routeLineInitializedCallback
    ) {
      this.routeLineInitializedCallback = routeLineInitializedCallback;
      return this;
    }

    /**
     * Build an instance of {@link NavigationMapRoute}
     */
    @NonNull
    public NavigationMapRoute build() {
      return new NavigationMapRoute(
          navigation,
          mapView,
          mapboxMap,
          lifecycleOwner,
          styleRes,
          belowLayer,
          vanishRouteLineEnabled,
          routeLineInitializedCallback,
          routeStyleDescriptors
      );
    }
  }
}
