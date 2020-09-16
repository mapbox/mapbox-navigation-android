package com.mapbox.navigation.ui.route;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.mapbox.bindgen.Value;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.maps.LayerPosition;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.style.expressions.Expression;
import com.mapbox.maps.plugin.style.layers.LineLayer;
import com.mapbox.maps.plugin.style.layers.SymbolLayer;
import com.mapbox.maps.plugin.style.layers.properties.IconRotationAlignment;
import com.mapbox.maps.plugin.style.layers.properties.LineCap;
import com.mapbox.maps.plugin.style.layers.properties.LineJoin;
import com.mapbox.maps.plugin.style.layers.properties.Visibility;
import com.mapbox.maps.plugin.style.sources.GeojsonSource;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.ui.R;
import com.mapbox.navigation.ui.internal.utils.MapImageUtils;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;

import java.util.*;

import static com.mapbox.navigation.ui.internal.route.RouteConstants.*;
import static com.mapbox.navigation.ui.internal.utils.CompareUtils.areEqualContentsIgnoreOrder;

class MapRouteArrow {

  @ColorInt
  private final int arrowColor;
  @ColorInt
  private final int arrowBorderColor;

  private List<Point> maneuverPoints;
  private List<String> arrowLayerIds;
  private GeojsonSource arrowShaftGeoJsonSource;
  private GeojsonSource arrowHeadGeoJsonSource;

  @NonNull
  private final MapView mapView;
  private final MapboxMap mapboxMap;
  private boolean isVisible = true;

  MapRouteArrow(@NonNull MapView mapView, MapboxMap mapboxMap, @StyleRes int styleRes, @NonNull String aboveLayer) {
    this.mapView = mapView;
    this.mapboxMap = mapboxMap;
    this.maneuverPoints = new ArrayList<>();

    Context context = mapView.getContext();
    TypedArray typedArray = context.obtainStyledAttributes(styleRes, R.styleable.MapboxStyleNavigationMapRoute);
    arrowColor = typedArray.getColor(R.styleable.MapboxStyleNavigationMapRoute_upcomingManeuverArrowColor,
            ContextCompat.getColor(context, R.color.mapbox_navigation_route_upcoming_maneuver_arrow_color));
    arrowBorderColor = typedArray.getColor(R.styleable.MapboxStyleNavigationMapRoute_upcomingManeuverArrowBorderColor,
            ContextCompat.getColor(context, R.color.mapbox_navigation_route_upcoming_maneuver_arrow_border_color));
    typedArray.recycle();

    initialize(aboveLayer);
    updateVisibilityTo(isVisible);
  }

  void addUpcomingManeuverArrow(@NonNull RouteProgress routeProgress) {
    boolean invalidUpcomingStepPoints = routeProgress.getUpcomingStepPoints() == null
            || routeProgress.getUpcomingStepPoints().size() < TWO_POINTS;
    boolean invalidCurrentStepPoints = routeProgress.getCurrentLegProgress() == null
            || routeProgress.getCurrentLegProgress().getCurrentStepProgress() == null
            || routeProgress.getCurrentLegProgress().getCurrentStepProgress().getStepPoints() == null
            || routeProgress.getCurrentLegProgress().getCurrentStepProgress().getStepPoints().size() < TWO_POINTS;
    if (invalidUpcomingStepPoints || invalidCurrentStepPoints) {
      updateVisibilityTo(false);
      return;
    }
    updateVisibilityTo(true);

    List<Point> newManeuverPoints = obtainArrowPointsFrom(routeProgress);
    if (!areEqualContentsIgnoreOrder(maneuverPoints, newManeuverPoints)) {
      maneuverPoints.clear();
      maneuverPoints.addAll(newManeuverPoints);
      updateArrowShaftWith(maneuverPoints);
      updateArrowHeadWith(maneuverPoints);
    }
  }

  void updateVisibilityTo(boolean visible) {
    this.isVisible = visible;
    mapboxMap.getStyle(style -> {
      for (String layerId : arrowLayerIds) {
        if (style.layerExists(layerId)) {
          Visibility targetVisibility = visible ? Visibility.VISIBLE : Visibility.NONE;
          style.setLayerProperty(layerId, "visibility", new Value(targetVisibility.getValue()));
        }
      }
    });
  }

  public boolean routeArrowIsVisible() {
    return isVisible;
  }

