package com.mapbox.navigation.ui;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.ViewModelProviders;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.navigation.base.TimeFormat;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.route.Router;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.replay.MapboxReplayer;
import com.mapbox.navigation.ui.camera.DynamicCamera;
import com.mapbox.navigation.ui.camera.NavigationCamera;
import com.mapbox.navigation.ui.instruction.InstructionView;
import com.mapbox.navigation.ui.instruction.NavigationAlertView;
import com.mapbox.navigation.ui.internal.NavigationContract;
import com.mapbox.navigation.ui.internal.ThemeSwitcher;
import com.mapbox.navigation.ui.internal.utils.ViewUtils;
import com.mapbox.navigation.ui.map.NavigationMapboxMap;
import com.mapbox.navigation.ui.map.WayNameView;
import com.mapbox.navigation.ui.puck.DefaultMapboxPuckDrawableSupplier;
import com.mapbox.navigation.ui.summary.SummaryBottomSheet;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * View that creates the drop-in UI.
 * <p>
 * Once started, this view will check if the {@link Activity} that inflated
 * it was launched with a {@link DirectionsRoute}.
 * <p>
 * Or, if not found, this view will look for a set of {@link Point} coordinates.
 * In the latter case, a new {@link DirectionsRoute} will be retrieved from {@link Router}.
 * <p>
 * Once valid data is obtained, this activity will immediately begin navigation
 * with {@link MapboxNavigation}.
 * <p>
 * If launched with the simulation boolean set to true, a {@link MapboxReplayer}
 * will be initialized and begin pushing updates.
 * <p>
 * This activity requires user permissions ACCESS_FINE_LOCATION
 * and ACCESS_COARSE_LOCATION have already been granted.
 * <p>
 * A Mapbox access token must also be set by the developer (to initialize navigation).
 */
