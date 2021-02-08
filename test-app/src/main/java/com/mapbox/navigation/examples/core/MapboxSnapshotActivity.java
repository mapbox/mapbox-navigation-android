package com.mapbox.navigation.examples.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.api.directions.v5.models.BannerView;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.EdgeInsets;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin;
import com.mapbox.maps.plugin.animation.CameraAnimationsPluginImplKt;
import com.mapbox.maps.plugin.animation.MapAnimationOptions;
import com.mapbox.maps.plugin.gestures.GesturesPluginImpl;
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener;
import com.mapbox.maps.plugin.location.LocationComponentActivationOptions;
import com.mapbox.maps.plugin.location.LocationPluginImpl;
import com.mapbox.maps.plugin.location.LocationUpdate;
import com.mapbox.maps.plugin.location.modes.RenderMode;
import com.mapbox.navigation.base.internal.route.RouteUrl;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback;
import com.mapbox.navigation.core.replay.MapboxReplayer;
import com.mapbox.navigation.core.replay.ReplayLocationEngine;
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.examples.util.Slackline;
import com.mapbox.navigation.examples.util.Utils;
import com.mapbox.navigation.ui.base.api.snapshotter.SnapshotterApi;
import com.mapbox.navigation.ui.base.model.snapshotter.SnapshotState;
import com.mapbox.navigation.ui.maps.snapshotter.api.MapboxSnapshotterApi;
import com.mapbox.navigation.ui.base.api.snapshotter.SnapshotReadyCallback;
import com.mapbox.navigation.ui.maps.snapshotter.model.MapboxSnapshotterOptions;
import com.mapbox.navigation.ui.maps.snapshotter.view.MapboxSnapshotView;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Note:
 * 1. In order to run this example, your default dev token needs to be coming from mapbox-map-design account
 * 2. This example requests route from staging server > "https://api-valhalla-route-staging.tilestream.net/"
 *    Hence, in order to get {@link BannerView} in directions response you would need a special staging token different
 *    from regular public token used to draw the main map.
 */
public class MapboxSnapshotActivity extends AppCompatActivity implements OnMapLongClickListener {

  private static final int ONE_HUNDRED_MILLISECONDS = 100;
  private MapView mapView;
  private MapboxMap mapboxMap;
  private LocationPluginImpl locationComponent;
  private CameraAnimationsPlugin mapCamera;
  private MapboxReplayer mapboxReplayer = new MapboxReplayer();
  private MapboxNavigation mapboxNavigation;
  private Button startNavigation;
  private Slackline slackline = new Slackline(this);
  private SnapshotterApi snapshotterApi;
  private MapboxSnapshotView snapshotView;

  private SnapshotReadyCallback callback = new SnapshotReadyCallback() {
    @Override public void onSnapshotReady(@NotNull SnapshotState.SnapshotReady bitmap) {
      snapshotView.render(bitmap);
    }

    @Override public void onFailure(@NotNull SnapshotState.SnapshotFailure error) {
      snapshotView.render(error);
    }
  };

