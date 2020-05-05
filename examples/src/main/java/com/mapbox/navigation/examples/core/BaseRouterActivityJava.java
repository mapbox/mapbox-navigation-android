package com.mapbox.navigation.examples.core;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.base.common.logger.model.Message;
import com.mapbox.base.common.logger.model.Tag;
import com.mapbox.common.logger.LogEntry;
import com.mapbox.common.logger.LoggerObserver;
import com.mapbox.common.logger.MapboxLogger;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.navigation.base.metrics.MetricsObserver;
import com.mapbox.navigation.base.options.MapboxOnboardRouterConfig;
import com.mapbox.navigation.base.route.Router;
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts;
import com.mapbox.navigation.examples.R;
import com.mapbox.navigation.examples.utils.Utils;
import com.mapbox.navigation.metrics.MapboxMetricsReporter;
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl;
import com.mapbox.navigation.route.hybrid.MapboxHybridRouter;
import com.mapbox.navigation.route.offboard.MapboxOffboardRouter;
import com.mapbox.navigation.route.onboard.MapboxOnboardRouter;
import com.mapbox.navigation.ui.route.NavigationMapRoute;
import com.mapbox.navigation.utils.internal.NetworkStatusService;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

import static com.google.android.material.snackbar.Snackbar.LENGTH_SHORT;
import static com.mapbox.mapboxsdk.log.Logger.DEBUG;
import static com.mapbox.mapboxsdk.log.Logger.ERROR;
import static com.mapbox.mapboxsdk.log.Logger.INFO;
import static com.mapbox.mapboxsdk.log.Logger.VERBOSE;
import static com.mapbox.mapboxsdk.log.Logger.WARN;
import static com.mapbox.navigation.base.internal.extensions.MapboxRouteOptionsUtils.applyDefaultParams;
import static com.mapbox.navigation.base.internal.extensions.MapboxRouteOptionsUtils.coordinates;
import static com.mapbox.navigation.base.route.internal.RouteUrl.PROFILE_DRIVING_TRAFFIC;