  @NonNull
  private List<Point> obtainArrowPointsFrom(@NonNull RouteProgress routeProgress) {
    List<Point> reversedCurrent =
      new ArrayList<>(routeProgress.getCurrentLegProgress().getCurrentStepProgress().getStepPoints());
    Collections.reverse(reversedCurrent);

    LineString arrowLineCurrent = LineString.fromLngLats(reversedCurrent);
    LineString arrowLineUpcoming = LineString.fromLngLats(routeProgress.getUpcomingStepPoints());

    LineString arrowCurrentSliced =
      TurfMisc.lineSliceAlong(arrowLineCurrent, 0, THIRTY, TurfConstants.UNIT_METERS);
    LineString arrowUpcomingSliced =
      TurfMisc.lineSliceAlong(arrowLineUpcoming, 0, THIRTY, TurfConstants.UNIT_METERS);

    Collections.reverse(arrowCurrentSliced.coordinates());

    List<Point> combined = new ArrayList<>();
    combined.addAll(arrowCurrentSliced.coordinates());
    combined.addAll(arrowUpcomingSliced.coordinates());
    return combined;
  }

  private void updateArrowShaftWith(@NonNull List<Point> points) {
    LineString shaft = LineString.fromLngLats(points);
    Feature arrowShaftGeoJsonFeature = Feature.fromGeometry(shaft);
    arrowShaftGeoJsonSource.feature(arrowShaftGeoJsonFeature);
  }

  private void updateArrowHeadWith(@NonNull List<Point> points) {
    double azimuth = TurfMeasurement.bearing(points.get(points.size() - 2), points.get(points.size() - 1));
    Feature arrowHeadGeoJsonFeature = Feature.fromGeometry(points.get(points.size() - 1));
    arrowHeadGeoJsonFeature.addNumberProperty(ARROW_BEARING, (float) wrap(azimuth, 0, MAX_DEGREES));
    arrowHeadGeoJsonSource.feature(arrowHeadGeoJsonFeature);
  }

  private void initialize(@NonNull String aboveLayer) {
    initializeArrowShaft();
    initializeArrowHead();

    addArrowHeadIcon();
    addArrowHeadIconCasing();

    mapboxMap.getStyle(style -> {
      LineLayer shaftLayer = createArrowShaftLayer(style);
      LineLayer shaftCasingLayer = createArrowShaftCasingLayer(style);
      SymbolLayer headLayer = createArrowHeadLayer(style);
      SymbolLayer headCasingLayer = createArrowHeadCasingLayer(style);

      shaftCasingLayer.bindTo(style, new LayerPosition(aboveLayer, null, null));
      headCasingLayer.bindTo(style, new LayerPosition(shaftCasingLayer.getLayerId(), null, null));
      shaftLayer.bindTo(style, new LayerPosition(headCasingLayer.getLayerId(), null, null));
      headLayer.bindTo(style, new LayerPosition(shaftLayer.getLayerId(), null, null));

      createArrowLayerList(shaftLayer, shaftCasingLayer, headLayer, headCasingLayer);
    });
  }

  private void initializeArrowShaft() {
    arrowShaftGeoJsonSource = new GeojsonSource.Builder(ARROW_SHAFT_SOURCE_ID)
        .featureCollection(FeatureCollection.fromFeatures(new Feature[]{}))
        .maxzoom(16)
        .build();

    mapboxMap.getStyle(style -> {
      arrowShaftGeoJsonSource.bindTo(style);
    });
  }

  private void initializeArrowHead() {
    arrowHeadGeoJsonSource = new GeojsonSource.Builder(ARROW_HEAD_SOURCE_ID)
        .featureCollection(FeatureCollection.fromFeatures(new Feature[]{}))
        .maxzoom(16)
        .build();

    mapboxMap.getStyle(style -> {
      arrowHeadGeoJsonSource.bindTo(style);
    });
  }

  private void addArrowHeadIcon() {
    int headResId = R.drawable.mapbox_ic_arrow_head;
    Drawable arrowHead = AppCompatResources.getDrawable(mapView.getContext(), headResId);
    if (arrowHead == null) {
      return;
    }
    Drawable head = DrawableCompat.wrap(arrowHead);
    DrawableCompat.setTint(head.mutate(), arrowColor);
    Bitmap icon = MapImageUtils.getBitmapFromDrawable(head);
    mapboxMap.getStyle(style -> style.addImage(ARROW_HEAD_ICON, icon));
  }

