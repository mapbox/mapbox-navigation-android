package com.mapbox.services.android.navigation.ui.v5;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionModel;
import com.mapbox.services.android.navigation.ui.v5.summary.SummaryModel;
import com.mapbox.services.android.navigation.ui.v5.voice.InstructionPlayer;
import com.mapbox.services.android.navigation.ui.v5.voice.NavigationInstructionPlayer;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;

import java.text.DecimalFormat;

public class NavigationViewModel extends AndroidViewModel implements LifecycleObserver, ProgressChangeListener,
  OffRouteListener, MilestoneEventListener, NavigationEventListener {

  public final MutableLiveData<InstructionModel> instructionModel = new MutableLiveData<>();
  public final MutableLiveData<SummaryModel> summaryModel = new MutableLiveData<>();
  public final MutableLiveData<Boolean> isOffRoute = new MutableLiveData<>();
  final MutableLiveData<Location> navigationLocation = new MutableLiveData<>();
  final MutableLiveData<Point> newOrigin = new MutableLiveData<>();
  final MutableLiveData<Boolean> isRunning = new MutableLiveData<>();

  private MapboxNavigation navigation;
  private NavigationInstructionPlayer instructionPlayer;
  private DecimalFormat decimalFormat;
  private SharedPreferences preferences;

  public NavigationViewModel(Application application) {
    super(application);
    preferences = PreferenceManager.getDefaultSharedPreferences(application);
    initNavigation(application);
    initVoiceInstructions(application);
    initDecimalFormat();
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  public void onStart() {
    addNavigationListeners();
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  public void onDestroy() {
    endNavigation();
    deactivateInstructionPlayer();
  }

  /**
   * Listener used to update the TODO.
   * <p>
   * Added to {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}.
   *
   * @param location      given to the TODO to show our current location
   * @param routeProgress ignored in this scenario
   * @since 0.6.0
   */
  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    instructionModel.setValue(new InstructionModel(routeProgress, decimalFormat));
    summaryModel.setValue(new SummaryModel(routeProgress, decimalFormat));
    navigationLocation.setValue(location);
  }

  /**
   * Listener used to update the TODO.
   * <p>
   * Added to {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}.
   *
   * @param location given to the TODO to show our current location
   * @since 0.6.0
   */
  @Override
  public void userOffRoute(Location location) {
    Point newOrigin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    this.newOrigin.setValue(newOrigin);
    isOffRoute.setValue(true);
  }

  /**
   * Listener used to play instructions and finish this activity
   * when the arrival milestone is triggered.
   * <p>
   * Added to {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}.
   *
   * @param routeProgress ignored in this scenario
   * @param instruction   to be voiced by the {@link InstructionPlayer}
   * @param identifier    used to determine the type of milestone
   * @since 0.6.0
   */
  @Override
  public void onMilestoneEvent(RouteProgress routeProgress, String instruction, int identifier) {
    instructionPlayer.play(instruction);
  }

  /**
   * Listener used to determine is navigation is running / not running.
   *
   * In {@link NavigationView}, views will be shown when true.  When false,
   * the {@link android.app.Activity} will be destroyed.
   *
   * @param running true if {@link MapboxNavigation} is up and running, false if not
   * @since 0.6.0
   */
  @Override
  public void onRunning(boolean running) {
    isRunning.setValue(running);
  }

  public void setMuted(boolean isMuted) {
    instructionPlayer.setMuted(isMuted);
  }

  public MapboxNavigation getNavigation() {
    return navigation;
  }

  void updateRoute(DirectionsRoute route) {
    startNavigation(route);
    isOffRoute.setValue(false);
  }

  void updateLocationEngine(LocationEngine locationEngine) {
    navigation.setLocationEngine(locationEngine);
  }

  /**
   * Initializes {@link MapboxNavigation} and adds all views that implement listeners.
   */
  private void initNavigation(Application application) {
    navigation = new MapboxNavigation(application.getApplicationContext(), Mapbox.getAccessToken(),
      MapboxNavigationOptions.builder().isFromNavigationUi(true).build());
  }

  /**
   * Initializes the {@link InstructionPlayer}.
   */
  private void initVoiceInstructions(Application application) {
    instructionPlayer = new NavigationInstructionPlayer(application.getBaseContext(),
      preferences.getString(NavigationConstants.NAVIGATION_VIEW_AWS_POOL_ID, null));
  }

  /**
   * Initializes decimal format to be used to populate views with
   * distance remaining.
   */
  private void initDecimalFormat() {
    decimalFormat = new DecimalFormat(NavigationConstants.DECIMAL_FORMAT);
  }

  /**
   * Adds this class as a listener for progress,
   * milestones, and off route events.
   */
  private void addNavigationListeners() {
    if (navigation != null) {
      navigation.addProgressChangeListener(this);
      navigation.addOffRouteListener(this);
      navigation.addMilestoneEventListener(this);
      navigation.addNavigationEventListener(this);
    }
  }

  /**
   * Starts navigation and sets isRunning to true.
   * <p>
   * This will notify any observer of isRunning that navigation has begun.
   *
   * @param route that is being navigated
   */
  private void startNavigation(DirectionsRoute route) {
    if (route != null) {
      navigation.startNavigation(route);
    }
  }

  /**
   * Destroys {@link MapboxNavigation} if not null
   */
  private void endNavigation() {
    if (navigation != null) {
      navigation.onDestroy();
    }
  }

  /**
   * Destroys the {@link InstructionPlayer} if not null
   */
  private void deactivateInstructionPlayer() {
    if (instructionPlayer != null) {
      instructionPlayer.onDestroy();
    }
  }
}
