package com.mapbox.services.android.navigation.ui.v5.route;

import android.content.res.TypedArray;
import android.location.Location;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;

import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.directions.v5.models.RouteLeg;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.functions.Function;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.LineString;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.mapboxsdk.style.functions.stops.Stop.stop;
import static com.mapbox.mapboxsdk.style.functions.stops.Stops.categorical;
import static com.mapbox.mapboxsdk.style.functions.stops.Stops.exponential;

/**
 * Provide a route using {@link NavigationMapRoute#addRoute(DirectionsRoute)} and a route will be drawn using runtime
 * styling. The route will automatically be placed below all labels independent of specific style. If the map styles
 * changed when a routes drawn on the map, the route will automatically be redrawn onto the new map style. If during
 * a navigation session, the user gets re-routed, the route line will be redrawn to reflect the new geometry. To remove
 * the route from the map, use {@link NavigationMapRoute#removeRoute()}.
 * <p>
 * You are given the option when first constructing an instance of this class to pass in a style resource. This allows
 * for custom colorizing and line scaling of the route. Inside your applications {@code style.xml} file, you extend
 * {@code <style name="NavigationMapRoute">} and change some or all the options currently offered. If no style files
 * provided in the constructor, the default style will be used.
 *
 * @since 0.4.0
 */
public class NavigationMapRoute implements ProgressChangeListener, MapView.OnMapChangedListener {

  private static final String CONGESTION_KEY = "congestion";
  @StyleRes
  private int styleRes;
  @ColorInt
  private int routeDefaultColor;
  @ColorInt
  private int routeModerateColor;
  @ColorInt
  private int routeSevereColor;

  private List<String> layerIds;
  private final MapView mapView;
  private final MapboxMap mapboxMap;
  private final MapboxNavigation navigation;
  private DirectionsRoute route;
  private boolean visible;
  private String belowLayer;

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
  public NavigationMapRoute(@NonNull MapView mapView, @NonNull MapboxMap mapboxMap, @Nullable String belowLayer) {
    this(null, mapView, mapboxMap, R.style.NavigationMapRoute, belowLayer);
  }

  /**
   * Construct an instance of {@link NavigationMapRoute}.
   *
   * @param navigation an instance of the {@link MapboxNavigation} object. Passing in null means your route won't
   *                   consider rerouting during a navigation session.
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
   * @param navigation an instance of the {@link MapboxNavigation} object. Passing in null means your route won't
   *                   consider rerouting during a navigation session.
   * @param mapView    the MapView to apply the route to
   * @param mapboxMap  the MapboxMap to apply route with
   * @param belowLayer optionally pass in a layer id to place the route line below
   * @since 0.4.0
   */
  public NavigationMapRoute(@Nullable MapboxNavigation navigation, @NonNull MapView mapView,
                            @NonNull MapboxMap mapboxMap, @Nullable String belowLayer) {
    this(navigation, mapView, mapboxMap, R.style.NavigationMapRoute);
  }

  /**
   * Construct an instance of {@link NavigationMapRoute}.
   *
   * @param navigation an instance of the {@link MapboxNavigation} object. Passing in null means your route won't
   *                   consider rerouting during a navigation session.
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
   * @param navigation an instance of the {@link MapboxNavigation} object. Passing in null means your route won't
   *                   consider rerouting during a navigation session.
   * @param mapView    the MapView to apply the route to
   * @param mapboxMap  the MapboxMap to apply route with
   * @param styleRes   a style resource with custom route colors, scale, etc.
   * @param belowLayer optionally pass in a layer id to place the route line below
   */
  public NavigationMapRoute(@Nullable MapboxNavigation navigation, @NonNull MapView mapView,
                            @NonNull MapboxMap mapboxMap, @StyleRes int styleRes, @Nullable String belowLayer) {
    this.styleRes = styleRes;
    this.mapView = mapView;
    this.mapboxMap = mapboxMap;
    this.navigation = navigation;
    this.belowLayer = belowLayer;
    addListeners();
    initialize();
  }

  /**
   * Adds source and layers to the map.
   */
  private void initialize() {
    layerIds = new ArrayList<>();

    addSource(route);

    TypedArray typedArray = mapView.getContext().obtainStyledAttributes(styleRes, R.styleable.NavigationMapRoute);

    routeDefaultColor = typedArray.getColor(R.styleable.NavigationMapRoute_routeColor,
      ContextCompat.getColor(mapView.getContext(), R.color.mapbox_navigation_route_layer_blue));
    routeModerateColor = typedArray.getColor(R.styleable.NavigationMapRoute_routeModerateCongestionColor,
      ContextCompat.getColor(mapView.getContext(), R.color.mapbox_navigation_route_layer_congestion_yellow));
    routeSevereColor = typedArray.getColor(R.styleable.NavigationMapRoute_routeSevereCongestionColor,
      ContextCompat.getColor(mapView.getContext(), R.color.mapbox_navigation_route_layer_congestion_red));
    @ColorInt int routeShieldColor = typedArray.getColor(R.styleable.NavigationMapRoute_routeShieldColor,
      ContextCompat.getColor(mapView.getContext(), R.color.mapbox_navigation_route_shield_layer_color));
    float routeScale = typedArray.getFloat(R.styleable.NavigationMapRoute_routeScale, 1.0f);

    addNavigationRouteLayer(routeScale);
    addNavigationRouteShieldLayer(routeShieldColor, routeScale);
    typedArray.recycle();
  }

