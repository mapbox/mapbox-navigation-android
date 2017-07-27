package com.mapbox.services.android.navigation.v5.navigation;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.services.android.navigation.v5.listeners.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class MapboxNavigation implements ServiceConnection {

  private List<Milestone> milestones;

  private NavigationEventDispatcher navigationEventDispatcher;
  private NavigationService navigationService;
  private DirectionsRoute directionsRoute;
  private MapboxNavigationOptions options;
  private LocationEngine locationEngine;
  private Context context;
  private boolean isBound;

  public MapboxNavigation(@NonNull Context context) {
    this(context, new MapboxNavigationOptions());
  }

  public MapboxNavigation(@NonNull Context context, @NonNull MapboxNavigationOptions options) {
    this.context = context;
    this.options = options;
    initialize();
  }

  private void initialize() {
    milestones = new ArrayList<>();
    navigationEventDispatcher = new NavigationEventDispatcher();
  }

  // Lifecycle

  public void onDestroy() {
    Timber.d("MapboxNavigation onDestroy.");
    if (isBound) {
      navigationService.onDestroy();
    }
  }

  // Public APIs

  public void addMilestone(@NonNull Milestone milestone) {
    milestones.add(milestone);
    invalidateMilestones();
  }

  public void removeMilestone(@Nullable Milestone milestone) {
    if (milestone == null) {
      milestones.clear();
    } else {
      milestones.remove(milestone);
    }
    invalidateMilestones();
  }

  public void setLocationEngine(LocationEngine locationEngine) {
    this.locationEngine = locationEngine;
  }

  public LocationEngine getLocationEngine() {
    return locationEngine;
  }

  public MapboxNavigationOptions getMapboxNavigationOptions() {
    return options;
  }

  public void startNavigation(DirectionsRoute route) {
    this.directionsRoute = route;
    Timber.d("MapboxNavigation startNavigation called.");
    if (!isBound) {
      Intent intent = getServiceIntent();
      context.startService(intent);
      context.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }
  }

  public void endNavigation() {
    Timber.d("MapboxNavigation endNavigation called");
    if (isBound) {
      context.unbindService(this);
      navigationService.onDestroy();
      isBound = false;
    }
  }

  // Listeners

  public void addMilestoneEventListener(@NonNull MilestoneEventListener milestoneEventListener) {
    navigationEventDispatcher.addMilestoneEventListener(milestoneEventListener);
  }

  public void removeMilestoneEventListener(@Nullable MilestoneEventListener milestoneEventListener) {
    navigationEventDispatcher.removeMilestoneEventListener(milestoneEventListener);
  }

  public void addProgressChangeListener(@NonNull ProgressChangeListener progressChangeListener) {
    navigationEventDispatcher.addProgressChangeListener(progressChangeListener);
  }

  public void removeProgressChangeListener(@Nullable ProgressChangeListener progressChangeListener) {
    navigationEventDispatcher.removeProgressChangeListener(progressChangeListener);
  }

  public void addOffRouteListener(@NonNull OffRouteListener offRouteListener) {
    navigationEventDispatcher.addOffRouteListener(offRouteListener);
  }

  public void removeOffRouteListener(@Nullable OffRouteListener offRouteListener) {
    navigationEventDispatcher.removeOffRouteListener(offRouteListener);
  }

  public void addNavigationEventListener(@NonNull NavigationEventListener navigationEventListener) {
    navigationEventDispatcher.addNavigationEventListener(navigationEventListener);
  }

  public void removeNavigationEventListener(@Nullable NavigationEventListener navigationEventListener) {
    navigationEventDispatcher.removeNavigationEventListener(navigationEventListener);
  }

  // TODO API for custom logic such as snapping or offroute

  // Service

  private void invalidateMilestones() {
    if (isBound) {
      navigationService.setMilestones(milestones);
    }
  }

  private Intent getServiceIntent() {
    return new Intent(context, NavigationService.class);
  }

  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    Timber.d("Connected to service.");
    NavigationService.LocalBinder binder = (NavigationService.LocalBinder) service;
    NavigationService navigationService = binder.getService();
    navigationService.setNavigationEventDispatcher(navigationEventDispatcher);
    navigationService.setMilestones(milestones);
    isBound = true;
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
    Timber.d("Disconnected from service.");
    navigationService = null;
    isBound = false;
  }
}
