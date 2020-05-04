package com.mapbox.navigation.ui.map;

import android.graphics.Color;
import android.graphics.PointF;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.List;

import androidx.annotation.NonNull;

import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

public class BuildingFootprintHighlightLayer {

  private static final String BUILDING_FOOTPRINT_SOURCE_ID = "building-source-id";
  private static final String BUILDING_FOOTPRINT_LAYER_ID = "building-footprint-layer-id";
  private static final String BUILDING_LAYER_ID = "building";
  private static final String BUILDING_STATION_LAYER_ID = "building station";
  private static final Integer DEFAULT_FOOTPRINT_COLOR = Color.RED;
  private static final Float DEFAULT_BUILDING_FOOTPRINT_OPACITY = 1f;
  private final MapboxMap mapboxMap;
  private final MapView mapView;
  private Polygon buildingPolygon;
  private Feature buildingPolygonFeature;
  private Integer color;
  private Float opacity;

  public BuildingFootprintHighlightLayer(MapboxMap mapboxMap, MapView mapView) {
    this.mapboxMap = mapboxMap;
    this.mapView = mapView;
  }

  /**
   * Toggles the visibility of the building footprint highlight layer.
   *
   * @param visible true if the layer should be placed/displayed. False if it should be hidden.
   */
  public void updateVisibility(final boolean visible) {
    if (mapView.isDestroyed()) {
      return;
    }
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintFillLayer = style.getLayerAs(BUILDING_FOOTPRINT_LAYER_ID);
        if (buildingFootprintFillLayer == null) {
          addFootprintHighlightFillLayerToMap();
        } else if ((buildingFootprintFillLayer.getVisibility().value.equals(VISIBLE)) != visible) {
          buildingFootprintFillLayer.setProperties(visibility(NONE));
        }
      }
    });
  }

  /**
   * Set the location of the building footprint highlight layer.
   *
   * @param targetLatLng the new coordinates to use in querying the building layer
   *                          to get the associated {@link Polygon} to eventually highlight.
   */
  public void setBuildingFootprintLocation(final LatLng targetLatLng) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        GeoJsonSource geoJsonSource = style.getSourceAs(BUILDING_FOOTPRINT_SOURCE_ID);
        Polygon polygon = getFootprintPolygonAssociatedWithBuilding(targetLatLng);
        if (polygon != null && geoJsonSource != null) {
          geoJsonSource.setGeoJson(polygon);
        }
      }
    });
  }

  /**
   * Set the color of the building footprint highlight layer.
   *
   * @param newFootprintColor the new color value
   */
  public void setColor(final int newFootprintColor) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintFillLayer = style.getLayerAs(BUILDING_FOOTPRINT_LAYER_ID);
        if (buildingFootprintFillLayer != null) {
          buildingFootprintFillLayer.setProperties(fillColor(newFootprintColor));
          color = newFootprintColor;
        }
      }
    });
  }

  /**
   * Set the opacity of the building footprint highlight layer.
   *
   * @param newFootprintOpacity the new opacity value
   */
  public void setOpacity(final Float newFootprintOpacity) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintFillLayer = style.getLayerAs(BUILDING_FOOTPRINT_LAYER_ID);
        if (buildingFootprintFillLayer != null) {
          buildingFootprintFillLayer.setProperties(fillOpacity(newFootprintOpacity));
          opacity = newFootprintOpacity;
        }
      }
    });
  }

  /**
   * Retrieve the {@link Polygon} geometry of the footprint of the building that's associated with
   * the latest targetLatLng.
   *
   * @return the {@link Polygon}
   */
  public Polygon getBuildingPolygon() {
    return buildingPolygon;
  }

  /**
   *
   * Retrieve the {@link Feature} the polygonal footprint of the building that's associated with
   * the latest targetLatLng.
   *
   * @return the {@link Feature}
   */
  public Feature getBuildingPolygonFeature() {
    return buildingPolygonFeature;
  }

  /**
   * Retrieve the latest set color of the building footprint highlight layer.
   *
   * @return the color Integer
   */
  public Integer getColor() {
    return color;
  }

  /**
   * Retrieve the latest set opacity of the building footprint highlight layer.
   *
   * @return the opacity Float
   */
  public Float getOpacity() {
    return opacity;
  }

  private Polygon getFootprintPolygonAssociatedWithBuilding(final LatLng targetLatLng) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        PointF pixel = mapboxMap.getProjection().toScreenLocation(new LatLng(
            targetLatLng.getLatitude(), targetLatLng.getLongitude()
        ));

        // Check whether the map style has a building layer
        if (style.getLayer(BUILDING_LAYER_ID) != null) {

          // Retrieve the building Feature that is associated with the target LatLng
          List<Feature> features = mapboxMap.queryRenderedFeatures(pixel, BUILDING_LAYER_ID, BUILDING_STATION_LAYER_ID);
          if (features.size() > 0 && features.get(0).geometry() instanceof Polygon) {
              buildingPolygonFeature = features.get(0);
              buildingPolygon = (Polygon) buildingPolygonFeature.geometry();
          }
        }
      }
    });
    return buildingPolygon;
  }

  private void addFootprintHighlightFillLayerToMap() {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer existingBuildingLayerId = style.getLayerAs(BUILDING_LAYER_ID);
        if (existingBuildingLayerId != null) {
          GeoJsonSource buildingFootprintGeoJsonSource = new GeoJsonSource(BUILDING_FOOTPRINT_SOURCE_ID);
          style.addSource(buildingFootprintGeoJsonSource);
          FillLayer buildingFillLayer = new FillLayer(BUILDING_FOOTPRINT_LAYER_ID,
              BUILDING_FOOTPRINT_SOURCE_ID);
          buildingFillLayer.setProperties(
            fillColor(color == null ? DEFAULT_FOOTPRINT_COLOR :
              color),
            fillOpacity(opacity == null ? DEFAULT_BUILDING_FOOTPRINT_OPACITY :
              opacity)
          );
          style.addLayerAbove(buildingFillLayer, BUILDING_LAYER_ID);
        }
      }
    });
  }
}