  private void addArrowHeadIconCasing() {
    int casingResId = R.drawable.mapbox_ic_arrow_head_casing;
    Drawable arrowHeadCasing = AppCompatResources.getDrawable(mapView.getContext(), casingResId);
    if (arrowHeadCasing == null) {
      return;
    }
    Drawable headCasing = DrawableCompat.wrap(arrowHeadCasing);
    DrawableCompat.setTint(headCasing.mutate(), arrowBorderColor);
    Bitmap icon = MapImageUtils.getBitmapFromDrawable(headCasing);
    mapboxMap.getStyle(style -> style.addImage(ARROW_HEAD_ICON_CASING, icon));
  }

  @NonNull
  private LineLayer createArrowShaftLayer(final Style style) {
    if (style.layerExists(ARROW_SHAFT_LINE_LAYER_ID)) {
      style.removeLayer(ARROW_SHAFT_LINE_LAYER_ID);
    }

    LineLayer shaftLayer = new LineLayer(ARROW_SHAFT_LINE_LAYER_ID, ARROW_SHAFT_SOURCE_ID);
    shaftLayer.lineColor(Expression.color(arrowColor));
    shaftLayer.lineWidth(getArrowShaftWidthExpression());
    shaftLayer.lineCap(LineCap.ROUND);
    shaftLayer.lineJoin(LineJoin.ROUND);
    shaftLayer.visibility(Visibility.NONE);
    shaftLayer.lineOpacity(getArrowShaftOpacityExpression());
    return shaftLayer;
  }

