package com.mapbox.services.android.navigation.ui.v5.route;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.content.res.AppCompatResources;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.MathUtils;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.utils.MapImageUtils;
import com.mapbox.services.android.navigation.ui.v5.utils.MapUtils;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.mapbox.mapboxsdk.style.expressions.Expression.color;
import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.linear;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.match;
import static com.mapbox.mapboxsdk.style.expressions.Expression.step;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.Property.ICON_ROTATION_ALIGNMENT_MAP;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

/**
 * Provide a route using {@link NavigationMapRoute#addRoutes(List)} and a route will be drawn using
 * runtime styling. The route will automatically be placed below all labels independent of specific
 * style. If the map styles changed when a routes drawn on the map, the route will automatically be
 * redrawn onto the new map style. If during a navigation session, the user gets re-routed, the
 * route line will be redrawn to reflect the new geometry. To remove the route from the map, use
 * {@link NavigationMapRoute#removeRoute()}.
 * <p>
 * You are given the option when first constructing an instance of this class to pass in a style
 * resource. This allows for custom colorizing and line scaling of the route. Inside your
 * applications {@code style.xml} file, you extend {@code <style name="NavigationMapRoute">} and
 * change some or all the options currently offered. If no style files provided in the constructor,
 * the default style will be used.
 *
 * @since 0.4.0
 */