  @SuppressLint("MissingPermission")
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.layout_activity_snapshot);
    mapView = findViewById(R.id.mapView);
    startNavigation = findViewById(R.id.startNavigation);
    snapshotView = findViewById(R.id.snapshotView);
    mapboxMap = mapView.getMapboxMap();
    locationComponent = getLocationComponent();
    mapCamera = getMapCamera();
    init();
  }

  private void init() {
    initNavigation();
    initStyle();
    slackline.initialize(mapView, mapboxNavigation);
    initListeners();
  }

  @SuppressLint("MissingPermission")
  private void initListeners() {
    startNavigation.setOnClickListener(v -> {
      locationComponent.setRenderMode(RenderMode.GPS);
      mapboxNavigation.startTripSession();
      startNavigation.setVisibility(View.GONE);
    });
  }

  @SuppressLint("MissingPermission")
  private void initNavigation() {
    mapboxNavigation = new MapboxNavigation(
            new NavigationOptions.Builder(this)
                    .accessToken(getMapboxAccessTokenFromResources())
                    .locationEngine(new ReplayLocationEngine(mapboxReplayer))
                    .build()
    );

    float density = getResources().getDisplayMetrics().density;
    MapboxSnapshotterOptions options = new MapboxSnapshotterOptions.Builder(getApplicationContext())
      .edgeInsets(new EdgeInsets(80.0 * density, 0.0 * density, 0.0 * density, 250.0 * density))
      .styleUri("mapbox://styles/mapbox-map-design/ckifcx2i84huf19pbvgi0cka6")
      .build();

    snapshotterApi = new MapboxSnapshotterApi(this, mapboxMap, options, mapView);

    mapboxReplayer.pushRealLocation(this, 0.0);
    mapboxReplayer.play();
  }

  @SuppressLint("MissingPermission")
  private void initStyle() {
    mapboxMap.loadStyleUri(Style.MAPBOX_STREETS, style -> {
      initializeLocationComponent(style);
      mapboxNavigation.getNavigationOptions().getLocationEngine().getLastLocation(locationEngineCallback);
      getGesturePlugin().addOnMapLongClickListener(this);
    }, (mapLoadError, s) -> { }
    );
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
    if (mapboxNavigation != null) {
      mapboxNavigation.registerLocationObserver(locationObserver);
      mapboxNavigation.registerRouteProgressObserver(replayProgressObserver);
      mapboxNavigation.registerRouteProgressObserver(routeProgressObserver);
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (mapboxNavigation != null) {
      mapboxNavigation.unregisterLocationObserver(locationObserver);
      mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver);
      mapboxNavigation.unregisterRouteProgressObserver(replayProgressObserver);
    }
    mapView.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
    mapboxNavigation.onDestroy();
    snapshotterApi.cancel();
  }

  @Override public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  public boolean onMapLongClick(@NotNull Point point) {
    vibrate();
    startNavigation.setVisibility(View.GONE);

    Location currentLocation = getLocationComponent().getLastKnownLocation();
    if (currentLocation != null) {
      Point or = Point.fromLngLat(-3.5870, 40.5719);
      Point de = Point.fromLngLat(-3.607835, 40.551486);
      findRoute(or, de);
    }
    return false;
  }

  public void findRoute(Point origin, Point destination) {
    RouteOptions routeOptions = RouteOptions.builder()
        .baseUrl("https://api-valhalla-route-staging.tilestream.net/")
        .user("directions-team")
        .profile(RouteUrl.PROFILE_DRIVING_TRAFFIC)
        .geometries(RouteUrl.GEOMETRY_POLYLINE6)
        .requestUuid("")
        .accessToken(Objects.requireNonNull(getMapboxRouteAccessToken(this)))
        .coordinates(Arrays.asList(origin, destination))
        .alternatives(true)
        .build();
    mapboxNavigation.requestRoutes(
        routeOptions,
        routesReqCallback
    );
  }

  private RoutesRequestCallback routesReqCallback = new RoutesRequestCallback() {
    @Override
    public void onRoutesReady(@NotNull List<? extends DirectionsRoute> routes) {
      if (!routes.isEmpty()) {
        startNavigation.setVisibility(View.VISIBLE);
      }
    }

    @Override
    public void onRoutesRequestFailure(@NotNull Throwable throwable, @NotNull RouteOptions routeOptions) {

    }

    @Override
    public void onRoutesRequestCanceled(@NotNull RouteOptions routeOptions) {

    }
  };

  @SuppressLint("MissingPermission")
  private void vibrate() {
    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    if (vibrator == null) {
      return;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vibrator.vibrate(VibrationEffect.createOneShot(ONE_HUNDRED_MILLISECONDS, VibrationEffect.DEFAULT_AMPLITUDE));
    } else {
      vibrator.vibrate(ONE_HUNDRED_MILLISECONDS);
    }
  }

  @SuppressWarnings("MissingPermission")
  private void initializeLocationComponent(Style style) {
    LocationComponentActivationOptions activationOptions = LocationComponentActivationOptions.builder(this, style)
        .useDefaultLocationEngine(false)
        .build();
    locationComponent.activateLocationComponent(activationOptions);
    locationComponent.setEnabled(true);
    locationComponent.setRenderMode(RenderMode.COMPASS);
  }

  private String getMapboxAccessTokenFromResources() {
    return Utils.INSTANCE.getMapboxAccessToken(this);
  }

  private LocationObserver locationObserver = new LocationObserver() {
    @Override
    public void onRawLocationChanged(@NotNull Location rawLocation) {

    }

    @Override
    public void onEnhancedLocationChanged(
        @NotNull Location enhancedLocation,
        @NotNull List<? extends Location> keyPoints
    ) {
      if (keyPoints.isEmpty()) {
        updateLocation(enhancedLocation);
      } else {
        updateLocation((List<Location>) keyPoints);
      }
    }
  };

  private void updateLocation(Location location) {
    updateLocation(Arrays.asList(location));
  }

  private void updateLocation(List<Location> locations) {
    Location location = locations.get(0);
    getLocationComponent().forceLocationUpdate(new LocationUpdate(location, null, null));

    MapAnimationOptions.Builder mapAnimationOptionsBuilder = new MapAnimationOptions.Builder();
    mapAnimationOptionsBuilder.setDuration(1500L);
    mapCamera.easeTo(
        new CameraOptions.Builder()
            .center(Point.fromLngLat(location.getLongitude(), location.getLatitude()))
            .bearing((double) location.getBearing())
            .pitch(45.0)
            .zoom(17.0)
            .padding(new EdgeInsets(1000, 0, 0, 0))
            .build(),
        mapAnimationOptionsBuilder.build()
    );
  }

  private LocationPluginImpl getLocationComponent() {
    return mapView.getPlugin(LocationPluginImpl.class);
  }

  private CameraAnimationsPlugin getMapCamera() {
    return CameraAnimationsPluginImplKt.getCameraAnimationsPlugin(mapView);
  }

  private GesturesPluginImpl getGesturePlugin() {
    return mapView.getPlugin(GesturesPluginImpl.class);
  }

  private ReplayProgressObserver replayProgressObserver = new ReplayProgressObserver(mapboxReplayer);

  private RouteProgressObserver routeProgressObserver = new RouteProgressObserver() {
    @Override public void onRouteProgressChanged(@NotNull RouteProgress routeProgress) {
      snapshotterApi.generateSnapshot(routeProgress, callback);
    }
  };

  private MyLocationEngineCallback locationEngineCallback = new MyLocationEngineCallback(this);

  private static class MyLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {

    private WeakReference<MapboxSnapshotActivity> activityRef;

    MyLocationEngineCallback(MapboxSnapshotActivity activity) {
      this.activityRef = new WeakReference<>(activity);
    }

    @Override
    public void onSuccess(LocationEngineResult result) {
      Location location = result.getLastLocation();
      MapboxSnapshotActivity activity = activityRef.get();
      if (location != null && activity != null) {
        Point point = Point.fromLngLat(location.getLongitude(), location.getLatitude());
        CameraOptions cameraOptions = new CameraOptions.Builder().center(point).zoom(13.0).build();
        activity.mapboxMap.jumpTo(cameraOptions);
        activity.locationComponent.forceLocationUpdate(location);
      }
    }

    @Override
    public void onFailure(@NonNull Exception exception) {

    }
  }


  // TODO: Remove this as this token is specially needed to get {@link BannerView} in directions response
  //       from staging server.
  /**
   * <p>
   * Returns the Mapbox access token set in the app resources.
   * </p>
   *
   * @param context The {@link Context} of the {@link android.app.Activity} or {@link android.app.Fragment}.
   * @return The Mapbox access token or null if not found.
   */
  private String getMapboxRouteAccessToken(@NonNull Context context) {
    int tokenResId = context.getResources()
        .getIdentifier("mapbox_route_token", "string", context.getPackageName());
    if (tokenResId != 0) {
      return context.getString(tokenResId);
    } else {
      throw new RuntimeException("mapbox_route_token needed (see code comments for details)");
    }
  }
}
