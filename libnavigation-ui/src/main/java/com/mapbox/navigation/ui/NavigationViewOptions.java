package com.mapbox.navigation.ui;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.auto.value.AutoValue;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.ui.camera.Camera;
import com.mapbox.navigation.ui.listeners.BannerInstructionsListener;
import com.mapbox.navigation.ui.listeners.FeedbackListener;
import com.mapbox.navigation.ui.listeners.InstructionListListener;
import com.mapbox.navigation.ui.listeners.NavigationListener;
import com.mapbox.navigation.ui.listeners.RouteListener;
import com.mapbox.navigation.ui.listeners.SpeechAnnouncementListener;
import com.mapbox.navigation.ui.voice.SpeechPlayer;

@AutoValue
public abstract class NavigationViewOptions extends NavigationUiOptions {

  public abstract NavigationOptions navigationOptions();

  @Nullable
  public abstract FeedbackListener feedbackListener();

  @Nullable
  public abstract RouteListener routeListener();

  @Nullable
  public abstract NavigationListener navigationListener();

  @Nullable
  public abstract RouteProgressObserver routeProgressObserver();

  @Nullable
  public abstract LocationObserver locationObserver();

  @Nullable
  public abstract BottomSheetBehavior.BottomSheetCallback bottomSheetCallback();

  @Nullable
  public abstract InstructionListListener instructionListListener();

  @Nullable
  public abstract SpeechAnnouncementListener speechAnnouncementListener();

  @Nullable
  public abstract BannerInstructionsListener bannerInstructionsListener();

  @Nullable
  public abstract SpeechPlayer speechPlayer();

  @Nullable
  public abstract LocationEngine locationEngine();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder directionsRoute(DirectionsRoute directionsRoute);

    public abstract Builder lightThemeResId(Integer lightThemeResId);

    public abstract Builder darkThemeResId(Integer darkThemeResId);

    public abstract Builder shouldSimulateRoute(boolean shouldSimulateRoute);

    public abstract Builder waynameChipEnabled(boolean waynameChipEnabled);

    public abstract Builder navigationOptions(NavigationOptions navigationOptions);

    public abstract Builder feedbackListener(FeedbackListener feedbackListener);

    public abstract Builder routeListener(RouteListener routeListener);

    public abstract Builder navigationListener(NavigationListener navigationListener);

    public abstract Builder routeProgressObserver(RouteProgressObserver routeProgressObserver);

    public abstract Builder locationObserver(LocationObserver locationObserver);

    public abstract Builder bottomSheetCallback(BottomSheetBehavior.BottomSheetCallback bottomSheetCallback);

    public abstract Builder instructionListListener(InstructionListListener instructionListListener);

    public abstract Builder speechAnnouncementListener(SpeechAnnouncementListener speechAnnouncementListener);

    public abstract Builder bannerInstructionsListener(BannerInstructionsListener bannerInstructionsListener);

    public abstract Builder speechPlayer(SpeechPlayer speechPlayer);

    public abstract Builder locationEngine(LocationEngine locationEngine);

    /**
     * Add an offline path for loading offline routing data.
     * <p>
     * When added, the {@link NavigationView} will try to initialize and use this data
     * for offline routing when no or poor internet connection is found.
     *
     * @param offlinePath to offline data on device
     * @return this builder
     */
    public abstract Builder offlineRoutingTilesPath(String offlinePath);

    /**
     * Add an offline tile version.  When providing a routing tile path, this version
     * is also required for configuration.
     * <p>
     * This version should directly correspond to the data in the offline path also provided.
     *
     * @param offlineVersion of data in tile path
     * @return this builder
     */
    public abstract Builder offlineRoutingTilesVersion(String offlineVersion);

    /**
     * Add options to configure offline maps.
     *
     * @param mapOfflineOptions for offline configuration
     * @return this builder
     */
    public abstract Builder offlineMapOptions(MapOfflineOptions mapOfflineOptions);

    /**
     * Add Navigation Camera
     *
     * @param camera {@link Camera}
     * @return this builder
     */
    public abstract Builder camera(Camera camera);

    public abstract NavigationViewOptions build();
  }

  public static Builder builder() {
    return new AutoValue_NavigationViewOptions.Builder()
            .navigationOptions(new NavigationOptions.Builder().build())
            .shouldSimulateRoute(false)
            .waynameChipEnabled(true);
  }
}