public abstract class BaseRouterActivityJava extends AppCompatActivity
    implements OnMapReadyCallback, MapboxMap.OnMapClickListener, MetricsObserver, LoggerObserver {

  public static final String MARKER_ROUTE = "marker.route";

  public static Router setupOffboardRouter(Context context) {
    return new MapboxOffboardRouter(
        Utils.getMapboxAccessToken(context),
        context,
        MapboxNavigationAccounts.getInstance(context));
  }

  public static Router setupOnboardRouter() {
    File file = new File(
        Environment.getExternalStoragePublicDirectory("Offline").getAbsolutePath(),
        "2019_04_13-00_00_11");
    File fileTiles = new File(file, "tiles");
    MapboxOnboardRouterConfig config = new MapboxOnboardRouterConfig(
        fileTiles.getAbsolutePath(),
        null,
        null,
        null,
        null // working with pre-fetched tiles only
    );

    return new MapboxOnboardRouter(MapboxNativeNavigatorImpl.INSTANCE, config, MapboxLogger.INSTANCE);
  }

  public static Router setupHybridRouter(Context applicationContext) {
    return new MapboxHybridRouter(
        setupOnboardRouter(),
        setupOffboardRouter(applicationContext),
        new NetworkStatusService(applicationContext)
    );
  }

  private Router router;
  private MapboxMap mapboxMap;

  private NavigationMapRoute navigationMapRoute;
  private Point origin;
  private Point destination;
  private Point waypoint;
  private SymbolManager symbolManager;

  @BindView(R.id.mapView)
  MapView mapView;

  abstract Router setupRouter();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_mock_navigation);
    ButterKnife.bind(this);

    MapboxLogger.INSTANCE.setLogLevel(VERBOSE);
    MapboxLogger.INSTANCE.setObserver(this);
    MapboxMetricsReporter.INSTANCE.setMetricsObserver(this);

    router = setupRouter();

    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @OnClick(R.id.newLocationFab)
  public void onNewLocationClick() {
    newOrigin();
  }

  private void newOrigin() {
    if (mapboxMap != null) {
      clearMap();
      LatLng latLng = Utils.getRandomLatLng(new double[] { -77.1825, 38.7825, -76.9790, 39.0157 });
      origin = Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude());
      mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
    }
  }

  @Override
  public void onMapReady(@NonNull MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    MapboxLogger.INSTANCE.d(new Message("Map is ready"));
    mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
      MapboxLogger.INSTANCE.d(new Message("Style setting finished"));
      Drawable image = ContextCompat.getDrawable(this, R.drawable.mapbox_marker_icon_default);
      style.addImage(MARKER_ROUTE, image);
      navigationMapRoute = new NavigationMapRoute(mapView, mapboxMap);
      symbolManager = new SymbolManager(mapView, mapboxMap, style);
      Snackbar.make(findViewById(R.id.container), R.string.msg_tap_map_to_place_waypoint, LENGTH_SHORT)
          .show();
      newOrigin();
      this.mapboxMap.addOnMapClickListener(this);
    });
  }

  private void clearMap() {
    symbolManager.deleteAll();
    destination = null;
    waypoint = null;
    navigationMapRoute.updateRouteVisibilityTo(false);
    navigationMapRoute.updateRouteArrowVisibilityTo(false);
  }

  private void findRoute() {
    if (origin != null && destination != null) {
      router.cancel();
      if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) > 50) {
        RouteOptions.Builder optionsBuilder = applyDefaultParams(RouteOptions.builder())
            .accessToken(Utils.getMapboxAccessToken(this))
            .profile(PROFILE_DRIVING_TRAFFIC)
            .annotations(
                DirectionsCriteria.ANNOTATION_CONGESTION + ","
                    + DirectionsCriteria.ANNOTATION_DISTANCE
                    + "," + DirectionsCriteria.ANNOTATION_DURATION
            );

        List<Point> waypoints = new ArrayList<>();
        waypoints.add(waypoint);

        coordinates(optionsBuilder, origin, waypoints, destination);

        router.getRoute(optionsBuilder.build(), new Router.Callback() {
          @Override
          public void onResponse(@NotNull List<? extends DirectionsRoute> routes) {
            MapboxLogger.INSTANCE.d(new Message("Router.Callback#onResponse"));
            if (!routes.isEmpty()) {
              navigationMapRoute.addRoute(routes.get(0));
            }
          }

          @Override
          public void onFailure(@NotNull Throwable throwable) {
            MapboxLogger.INSTANCE.e(new Message("Router.Callback#onFailure"), throwable);
          }

          @Override
          public void onCanceled() {
            MapboxLogger.INSTANCE.d(new Message("Router.Callback#onCanceled"));
          }
        });
      }
    }
  }

  @Override
  public boolean onMapClick(@NonNull LatLng point) {
    if (destination == null) {
      destination = Point.fromLngLat(point.getLongitude(), point.getLatitude());
      addMarker(destination);
      findRoute();
    } else if (waypoint == null) {
      waypoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
      addMarker(waypoint);
      findRoute();
    } else {
      Toast.makeText(this, R.string.only_two_waypoints_supported, Toast.LENGTH_LONG).show();
      clearMap();
    }
    return false;
  }

  private void addMarker(Point point) {
    symbolManager.create(
        new SymbolOptions()
            .withIconImage(MARKER_ROUTE)
            .withGeometry(point)
    );
  }

  /*
   * Activity lifecycle methods
   */

  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (router != null) {
      router.shutdown();
      router = null;
    }
    if (mapboxMap != null) {
      mapboxMap.removeOnMapClickListener(this);
    }
    mapView.onDestroy();
    MapboxLogger.INSTANCE.removeObserver();
    MapboxMetricsReporter.INSTANCE.removeObserver();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @Override
  public void onMetricUpdated(@NotNull String metricName, @NotNull String jsonStringData) {
    MapboxLogger.INSTANCE.d(new Tag("METRICS_LOG"), new Message(metricName));
    MapboxLogger.INSTANCE.d(new Tag("METRICS_LOG"), new Message(jsonStringData));
  }

  @Override
  public void log(int level, @NotNull LogEntry entry) {
    if (entry.getTag() != null) {
      Timber.tag(entry.getTag());
    }

    switch (level) {
      case VERBOSE:
        Timber.v(entry.getThrowable(), entry.getMessage());
        break;
      case DEBUG:
        Timber.d(entry.getThrowable(), entry.getMessage());
        break;
      case INFO:
        Timber.i(entry.getThrowable(), entry.getMessage());
        break;
      case WARN:
        Timber.w(entry.getThrowable(), entry.getMessage());
        break;
      case ERROR:
        Timber.e(entry.getThrowable(), entry.getMessage());
        break;
      default:
        break;
    }
  }
}
