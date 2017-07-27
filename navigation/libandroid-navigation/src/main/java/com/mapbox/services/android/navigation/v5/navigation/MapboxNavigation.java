package com.mapbox.services.android.navigation.v5.navigation;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.services.android.navigation.v5.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;

import java.util.ArrayList;
import java.util.List;

public class MapboxNavigation {

  private List<Milestone> milestones;

  private NavigationEventDispatcher navigationEventDispatcher;
  private NavigationService navigationService;
  private Context context;
  private boolean bound;

  public MapboxNavigation(@NonNull Context context) {
    this(context, new MapboxNavigationOptions());
  }

  public MapboxNavigation(@NonNull Context context, @NonNull MapboxNavigationOptions options) {
    this.context = context;
    initialize();
  }

  private void initialize() {
    milestones = new ArrayList<>();
    navigationEventDispatcher = new NavigationEventDispatcher();
  }

  public void addMilestone(@NonNull Milestone milestone) {
    milestones.add(milestone);
    if (bound) {
//      navigationService.set
    }
  }

  public void removeMilestone(@Nullable Milestone milestone) {
    if (milestone == null) {
      milestones.clear();
    } else {
      milestones.remove(milestone);
    }
  }

  // Listeners

  public void addMilestoneEventListener(@NonNull MilestoneEventListener milestoneEventListener) {
    navigationEventDispatcher.addMilestoneEventListener(milestoneEventListener);
  }

  public void removeMilestoneEventListener(@Nullable MilestoneEventListener milestoneEventListener) {
    navigationEventDispatcher.removeMilestoneEventListener(milestoneEventListener);
  }

  // TODO API for custom logic such as snapping or offroute


  public void startNavigation() {
    context.startService(new Intent(context, NavigationService.class));
    context.bindService(new Intent(context, NavigationService.class), connection, Context.BIND_AUTO_CREATE);
  }

  public void stop() {
    if (bound) {
      context.unbindService(connection);
      context.stopService(new Intent(context, NavigationService.class));
      bound = false;
    }
  }

  private void invalidateMilestones() {
    if (bound) {
      navigationService.setMilestones(milestones);
    }
  }

  private final ServiceConnection connection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
      NavigationService.LocalBinder binder = (NavigationService.LocalBinder) service;
      NavigationService navigationService = binder.getService();
      navigationService.setNavigationEventDispatcher(navigationEventDispatcher);
      navigationService.setMilestones(milestones);
      bound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      navigationService = null;
      bound = false;
    }
  };


}