  /**
   * Adds the necessary listeners
   */
  private void addListeners() {
    if (navigation != null) {
      navigation.addProgressChangeListener(this);
    }
    mapView.addOnMapChangedListener(this);
  }

  /**
   * Pass in a {@link DirectionsRoute} and display the route geometry on your map.
   *
   * @param route a {@link DirectionsRoute} used to draw the route line
   * @since 0.4.0
   */
  public void addRoute(@NonNull DirectionsRoute route) {
    this.route = route;
    addSource(route);
    setLayerVisibility(true);
  }

  /**
   * Remove the route line from the map style, note that this only changes the visibility and does not remove any layers
   * or sources.
   *
   * @since 0.4.0
   */
  public void removeRoute() {
    setLayerVisibility(false);
  }

  /**
   * Get the current route being used to draw the route, if one hasn't been added to the map yet, this will return
   * {@code null}
   *
   * @return the {@link DirectionsRoute} used to draw the route line
   * @since 0.4.0
   */
  public DirectionsRoute getRoute() {
    return route;
  }

  /**
   * Called when a map change events occurs. Used specifically to detect loading of a new style, if applicable reapply
   * the route line source and layers.
   *
   * @param change the map change event that occurred
   * @since 0.4.0
   */
  @Override
  public void onMapChanged(int change) {
    if (change == MapView.DID_FINISH_LOADING_STYLE) {
      initialize();
      setLayerVisibility(visible);
    }
  }