public class NavigationView extends CoordinatorLayout implements LifecycleOwner, OnMapReadyCallback,
    NavigationContract.View {

  private static final int INVALID_STATE = 0;
  private static final int DEFAULT_PX_BETWEEN_BOTTOM_SHEET_LOGO_AND_ATTRIBUTION = 16;
  private static final long WAY_NAME_TRANSLATIONX_DURATION = 750L;
  private MapView mapView;
  private InstructionView instructionView;
  private SummaryBottomSheet summaryBottomSheet;
  private BottomSheetBehavior summaryBehavior;
  private ImageButton cancelBtn;
  private RecenterButton recenterBtn;
  private WayNameView wayNameView;
  private ImageButton routeOverviewBtn;

  private NavigationPresenter navigationPresenter;
  private NavigationViewEventDispatcher navigationViewEventDispatcher;
  private NavigationViewModel navigationViewModel;
  @Nullable
  private NavigationMapboxMap navigationMap;
  private NavigationOnCameraTrackingChangedListener onTrackingChangedListener;
  private Bundle mapInstanceState;
  private CameraPosition initialMapCameraPosition;
  private boolean isMapInitialized;
  private boolean isSubscribed;
  private boolean logoAndAttributionShownForFirstTime;
  private LifecycleRegistry lifecycleRegistry;
  @NonNull
  private Set<OnNavigationReadyCallback> onNavigationReadyCallbacks = new CopyOnWriteArraySet<>();

  public NavigationView(@NonNull Context context) {
    this(context, null);
  }

  public NavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public NavigationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    ThemeSwitcher.setTheme(context, attrs);
    initializeView();
    lifecycleRegistry = new LifecycleRegistry(this);
    onNavigationReadyCallbacks.add(internalNavigationReadyCallback);
  }

  /**
   * Uses savedInstanceState as a cue to restore state (if not null).
   *
   * @param savedInstanceState to restore state if not null
   */
  public void onCreate(@Nullable Bundle savedInstanceState) {
    mapView.onCreate(savedInstanceState);
    updatePresenterState(savedInstanceState);
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
  }

  /**
   * Low memory must be reported so the {@link MapView}
   * can react appropriately.
   */
  public void onLowMemory() {
    mapView.onLowMemory();
  }

  /**
   * If the instruction list is showing and onBackPressed is called,
   * hide the instruction list and do not hide the activity or fragment.
   *
   * @return true if back press handled, false if not
   */
  public boolean onBackPressed() {
    return instructionView.handleBackPressed();
  }

  /**
   * Used to store the bottomsheet state and re-center
   * button visibility.  As well as anything the {@link MapView}
   * needs to store in the bundle.
   *
   * @param outState to store state variables
   */
  public void onSaveInstanceState(@NonNull Bundle outState) {
    int bottomSheetBehaviorState = summaryBehavior == null ? INVALID_STATE : summaryBehavior.getState();
    boolean isWayNameVisible = wayNameView.getVisibility() == VISIBLE;
    NavigationViewInstanceState navigationViewInstanceState = new NavigationViewInstanceState(
        bottomSheetBehaviorState, recenterBtn.getVisibility(), instructionView.isShowingInstructionList(),
        isWayNameVisible, wayNameView.retrieveWayNameText(), navigationViewModel.isMuted());
    String instanceKey = getContext().getString(R.string.mapbox_navigation_view_instance_state);
    outState.putParcelable(instanceKey, navigationViewInstanceState);
    outState.putBoolean(getContext().getString(R.string.mapbox_navigation_running), navigationViewModel.isRunning());
    mapView.onSaveInstanceState(outState);
    saveNavigationMapInstanceState(outState);
  }

  /**
   * Used to restore the bottomsheet state and re-center
   * button visibility.  As well as the {@link MapView}
   * position prior to a configuration change.
   *
   * @param savedInstanceState to extract state variables
   */
  public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
    String instanceKey = getContext().getString(R.string.mapbox_navigation_view_instance_state);
    NavigationViewInstanceState navigationViewInstanceState = savedInstanceState.getParcelable(instanceKey);
    recenterBtn.setVisibility(navigationViewInstanceState.getRecenterButtonVisibility());
    wayNameView.setVisibility(navigationViewInstanceState.isWayNameVisible() ? VISIBLE : INVISIBLE);
    wayNameView.updateWayNameText(navigationViewInstanceState.getWayNameText());
    resetBottomSheetState(navigationViewInstanceState.getBottomSheetBehaviorState());
    updateInstructionListState(navigationViewInstanceState.isInstructionViewVisible());
    restoreInstructionMutedState(navigationViewInstanceState.isMuted());
    mapInstanceState = savedInstanceState;
  }

  public void onStart() {
    mapView.onStart();
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
  }

  public void onResume() {
    mapView.onResume();
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
  }

  public void onPause() {
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
    mapView.onPause();
  }

  public void onStop() {
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
    mapView.onStop();
  }

  /**
   * Called to ensure the {@link MapView} is destroyed
   * properly.
   * <p>
   * In an {@link Activity} this should be in onDestroy().
   * <p>
   * In a {@link androidx.fragment.app.Fragment}, this should
   * be in {@link androidx.fragment.app.Fragment#onDestroyView()}.
   */
  public void onDestroy() {
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
    shutdown();
  }

  @NonNull
  @Override
  public Lifecycle getLifecycle() {
    return lifecycleRegistry;
  }

  /**
   * Fired after the map is ready, this is our cue to finish
   * setting up the rest of the plugins / location engine.
   * <p>
   * Also, we check for launch data (coordinates or route).
   *
   * @param mapboxMap used for route, camera, and location UI
   */
  @Override
  public void onMapReady(@NonNull final MapboxMap mapboxMap) {
    mapboxMap.setStyle(ThemeSwitcher.retrieveMapStyle(getContext()), new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        initializeNavigationMap(mapView, mapboxMap);
        moveMapboxLogoAboveBottomSheet();
        moveMapboxAttributionAboveBottomSheet();
        logoAndAttributionShownForFirstTime = true;
        initializeWayNameListener();
        updateNavigationReadyListeners(navigationViewModel.isRunning());
        isMapInitialized = true;
      }
    });
  }

  @Override
  public void setSummaryBehaviorState(int state) {
    // Adjust the Mapbox logo and attribution based on
    // the SummaryBottomSheet's new state
    if (logoAndAttributionShownForFirstTime) {
      if (mapboxLogoEnabled() && attributionEnabled()) {
        updateLogoAndAttributionVisibility(state != BottomSheetBehavior.STATE_HIDDEN);
        logoAndAttributionShownForFirstTime = false;
      }
    } else {
      updateLogoAndAttributionVisibility(state != BottomSheetBehavior.STATE_HIDDEN);
    }
    // Update the SummaryBottomSheet's state
    summaryBehavior.setState(state);
  }

  @Override
  public void setSummaryBehaviorHideable(boolean isHideable) {
    summaryBehavior.setHideable(isHideable);
  }

  @Override
  public boolean isSummaryBottomSheetHidden() {
    return summaryBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN;
  }

  @Override
  public void resetCameraPosition() {
    if (navigationMap != null) {
      navigationMap.resetPadding();
      navigationMap.resetCameraPositionWith(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
    }
  }

  @Override
  public void showRecenterBtn() {
    recenterBtn.show();
  }

  @Override
  public void hideRecenterBtn() {
    recenterBtn.hide();
  }

  @Override
  public boolean isRecenterButtonVisible() {
    return recenterBtn.getVisibility() == View.VISIBLE;
  }

  @Override
  public void drawRoute(@NonNull DirectionsRoute directionsRoute) {
    if (navigationMap != null) {
      navigationMap.drawRoute(directionsRoute);
    }
  }

  @Override
  public void addMarker(Point position) {
    if (navigationMap != null) {
      navigationMap.addDestinationMarker(position);
    }
  }

  /**
   * Provides the current visibility of the way name view.
   *
   * @return true if visible, false if not visible
   */
  public boolean isWayNameVisible() {
    return wayNameView.getVisibility() == VISIBLE;
  }

  /**
   * Updates the text of the way name view below the
   * navigation icon.
   * <p>
   * If you'd like to use this method without being overridden by the default way names
   * values we provide, please disabled auto-query with
   * {@link NavigationMapboxMap#updateWaynameQueryMap(boolean)}.
   *
   * @param wayName to update the view
   */
  @Override
  public void updateWayNameView(@NonNull String wayName) {
    wayNameView.updateWayNameText(wayName);
  }

  /**
   * Set the auto-query state to provide the default way names value
   * with {@link NavigationMapboxMap#updateWaynameQueryMap(boolean)}.
   *
   * @param isActive true if auto-query is enabled, false otherwise.
   */
  @Override
  public void setWayNameActive(boolean isActive) {
    if (navigationMap != null) {
      navigationMap.updateWaynameQueryMap(isActive);
    }
  }

  /**
   * Set the visibility state of the way name view.
   *
   * @param isVisible true if visible, false otherwise.
   */
  @Override
  public void setWayNameVisibility(boolean isVisible) {
    wayNameView.updateVisibility(isVisible);
  }

  /**
   * Provides the current text of the way name view.
   *
   * @return the current text of the way name view
   */
  @NonNull
  @Override
  public String retrieveWayNameText() {
    return wayNameView.retrieveWayNameText();
  }

  @Override
  public void takeScreenshot() {
    if (navigationMap != null) {
      navigationMap.takeScreenshot(new NavigationSnapshotReadyCallback(this, navigationViewModel));
    }
  }

  /**
   * Used when starting this {@link Activity}
   * for the first time.
   * <p>
   * Zooms to the beginning of the {@link DirectionsRoute}.
   *
   * @param directionsRoute where camera should move to
   */
  @Override
  public void startCamera(@NonNull DirectionsRoute directionsRoute) {
    if (navigationMap != null) {
      navigationMap.updateLocationLayerRenderMode(RenderMode.GPS);
      navigationMap.updateCameraTrackingMode(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
      navigationMap.startCamera(directionsRoute);
    }
  }

  /**
   * Used after configuration changes to resume the camera
   * to the last location update from the Navigation SDK.
   *
   * @param location where the camera should move to
   */
  @Override
  public void resumeCamera(@NonNull Location location) {
    if (navigationMap != null) {
      navigationMap.resumeCamera(location);
    }
  }

  @Override
  public void updateCameraRouteOverview() {
    if (navigationMap != null) {
      int[] padding = buildRouteOverviewPadding(getContext());
      navigationMap.showRouteOverview(padding);
    }
  }

  @Override
  public void onFeedbackSent() {
    Snackbar snackbar = Snackbar.make(this, R.string.mapbox_feedback_reported, Snackbar.LENGTH_SHORT);
    if (!isSummaryBottomSheetHidden()) {
      snackbar.setAnchorView(summaryBottomSheet);
    }

    snackbar.getView().setBackgroundColor(
        ContextCompat.getColor(getContext(), R.color.mapbox_feedback_bottom_sheet_secondary));
    snackbar.setTextColor(ContextCompat.getColor(getContext(), R.color.mapbox_feedback_bottom_sheet_primary_text));

    snackbar.show();
  }

  @Override
  public void onGuidanceViewChange(int left, int top, int width, int height) {
    if (ViewUtils.isLandscape(getContext())) {
      navigationMap.adjustLocationIconWith(new int[] { width, 0, 0, 0 });
      ObjectAnimator animator = ObjectAnimator.ofFloat(wayNameView, "translationX", (float) width / 2);
      animator.setDuration(WAY_NAME_TRANSLATIONX_DURATION);
      animator.start();
    } else {
      navigationMap.adjustLocationIconWith(new int[] { 0, height, 0, 0 });
    }
  }

  /**
   * Should be called when this view is completely initialized.
   *
   * @param options with containing route / coordinate data
   */
  @SuppressLint("MissingPermission")
  public void startNavigation(@NonNull NavigationViewOptions options) {
    navigationMap.drawRoute(options.directionsRoute());
    initializeNavigation(options);
    startCamera(options.directionsRoute());
  }

  /**
   * Call this when the navigation session needs to end navigation without finishing the whole view
   */
  public void stopNavigation() {
    navigationPresenter.onNavigationStopped();
    navigationViewModel.stopNavigation();
  }

  /**
   * Should be called after {@link NavigationView#onCreate(Bundle)}.
   * <p>
   * This method adds the {@link OnNavigationReadyCallback},
   * which will fire the ready events for this view.
   *
   * @param onNavigationReadyCallback to be set to this view
   */
  public void initialize(OnNavigationReadyCallback onNavigationReadyCallback) {
    onNavigationReadyCallbacks.add(onNavigationReadyCallback);
    if (!isMapInitialized) {
      mapView.getMapAsync(this);
    } else {
      updateNavigationReadyListeners(navigationViewModel.isRunning());
    }
  }

  /**
   * Should be called after {@link NavigationView#onCreate(Bundle)}.
   * <p>
   * This method adds the {@link OnNavigationReadyCallback},
   * which will fire the ready events for this view.
   * <p>
   * This method also accepts a {@link CameraPosition} that will be set as soon as the map is
   * ready.  Note, this position is ignored during a configuration change in favor of the last known map position.
   *
   * @param onNavigationReadyCallback to be set to this view
   * @param initialMapCameraPosition to be shown once the map is ready
   */
  public void initialize(OnNavigationReadyCallback onNavigationReadyCallback,
      @NonNull CameraPosition initialMapCameraPosition) {
    this.initialMapCameraPosition = initialMapCameraPosition;
    onNavigationReadyCallbacks.add(onNavigationReadyCallback);
    if (!isMapInitialized) {
      mapView.getMapAsync(this);
    } else {
      updateNavigationReadyListeners(navigationViewModel.isRunning());
    }
  }

  /**
   * Gives the ability to manipulate the map directly for anything that might not currently be
   * supported. This returns null until the view is initialized.
   * <p>
   * The {@link NavigationMapboxMap} gives direct access to the map UI (location marker, route, etc.).
   *
   * @return navigation mapbox map object, or null if view has not been initialized
   */
  @Nullable
  public NavigationMapboxMap retrieveNavigationMapboxMap() {
    return navigationMap;
  }

  /**
   * Returns the instance of {@link MapboxNavigation} powering the {@link NavigationView}
   * once navigation has started.  Will return null if navigation has not been started with
   * {@link NavigationView#startNavigation(NavigationViewOptions)}.
   *
   * @return mapbox navigation, or null if navigation has not started
   */
  @Nullable
  public MapboxNavigation retrieveMapboxNavigation() {
    return navigationViewModel.retrieveNavigation();
  }

  /**
   * Returns the sound button used for muting instructions
   *
   * @return sound button
   */
  public NavigationButton retrieveSoundButton() {
    return instructionView.retrieveSoundButton();
  }

  /**
   * Returns the feedback button for sending feedback about navigation
   *
   * @return feedback button
   */
  public NavigationButton retrieveFeedbackButton() {
    return instructionView.retrieveFeedbackButton();
  }

  /**
   * Returns the re-center button for recentering on current location
   *
   * @return recenter button
   */
  public NavigationButton retrieveRecenterButton() {
    return recenterBtn;
  }

  /**
   * Returns the {@link NavigationAlertView} that is shown during off-route events with
   * "Report a Problem" text.
   *
   * @return alert view that is used in the instruction view
   */
  public NavigationAlertView retrieveAlertView() {
    return instructionView.retrieveAlertView();
  }

  /**
   * Returns the state of speech player whether it is muted or unmuted
   * @return true if speech player is mute else false
   */
  public boolean isVoiceGuidanceMuted() {
    return navigationViewModel.isMuted();
  }

  /**
   * This method toggles the mute state of speech player. It also updates the UI
   * to reflect the new state of speech player.
   * <p>
   * Should be called after {@link NavigationView#startNavigation}.
   * Otherwise, it does nothing.
   * <p>
   * Can check the current mute state by calling {@link NavigationView#isVoiceGuidanceMuted()}
   */
  public void toggleMute() {
    navigationViewModel.setMuted(((SoundButton) retrieveSoundButton()).toggleMute());
  }

  /**
   * Updates the visibility of the Mapbox logo and attribution button
   *
   * @param isVisible what the new visibility should be. True makes
   * them visible, false to hide.
   */
  private void updateLogoAndAttributionVisibility(boolean isVisible) {
    if (navigationMap != null) {
      if (navigationMap.retrieveMap() != null) {
        UiSettings uiSettings = navigationMap.retrieveMap().getUiSettings();
        uiSettings.setLogoEnabled(isVisible);
        uiSettings.setAttributionEnabled(isVisible);
      }
    }
  }

  private boolean mapboxLogoEnabled() {
    return navigationMap.retrieveMap().getUiSettings().isLogoEnabled();
  }

  private boolean attributionEnabled() {
    return navigationMap.retrieveMap().getUiSettings().isAttributionEnabled();
  }

  /**
   * Set the margins of the Mapbox logo within the {@link NavigationView}.
   *
   * @param left margin in pixels
   * @param top margin in pixels
   * @param right margin in pixels
   * @param bottom margin in pixels
   */
  private void updateMapboxLogoMargins(int left, int top, int right, int bottom) {
    if (navigationMap != null) {
      if (navigationMap.retrieveMap() != null) {
        navigationMap.retrieveMap().getUiSettings().setLogoMargins(left, top, right, bottom);
      }
    }
  }

  /**
   * Set the margins of the attribution icon within the {@link NavigationView}.
   *
   * @param left margin in pixels
   * @param top margin in pixels
   * @param right margin in pixels
   * @param bottom margin in pixels
   */
  private void updateAttributionMargins(int left, int top, int right, int bottom) {
    if (navigationMap != null) {
      if (navigationMap.retrieveMap() != null) {
        navigationMap.retrieveMap().getUiSettings().setAttributionMargins(left, top, right, bottom);
      }
    }
  }

  private void updateNavigationReadyListeners(boolean isRunning) {
    for (OnNavigationReadyCallback callback : onNavigationReadyCallbacks) {
      callback.onNavigationReady(isRunning);
    }
  }

  @NonNull
  private OnNavigationReadyCallback internalNavigationReadyCallback = new OnNavigationReadyCallback() {
    @Override
    public void onNavigationReady(final boolean isRunning) {
      if (isRunning) {
        final NavigationViewOptions navigationViewOptions = navigationViewModel.getNavigationViewOptions();
        if (navigationViewOptions != null) {
          establish(navigationViewOptions);
          if (navigationViewOptions.puckDrawableSupplier() == null) {
            navigationMap.setPuckDrawableSupplier(new DefaultMapboxPuckDrawableSupplier());
          } else {
            navigationMap.setPuckDrawableSupplier(navigationViewOptions.puckDrawableSupplier());
          }

          if (navigationViewOptions.camera() == null) {
            navigationMap.setCamera(new DynamicCamera(navigationMap.retrieveMap()));
          } else {
            navigationMap.setCamera(navigationViewOptions.camera());
          }

          initializeNavigationListeners(navigationViewOptions, navigationViewModel);
          setupNavigationMapboxMap(navigationViewOptions);

          if (!isSubscribed) {
            initializeClickListeners();
            initializeOnCameraTrackingChangedListener();
            subscribeViewModels();
          }
        }
      }
    }
  };

  private void initializeView() {
    inflate(getContext(), R.layout.mapbox_navigation_view, this);
    bind();
    initializeNavigationViewModel();
    initializeNavigationEventDispatcher();
    initializeNavigationPresenter();
    initializeInstructionListener();
    initializeSummaryBottomSheet();
  }

  private void bind() {
    mapView = findViewById(R.id.navigationMapView);
    instructionView = findViewById(R.id.instructionView);
    ViewCompat.setElevation(instructionView, 10);
    summaryBottomSheet = findViewById(R.id.summaryBottomSheet);
    cancelBtn = findViewById(R.id.cancelBtn);
    recenterBtn = findViewById(R.id.recenterBtn);
    wayNameView = findViewById(R.id.wayNameView);
    routeOverviewBtn = findViewById(R.id.routeOverviewBtn);
  }

  private void initializeNavigationViewModel() {
    try {
      Context context = getContext();
      // unwrap the context if needed, see https://github.com/mapbox/mapbox-navigation-android/issues/2777
      while (!(context instanceof FragmentActivity) && context instanceof ContextWrapper) {
        context = ((ContextWrapper) context).getBaseContext();
      }
      navigationViewModel = ViewModelProviders.of((FragmentActivity) context).get(NavigationViewModel.class);
    } catch (ClassCastException exception) {
      throw new ClassCastException("Please ensure that the provided Context is a valid FragmentActivity");
    }
  }

  private void initializeSummaryBottomSheet() {
    summaryBehavior = BottomSheetBehavior.from(summaryBottomSheet);
    summaryBehavior.setHideable(false);
    summaryBehavior.setBottomSheetCallback(new SummaryBottomSheetCallback(navigationPresenter,
        navigationViewEventDispatcher));
  }

  private void initializeNavigationEventDispatcher() {
    navigationViewEventDispatcher = new NavigationViewEventDispatcher();
    navigationViewModel.initializeEventDispatcher(navigationViewEventDispatcher);
  }

  private void initializeInstructionListener() {
    instructionView.setInstructionListListener(new NavigationInstructionListListener(navigationViewEventDispatcher));
    instructionView.setGuidanceViewListener(new NavigationGuidanceViewListener(navigationPresenter));
  }

  private void initializeNavigationMap(@NonNull MapView mapView, @NonNull MapboxMap map) {
    if (initialMapCameraPosition != null) {
      map.setCameraPosition(initialMapCameraPosition);
    }
    navigationMap = new NavigationMapboxMap(mapView, map, this, null, false, false);
    navigationMap.updateLocationLayerRenderMode(RenderMode.GPS);
    if (mapInstanceState != null) {
      navigationMap.restoreStateFrom(mapInstanceState);
      return;
    }
  }

  /**
   * Sets the margins of the Mapbox logo so that it's visible just
   * above the default {@link SummaryBottomSheet}.
   */
  private void moveMapboxLogoAboveBottomSheet() {
    int summaryBottomSheetHeight = (int) getContext().getResources()
        .getDimension(R.dimen.mapbox_summary_bottom_sheet_height);
    if (navigationMap != null && navigationMap.retrieveMap() != null) {
      UiSettings uiSettings = navigationMap.retrieveMap().getUiSettings();
      updateMapboxLogoMargins(
          uiSettings.getLogoMarginLeft(),
          uiSettings.getLogoMarginTop(),
          uiSettings.getAttributionMarginLeft(),
          summaryBottomSheetHeight
              + DEFAULT_PX_BETWEEN_BOTTOM_SHEET_LOGO_AND_ATTRIBUTION);
    }
  }

  /**
   * Sets the margins of the attribution button so that
   * it's visible just above the default {@link SummaryBottomSheet}.
   */
  private void moveMapboxAttributionAboveBottomSheet() {
    int summaryBottomSheetHeight = (int) getContext().getResources()
        .getDimension(R.dimen.mapbox_summary_bottom_sheet_height);
    if (navigationMap != null && navigationMap.retrieveMap() != null) {
      UiSettings uiSettings = navigationMap.retrieveMap().getUiSettings();
      updateAttributionMargins(
          uiSettings.getAttributionMarginLeft(),
          uiSettings.getAttributionMarginTop(),
          uiSettings.getAttributionMarginRight(),
          summaryBottomSheetHeight + DEFAULT_PX_BETWEEN_BOTTOM_SHEET_LOGO_AND_ATTRIBUTION);
    }
  }

  private void initializeWayNameListener() {
    NavigationViewWayNameListener wayNameListener = new NavigationViewWayNameListener(navigationPresenter);
    navigationMap.addOnWayNameChangedListener(wayNameListener);
  }

  private void saveNavigationMapInstanceState(@NonNull Bundle outState) {
    if (navigationMap != null) {
      navigationMap.saveStateWith(outState);
    }
  }

  private void resetBottomSheetState(int bottomSheetState) {
    if (bottomSheetState > INVALID_STATE) {
      boolean isShowing = bottomSheetState == BottomSheetBehavior.STATE_EXPANDED;
      summaryBehavior.setHideable(!isShowing);
      summaryBehavior.setState(bottomSheetState);
    }
  }

  private void updateInstructionListState(boolean visible) {
    if (visible) {
      instructionView.showInstructionList();
    } else {
      instructionView.hideInstructionList();
    }
  }

  private void restoreInstructionMutedState(boolean isMuted) {
    navigationViewModel.setMuted(isMuted);
    ((SoundButton) instructionView.retrieveSoundButton()).onRestoreInstanceState(isMuted);
  }

  @NonNull
  private int[] buildRouteOverviewPadding(@NonNull Context context) {
    Resources resources = context.getResources();
    int leftRightPadding = (int) resources.getDimension(R.dimen.mapbox_route_overview_left_right_padding);
    int paddingBuffer = (int) resources.getDimension(R.dimen.mapbox_route_overview_buffer_padding);
    int instructionHeight = (int) (resources.getDimension(R.dimen.mapbox_instruction_content_height) + paddingBuffer);
    int summaryHeight = (int) resources.getDimension(R.dimen.mapbox_summary_bottom_sheet_height);
    return new int[] { leftRightPadding, instructionHeight, leftRightPadding, summaryHeight };
  }

  private boolean isChangingConfigurations() {
    try {
      return ((FragmentActivity) getContext()).isChangingConfigurations();
    } catch (ClassCastException exception) {
      throw new ClassCastException("Please ensure that the provided Context is a valid FragmentActivity");
    }
  }

  private void initializeNavigationPresenter() {
    navigationPresenter = new NavigationPresenter(this);
  }

  private void updatePresenterState(@Nullable Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      String navigationRunningKey = getContext().getString(R.string.mapbox_navigation_running);
      boolean resumeState = savedInstanceState.getBoolean(navigationRunningKey);
      navigationPresenter.updateResumeState(resumeState);
    }
  }

  private void initializeNavigation(@NonNull NavigationViewOptions options) {
    navigationViewModel.initialize(options);
    establish(options);

    if (options.puckDrawableSupplier() == null) {
      navigationMap.setPuckDrawableSupplier(new DefaultMapboxPuckDrawableSupplier());
    } else {
      navigationMap.setPuckDrawableSupplier(options.puckDrawableSupplier());
    }

    if (options.camera() == null) {
      navigationMap.setCamera(new DynamicCamera(navigationMap.retrieveMap()));
    } else {
      navigationMap.setCamera(options.camera());
    }

    if (options.enableVanishingRouteLine()) {
      navigationMap.enableVanishingRouteLine();
    } else {
      navigationMap.disableVanishingRouteLine();
    }

    initializeVoiceGuidanceMuteState(options);
    initializeNavigationListeners(options, navigationViewModel);
    setupNavigationMapboxMap(options);

    if (!isSubscribed) {
      initializeClickListeners();
      initializeOnCameraTrackingChangedListener();
      subscribeViewModels();
    }
  }

  private void initializeVoiceGuidanceMuteState(@NonNull final NavigationViewOptions options) {
    if (options.muteVoiceGuidance() && !isVoiceGuidanceMuted()) {
      navigationViewModel.setMuted(((SoundButton) retrieveSoundButton()).toggleMute());
    }
  }

  private void initializeClickListeners() {
    cancelBtn.setOnClickListener(new CancelBtnClickListener(navigationViewEventDispatcher));
    recenterBtn.addOnClickListener(new RecenterBtnClickListener(navigationPresenter));
    routeOverviewBtn.setOnClickListener(new RouteOverviewBtnClickListener(navigationPresenter));
    retrieveFeedbackButton().addOnClickListener(view -> navigationViewModel.takeScreenshot());
  }

  private void initializeOnCameraTrackingChangedListener() {
    onTrackingChangedListener = new NavigationOnCameraTrackingChangedListener(navigationPresenter, summaryBehavior);
    navigationMap.addOnCameraTrackingChangedListener(onTrackingChangedListener);
  }

  private void establish(@NonNull NavigationViewOptions options) {
    establishDistanceFormatter();
    establishTimeFormat(options);
  }

  private void establishDistanceFormatter() {
    final DistanceFormatter formatter = navigationViewModel.getDistanceFormatter();
    instructionView.setDistanceFormatter(formatter);
    summaryBottomSheet.setDistanceFormatter(formatter);
  }

  private void establishTimeFormat(@NonNull NavigationViewOptions options) {
    @TimeFormat.Type
    int timeFormatType = options.navigationOptions().getTimeFormatType();
    summaryBottomSheet.setTimeFormat(timeFormatType);
  }

  private void initializeNavigationListeners(
          @NonNull NavigationViewOptions options,
          @NonNull NavigationViewModel navigationViewModel
  ) {
    navigationMap.addProgressChangeListener(
        navigationViewModel.retrieveNavigation()
    );
    navigationViewEventDispatcher.initializeListeners(options, navigationViewModel);
  }

  private void setupNavigationMapboxMap(@NonNull NavigationViewOptions options) {
    navigationMap.updateWaynameQueryMap(options.waynameChipEnabled());
  }

  /**
   * Subscribes the {@link InstructionView} and {@link SummaryBottomSheet} to the {@link NavigationViewModel}.
   * <p>
   * Then, creates an instance of {@link NavigationViewSubscriber}, which takes a presenter.
   * <p>
   * The subscriber then subscribes to the view models, setting up the appropriate presenter / listener
   * method calls based on the {@link androidx.lifecycle.LiveData} updates.
   */
  private void subscribeViewModels() {
    instructionView.subscribe(this, navigationViewModel);
    summaryBottomSheet.subscribe(this, navigationViewModel);

    new NavigationViewSubscriber(this, navigationViewModel, navigationPresenter).subscribe();
    isSubscribed = true;
  }

  private void shutdown() {
    if (navigationMap != null) {
      navigationMap.removeOnCameraTrackingChangedListener(onTrackingChangedListener);
    }
    navigationViewEventDispatcher.onDestroy();
    mapView.onDestroy();
    navigationViewModel.onDestroy(isChangingConfigurations());
    navigationMap = null;
  }
}