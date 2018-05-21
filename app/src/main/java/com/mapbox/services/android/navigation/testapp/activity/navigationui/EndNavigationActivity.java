package com.mapbox.services.android.navigation.testapp.activity.navigationui;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE;

public class EndNavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback, NavigationListener,
  Callback<DirectionsResponse>, ProgressChangeListener {

  private NavigationView navigationView;
  private ProgressBar loading;
  private TextView message;
  private FloatingActionButton launchNavigationFab;
  private Point origin = Point.fromLngLat(-122.423579, 37.761689);
  private Point pickup = Point.fromLngLat(-122.424467, 37.761027);
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
  }

  @Override
  public void onNavigationReady(boolean isRunning) {
    isNavigationRunning = isRunning;
    fetchRoute();
  }

  @Override
  public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
    if (validRouteResponse(response)) {
      updateLoadingTo(false);
      message.setText("Launch Navigation");
      launchNavigationFab.setVisibility(View.VISIBLE);
      launchNavigationFab.show();
      route = response.body().routes().get(0);
      if (isNavigationRunning) {
        launchNavigation();
      }
    }
  }

  @Override
  public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    boolean isCurrentStepArrival = routeProgress.currentLegProgress().currentStep().maneuver().type()
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
    NavigationRoute builder = NavigationRoute.builder(this)
      .accessToken(getString(R.string.mapbox_access_token))
      .origin(origin)
      .addWaypoint(pickup)
      .destination(destination)
      .alternatives(true)
      .build();
    builder.getRoute(this);
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
      .progressChangeListener(this)
      .directionsRoute(route)
      .shouldSimulateRoute(true);
    navigationView.startNavigation(options.build());
    updateUiPickingUp();
  }

  private void drawPaella() {
    Icon paellaIcon = IconFactory.getInstance(this).fromResource(R.drawable.paella_icon);
    paella = navigationView.retrieveMapboxMap().addMarker(new MarkerOptions()
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
    navigationView.retrieveMapboxMap().removeMarker(paella);
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
  }
}