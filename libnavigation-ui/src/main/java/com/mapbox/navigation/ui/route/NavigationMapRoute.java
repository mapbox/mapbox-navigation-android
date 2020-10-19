package com.mapbox.navigation.ui.route;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.StyleRes;
import androidx.annotation.UiThread;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.mapboxsdk.location.OnIndicatorPositionChangedListener;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.ui.R;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.ui.internal.route.MapRouteSourceProvider;
import com.mapbox.navigation.ui.internal.route.RouteLayerProvider;
import com.mapbox.navigation.ui.internal.utils.CompareUtils;

import org.jetbrains.annotations.TestOnly;

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
  @NonNull
  private final LifecycleOwner lifecycleOwner;
  @Nullable
  private MapRouteClickListener mapRouteClickListener;
  @Nullable
  private MapRouteProgressChangeListener mapRouteProgressChangeListener;
  private boolean isMapClickListenerAdded = false;
  private MapView.OnDidFinishLoadingStyleListener didFinishLoadingStyleListener;
  private boolean isDidFinishLoadingStyleListenerAdded = false;
  @Nullable
  private MapboxNavigation navigation;
  @Nullable
  private MapRouteLine routeLine;
  private MapRouteArrow routeArrow;
  private boolean vanishRouteLineEnabled;
  @Nullable
  private MapRouteLineInitializedCallback routeLineInitializedCallback;
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
    } else if (CompareUtils.areEqualContentsIgnoreOrder(
            routeLine.retrieveDirectionsRoutes(),
            routeList)
    ) {
      routeLine.updatePrimaryRouteIndex(routeList.get(0));
    } else {
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
   * This method will allow this class to listen to route progress and adapt the route line
   * whenever {@link com.mapbox.navigation.core.trip.session.TripSessionState#STARTED}.
   *
   * In order to use the vanishing route line feature be sure to enable the feature before
   * calling this method.
   *
   * @param navigation to add the progress change listener
   * @see MapboxNavigation#startTripSession()
   */
  public void addProgressChangeListener(@NonNull MapboxNavigation navigation) {
    this.navigation = navigation;
    this.mapRouteProgressChangeListener = buildMapRouteProgressChangeListener();
    navigation.registerRouteProgressObserver(mapRouteProgressChangeListener);
  }

  /**
   * Should be called if {@link #addProgressChangeListener(MapboxNavigation)} was
   * called to prevent leaking.
   *
   * @param navigation to remove the progress change listener
   */
  public void removeProgressChangeListener(@Nullable MapboxNavigation navigation) {
    if (navigation != null) {
      navigation.unregisterRouteProgressObserver(mapRouteProgressChangeListener);
    }
  }

  /**
   * Determines whether or not the vanishing route line feature is enabled. This should be
   * called prior to adding a ProgressChangeListener if this feature wasn't enabled via the builder.
   */
  public void setVanishRouteLineEnabled(boolean enabled) {
    this.vanishRouteLineEnabled = enabled;
    if (vanishRouteLineEnabled) {
      mapboxMap.getLocationComponent().addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
    } else {
      mapboxMap.getLocationComponent().removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
    }
  }

  /**
   * Can be used to manually update the route progress.
   * <p>
   * {@link NavigationMapRoute} automatically listens to
   * {@link RouteProgressObserver#onRouteProgressChanged(RouteProgress)} when a progress observer
   * is subscribed with {@link #addProgressChangeListener(MapboxNavigation)}
   * and invoking this method in that scenario will lead to competing updates.
   * @param routeProgress current progress
   */
  public void onNewRouteProgress(@NonNull RouteProgress routeProgress) {
    if (mapRouteProgressChangeListener != null) {
      mapRouteProgressChangeListener.onRouteProgressChanged(routeProgress);
    }
  }

  private OnIndicatorPositionChangedListener onIndicatorPositionChangedListener = point -> {
    routeLine.updateTraveledRouteLine(point);
  };

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
    removeListeners();
  }

  private void registerLifecycleObserver() {
    lifecycleOwner.getLifecycle().addObserver(this);
  }

  @Nullable
  private MapRouteLine buildMapRouteLine(@NonNull final MapView mapView, @NonNull final MapboxMap mapboxMap,
                                         @StyleRes final int styleRes, @Nullable final String belowLayer,
                                         @Nullable final List<RouteStyleDescriptor> routeStyleDescriptors,
                                         final MapRouteLineInitializedCallback routeLineInitializedCallback) {
    final Context context = mapView.getContext();
    final List<RouteStyleDescriptor> routeStyleDescriptorsToUse = routeStyleDescriptors == null
            ? Collections.emptyList() : routeStyleDescriptors;
    final RouteLayerProvider layerProvider = getLayerProvider(routeStyleDescriptorsToUse, context, styleRes);

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

    if (vanishRouteLineEnabled) {
      mapboxMap.getLocationComponent().addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
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

    mapboxMap.getLocationComponent().removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
  }

  private void redraw(@NonNull Style style) {
    recreateRouteLine(style);
    boolean arrowVisibility = routeArrow.routeArrowIsVisible();
    routeArrow = new MapRouteArrow(mapView, mapboxMap, styleRes, routeLine.getTopLayerId());
    routeArrow.updateVisibilityTo(arrowVisibility);
    updateProgressChangeListener();
  }

  private void recreateRouteLine(@NonNull final Style style) {
    final Context context = mapView.getContext();
    final List<RouteStyleDescriptor> routeStyleDescriptorsToUse = routeStyleDescriptors == null
            ? Collections.emptyList() : routeStyleDescriptors;
    final RouteLayerProvider layerProvider = getLayerProvider(routeStyleDescriptorsToUse, context, styleRes);

    final double vanishingPointOffset = routeLine.getVanishPointOffset();
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
    OnRouteSelectionChangeListener listener = mapRouteClickListener.getOnRouteSelectionChangeListener();
    mapRouteClickListener = new MapRouteClickListener(routeLine);
    mapRouteClickListener.setOnRouteSelectionChangeListener(listener);
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

  @Nullable
  private MapRouteProgressChangeListener buildMapRouteProgressChangeListener() {
    return new MapRouteProgressChangeListener(this.routeLine, routeArrow);
  }

  /**
   * The Builder of {@link NavigationMapRoute}.
   */
  public static class Builder {
    @NonNull private MapView mapView;
    @NonNull private MapboxMap mapboxMap;
    @NonNull private LifecycleOwner lifecycleOwner;
    @Nullable private MapboxNavigation navigation;
    @StyleRes private int styleRes = R.style.MapboxStyleNavigationMapRoute;
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