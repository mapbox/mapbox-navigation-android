package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior.BottomSheetCallback;

import com.google.auto.value.AutoValue;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.listeners.FeedbackListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;

@AutoValue
public abstract class NavigationViewOptions {

  @Nullable
  public abstract DirectionsRoute directionsRoute();

  @Nullable
  public abstract String directionsProfile();

  @Nullable
  public abstract Point origin();

  @Nullable
  public abstract Point destination();

  @Nullable
  public abstract String awsPoolId();

  public abstract MapboxNavigationOptions navigationOptions();

  public abstract boolean shouldSimulateRoute();

  @Nullable
  public abstract Integer lightThemeResId();

  @Nullable
  public abstract Integer darkThemeResId();

  @Nullable
  public abstract FeedbackListener feedbackListener();

  @Nullable
  public abstract RouteListener routeListener();

  @Nullable
  public abstract NavigationListener navigationListener();

  @Nullable
  public abstract ProgressChangeListener progressChangeListener();

  @Nullable
  public abstract MilestoneEventListener milestoneEventListener();

  @Nullable
  public abstract BottomSheetCallback bottomSheetCallback();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder directionsRoute(DirectionsRoute directionsRoute);

    public abstract Builder directionsProfile(@DirectionsCriteria.ProfileCriteria String directionsProfile);

    public abstract Builder origin(Point origin);

    public abstract Builder destination(Point destination);

    public abstract Builder awsPoolId(String awsPoolId);

    public abstract Builder navigationOptions(MapboxNavigationOptions navigationOptions);

    public abstract Builder shouldSimulateRoute(boolean shouldSimulateRoute);

    public abstract Builder lightThemeResId(Integer lightThemeResId);

    public abstract Builder darkThemeResId(Integer darkThemeResId);

    public abstract Builder feedbackListener(FeedbackListener feedbackListener);

    public abstract Builder routeListener(RouteListener routeListener);

    public abstract Builder navigationListener(NavigationListener navigationListener);

    public abstract Builder progressChangeListener(ProgressChangeListener progressChangeListener);

    public abstract Builder milestoneEventListener(MilestoneEventListener milestoneEventListener);

    public abstract Builder bottomSheetCallback(BottomSheetCallback bottomSheetCallback);

    public abstract NavigationViewOptions build();
  }

  public static Builder builder() {
    return new AutoValue_NavigationViewOptions.Builder()
      .awsPoolId(null)
      .navigationOptions(MapboxNavigationOptions.builder().build())
      .shouldSimulateRoute(false);
  }
}