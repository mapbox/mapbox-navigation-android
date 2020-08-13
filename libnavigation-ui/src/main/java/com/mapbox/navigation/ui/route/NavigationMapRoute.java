package com.mapbox.navigation.ui.route;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.StyleRes;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.libnavigation.ui.R;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.ui.internal.route.MapRouteSourceProvider;
import com.mapbox.navigation.ui.internal.route.RouteLayerProvider;
import com.mapbox.navigation.ui.internal.route.RouteConstants;
import com.mapbox.navigation.ui.internal.utils.CompareUtils;
import com.mapbox.navigation.ui.internal.utils.RouteLineValueAnimator;

import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mapbox.navigation.ui.internal.route.RouteConstants.LAYER_ABOVE_UPCOMING_MANEUVER_ARROW;
import static com.mapbox.navigation.ui.internal.route.MapboxRouteLayerProviderFactory.getLayerProvider;
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
 */

public class NavigationMapRoute implements LifecycleObserver {

  @StyleRes
  private final int styleRes;
  private final String belowLayer;
  @NonNull
  private final MapboxMap mapboxMap;
  @NonNull
  private final MapView mapView;
  @NonNull
  private final LifecycleOwner lifecycleOwner;
  private MapRouteClickListener mapRouteClickListener;
  private MapRouteProgressChangeListener mapRouteProgressChangeListener;
  private boolean isMapClickListenerAdded = false;
  private MapView.OnDidFinishLoadingStyleListener didFinishLoadingStyleListener;
  private boolean isDidFinishLoadingStyleListenerAdded = false;
  private MapboxNavigation navigation;
  private MapRouteLine routeLine;
  private MapRouteArrow routeArrow;
  private boolean vanishRouteLineEnabled;
  private MapRouteLineInitializedCallback routeLineInitializedCallback;
  private RouteLineValueAnimator vanishingRouteLineAnimator;
  private List<RouteStyleDescriptor> routeStyleDescriptors;

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
    this.routeStyleDescriptors = routeStyleDescriptors;
    this.vanishRouteLineEnabled = vanishRouteLineEnabled;
    this.styleRes = styleRes;
    this.belowLayer = belowLayer;
    this.mapView = mapView;
    this.mapboxMap = mapboxMap;
    this.navigation = navigation;
    this.routeLine = buildMapRouteLine(
            mapView,
            mapboxMap,
            styleRes,
            belowLayer,
            routeStyleDescriptors,
            routeLineInitializedCallback
    );
    this.routeArrow = new MapRouteArrow(mapView, mapboxMap, styleRes, LAYER_ABOVE_UPCOMING_MANEUVER_ARROW);
    this.mapRouteClickListener = new MapRouteClickListener(this.routeLine);
    this.mapRouteProgressChangeListener = buildMapRouteProgressChangeListener();
    this.routeLineInitializedCallback = routeLineInitializedCallback;
    this.lifecycleOwner = lifecycleOwner;
    initializeDidFinishLoadingStyleListener();
    registerLifecycleObserver();
  }

  @TestOnly NavigationMapRoute(@Nullable MapboxNavigation navigation,
      @NonNull MapView mapView,
      @NonNull MapboxMap mapboxMap,
      @NonNull LifecycleOwner lifecycleOwner,
      @StyleRes int styleRes,
      @Nullable String belowLayer,
      MapRouteClickListener mapClickListener,
      MapView.OnDidFinishLoadingStyleListener didFinishLoadingStyleListener,
      MapRouteProgressChangeListener progressChangeListener) {
    this.navigation = navigation;
    this.mapView = mapView;
    this.mapboxMap = mapboxMap;
    this.lifecycleOwner = lifecycleOwner;
    this.styleRes = styleRes;
    this.belowLayer = belowLayer;
    this.mapRouteClickListener = mapClickListener;
    this.didFinishLoadingStyleListener = didFinishLoadingStyleListener;
    this.mapRouteProgressChangeListener = progressChangeListener;
    addListeners();
  }

  @TestOnly NavigationMapRoute(@Nullable MapboxNavigation navigation,
      @NonNull MapView mapView,
      @NonNull MapboxMap mapboxMap,
      @NonNull LifecycleOwner lifecycleOwner,
      @StyleRes int styleRes,
      @Nullable String belowLayer,
      MapRouteClickListener mapClickListener,
      MapView.OnDidFinishLoadingStyleListener didFinishLoadingStyleListener,
      MapRouteProgressChangeListener progressChangeListener,
      MapRouteLine routeLine,
      MapRouteArrow routeArrow) {
    this.navigation = navigation;
    this.mapView = mapView;
    this.mapboxMap = mapboxMap;
    this.lifecycleOwner = lifecycleOwner;
    this.styleRes = styleRes;
    this.belowLayer = belowLayer;
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
    if (directionsRoutes.isEmpty()) {
      cancelVanishingRouteLineAnimator();
      routeLine.draw(directionsRoutes);
    } else if (!CompareUtils.areEqualContentsIgnoreOrder(
        routeLine.retrieveDirectionsRoutes(),
        directionsRoutes)
    ) {
      cancelVanishingRouteLineAnimator();
      routeLine.draw(directionsRoutes);
    }
  }

  public void addIdentifiableRoute(IdentifiableRoute directionsRoute) {
    List<IdentifiableRoute> routes = new ArrayList<>();
    routes.add(directionsRoute);
    addIdentifiableRoutes(routes);
  }

  public void addIdentifiableRoutes(@NonNull @Size(min = 1) List<IdentifiableRoute> directionsRoutes) {
    List<DirectionsRoute> routeList = new ArrayList<>();
    for (IdentifiableRoute identifiableRoute : directionsRoutes) {
      routeList.add(identifiableRoute.getRoute());
    }

    if (directionsRoutes.isEmpty()) {
      routeLine.drawIdentifiableRoutes(directionsRoutes);
    } else if (!CompareUtils.areEqualContentsIgnoreOrder(
            routeLine.retrieveDirectionsRoutes(),
            routeList)
    ) {
      routeLine.drawIdentifiableRoutes(directionsRoutes);
    }
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
   * the primary route and provides the current direction
   * route which the user has selected
   */
  public void setOnRouteSelectionChangeListener(
      @Nullable OnRouteSelectionChangeListener onRouteSelectionChangeListener
  ) {
    mapRouteClickListener.setOnRouteSelectionChangeListener(onRouteSelectionChangeListener);
  }

  /**
   * Toggle whether or not you'd like the map to display the alternative routes. This options great
   * for when the user actually begins the navigation session and alternative routes aren't needed
   * anymore.
   *
   * @param alternativesVisible true if you'd like alternative routes to be displayed on the map,
   * else false
   */
  public void showAlternativeRoutes(boolean alternativesVisible) {
    mapRouteClickListener.updateAlternativesVisible(alternativesVisible);
    routeLine.toggleAlternativeVisibilityWith(alternativesVisible);
  }

  /**
   * This method will allow this class to listen to new routes based on
   * the progress updates from {@link MapboxNavigation}.
   * <p>
   * If a new route is given to {@link MapboxNavigation#startTripSession()}, this
   * class will automatically draw the new route.
   *
   * @param navigation to add the progress change listener
   */
  public void addProgressChangeListener(MapboxNavigation navigation) {
    this.navigation = navigation;
    navigation.registerRouteProgressObserver(mapRouteProgressChangeListener);
  }

  /**
   * This method will allow this class to listen to new routes based on
   * the progress updates from {@link MapboxNavigation}.
   * <p>
   * If a new route is given to {@link MapboxNavigation#startTripSession()}, this
   * class will automatically draw the new route.
   *
   * @param navigation to add the progress change listener
   * @param vanishRouteLineEnabled determines if the route line should vanish behind the puck.
   */
  public void addProgressChangeListener(MapboxNavigation navigation, boolean vanishRouteLineEnabled) {
    this.navigation = navigation;
    this.vanishRouteLineEnabled = vanishRouteLineEnabled;
    this.mapRouteProgressChangeListener = buildMapRouteProgressChangeListener();
    navigation.registerRouteProgressObserver(mapRouteProgressChangeListener);
  }

  /**
   * Should be called if {@link NavigationMapRoute#addProgressChangeListener(MapboxNavigation)} was
   * called to prevent leaking.
   *
   * @param navigation to remove the progress change listener
   */
  public void removeProgressChangeListener(MapboxNavigation navigation) {
    shutdownVanishingRouteLineAnimator();
    if (navigation != null) {
      navigation.unregisterRouteProgressObserver(mapRouteProgressChangeListener);
    }
  }

  public void onNewRouteProgress(RouteProgress routeProgress) {
    if (mapRouteProgressChangeListener != null) {
      mapRouteProgressChangeListener.onRouteProgressChanged(routeProgress);
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

  private void registerLifecycleObserver() {
    lifecycleOwner.getLifecycle().addObserver(this);
  }

  private MapRouteLine buildMapRouteLine(@NonNull final MapView mapView, @NonNull final MapboxMap mapboxMap,
                                         @StyleRes final int styleRes, @Nullable final String belowLayer,
                                         @Nullable final List<RouteStyleDescriptor> routeStyleDescriptors,
                                         final MapRouteLineInitializedCallback routeLineInitializedCallback) {
    final Context context = mapView.getContext();
    final List<RouteStyleDescriptor> routeStyleDescriptorsToUse = routeStyleDescriptors == null
            ? Collections.emptyList() : routeStyleDescriptors;
    final RouteLayerProvider layerProvider = getLayerProvider(routeStyleDescriptorsToUse);

    return new MapRouteLine(
        context,
        mapboxMap.getStyle(),
        styleRes,
        belowLayer,
        layerProvider,
        new MapRouteSourceProvider(),
        routeLineInitializedCallback
    );
  }

  private void initializeDidFinishLoadingStyleListener() {
    didFinishLoadingStyleListener = new MapView.OnDidFinishLoadingStyleListener() {
      @Override
      public void onDidFinishLoadingStyle() {
        mapboxMap.getStyle(new Style.OnStyleLoaded() {
          @Override
          public void onStyleLoaded(@NonNull Style style) {
            redraw(style);
          }
        });
      }
    };
  }

  private void addListeners() {
    if (!isMapClickListenerAdded) {
      mapboxMap.addOnMapClickListener(mapRouteClickListener);
      isMapClickListenerAdded = true;
    }
    if (navigation != null) {
      navigation.registerRouteProgressObserver(mapRouteProgressChangeListener);
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
      navigation.unregisterRouteProgressObserver(mapRouteProgressChangeListener);
    }
    if (isDidFinishLoadingStyleListenerAdded) {
      mapView.removeOnDidFinishLoadingStyleListener(didFinishLoadingStyleListener);
      isDidFinishLoadingStyleListenerAdded = false;
    }
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

  private void redraw(Style style) {
    recreateRouteLine(style);
    boolean arrowVisibility = routeArrow.routeArrowIsVisible();
    routeArrow = new MapRouteArrow(mapView, mapboxMap, styleRes, routeLine.getTopLayerId());
    routeArrow.updateVisibilityTo(arrowVisibility);
    updateProgressChangeListener();
  }

  private void recreateRouteLine(final Style style) {
    final Context context = mapView.getContext();
    final List<RouteStyleDescriptor> routeStyleDescriptorsToUse = routeStyleDescriptors == null
            ? Collections.emptyList() : routeStyleDescriptors;
    final RouteLayerProvider layerProvider = getLayerProvider(routeStyleDescriptorsToUse);

    final float vanishingPointOffset = routeLine.getVanishPointOffset();
    routeLine = new MapRouteLine(
        context,
        style,
        styleRes,
        belowLayer,
        layerProvider,
        routeLine.retrieveRouteFeatureData(),
        routeLine.retrieveRouteExpressionData(),
        routeLine.retrieveVisibility(),
        routeLine.retrieveAlternativesVisible(),
        new MapRouteSourceProvider(),
        vanishingPointOffset,
        routeLineInitializedCallback
    );
    mapboxMap.removeOnMapClickListener(mapRouteClickListener);
    mapRouteClickListener = new MapRouteClickListener(routeLine);
    mapboxMap.addOnMapClickListener(mapRouteClickListener);
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

  private MapRouteProgressChangeListener buildMapRouteProgressChangeListener() {
    shutdownVanishingRouteLineAnimator();
    if (vanishRouteLineEnabled) {
      vanishingRouteLineAnimator = new RouteLineValueAnimator();
      vanishingRouteLineAnimator.setAnimationDelay(RouteConstants.ROUTE_LINE_VANISH_ANIMATION_DELAY);
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

  /**
   * Returns the percentage of the distance traveled that was last calculated. This is only
   * calculated if the vanishing route line feature is enabled.
   *
   * @return the value calculated during the last progress update event or 0 if not enabled.
   */
  public float getPercentDistanceTraveled() {
    return mapRouteProgressChangeListener.getPercentDistanceTraveled();
  }

  public void updateRouteLineWithDistanceTraveled(float distanceTraveled) {
    routeLine.hideCasingLineAtOffset(distanceTraveled);
    routeLine.hideRouteLineAtOffset(distanceTraveled);
    mapRouteProgressChangeListener.updatePercentDistanceTraveled(distanceTraveled);
  }

  /**
   * The Builder of {@link NavigationMapRoute}.
   */
  public static class Builder {
    @NonNull private MapView mapView;
    @NonNull private MapboxMap mapboxMap;
    @NonNull private LifecycleOwner lifecycleOwner;
    @Nullable private MapboxNavigation navigation;
    @StyleRes private int styleRes = R.style.NavigationMapRoute;
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
     * @param vanishRouteLineEnabled determines if the route line should vanish behind the puck
     * during navigation. By default is `false`
     * @return the builder
     */
    public Builder withMapboxNavigation(@Nullable MapboxNavigation navigation, boolean vanishRouteLineEnabled) {
      this.navigation = navigation;
      this.vanishRouteLineEnabled = vanishRouteLineEnabled;
      return this;
    }

    /**
     * Style resource with custom route colors, scale, etc. Default value is R.style.NavigationMapRoute
     *
     * @return the builder
     */
    public Builder withStyle(@StyleRes int styleRes) {
      this.styleRes = styleRes;
      return this;
    }

    /**
     * BelowLayer optionally pass in a layer id to place the route line below
     *
     * @return the builder
     */
    public Builder withBelowLayer(@Nullable String belowLayer) {
      this.belowLayer = belowLayer;
      return this;
    }

    /**
     * RouteStyleDescriptors can be used for programmatically styling the route lines.
     *
     * @return the builder
     */
    public Builder withRouteStyleDescriptors(List<RouteStyleDescriptor> routeStyleDescriptors) {
      this.routeStyleDescriptors = routeStyleDescriptors;
      return this;
    }

    /**
     * Indicate that the route line layer has been added to the current style
     *
     * @return the builder
     */
    public Builder withRouteLineInitializedCallback(
        @Nullable MapRouteLineInitializedCallback routeLineInitializedCallback
    ) {
      this.routeLineInitializedCallback = routeLineInitializedCallback;
      return this;
    }

    /**
     * Build an instance of {@link NavigationMapRoute}
     */
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