  /**
   * Called when the user makes new progress during a navigation session. Used to determine whether or not a re-route
   * has occurred and if so the route is redrawn to reflect the change.
   *
   * @param location      the users current location
   * @param routeProgress a {@link RouteProgress} reflecting the users latest progress along the route
   * @since 0.4.0
   */
  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    // TODO they'll probably never be equal till https://github.com/mapbox/mapbox-java/issues/440 gets resolved
    // Check if the route's the same as the route currently drawn
    if (!routeProgress.directionsRoute().equals(route)) {
      route = routeProgress.directionsRoute();
      addSource(route);
    }
  }

  /**
   * Toggle whether or not the route lines visible or not, used in {@link NavigationMapRoute#addRoute(DirectionsRoute)}
   * and {@link NavigationMapRoute#removeRoute()}.
   *
   * @param visible true if you want the route to be visible, else false
   */
  private void setLayerVisibility(boolean visible) {
    this.visible = visible;
    List<Layer> layers = mapboxMap.getLayers();
    String id;

    for (Layer layer : layers) {
      id = layer.getId();
      if (layerIds.contains(layer.getId())) {
        if (id.equals(NavigationMapLayers.NAVIGATION_ROUTE_LAYER)
          || id.equals(NavigationMapLayers.NAVIGATION_ROUTE_SHIELD_LAYER)) {
          layer.setProperties(PropertyFactory.visibility(visible ? Property.VISIBLE : Property.NONE));
        }
      }
    }
  }

  /**
   * Adds the route source to the map.
   */
  private void addSource(@Nullable DirectionsRoute route) {
    FeatureCollection routeLineFeature;
    // Either add an empty GeoJson featureCollection or the route's Geometry
    if (route == null) {
      routeLineFeature = FeatureCollection.fromFeatures(new Feature[] {});
    } else {
      routeLineFeature = addTrafficToSource(route);
    }

    // Determine whether the source needs to be added or updated
    GeoJsonSource source = mapboxMap.getSourceAs(NavigationMapSources.NAVIGATION_ROUTE_SOURCE);
    if (source == null) {
      GeoJsonSource routeSource = new GeoJsonSource(NavigationMapSources.NAVIGATION_ROUTE_SOURCE, routeLineFeature);
      mapboxMap.addSource(routeSource);
    } else {
      source.setGeoJson(routeLineFeature);
    }
  }

  /**
   * Generic method for adding layers to the map.
   */
  private void addLayerToMap(@NonNull Layer layer, @Nullable String idBelowLayer) {
    if (idBelowLayer == null) {
      mapboxMap.addLayer(layer);
    } else {
      mapboxMap.addLayerBelow(layer, idBelowLayer);
    }
    layerIds.add(layer.getId());
  }

  /**
   * If the {@link DirectionsRoute} request contains congestion information via annotations, breakup the source into
   * pieces so data-driven styling can be used to change the route colors accordingly.
   */
  private FeatureCollection addTrafficToSource(DirectionsRoute route) {
    List<Feature> features = new ArrayList<>();
    LineString originalGeometry = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);
    features.add(Feature.fromGeometry(originalGeometry));

    LineString lineString = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);
    for (RouteLeg leg : route.legs()) {
      if (leg.annotation() != null) {
        if (leg.annotation().congestion() != null) {
          for (int i = 0; i < leg.annotation().congestion().size(); i++) {
            double[] startCoord = lineString.getCoordinates().get(i).getCoordinates();
            double[] endCoord = lineString.getCoordinates().get(i + 1).getCoordinates();

            LineString congestionLineString = LineString.fromCoordinates(new double[][] {startCoord, endCoord});
            Feature feature = Feature.fromGeometry(congestionLineString);
            feature.addStringProperty(CONGESTION_KEY, leg.annotation().congestion().get(i));
            features.add(feature);
          }
        }
      } else {
        Feature feature = Feature.fromGeometry(lineString);
        features.add(feature);
      }
    }
    return FeatureCollection.fromFeatures(features);
  }

  /**
   * Iterate through map style layers backwards till the first not-symbol layer is found.
   */
  private String placeRouteBelow() {
    if (belowLayer == null || belowLayer.isEmpty()) {
      List<Layer> styleLayers = mapboxMap.getLayers();
      for (int i = styleLayers.size() - 1; i >= 0; i--) {
        if (!(styleLayers.get(i) instanceof SymbolLayer)) {
          return styleLayers.get(i).getId();
        }
      }
    }
    return belowLayer;
  }

  /**
   * Add the route layer to the map either using the custom style values or the default.
   */
  private void addNavigationRouteLayer(float scale) {
    Layer routeLayer = new LineLayer(NavigationMapLayers.NAVIGATION_ROUTE_LAYER,
      NavigationMapSources.NAVIGATION_ROUTE_SOURCE).withProperties(
      PropertyFactory.lineCap(Property.LINE_CAP_SQUARE),
      PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
      PropertyFactory.visibility(Property.NONE),
      PropertyFactory.lineWidth(Function.zoom(
        exponential(
          stop(4f, PropertyFactory.lineWidth(2f * scale)),
          stop(10f, PropertyFactory.lineWidth(3f * scale)),
          stop(13f, PropertyFactory.lineWidth(4f * scale)),
          stop(16f, PropertyFactory.lineWidth(7f * scale)),
          stop(19f, PropertyFactory.lineWidth(14f * scale)),
          stop(22f, PropertyFactory.lineWidth(18f * scale))
        ).withBase(1.5f))
      ),
      PropertyFactory.lineColor(
        Function.property(CONGESTION_KEY, categorical(
          stop("moderate", PropertyFactory.lineColor(routeModerateColor)),
          stop("heavy", PropertyFactory.lineColor(routeSevereColor)),
          stop("severe", PropertyFactory.lineColor(routeSevereColor))
        )).withDefaultValue(PropertyFactory.lineColor(routeDefaultColor)))
    );
    addLayerToMap(routeLayer, placeRouteBelow());
  }

  /**
   * Add the route shield layer to the map either using the custom style values or the default.
   */
  private void addNavigationRouteShieldLayer(@ColorInt int routeShieldColor, float scale) {
    Layer routeLayer = new LineLayer(NavigationMapLayers.NAVIGATION_ROUTE_SHIELD_LAYER,
      NavigationMapSources.NAVIGATION_ROUTE_SOURCE).withProperties(
      PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
      PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
      PropertyFactory.visibility(Property.NONE),
      PropertyFactory.lineWidth(Function.zoom(
        exponential(
          stop(16f, PropertyFactory.lineWidth(0f)),
          stop(16.5f, PropertyFactory.lineWidth(10.5f * scale)),
          stop(19f, PropertyFactory.lineWidth(21f * scale)),
          stop(22f, PropertyFactory.lineWidth(27f * scale))
        ).withBase(1.5f))
      ),
      PropertyFactory.lineColor(routeShieldColor)
    );
    addLayerToMap(routeLayer, NavigationMapLayers.NAVIGATION_ROUTE_LAYER);
  }

  /**
   * Layer id constants.
   */
  public static class NavigationMapLayers {
    public static final String NAVIGATION_ROUTE_SHIELD_LAYER = "mapbox-plugin-navigation-route-shield-layer";
    public static final String NAVIGATION_ROUTE_LAYER = "mapbox-plugin-navigation-route-layer";
  }

  /**
   * Source id constants.
   */
  public static class NavigationMapSources {
    public static final String NAVIGATION_ROUTE_SOURCE = "mapbox-plugin-navigation-route-source";
  }
}