  @NonNull
  private LineLayer createArrowShaftCasingLayer(final Style style) {
    if (style.layerExists(ARROW_SHAFT_LINE_LAYER_ID)) {
      style.removeLayer(ARROW_SHAFT_LINE_LAYER_ID);
    }

    LineLayer shaftCasingLayer = new LineLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID, ARROW_SHAFT_SOURCE_ID);
    shaftCasingLayer.lineColor(Expression.color(arrowBorderColor));
    shaftCasingLayer.lineWidth(getArrowShaftWidthExpression());
    shaftCasingLayer.lineCap(LineCap.ROUND);
    shaftCasingLayer.lineJoin(LineJoin.ROUND);
    shaftCasingLayer.visibility(Visibility.NONE);
    shaftCasingLayer.lineOpacity(getArrowShaftOpacityExpression());
    return shaftCasingLayer;
  }

  @NonNull
  private SymbolLayer createArrowHeadLayer(final Style style) {
    if (style.layerExists(ARROW_HEAD_LAYER_ID)) {
      style.removeLayer(ARROW_HEAD_LAYER_ID);
    }

    SymbolLayer arrowHeadLayer = new SymbolLayer(ARROW_HEAD_LAYER_ID, ARROW_HEAD_SOURCE_ID);
    arrowHeadLayer.iconImage(ARROW_HEAD_ICON);
    arrowHeadLayer.iconAllowOverlap(true);
    arrowHeadLayer.iconIgnorePlacement(true);
    arrowHeadLayer.iconSize(getArrowHeadIconSizeExpression());
    arrowHeadLayer.iconOffset(Arrays.asList(ARROW_HEAD_OFFSET));
    arrowHeadLayer.iconRotationAlignment(IconRotationAlignment.MAP);
    arrowHeadLayer.iconRotate(getArrowHeadIconRotateExpression());
    arrowHeadLayer.visibility(Visibility.NONE);
    arrowHeadLayer.iconOpacity(getArrowHeadIconOpacityExpression());
    return arrowHeadLayer;
  }

  @NonNull
  private SymbolLayer createArrowHeadCasingLayer(final Style style) {
    if (style.layerExists(ARROW_HEAD_CASING_LAYER_ID)) {
      style.removeLayer(ARROW_HEAD_CASING_LAYER_ID);
    }

    SymbolLayer casingLayer = new SymbolLayer(ARROW_HEAD_CASING_LAYER_ID, ARROW_HEAD_SOURCE_ID);
    casingLayer.iconImage(ARROW_HEAD_ICON_CASING);
    casingLayer.iconAllowOverlap(true);
    casingLayer.iconSize(getArrowHeadIconSizeExpression());
    casingLayer.iconOffset(Arrays.asList(ARROW_HEAD_CASING_OFFSET));
    casingLayer.iconRotationAlignment(IconRotationAlignment.MAP);
    casingLayer.iconRotate(getArrowHeadIconRotateExpression());
    casingLayer.visibility(Visibility.NONE);
    casingLayer.iconOpacity(getArrowHeadIconOpacityExpression()); //["step", ["zoom"], 0.0, 14.0, 1.0]
    return casingLayer;
  }

  private Expression getArrowShaftOpacityExpression() {
    Expression.ExpressionBuilder lineOpacityExpressionBuilder = new Expression.ExpressionBuilder("step");
    lineOpacityExpressionBuilder.zoom();
    lineOpacityExpressionBuilder.literal(OPAQUE);
    lineOpacityExpressionBuilder.stop(expressionBuilder -> {
      expressionBuilder.literal(ARROW_HIDDEN_ZOOM_LEVEL);
      expressionBuilder.literal(TRANSPARENT);
      return null;
    });

    return lineOpacityExpressionBuilder.build(); //[step, [zoom], 0.0, 14, 1.0]
  }

  private Expression getArrowShaftWidthExpression() {
    Expression.ExpressionBuilder lineWidthExpressionBuilder = new Expression.ExpressionBuilder("interpolate");
    lineWidthExpressionBuilder.addArgument(Expression.linear());
    lineWidthExpressionBuilder.zoom();
    lineWidthExpressionBuilder.stop(expressionBuilder -> {
      expressionBuilder.literal(MIN_ARROW_ZOOM);
      expressionBuilder.literal(MIN_ZOOM_ARROW_SHAFT_SCALE);
      return null;
    });
    lineWidthExpressionBuilder.stop(expressionBuilder -> {
      expressionBuilder.literal(MAX_ARROW_ZOOM);
      expressionBuilder.literal(MAX_ZOOM_ARROW_SHAFT_SCALE);
      return null;
    });

    return lineWidthExpressionBuilder.build();
  }

  private Expression getArrowHeadIconOpacityExpression() {
    Expression.ExpressionBuilder iconOpacityExpressionBuilder = new Expression.ExpressionBuilder("step");
    iconOpacityExpressionBuilder.zoom();
    iconOpacityExpressionBuilder.literal(OPAQUE);
    iconOpacityExpressionBuilder.stop(expressionBuilder -> {
      expressionBuilder.literal(ARROW_HIDDEN_ZOOM_LEVEL);
      expressionBuilder.literal(TRANSPARENT);
      return null;
    });
    return iconOpacityExpressionBuilder.build(); //[step, [zoom], 0.0, 14, 1.0]
  }

  private Expression getArrowHeadIconRotateExpression() {
    Expression.ExpressionBuilder iconRotateExpressionBuilder = new Expression.ExpressionBuilder("get");
    iconRotateExpressionBuilder.literal(ARROW_BEARING);
    return iconRotateExpressionBuilder.build();
  }

  private Expression getArrowHeadIconSizeExpression() {
    Expression.ExpressionBuilder iconSizeExpressionBuilder = new Expression.ExpressionBuilder("interpolate");
    iconSizeExpressionBuilder.addArgument(Expression.linear());
    iconSizeExpressionBuilder.zoom();
    iconSizeExpressionBuilder.stop(expressionBuilderRef -> {
      expressionBuilderRef.literal(MIN_ARROW_ZOOM);
      expressionBuilderRef.literal(MIN_ZOOM_ARROW_HEAD_CASING_SCALE);
      return null;
    });
    iconSizeExpressionBuilder.stop(expressionBuilderRef -> {
      expressionBuilderRef.literal(MAX_ARROW_ZOOM);
      expressionBuilderRef.literal(MAX_ZOOM_ARROW_HEAD_CASING_SCALE);
      return null;
    });
    return iconSizeExpressionBuilder.build();
  }

  private void createArrowLayerList(
          @NonNull LineLayer shaftLayer,
          @NonNull LineLayer shaftCasingLayer,
          @NonNull SymbolLayer headLayer,
          @NonNull SymbolLayer headCasingLayer) {
    arrowLayerIds = new ArrayList<>();
    arrowLayerIds.add(shaftCasingLayer.getLayerId());
    arrowLayerIds.add(shaftLayer.getLayerId());
    arrowLayerIds.add(headCasingLayer.getLayerId());
    arrowLayerIds.add(headLayer.getLayerId());
  }

  // This came from MathUtils which may have been removed
  private double wrap(double value, double min, double max) {
    double delta = max - min;

    double firstMod = (value - min) % delta;
    double secondMod = (firstMod + delta) % delta;

    return secondMod + min;
  }
}
