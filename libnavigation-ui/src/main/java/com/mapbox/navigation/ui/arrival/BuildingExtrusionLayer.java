package com.mapbox.navigation.ui.arrival;

import android.graphics.Color;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;

import androidx.annotation.NonNull;

import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionBase;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionHeight;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

public class BuildingExtrusionLayer {

  private static final String FILL_EXTRUSION_LAYER_ID = "extruded-buildings";
  private static final String BUILDING_LAYER_ID = "building";
  private static final String HEIGHT = "height";
  private static final String MIN_HEIGHT = "min_height";
  private static final Float DEFAULT_FILL_EXTRUSION_OPACITY = 0.6f;
  private static final Integer DEFAULT_FILL_EXTRUSION_COLOR = Color.parseColor("#F9F9F9");
  private static final Float DEFAULT_MIN_ZOOM_LEVEL = 15f;
  private Float opacity;
  private Integer color;
  private Float minZoomLevel;
  private Boolean layerVisible = false;
  private MapboxMap mapboxMap;
  private MapView mapView;

  public BuildingExtrusionLayer(MapboxMap mapboxMap, MapView mapView) {
    this.mapboxMap = mapboxMap;
    this.mapView = mapView;
  }

  /**
   * Toggles the visibility of the building extrusion layer.
   *
   * @param visible true if the layer should be placed/displayed. False if it should be hidden.
   */
  public void updateVisibility(final Boolean visible) {
    if (mapView.isDestroyed()) {
      return;
    }
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        Layer layer = style.getLayer(FILL_EXTRUSION_LAYER_ID);
        if (layer == null) {
          addLayerToMap(visible);
        } else if (layer.getVisibility().value.equals(VISIBLE) != visible) {
          layer.setProperties(visibility(visible ? VISIBLE : NONE));
          layerVisible = visible;
        }
      }
    });
  }

  /**
   * Retrieve the visibility status of the layer.
   *
   * @return boolean about whether the extrusion layer is visible
   */
  public Boolean getVisibility() {
    if (mapView.isDestroyed()) {
      return false;
    }
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        Layer layer = style.getLayer(FILL_EXTRUSION_LAYER_ID);
        if (layer == null) {
          layerVisible = false;
        } else {
          layerVisible = layer.getVisibility().value.equals(VISIBLE);
        }
      }
    });
    return layerVisible;
  }

  /**
   * Set the color of the building extrusion layer.
   *
   * @param newExtrusionColor the new color value
   */
  public void setColor(final Integer newExtrusionColor) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillExtrusionLayer extrusionLayer = style.getLayerAs(FILL_EXTRUSION_LAYER_ID);
        if (extrusionLayer != null) {
          extrusionLayer.setProperties(
            fillExtrusionColor(newExtrusionColor));
          color = newExtrusionColor;
        }
      }
    });
  }

  /**
   * Set the opacity of the building extrusion layer.
   *
   * @param newExtrusionOpacity the new opacity value
   */
  public void setOpacity(final Float newExtrusionOpacity) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillExtrusionLayer extrusionLayer = style.getLayerAs(FILL_EXTRUSION_LAYER_ID);
        if (extrusionLayer != null) {
          extrusionLayer.setProperties(
            fillExtrusionOpacity(newExtrusionOpacity));
          opacity = newExtrusionOpacity;
        }
      }
    });
  }

  /**
   * Set the building min zoom level. This is the minimum zoom level where buildings will start
   * to show. useful to limit showing buildings at higher zoom levels.
   *
   * @param newMinZoomLevel a {@code float} value between the maps minimum and maximum zoom level which
   *                        defines at which level the buildings should show up
   */
  public void setMinZoomLevel(final Float newMinZoomLevel) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillExtrusionLayer extrusionLayer = style.getLayerAs(FILL_EXTRUSION_LAYER_ID);
        if (extrusionLayer != null) {
          extrusionLayer.setMinZoom(newMinZoomLevel);
          minZoomLevel = newMinZoomLevel;
        }
      }
    });
  }

  /**
   * Retrieve the latest set opacity of the building extrusion layer.
   *
   * @return the opacity Float
   */
  public Float getOpacity() {
    return opacity;
  }

  /**
   * Retrieve the latest set opacity of the building extrusion layer.
   *
   * @return the color Integer
   */
  public Integer getColor() {
    return color;
  }

  private void addLayerToMap(final Boolean visible) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        Layer buildingLayer = style.getLayer(BUILDING_LAYER_ID);
        if (buildingLayer != null) {
          FillExtrusionLayer fillExtrusionLayer = new FillExtrusionLayer(FILL_EXTRUSION_LAYER_ID, "composite");
          fillExtrusionLayer.setSourceLayer("building");
          fillExtrusionLayer.setMinZoom(DEFAULT_MIN_ZOOM_LEVEL);
          fillExtrusionLayer.setProperties(
            fillExtrusionHeight(
              interpolate(
                exponential(1f),
                zoom(),
                stop(15, literal(0)),
                stop(16, get("height"))
              )
            ),
            fillExtrusionBase(get(MIN_HEIGHT)),
            fillExtrusionOpacity(DEFAULT_FILL_EXTRUSION_OPACITY),
            fillExtrusionColor(DEFAULT_FILL_EXTRUSION_COLOR),
            visibility(visible ? VISIBLE : NONE));
          style.addLayer(fillExtrusionLayer);
        }
      }
    });
  }
}
