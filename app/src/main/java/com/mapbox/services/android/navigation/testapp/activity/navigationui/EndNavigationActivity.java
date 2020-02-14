package com.mapbox.services.android.navigation.testapp.activity.navigationui;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.navigation.base.extensions.MapboxRouteOptionsUtils;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.ui.NavigationView;
import com.mapbox.navigation.ui.NavigationViewOptions;
import com.mapbox.navigation.ui.OnNavigationReadyCallback;
import com.mapbox.navigation.ui.listeners.NavigationListener;
import com.mapbox.services.android.navigation.testapp.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Response;

import static com.mapbox.navigation.base.internal.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE;

public class EndNavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback, NavigationListener,
        RoutesObserver, RouteProgressObserver {

  private NavigationView navigationView;
  private MapboxNavigation mapboxNavigation;
  private ProgressBar loading;
  private TextView message;
  private FloatingActionButton launchNavigationFab;
  private Point origin = Point.fromLngLat(-122.423579, 37.761689);
  private Point pickup = Point.fromLngLat(-122.424467, 37.761027);
  private Point middlePickup = Point.fromLngLat(-122.428604, 37.763559);
  private Point destination = Point.fromLngLat(-122.426183, 37.760872);
  private DirectionsRoute route;
  private boolean paellaPickedUp = false;
  private Marker paella;
  private boolean isNavigationRunning;
  private ConstraintLayout endNavigationLayout;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setTheme(R.style.Theme_AppCompat_NoActionBar);
    super.onCreate(savedInstanceState);
    initializeViews(savedInstanceState);
    navigationView.initialize(this);
    launchNavigationFab.setOnClickListener(v -> launchNavigation());
    mapboxNavigation = new MapboxNavigation(getApplicationContext(), Mapbox.getAccessToken());
    mapboxNavigation.registerRoutesObserver(this);
  }

  @Override
  public void onNavigationReady(boolean isRunning) {
    isNavigationRunning = isRunning;
    fetchRoute();
  }

  @Override
  public void onRoutesChanged(@NotNull List<? extends DirectionsRoute> routes) {
    updateLoadingTo(false);
    message.setText("Launch Navigation");
    launchNavigationFab.setVisibility(View.VISIBLE);
    launchNavigationFab.show();
    route = routes.get(0);
    if (isNavigationRunning) {
      launchNavigation();
    }
  }

  @Override
  public void onRouteProgressChanged(@NotNull RouteProgress routeProgress) {
    boolean isCurrentStepArrival = routeProgress.currentLegProgress().currentStepProgress().step().maneuver().type()
            .contains(STEP_MANEUVER_TYPE_ARRIVE);

    if (isCurrentStepArrival && !paellaPickedUp) {
      updateUiDelivering();
    } else if (isCurrentStepArrival && paellaPickedUp) {
      updateUiDelivered();
    }
  }

  @Override
  public void onCancelNavigation() {
    navigationView.stopNavigation();
    updateUiNavigationFinished();
  }

  @Override
  public void onNavigationFinished() {
  }

  @Override
  public void onNavigationRunning() {
  }

  private void fetchRoute() {
    ArrayList<Point> coordinates = new ArrayList<>();
    coordinates.add(origin);
    coordinates.addAll(Arrays.asList(middlePickup, destination));
    coordinates.add(destination);

    mapboxNavigation.requestRoutes(MapboxRouteOptionsUtils.applyDefaultParams(RouteOptions.builder())
            .accessToken(getString(R.string.mapbox_access_token))
            .coordinates(coordinates)
            .alternatives(true)
            .build());
    updateLoadingTo(true);
  }

  private void launchNavigation() {
    launchNavigationFab.hide();
    drawPaella();
    navigationView.setVisibility(View.VISIBLE);
    int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
    message.getLayoutParams().height = height;
    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(endNavigationLayout);
    constraintSet.connect(R.id.message, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
    constraintSet.connect(R.id.message, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
    constraintSet.connect(R.id.message, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
    constraintSet.applyTo(endNavigationLayout);
    NavigationViewOptions.Builder options = NavigationViewOptions.builder()
            .navigationListener(this)
            .routeProgressObserver(this)
            .directionsRoute(route)
            .shouldSimulateRoute(true);
    navigationView.startNavigation(options.build());
    updateUiPickingUp();
  }

  private void drawPaella() {
    Icon paellaIcon = IconFactory.getInstance(this).fromResource(R.drawable.paella_icon);
    paella = navigationView.retrieveNavigationMapboxMap().retrieveMap().addMarker(new MarkerOptions()
            .position(new LatLng(37.760615, -122.424306))
            .icon(paellaIcon)
    );
  }

  private void initializeViews(@Nullable Bundle savedInstanceState) {
    setContentView(R.layout.activity_end_navigation);
    endNavigationLayout = findViewById(R.id.endNavigationLayout);
    navigationView = findViewById(R.id.navigationView);
    loading = findViewById(R.id.loading);
    message = findViewById(R.id.message);
    launchNavigationFab = findViewById(R.id.launchNavigation);
    navigationView.onCreate(savedInstanceState);
  }

  private void updateLoadingTo(boolean isVisible) {
    if (isVisible) {
      loading.setVisibility(View.VISIBLE);
    } else {
      loading.setVisibility(View.INVISIBLE);
    }
  }

  private boolean validRouteResponse(Response<DirectionsResponse> response) {
    return response.body() != null && !response.body().routes().isEmpty();
  }

  private void updateUiPickingUp() {
    message.setText("Picking the paella up...");
  }

  private void updateUiDelivering() {
    paellaPickedUp = true;
    message.setText("Delivering...");
  }

  private void updateUiDelivered() {
    message.setText("Delicious paella delivered!");
  }

  private void updateUiNavigationFinished() {
    navigationView.retrieveNavigationMapboxMap().retrieveMap().removeMarker(paella);
    navigationView.setVisibility(View.GONE);
    message.setText("Launch Navigation");
    message.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT));
    launchNavigationFab.show();
  }

  @Override
  public void onStart() {
    super.onStart();
    navigationView.onStart();
  }

  @Override
  public void onResume() {
    super.onResume();
    navigationView.onResume();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    navigationView.onLowMemory();
  }

  @Override
  public void onBackPressed() {
    if (!navigationView.onBackPressed()) {
      super.onBackPressed();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    navigationView.onSaveInstanceState(outState);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    navigationView.onRestoreInstanceState(savedInstanceState);
  }

  @Override
  public void onPause() {
    super.onPause();
    navigationView.onPause();
  }

  @Override
  public void onStop() {
    super.onStop();
    navigationView.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    navigationView.onDestroy();
    mapboxNavigation.unregisterRoutesObserver(this);
  }
}