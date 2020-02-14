package com.mapbox.services.android.navigation.testapp.activity.navigationui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
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

import java.util.Arrays;
import java.util.List;

public class NavigationFragment extends Fragment implements OnNavigationReadyCallback, NavigationListener,
        RouteProgressObserver, RoutesObserver {

  private static final double ORIGIN_LONGITUDE = -3.714873;
  private static final double ORIGIN_LATITUDE = 40.397389;
  private static final double DESTINATION_LONGITUDE = -3.712331;
  private static final double DESTINATION_LATITUDE = 40.401686;

  private MapboxNavigation mapboxNavigation;
  private NavigationView navigationView;
  private DirectionsRoute directionsRoute;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.navigation_view_fragment_layout, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    updateNightMode();
    navigationView = view.findViewById(R.id.navigation_view_fragment);
    navigationView.onCreate(savedInstanceState);
    navigationView.initialize(this);
    mapboxNavigation = new MapboxNavigation(view.getContext().getApplicationContext(), Mapbox.getAccessToken());
    mapboxNavigation.registerRoutesObserver(this);
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
  public void onSaveInstanceState(@NonNull Bundle outState) {
    navigationView.onSaveInstanceState(outState);
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    if (savedInstanceState != null) {
      navigationView.onRestoreInstanceState(savedInstanceState);
    }
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
  public void onLowMemory() {
    super.onLowMemory();
    navigationView.onLowMemory();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    navigationView.onDestroy();
    mapboxNavigation.unregisterRoutesObserver(this);
  }

  @Override
  public void onNavigationReady(boolean isRunning) {
    Point origin = Point.fromLngLat(ORIGIN_LONGITUDE, ORIGIN_LATITUDE);
    Point destination = Point.fromLngLat(DESTINATION_LONGITUDE, DESTINATION_LATITUDE);
    fetchRoute(origin, destination);
  }

  @Override
  public void onCancelNavigation() {
    navigationView.stopNavigation();
    stopNavigation();
  }

  @Override
  public void onNavigationFinished() {
    // no-op
  }

  @Override
  public void onNavigationRunning() {
    // no-op
  }

  @Override
  public void onRouteProgressChanged(@NotNull RouteProgress routeProgress) {
    boolean isInTunnel = routeProgress.inTunnel();
    boolean wasInTunnel = wasInTunnel();
    if (isInTunnel) {
      if (!wasInTunnel) {
        updateWasInTunnel(true);
        updateCurrentNightMode(AppCompatDelegate.MODE_NIGHT_YES);
      }
    } else {
      if (wasInTunnel) {
        updateWasInTunnel(false);
        updateCurrentNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
      }
    }
  }

  /*
    RouteObserver
   */
  @Override
  public void onRoutesChanged(@NotNull List<? extends DirectionsRoute> routes) {
    directionsRoute = routes.get(0);
    startNavigation();
  }

  /*
    RouteObserver end
   */

  private void updateNightMode() {
    if (wasNavigationStopped()) {
      updateWasNavigationStopped(false);
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
      getActivity().recreate();
    }
  }

  private void fetchRoute(Point origin, Point destination) {
    mapboxNavigation.requestRoutes(RouteOptions.builder()
            .accessToken(Mapbox.getAccessToken())
            .coordinates(Arrays.asList(origin, destination))
            .build());
  }

  private void startNavigation() {
    if (directionsRoute == null) {
      return;
    }
    NavigationViewOptions options = NavigationViewOptions.builder()
            .directionsRoute(directionsRoute)
            .shouldSimulateRoute(true)
            .navigationListener(NavigationFragment.this)
            .routeProgressObserver(this)
            .build();
    navigationView.startNavigation(options);
  }

  private void stopNavigation() {
    FragmentActivity activity = getActivity();
    if (activity != null && activity instanceof FragmentNavigationActivity) {
      FragmentNavigationActivity fragmentNavigationActivity = (FragmentNavigationActivity) activity;
      fragmentNavigationActivity.showPlaceholderFragment();
      fragmentNavigationActivity.showNavigationFab();
      updateWasNavigationStopped(true);
      updateWasInTunnel(false);
    }
  }

  private boolean wasInTunnel() {
    Context context = getActivity();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    return preferences.getBoolean(context.getString(R.string.was_in_tunnel), false);
  }

  private void updateWasInTunnel(boolean wasInTunnel) {
    Context context = getActivity();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putBoolean(context.getString(R.string.was_in_tunnel), wasInTunnel);
    editor.apply();
  }

  private void updateCurrentNightMode(int nightMode) {
    AppCompatDelegate.setDefaultNightMode(nightMode);
    getActivity().recreate();
  }

  private boolean wasNavigationStopped() {
    Context context = getActivity();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    return preferences.getBoolean(getString(R.string.was_navigation_stopped), false);
  }

  public void updateWasNavigationStopped(boolean wasNavigationStopped) {
    Context context = getActivity();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putBoolean(getString(R.string.was_navigation_stopped), wasNavigationStopped);
    editor.apply();
  }
}