public class NavigationMapRoute implements MapView.OnMapChangedListener,
  MapboxMap.OnMapClickListener, LifecycleObserver {

  private static final String CONGESTION_KEY = "congestion";
  private static final String SOURCE_KEY = "source";
  private static final String INDEX_KEY = "index";

  private static final String GENERIC_ROUTE_SOURCE_ID = "mapbox-navigation-route-source";
  private static final String GENERIC_ROUTE_LAYER_ID = "mapbox-navigation-route-layer";
  private static final String WAYPOINT_SOURCE_ID = "mapbox-navigation-waypoint-source";
  private static final String WAYPOINT_LAYER_ID = "mapbox-navigation-waypoint-layer";
  private static final String ID_FORMAT = "%s-%d";
  private static final String GENERIC_ROUTE_SHIELD_LAYER_ID = "mapbox-navigation-route-shield-layer";
  private static final int TWO_POINTS = 2;
  private static final int THIRTY = 30;
  private static final String ARROW_BEARING = "mapbox-navigation-arrow-bearing";
  private static final String ARROW_SHAFT_SOURCE_ID = "mapbox-navigation-arrow-shaft-source";
  private static final String ARROW_HEAD_SOURCE_ID = "mapbox-navigation-arrow-head-source";
  private static final String ARROW_SHAFT_CASING_LINE_LAYER_ID = "mapbox-navigation-arrow-shaft-casing-layer";
  private static final String ARROW_SHAFT_LINE_LAYER_ID = "mapbox-navigation-arrow-shaft-layer";
  private static final String ARROW_HEAD_ICON = "mapbox-navigation-arrow-head-icon";
  private static final String ARROW_HEAD_ICON_CASING = "mapbox-navigation-arrow-head-icon-casing";
  private static final int MAX_DEGREES = 360;
  private static final String ARROW_HEAD_CASING_LAYER_ID = "mapbox-navigation-arrow-head-casing-layer";
  private static final Float[] ARROW_HEAD_CASING_OFFSET = {0f, -7f};
  private static final String ARROW_HEAD_LAYER_ID = "mapbox-navigation-arrow-head-layer";
  private static final Float[] ARROW_HEAD_OFFSET = {0f, -7f};
  private static final int MIN_ARROW_ZOOM = 10;
  private static final int MAX_ARROW_ZOOM = 22;
  private static final float MIN_ZOOM_ARROW_SHAFT_SCALE = 2.6f;
  private static final float MAX_ZOOM_ARROW_SHAFT_SCALE = 13.0f;
  private static final float MIN_ZOOM_ARROW_SHAFT_CASING_SCALE = 3.4f;
  private static final float MAX_ZOOM_ARROW_SHAFT_CASING_SCALE = 17.0f;
  private static final float MIN_ZOOM_ARROW_HEAD_SCALE = 0.2f;
  private static final float MAX_ZOOM_ARROW_HEAD_SCALE = 0.8f;
  private static final float MIN_ZOOM_ARROW_HEAD_CASING_SCALE = 0.2f;
  private static final float MAX_ZOOM_ARROW_HEAD_CASING_SCALE = 0.8f;
  private static final float OPAQUE = 0.0f;
  private static final int ARROW_HIDDEN_ZOOM_LEVEL = 14;
  private static final float TRANSPARENT = 1.0f;
  private static final String LAYER_ABOVE_UPCOMING_MANEUVER_ARROW = "com.mapbox.annotations.points";

  @StyleRes
  private int styleRes;
  @ColorInt
  private int routeDefaultColor;
  @ColorInt
  private int routeModerateColor;
  @ColorInt
  private int routeSevereColor;
  @ColorInt
  private int alternativeRouteDefaultColor;
  @ColorInt
  private int alternativeRouteModerateColor;
  @ColorInt
  private int alternativeRouteSevereColor;
  @ColorInt
  private int alternativeRouteShieldColor;
  @ColorInt
  private int routeShieldColor;
  @ColorInt
  private int arrowColor;
  @ColorInt
  private int arrowBorderColor;
  @DrawableRes
  private int originWaypointIcon;
  @DrawableRes
  private int destinationWaypointIcon;

  private MapboxNavigation navigation;
  private final MapboxMap mapboxMap;
  private final HashMap<LineString, DirectionsRoute> routeLineStrings;
  private final List<FeatureCollection> featureCollections;
  private final List<DirectionsRoute> directionsRoutes;
  private final List<String> layerIds;
  private final MapView mapView;
  private int primaryRouteIndex;
  private float routeScale;
  private float alternativeRouteScale;
  private String belowLayer;
  private boolean alternativesVisible;
  private OnRouteSelectionChangeListener onRouteSelectionChangeListener;
  private List<Layer> arrowLayers;
  private GeoJsonSource arrowShaftGeoJsonSource;
  private GeoJsonSource arrowHeadGeoJsonSource;
  private Feature arrowShaftGeoJsonFeature = Feature.fromGeometry(Point.fromLngLat(0, 0));
  private Feature arrowHeadGeoJsonFeature = Feature.fromGeometry(Point.fromLngLat(0, 0));
  private ProgressChangeListener progressChangeListener = new ProgressChangeListener() {
    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
      boolean noRoutes = directionsRoutes.isEmpty();
      boolean newCurrentRoute = !routeProgress.directionsRoute().equals(directionsRoutes.get(primaryRouteIndex));
      boolean isANewRoute = noRoutes || newCurrentRoute;
      if (isANewRoute) {
        addRoute(routeProgress.directionsRoute());
      }
      addUpcomingManeuverArrow(routeProgress);
    }
  };


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
    this.mapView = mapView;
    this.mapboxMap = mapboxMap;
    this.navigation = navigation;
    this.belowLayer = belowLayer;
    featureCollections = new ArrayList<>();
    directionsRoutes = new ArrayList<>();
    routeLineStrings = new HashMap<>();
    layerIds = new ArrayList<>();
    initialize();
    addListeners();
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
    clearRoutes();
    this.directionsRoutes.addAll(directionsRoutes);
    primaryRouteIndex = 0;
    alternativesVisible = directionsRoutes.size() > 1;
    generateFeatureCollectionList(directionsRoutes);
    drawRoutes();
    addDirectionWaypoints();
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
    this.onRouteSelectionChangeListener = onRouteSelectionChangeListener;
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
    this.alternativesVisible = alternativesVisible;
    toggleAlternativeVisibility(alternativesVisible);
  }

  public void addProgressChangeListener(MapboxNavigation navigation) {
    this.navigation = navigation;
    navigation.addProgressChangeListener(progressChangeListener);
  }

  public void removeProgressChangeListener(MapboxNavigation navigation) {
    if (navigation != null) {
      navigation.removeProgressChangeListener(progressChangeListener);
    }
  }

  //
  // Private methods
  //

  /**
   * Loops through all the route layers stored inside the layerId list and toggles the visibility.
   * if the layerId matches the primary route index, we skip since we still want that route to be
   * displayed.
   */
  private void toggleAlternativeVisibility(boolean visible) {
    for (String layerId : layerIds) {
      if (layerId.contains(String.valueOf(primaryRouteIndex))
        || layerId.contains(WAYPOINT_LAYER_ID)) {
        continue;
      }
      Layer layer = mapboxMap.getLayer(layerId);
      if (layer != null) {
        layer.setProperties(
          visibility(visible ? VISIBLE : NONE)
        );
      }
    }
  }

  /**
   * Takes the directions route list and draws each line on the map.
   */
  private void drawRoutes() {
    // Add all the sources, the list is traversed backwards to ensure the primary route always gets
    // drawn on top of the others since it initially has a index of zero.
    for (int i = featureCollections.size() - 1; i >= 0; i--) {
      MapUtils.updateMapSourceFromFeatureCollection(
        mapboxMap, featureCollections.get(i),
        featureCollections.get(i).features().get(0).getStringProperty(SOURCE_KEY)
      );

      // Get some required information for the next step
      String sourceId = featureCollections.get(i).features()
        .get(0).getStringProperty(SOURCE_KEY);
      int index = featureCollections.indexOf(featureCollections.get(i));

      // Add the layer IDs to a list so we can quickly remove them when needed without traversing
      // through all the map layers.
      layerIds.add(String.format(Locale.US, ID_FORMAT, GENERIC_ROUTE_SHIELD_LAYER_ID, index));
      layerIds.add(String.format(Locale.US, ID_FORMAT, GENERIC_ROUTE_LAYER_ID, index));

      // Add the route shield first followed by the route to ensure the shield is always on the
      // bottom.
      addRouteShieldLayer(layerIds.get(layerIds.size() - 2), sourceId, index);
      addRouteLayer(layerIds.get(layerIds.size() - 1), sourceId, index);
    }
  }

  private void clearRoutes() {
    removeLayerIds();
    updateArrowLayersVisibilityTo(false);
    clearRouteListData();
  }

  private void generateFeatureCollectionList(List<DirectionsRoute> directionsRoutes) {
    // Each route contains traffic information and should be recreated considering this traffic
    // information.
    for (int i = 0; i < directionsRoutes.size(); i++) {
      featureCollections.add(addTrafficToSource(directionsRoutes.get(i), i));
    }

    // Add the waypoint geometries to represent them as an icon
    featureCollections.add(
      waypointFeatureCollection(directionsRoutes.get(primaryRouteIndex))
    );
  }

  /**
   * The routes also display an icon for each waypoint in the route, we use symbol layers for this.
   */
  private FeatureCollection waypointFeatureCollection(DirectionsRoute route) {
    final List<Feature> waypointFeatures = new ArrayList<>();
    for (RouteLeg leg : route.legs()) {
      waypointFeatures.add(getPointFromLineString(leg, 0));
      waypointFeatures.add(getPointFromLineString(leg, leg.steps().size() - 1));
    }
    return FeatureCollection.fromFeatures(waypointFeatures);
  }

  private void addDirectionWaypoints() {
    MapUtils.updateMapSourceFromFeatureCollection(
      mapboxMap, featureCollections.get(featureCollections.size() - 1), WAYPOINT_SOURCE_ID);
    drawWaypointMarkers(mapboxMap,
      AppCompatResources.getDrawable(mapView.getContext(), originWaypointIcon),
      AppCompatResources.getDrawable(mapView.getContext(), destinationWaypointIcon)
    );
  }

  private void addUpcomingManeuverArrow(RouteProgress routeProgress) {
    boolean invalidUpcomingStepPoints = routeProgress.upcomingStepPoints() == null
            || routeProgress.upcomingStepPoints().size() < TWO_POINTS;
    boolean invalidCurrentStepPoints = routeProgress.currentStepPoints().size() < TWO_POINTS;
    if (invalidUpcomingStepPoints || invalidCurrentStepPoints) {
      updateArrowLayersVisibilityTo(false);
      return;
    }
    updateArrowLayersVisibilityTo(true);

    List<Point> maneuverPoints = obtainArrowPointsFrom(routeProgress);

    updateArrowShaftWith(maneuverPoints);
    updateArrowHeadWith(maneuverPoints);
  }

  private void updateArrowLayersVisibilityTo(boolean visible) {
    for (Layer layer : arrowLayers) {
      String targetVisibility = visible ? VISIBLE : NONE;
      if (!targetVisibility.equals(layer.getVisibility().getValue())) {
        layer.setProperties(visibility(targetVisibility));
      }
    }
  }

  private List<Point> obtainArrowPointsFrom(RouteProgress routeProgress) {
    List<Point> reversedCurrent = new ArrayList<>(routeProgress.currentStepPoints());
    Collections.reverse(reversedCurrent);

    LineString arrowLineCurrent = LineString.fromLngLats(reversedCurrent);
    LineString arrowLineUpcoming = LineString.fromLngLats(routeProgress.upcomingStepPoints());

    LineString arrowCurrentSliced = TurfMisc.lineSliceAlong(arrowLineCurrent, 0, THIRTY, TurfConstants.UNIT_METERS);
    LineString arrowUpcomingSliced = TurfMisc.lineSliceAlong(arrowLineUpcoming, 0, THIRTY, TurfConstants.UNIT_METERS);

    Collections.reverse(arrowCurrentSliced.coordinates());

    List<Point> combined = new ArrayList<>();
    combined.addAll(arrowCurrentSliced.coordinates());
    combined.addAll(arrowUpcomingSliced.coordinates());
    return combined;
  }

  private void updateArrowShaftWith(List<Point> points) {
    LineString shaft = LineString.fromLngLats(points);
    arrowShaftGeoJsonFeature = Feature.fromGeometry(shaft);
    arrowShaftGeoJsonSource.setGeoJson(arrowShaftGeoJsonFeature);
  }

  private void updateArrowHeadWith(List<Point> points) {
    double azimuth = TurfMeasurement.bearing(points.get(points.size() - 2), points.get(points.size() - 1));
    arrowHeadGeoJsonFeature = Feature.fromGeometry(points.get(points.size() - 1));
    arrowHeadGeoJsonFeature.addNumberProperty(ARROW_BEARING, (float) MathUtils.wrap(azimuth, 0, MAX_DEGREES));
    arrowHeadGeoJsonSource.setGeoJson(arrowHeadGeoJsonFeature);
  }

  private void initializeUpcomingManeuverArrow() {
    arrowShaftGeoJsonSource = (GeoJsonSource) mapboxMap.getSource(ARROW_SHAFT_SOURCE_ID);
    arrowHeadGeoJsonSource = (GeoJsonSource) mapboxMap.getSource(ARROW_HEAD_SOURCE_ID);

    LineLayer shaftLayer = createArrowShaftLayer();
    LineLayer shaftCasingLayer = createArrowShaftCasingLayer();
    SymbolLayer headLayer = createArrowHeadLayer();
    SymbolLayer headCasingLayer = createArrowHeadCasingLayer();

    if (arrowShaftGeoJsonSource == null && arrowHeadGeoJsonSource == null) {
      initializeArrowShaft();
      initializeArrowHead();

      addArrowHeadIcon();
      addArrowHeadIconCasing();

      mapboxMap.addLayerBelow(shaftCasingLayer, LAYER_ABOVE_UPCOMING_MANEUVER_ARROW);
      mapboxMap.addLayerAbove(headCasingLayer, shaftCasingLayer.getId());

      mapboxMap.addLayerAbove(shaftLayer, headCasingLayer.getId());
      mapboxMap.addLayerAbove(headLayer, shaftLayer.getId());
    }
    initializeArrowLayers(shaftLayer, shaftCasingLayer, headLayer, headCasingLayer);
  }

  private void initializeArrowShaft() {
    arrowShaftGeoJsonSource = new GeoJsonSource(
      ARROW_SHAFT_SOURCE_ID,
      arrowShaftGeoJsonFeature,
      new GeoJsonOptions().withMaxZoom(16)
    );
    mapboxMap.addSource(arrowShaftGeoJsonSource);
  }

  private void initializeArrowHead() {
    arrowHeadGeoJsonSource = new GeoJsonSource(
      ARROW_HEAD_SOURCE_ID,
      arrowShaftGeoJsonFeature,
      new GeoJsonOptions().withMaxZoom(16)
    );
    mapboxMap.addSource(arrowHeadGeoJsonSource);
  }

  private void addArrowHeadIcon() {
    Drawable head = DrawableCompat.wrap(AppCompatResources.getDrawable(mapView.getContext(), R.drawable.ic_arrow_head));
    DrawableCompat.setTint(head.mutate(), arrowColor);
    Bitmap icon = MapImageUtils.getBitmapFromDrawable(head);
    mapboxMap.addImage(ARROW_HEAD_ICON, icon);
  }

  private void addArrowHeadIconCasing() {
    Drawable headCasing = DrawableCompat.wrap(AppCompatResources.getDrawable(mapView.getContext(),
      R.drawable.ic_arrow_head_casing));
    DrawableCompat.setTint(headCasing.mutate(), arrowBorderColor);
    Bitmap icon = MapImageUtils.getBitmapFromDrawable(headCasing);
    mapboxMap.addImage(ARROW_HEAD_ICON_CASING, icon);
  }

  private LineLayer createArrowShaftLayer() {
    LineLayer shaftLayer = (LineLayer) mapboxMap.getLayer(ARROW_SHAFT_LINE_LAYER_ID);
    if (shaftLayer != null) {
      return shaftLayer;
    }
    return new LineLayer(ARROW_SHAFT_LINE_LAYER_ID, ARROW_SHAFT_SOURCE_ID).withProperties(
      PropertyFactory.lineColor(color(arrowColor)),
      PropertyFactory.lineWidth(
        interpolate(linear(), zoom(),
          stop(MIN_ARROW_ZOOM, MIN_ZOOM_ARROW_SHAFT_SCALE),
          stop(MAX_ARROW_ZOOM, MAX_ZOOM_ARROW_SHAFT_SCALE)
        )
      ),
      PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
      PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
      PropertyFactory.visibility(NONE),
      PropertyFactory.lineOpacity(
        step(zoom(), OPAQUE,
          stop(
            ARROW_HIDDEN_ZOOM_LEVEL, TRANSPARENT
          )
        )
      )
    );
  }

  private LineLayer createArrowShaftCasingLayer() {
    LineLayer shaftCasingLayer = (LineLayer) mapboxMap.getLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID);
    if (shaftCasingLayer != null) {
      return shaftCasingLayer;
    }
    return new LineLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID, ARROW_SHAFT_SOURCE_ID).withProperties(
      PropertyFactory.lineColor(color(arrowBorderColor)),
      PropertyFactory.lineWidth(
        interpolate(linear(), zoom(),
          stop(MIN_ARROW_ZOOM, MIN_ZOOM_ARROW_SHAFT_CASING_SCALE),
          stop(MAX_ARROW_ZOOM, MAX_ZOOM_ARROW_SHAFT_CASING_SCALE)
        )
      ),
      PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
      PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
      PropertyFactory.visibility(NONE),
      PropertyFactory.lineOpacity(
        step(zoom(), OPAQUE,
          stop(
            ARROW_HIDDEN_ZOOM_LEVEL, TRANSPARENT
          )
        )
      )
    );
  }

  private SymbolLayer createArrowHeadLayer() {
    SymbolLayer headLayer = (SymbolLayer) mapboxMap.getLayer(ARROW_HEAD_LAYER_ID);
    if (headLayer != null) {
      return headLayer;
    }
    return new SymbolLayer(ARROW_HEAD_LAYER_ID, ARROW_HEAD_SOURCE_ID)
      .withProperties(
        PropertyFactory.iconImage(ARROW_HEAD_ICON),
        iconAllowOverlap(true),
        iconIgnorePlacement(true),
        PropertyFactory.iconSize(interpolate(linear(), zoom(),
          stop(MIN_ARROW_ZOOM, MIN_ZOOM_ARROW_HEAD_SCALE),
          stop(MAX_ARROW_ZOOM, MAX_ZOOM_ARROW_HEAD_SCALE)
          )
        ),
        PropertyFactory.iconOffset(ARROW_HEAD_OFFSET),
        PropertyFactory.iconRotationAlignment(ICON_ROTATION_ALIGNMENT_MAP),
        PropertyFactory.iconRotate(get(ARROW_BEARING)),
        PropertyFactory.visibility(NONE),
        PropertyFactory.iconOpacity(
          step(zoom(), OPAQUE,
            stop(
              ARROW_HIDDEN_ZOOM_LEVEL, TRANSPARENT
            )
          )
        )
      );
  }

  private SymbolLayer createArrowHeadCasingLayer() {
    SymbolLayer headCasingLayer = (SymbolLayer) mapboxMap.getLayer(ARROW_HEAD_CASING_LAYER_ID);
    if (headCasingLayer != null) {
      return headCasingLayer;
    }
    return new SymbolLayer(ARROW_HEAD_CASING_LAYER_ID, ARROW_HEAD_SOURCE_ID).withProperties(
      PropertyFactory.iconImage(ARROW_HEAD_ICON_CASING),
      iconAllowOverlap(true),
      iconIgnorePlacement(true),
      PropertyFactory.iconSize(interpolate(
        linear(), zoom(),
        stop(MIN_ARROW_ZOOM, MIN_ZOOM_ARROW_HEAD_CASING_SCALE),
        stop(MAX_ARROW_ZOOM, MAX_ZOOM_ARROW_HEAD_CASING_SCALE)
      )),
      PropertyFactory.iconOffset(ARROW_HEAD_CASING_OFFSET),
      PropertyFactory.iconRotationAlignment(ICON_ROTATION_ALIGNMENT_MAP),
      PropertyFactory.iconRotate(get(ARROW_BEARING)),
      PropertyFactory.visibility(NONE),
      PropertyFactory.iconOpacity(
        step(zoom(), OPAQUE,
          stop(
            ARROW_HIDDEN_ZOOM_LEVEL, TRANSPARENT
          )
        )
      )
    );
  }

  private void initializeArrowLayers(LineLayer shaftLayer, LineLayer shaftCasingLayer, SymbolLayer headLayer,
                                     SymbolLayer headCasingLayer) {
    arrowLayers = new ArrayList<>();
    arrowLayers.add(shaftCasingLayer);
    arrowLayers.add(shaftLayer);
    arrowLayers.add(headCasingLayer);
    arrowLayers.add(headLayer);
  }

  /**
   * When the user switches an alternative route to a primary route, this method alters the
   * appearance.
   */
  private void updatePrimaryRoute(String layerId, int index) {
    Layer layer = mapboxMap.getLayer(layerId);
    if (layer != null) {
      layer.setProperties(
        PropertyFactory.lineColor(match(
          Expression.toString(get(CONGESTION_KEY)),
          color(index == primaryRouteIndex ? routeDefaultColor : alternativeRouteDefaultColor),
          stop("moderate", color(index == primaryRouteIndex ? routeModerateColor : alternativeRouteModerateColor)),
          stop("heavy", color(index == primaryRouteIndex ? routeSevereColor : alternativeRouteSevereColor)),
          stop("severe", color(index == primaryRouteIndex ? routeSevereColor : alternativeRouteSevereColor))
          )
        )
      );
      if (index == primaryRouteIndex) {
        mapboxMap.removeLayer(layer);
        mapboxMap.addLayerBelow(layer, WAYPOINT_LAYER_ID);
      }
    }
  }

  private void updatePrimaryShieldRoute(String layerId, int index) {
    Layer layer = mapboxMap.getLayer(layerId);
    if (layer != null) {
      layer.setProperties(
        PropertyFactory.lineColor(index == primaryRouteIndex ? routeShieldColor : alternativeRouteShieldColor)
      );
      if (index == primaryRouteIndex) {
        mapboxMap.removeLayer(layer);
        mapboxMap.addLayerBelow(layer, WAYPOINT_LAYER_ID);
      }
    }
  }

  /**
   * Add the route layer to the map either using the custom style values or the default.
   */
  private void addRouteLayer(String layerId, String sourceId, int index) {
    float scale = index == primaryRouteIndex ? routeScale : alternativeRouteScale;
    Layer routeLayer = new LineLayer(layerId, sourceId).withProperties(
      PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
      PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
      PropertyFactory.lineWidth(interpolate(
        exponential(1.5f), zoom(),
        stop(4f, 3f * scale),
        stop(10f, 4f * scale),
        stop(13f, 6f * scale),
        stop(16f, 10f * scale),
        stop(19f, 14f * scale),
        stop(22f, 18f * scale)
        )
      ),
      PropertyFactory.lineColor(match(
        Expression.toString(get(CONGESTION_KEY)),
        color(index == primaryRouteIndex ? routeDefaultColor : alternativeRouteDefaultColor),
        stop("moderate", color(index == primaryRouteIndex ? routeModerateColor : alternativeRouteModerateColor)),
        stop("heavy", color(index == primaryRouteIndex ? routeSevereColor : alternativeRouteSevereColor)),
        stop("severe", color(index == primaryRouteIndex ? routeSevereColor : alternativeRouteSevereColor))
        )
      )
    );
    MapUtils.addLayerToMap(mapboxMap, routeLayer, belowLayer);
  }

  private void removeLayerIds() {
    if (!layerIds.isEmpty()) {
      for (String id : layerIds) {
        mapboxMap.removeLayer(id);
      }
    }
  }

  private void clearRouteListData() {
    if (!directionsRoutes.isEmpty()) {
      directionsRoutes.clear();
    }
    if (!routeLineStrings.isEmpty()) {
      routeLineStrings.clear();
    }
    if (!featureCollections.isEmpty()) {
      featureCollections.clear();
    }
  }

  /**
   * Add the route shield layer to the map either using the custom style values or the default.
   */
  private void addRouteShieldLayer(String layerId, String sourceId, int index) {
    float scale = index == primaryRouteIndex ? routeScale : alternativeRouteScale;
    Layer routeLayer = new LineLayer(layerId, sourceId).withProperties(
      PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
      PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
      PropertyFactory.lineWidth(interpolate(
        exponential(1.5f), zoom(),
        stop(10f, 7f),
        stop(14f, 10.5f * scale),
        stop(16.5f, 15.5f * scale),
        stop(19f, 24f * scale),
        stop(22f, 29f * scale)
        )
      ),
      PropertyFactory.lineColor(
        index == primaryRouteIndex ? routeShieldColor : alternativeRouteShieldColor)
    );
    MapUtils.addLayerToMap(mapboxMap, routeLayer, belowLayer);
  }

  /**
   * Loads in all the custom values the user might have set such as colors and line width scalars.
   * Anything they didn't set, results in using the default values.
   */
  private void getAttributes() {
    Context context = mapView.getContext();
    TypedArray typedArray = context.obtainStyledAttributes(styleRes, R.styleable.NavigationMapRoute);

    // Primary Route attributes
    routeDefaultColor = typedArray.getColor(R.styleable.NavigationMapRoute_routeColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_layer_blue));
    routeModerateColor = typedArray.getColor(
      R.styleable.NavigationMapRoute_routeModerateCongestionColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_layer_congestion_yellow));
    routeSevereColor = typedArray.getColor(
      R.styleable.NavigationMapRoute_routeSevereCongestionColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_layer_congestion_red));
    routeShieldColor = typedArray.getColor(R.styleable.NavigationMapRoute_routeShieldColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_shield_layer_color));
    routeScale = typedArray.getFloat(R.styleable.NavigationMapRoute_routeScale, 1.0f);

    // Secondary Routes attributes
    alternativeRouteDefaultColor = typedArray.getColor(
      R.styleable.NavigationMapRoute_alternativeRouteColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_alternative_color));
    alternativeRouteModerateColor = typedArray.getColor(
      R.styleable.NavigationMapRoute_alternativeRouteModerateCongestionColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_alternative_congestion_yellow));
    alternativeRouteSevereColor = typedArray.getColor(
      R.styleable.NavigationMapRoute_alternativeRouteSevereCongestionColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_alternative_congestion_red));
    alternativeRouteShieldColor = typedArray.getColor(
      R.styleable.NavigationMapRoute_alternativeRouteShieldColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_alternative_shield_color));
    alternativeRouteScale = typedArray.getFloat(
      R.styleable.NavigationMapRoute_alternativeRouteScale, 1.0f);

    // Waypoint attributes
    originWaypointIcon = typedArray.getResourceId(
      R.styleable.NavigationMapRoute_originWaypointIcon, R.drawable.ic_route_origin);
    destinationWaypointIcon = typedArray.getResourceId(
      R.styleable.NavigationMapRoute_destinationWaypointIcon, R.drawable.ic_route_destination);

    arrowColor = typedArray.getColor(R.styleable.NavigationMapRoute_upcomingManeuverArrowColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_upcoming_maneuver_arrow_color));
    arrowBorderColor = typedArray.getColor(R.styleable.NavigationMapRoute_upcomingManeuverArrowBorderColor,
      ContextCompat.getColor(context, R.color.mapbox_navigation_route_upcoming_maneuver_arrow_border_color));

    typedArray.recycle();
  }

  /**
   * Iterate through map style layers backwards till the first not-symbol layer is found.
   */
  private void placeRouteBelow() {
    if (belowLayer == null || belowLayer.isEmpty()) {
      List<Layer> styleLayers = mapboxMap.getLayers();
      if (styleLayers == null) {
        return;
      }
      for (int i = 0; i < styleLayers.size(); i++) {
        if (!(styleLayers.get(i) instanceof SymbolLayer)
          // Avoid placing the route on top of the user location layer
          && !styleLayers.get(i).getId().contains("mapbox-location")) {
          belowLayer = styleLayers.get(i).getId();
        }
      }
    }
  }

  private void drawWaypointMarkers(@NonNull MapboxMap mapboxMap, @Nullable Drawable originMarker,
                                   @Nullable Drawable destinationMarker) {
    if (originMarker == null || destinationMarker == null) {
      return;
    }

    SymbolLayer waypointLayer = mapboxMap.getLayerAs(WAYPOINT_LAYER_ID);
    if (waypointLayer == null) {
      Bitmap bitmap = MapImageUtils.getBitmapFromDrawable(originMarker);
      mapboxMap.addImage("originMarker", bitmap);
      bitmap = MapImageUtils.getBitmapFromDrawable(destinationMarker);
      mapboxMap.addImage("destinationMarker", bitmap);

      waypointLayer = new SymbolLayer(WAYPOINT_LAYER_ID, WAYPOINT_SOURCE_ID).withProperties(
        PropertyFactory.iconImage(match(
          Expression.toString(get("waypoint")), literal("originMarker"),
          stop("origin", literal("originMarker")),
          stop("destination", literal("destinationMarker"))
          )
        ),
        PropertyFactory.iconSize(interpolate(
          exponential(1.5f), zoom(),
          stop(22f, 2.8f),
          stop(12f, 1.3f),
          stop(10f, 0.8f),
          stop(0f, 0.6f)
        )),
        PropertyFactory.iconPitchAlignment(Property.ANCHOR_MAP),
        PropertyFactory.iconAllowOverlap(true),
        PropertyFactory.iconIgnorePlacement(true)
      );
      layerIds.add(WAYPOINT_LAYER_ID);
      MapUtils.addLayerToMap(mapboxMap, waypointLayer, belowLayer);
    }
  }

  private Feature getPointFromLineString(RouteLeg leg, int index) {
    Feature feature = Feature.fromGeometry(Point.fromLngLat(
      leg.steps().get(index).maneuver().location().longitude(),
      leg.steps().get(index).maneuver().location().latitude()
    ));
    feature.addStringProperty(SOURCE_KEY, WAYPOINT_SOURCE_ID);
    feature.addStringProperty("waypoint",
      index == 0 ? "origin" : "destination"
    );
    return feature;
  }

  private void initialize() {
    alternativesVisible = true;
    getAttributes();
    placeRouteBelow();
    initializeUpcomingManeuverArrow();
  }

  private void addListeners() {
    mapboxMap.addOnMapClickListener(this);
    if (navigation != null) {
      navigation.addProgressChangeListener(progressChangeListener);
    }
    mapView.addOnMapChangedListener(this);
  }

  /**
   * Remove the route line from the map style.
   *
   * @since 0.4.0
   */
  public void removeRoute() {
    clearRoutes();
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    if (invalidMapClick()) {
      return;
    }
    final int currentRouteIndex = primaryRouteIndex;

    if (findClickedRoute(point)) {
      return;
    }
    checkNewRouteFound(currentRouteIndex);
  }

  private boolean invalidMapClick() {
    return routeLineStrings == null || routeLineStrings.isEmpty() || !alternativesVisible;
  }

  private boolean findClickedRoute(@NonNull LatLng point) {
    HashMap<Double, DirectionsRoute> routeDistancesAwayFromClick = new HashMap<>();

    Point clickPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());

    if (calculateClickDistancesFromRoutes(routeDistancesAwayFromClick, clickPoint)) {
      return true;
    }
    List<Double> distancesAwayFromClick = new ArrayList<>(routeDistancesAwayFromClick.keySet());
    Collections.sort(distancesAwayFromClick);

    DirectionsRoute clickedRoute = routeDistancesAwayFromClick.get(distancesAwayFromClick.get(0));
    primaryRouteIndex = directionsRoutes.indexOf(clickedRoute);
    return false;
  }

  private boolean calculateClickDistancesFromRoutes(HashMap<Double, DirectionsRoute> routeDistancesAwayFromClick,
                                                    Point clickPoint) {
    for (LineString lineString : routeLineStrings.keySet()) {
      Point pointOnLine = findPointOnLine(clickPoint, lineString);

      if (pointOnLine == null) {
        return true;
      }
      double distance = TurfMeasurement.distance(clickPoint, pointOnLine, TurfConstants.UNIT_METERS);
      routeDistancesAwayFromClick.put(distance, routeLineStrings.get(lineString));
    }
    return false;
  }

  private Point findPointOnLine(Point clickPoint, LineString lineString) {
    List<Point> linePoints = lineString.coordinates();
    Feature feature = TurfMisc.nearestPointOnLine(clickPoint, linePoints);
    return (Point) feature.geometry();
  }

  private void checkNewRouteFound(int currentRouteIndex) {
    if (currentRouteIndex != primaryRouteIndex) {
      updateRoute();
      boolean isValidPrimaryIndex = primaryRouteIndex >= 0 && primaryRouteIndex < directionsRoutes.size();
      if (isValidPrimaryIndex && onRouteSelectionChangeListener != null) {
        DirectionsRoute selectedRoute = directionsRoutes.get(primaryRouteIndex);
        onRouteSelectionChangeListener.onNewPrimaryRouteSelected(selectedRoute);
      }
    }
  }

  private void updateRoute() {
    // Update all route geometries to reflect their appropriate colors depending on if they are
    // alternative or primary.
    for (FeatureCollection featureCollection : featureCollections) {
      if (!(featureCollection.features().get(0).geometry() instanceof Point)) {
        int index = featureCollection.features().get(0).getNumberProperty(INDEX_KEY).intValue();
        updatePrimaryShieldRoute(String.format(Locale.US, ID_FORMAT, GENERIC_ROUTE_SHIELD_LAYER_ID,
          index), index);
        updatePrimaryRoute(String.format(Locale.US, ID_FORMAT, GENERIC_ROUTE_LAYER_ID,
          index), index);
      }
    }
  }

  /**
   * Called when a map change events occurs. Used specifically to detect loading of a new style, if
   * applicable reapply the route line source and layers.
   *
   * @param change the map change event that occurred
   * @since 0.4.0
   */
  @Override
  public void onMapChanged(int change) {
    if (change == MapView.DID_FINISH_LOADING_STYLE) {
      placeRouteBelow();
      drawRoutes();
      addDirectionWaypoints();
      showAlternativeRoutes(alternativesVisible);
    }
  }

  /**
   * This method should be called only if you have passed {@link MapboxNavigation}
   * into the constructor.
   * <p>
   * This method will add the {@link ProgressChangeListener} that was originally added so updates
   * to the {@link MapboxMap} continue.
   *
   * @since 0.15.0
   */
  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  public void onStart() {
    if (navigation != null) {
      navigation.addProgressChangeListener(progressChangeListener);
    }
  }

  /**
   * This method should be called only if you have passed {@link MapboxNavigation}
   * into the constructor.
   * <p>
   * This method will remove the {@link ProgressChangeListener} that was originally added so updates
   * to the {@link MapboxMap} discontinue.
   *
   * @since 0.15.0
   */
  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  public void onStop() {
    if (navigation != null) {
      navigation.removeProgressChangeListener(progressChangeListener);
    }
  }

  /**
   * If the {@link DirectionsRoute} request contains congestion information via annotations, breakup
   * the source into pieces so data-driven styling can be used to change the route colors
   * accordingly.
   */
  private FeatureCollection addTrafficToSource(DirectionsRoute route, int index) {
    final List<Feature> features = new ArrayList<>();
    LineString originalGeometry = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);
    buildRouteFeatureFromGeometry(index, features, originalGeometry);
    routeLineStrings.put(originalGeometry, route);

    LineString lineString = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);
    buildTrafficFeaturesFromRoute(route, index, features, lineString);
    return FeatureCollection.fromFeatures(features);
  }

  private void buildRouteFeatureFromGeometry(int index, List<Feature> features, LineString originalGeometry) {
    Feature feat = Feature.fromGeometry(originalGeometry);
    feat.addStringProperty(SOURCE_KEY, String.format(Locale.US, ID_FORMAT, GENERIC_ROUTE_SOURCE_ID, index));
    feat.addNumberProperty(INDEX_KEY, index);
    features.add(feat);
  }

  private void buildTrafficFeaturesFromRoute(DirectionsRoute route, int index,
                                             List<Feature> features, LineString lineString) {
    for (RouteLeg leg : route.legs()) {
      if (leg.annotation() != null && leg.annotation().congestion() != null) {
        for (int i = 0; i < leg.annotation().congestion().size(); i++) {
          // See https://github.com/mapbox/mapbox-navigation-android/issues/353
          if (leg.annotation().congestion().size() + 1 <= lineString.coordinates().size()) {

            List<Point> points = new ArrayList<>();
            points.add(lineString.coordinates().get(i));
            points.add(lineString.coordinates().get(i + 1));

            LineString congestionLineString = LineString.fromLngLats(points);
            Feature feature = Feature.fromGeometry(congestionLineString);
            feature.addStringProperty(CONGESTION_KEY, leg.annotation().congestion().get(i));
            feature.addStringProperty(SOURCE_KEY, String.format(Locale.US, ID_FORMAT,
              GENERIC_ROUTE_SOURCE_ID, index));
            feature.addNumberProperty(INDEX_KEY, index);
            features.add(feature);
          }
        }
      } else {
        Feature feature = Feature.fromGeometry(lineString);
        features.add(feature);
      }
    }
  }
}