package com.mapbox.services.android.navigation.ui.v5.route;

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

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.core.constants.Constants;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.functions.Function;
import com.mapbox.mapboxsdk.style.functions.stops.Stop;
import com.mapbox.mapboxsdk.style.functions.stops.Stops;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.utils.MapImageUtils;
import com.mapbox.services.android.navigation.ui.v5.utils.MapUtils;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.mapbox.mapboxsdk.style.functions.stops.Stop.stop;
import static com.mapbox.mapboxsdk.style.functions.stops.Stops.categorical;
import static com.mapbox.mapboxsdk.style.functions.stops.Stops.exponential;

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
public class NavigationMapRoute implements ProgressChangeListener, MapView.OnMapChangedListener,
  MapboxMap.OnMapClickListener {

  private static final String CONGESTION_KEY = "congestion";
  private static final String SOURCE_KEY = "source";
  private static final String INDEX_KEY = "index";

  private static final String GENERIC_ROUTE_SOURCE_ID = "mapbox-navigation-route-source";
  private static final String GENERIC_ROUTE_LAYER_ID = "mapbox-navigation-route-layer";
  private static final String WAYPOINT_SOURCE_ID = "mapbox-navigation-waypoint-source";
  private static final String WAYPOINT_LAYER_ID = "mapbox-navigation-waypoint-layer";
  private static final String ID_FORMAT = "%s-%d";
  private static final String GENERIC_ROUTE_SHIELD_LAYER_ID
    = "mapbox-navigation-route-shield-layer";

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
  @DrawableRes
  private int originWaypointIcon;
  @DrawableRes
  private int destinationWaypointIcon;

  private final MapboxNavigation navigation;
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
          PropertyFactory.visibility(visible ? Property.VISIBLE : Property.NONE)
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
        featureCollections.get(i).getFeatures().get(0).getStringProperty(SOURCE_KEY)
      );

      // Get some required information for the next step
      String sourceId = featureCollections.get(i).getFeatures()
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
  private static FeatureCollection waypointFeatureCollection(DirectionsRoute route) {
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
      ContextCompat.getDrawable(mapView.getContext(), originWaypointIcon),
      ContextCompat.getDrawable(mapView.getContext(), destinationWaypointIcon)
    );
  }

  /**
   * When the user switches an alternative route to a primary route, this method alters the
   * appearance.
   */
  private void updatePrimaryRoute(String layerId, int index) {
    Layer layer = mapboxMap.getLayer(layerId);
    if (layer != null) {
      layer.setProperties(
        PropertyFactory.lineColor(
          Function.property(CONGESTION_KEY, categorical(
            stop("moderate", PropertyFactory.lineColor(
              index == primaryRouteIndex ? routeModerateColor : alternativeRouteModerateColor)),
            stop("heavy", PropertyFactory.lineColor(
              index == primaryRouteIndex ? routeSevereColor : alternativeRouteSevereColor)),
            stop("severe", PropertyFactory.lineColor(
              index == primaryRouteIndex ? routeSevereColor : alternativeRouteSevereColor))
          )).withDefaultValue(PropertyFactory.lineColor(
            index == primaryRouteIndex ? routeDefaultColor : alternativeRouteDefaultColor)))
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
        PropertyFactory.lineColor(
          index == primaryRouteIndex ? routeShieldColor : alternativeRouteShieldColor)
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
      PropertyFactory.lineWidth(Function.zoom(
        exponential(
          stop(4f, PropertyFactory.lineWidth(3f * scale)),
          stop(10f, PropertyFactory.lineWidth(4f * scale)),
          stop(13f, PropertyFactory.lineWidth(6f * scale)),
          stop(16f, PropertyFactory.lineWidth(10f * scale)),
          stop(19f, PropertyFactory.lineWidth(14f * scale)),
          stop(22f, PropertyFactory.lineWidth(18f * scale))
        ).withBase(1.5f))
      ),
      PropertyFactory.lineColor(
        Function.property(CONGESTION_KEY, categorical(
          stop("moderate", PropertyFactory.lineColor(
            index == primaryRouteIndex ? routeModerateColor : alternativeRouteModerateColor)),
          stop("heavy", PropertyFactory.lineColor(
            index == primaryRouteIndex ? routeSevereColor : alternativeRouteSevereColor)),
          stop("severe", PropertyFactory.lineColor(
            index == primaryRouteIndex ? routeSevereColor : alternativeRouteSevereColor))
        )).withDefaultValue(PropertyFactory.lineColor(
          index == primaryRouteIndex ? routeDefaultColor : alternativeRouteDefaultColor)))
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
      PropertyFactory.lineWidth(Function.zoom(
        exponential(
          stop(10f, PropertyFactory.lineWidth(7f)),
          stop(14f, PropertyFactory.lineWidth(10.5f * scale)),
          stop(16.5f, PropertyFactory.lineWidth(15.5f * scale)),
          stop(19f, PropertyFactory.lineWidth(24f * scale)),
          stop(22f, PropertyFactory.lineWidth(29f * scale))
        ).withBase(1.5f))
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
    TypedArray typedArray
      = context.obtainStyledAttributes(styleRes, R.styleable.NavigationMapRoute);

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

    typedArray.recycle();
  }

  /**
   * Iterate through map style layers backwards till the first not-symbol layer is found.
   */
  private void placeRouteBelow() {
    if (belowLayer == null || belowLayer.isEmpty()) {
      List<Layer> styleLayers = mapboxMap.getLayers();
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
        PropertyFactory.iconImage(Function.property(
          "waypoint",
          categorical(
            stop("origin", PropertyFactory.iconImage("originMarker")),
            stop("destination", PropertyFactory.iconImage("destinationMarker"))
          )
        )),
        PropertyFactory.iconSize(Function.zoom(
          Stops.exponential(
            Stop.stop(22f, PropertyFactory.iconSize(2.8f)),
            Stop.stop(12f, PropertyFactory.iconSize(1.3f)),
            Stop.stop(10f, PropertyFactory.iconSize(0.8f)),
            Stop.stop(0f, PropertyFactory.iconSize(0.6f))
          ).withBase(1.5f))),
        PropertyFactory.iconPitchAlignment(Property.ANCHOR_MAP),
        PropertyFactory.iconAllowOverlap(true),
        PropertyFactory.iconIgnorePlacement(true)
      );
      layerIds.add(WAYPOINT_LAYER_ID);
      MapUtils.addLayerToMap(mapboxMap, waypointLayer, belowLayer);
    }
  }

  private static Feature getPointFromLineString(RouteLeg leg, int stepIndex) {
    Feature feature = Feature.fromGeometry(Point.fromCoordinates(
      new double[] {
        leg.steps().get(stepIndex).maneuver().location().longitude(),
        leg.steps().get(stepIndex).maneuver().location().latitude()
      }));
    feature.addStringProperty(SOURCE_KEY, WAYPOINT_SOURCE_ID);
    feature.addStringProperty("waypoint",
      stepIndex == 0 ? "origin" : "destination"
    );
    return feature;
  }

  private void initialize() {
    alternativesVisible = true;
    getAttributes();
    placeRouteBelow();
  }

  private void addListeners() {
    mapboxMap.addOnMapClickListener(this);
    if (navigation != null) {
      navigation.addProgressChangeListener(this);
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

    com.mapbox.geojson.Point clickPoint
      = com.mapbox.geojson.Point.fromLngLat(point.getLongitude(), point.getLatitude());

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
                                                    com.mapbox.geojson.Point clickPoint) {
    for (LineString lineString : routeLineStrings.keySet()) {
      com.mapbox.geojson.Point pointOnLine = findPointOnLine(clickPoint, lineString);

      if (pointOnLine == null) {
        return true;
      }
      double distance = TurfMeasurement.distance(clickPoint, pointOnLine, TurfConstants.UNIT_METERS);
      routeDistancesAwayFromClick.put(distance, routeLineStrings.get(lineString));
    }
    return false;
  }

  private com.mapbox.geojson.Point findPointOnLine(com.mapbox.geojson.Point clickPoint, LineString lineString) {
    List<com.mapbox.geojson.Point> linePoints = new ArrayList<>();
    List<Position> positions = lineString.getCoordinates();
    for (Position pos : positions) {
      linePoints.add(com.mapbox.geojson.Point.fromLngLat(pos.getLongitude(), pos.getLatitude()));
    }

    com.mapbox.geojson.Feature feature = TurfMisc.pointOnLine(clickPoint, linePoints);
    return (com.mapbox.geojson.Point) feature.geometry();
  }

  private void checkNewRouteFound(int currentRouteIndex) {
    if (currentRouteIndex != primaryRouteIndex) {
      updateRoute();
      boolean isValidPrimaryIndex = primaryRouteIndex > 0 && primaryRouteIndex < directionsRoutes.size();
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
      if (!(featureCollection.getFeatures().get(0).getGeometry() instanceof Point)) {
        int index = featureCollection.getFeatures().get(0).getNumberProperty(INDEX_KEY).intValue();
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
   * Called when the user makes new progress during a navigation session. Used to determine whether
   * or not a re-route has occurred and if so the route is redrawn to reflect the change.
   *
   * @param location      the users current location
   * @param routeProgress a {@link RouteProgress} reflecting the users latest progress along the
   *                      route
   * @since 0.4.0
   */
  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    // Check if the route's the same as the route currently drawn
    if (!routeProgress.directionsRoute().equals(directionsRoutes.get(primaryRouteIndex))) {
      addRoute(routeProgress.directionsRoute());
      drawRoutes();
      addDirectionWaypoints();
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
    feat.addStringProperty(SOURCE_KEY, String.format(Locale.US, ID_FORMAT, GENERIC_ROUTE_SOURCE_ID,
      index));
    feat.addNumberProperty(INDEX_KEY, index);
    features.add(feat);
  }

  private void buildTrafficFeaturesFromRoute(DirectionsRoute route, int index,
                                             List<Feature> features, LineString lineString) {
    for (RouteLeg leg : route.legs()) {
      if (leg.annotation() != null && leg.annotation().congestion() != null) {
        for (int i = 0; i < leg.annotation().congestion().size(); i++) {
          // See https://github.com/mapbox/mapbox-navigation-android/issues/353
          if (leg.annotation().congestion().size() + 1 <= lineString.getCoordinates().size()) {
            double[] startCoord = lineString.getCoordinates().get(i).getCoordinates();
            double[] endCoord = lineString.getCoordinates().get(i + 1).getCoordinates();

            LineString congestionLineString = LineString.fromCoordinates(new double[][] {startCoord,
              endCoord});
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