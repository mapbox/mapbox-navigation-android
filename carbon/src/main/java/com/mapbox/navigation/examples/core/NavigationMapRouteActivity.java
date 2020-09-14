package com.mapbox.navigation.examples.core;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.mapbox.maps.MapLoadError;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.navigation.core.replay.MapboxReplayer;
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

public class NavigationMapRouteActivity extends AppCompatActivity {

  MapView mapView;
  private MapboxMap mapboxMap;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_map_route);
    mapView = findViewById(R.id.mapView);
    mapboxMap = mapView.getMapboxMap();
    initStyle();
  }

  public void initStyle() {
    mapboxMap.loadStyleUri(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
      @Override public void onStyleLoaded(@NotNull Style style) {

      }
    }, new MapboxMap.OnMapLoadErrorListener() {
      @Override public void onMapLoadError(@NotNull MapLoadError mapLoadError, @NotNull String s) {
        Timber.e("Error loading map: " + mapLoadError.name());
      }
    });
  }

  @Override protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }
}
