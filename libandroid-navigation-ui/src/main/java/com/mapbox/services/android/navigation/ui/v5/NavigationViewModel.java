package com.mapbox.services.android.navigation.ui.v5;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackItem;
import com.mapbox.services.android.navigation.ui.v5.instruction.BannerInstructionModel;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionModel;
import com.mapbox.services.android.navigation.ui.v5.route.OffRouteEvent;
import com.mapbox.services.android.navigation.ui.v5.summary.SummaryModel;
import com.mapbox.services.android.navigation.ui.v5.voice.InstructionPlayer;
import com.mapbox.services.android.navigation.ui.v5.voice.NavigationInstructionPlayer;
import com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.navigation.metrics.FeedbackEvent;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.route.FasterRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;
import com.mapbox.services.android.telemetry.location.LocationEngine;

import java.util.Locale;

public class NavigationViewModel extends AndroidViewModel implements ProgressChangeListener,
  OffRouteListener, MilestoneEventListener, NavigationEventListener, FasterRouteListener {

  public final MutableLiveData<InstructionModel> instructionModel = new MutableLiveData<>();
  public final MutableLiveData<BannerInstructionModel> bannerInstructionModel = new MutableLiveData<>();
  public final MutableLiveData<SummaryModel> summaryModel = new MutableLiveData<>();
  public final MutableLiveData<Boolean> isOffRoute = new MutableLiveData<>();
  public final MutableLiveData<Boolean> isFeedbackShowing = new MutableLiveData<>();
  final MutableLiveData<FeedbackItem> selectedFeedbackItem = new MutableLiveData<>();
  final MutableLiveData<Location> navigationLocation = new MutableLiveData<>();
  final MutableLiveData<DirectionsRoute> fasterRoute = new MutableLiveData<>();
  final MutableLiveData<OffRouteEvent> offRouteEvent = new MutableLiveData<>();
  final MutableLiveData<Boolean> isRunning = new MutableLiveData<>();
  final MutableLiveData<Boolean> shouldRecordScreenshot = new MutableLiveData<>();

  private MapboxNavigation navigation;
  private NavigationInstructionPlayer instructionPlayer;
  private ConnectivityManager connectivityManager;
  private SharedPreferences preferences;
  private RouteProgress routeProgress;
  private String feedbackId;
  private String screenshot;
  private Locale locale;
  private @NavigationUnitType.UnitType int unitType;

  public NavigationViewModel(Application application) {
    super(application);
    preferences = PreferenceManager.getDefaultSharedPreferences(application);
    initConnectivityManager(application);
  }

  public void onDestroy() {
    endNavigation();
    deactivateInstructionPlayer();
  }

  /**
   * Listener used to update the location on screen
   * and the data in the top / bottom views of the drop-in UI.
   * <p>
   * Added to {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}.
   *
   * @param location      modified to snap to the route being driven
   * @param routeProgress used to create new models for our top / bottom views
   * @since 0.6.0
   */
  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    this.routeProgress = routeProgress;
    instructionModel.setValue(new InstructionModel(getApplication(), routeProgress, locale, unitType));
    summaryModel.setValue(new SummaryModel(getApplication(), routeProgress, locale, unitType));
    navigationLocation.setValue(location);
  }

  /**
   * Listener used to update when a user has gone off-route.
   * <p>
   * This is being used as a cue for a re-route UX / to fetch a new route to
   * keep the user on course.
   * <p>
   * Added to {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}.
   *
   * @param location given to create a new origin
   * @since 0.6.0
   */
  @Override
  public void userOffRoute(Location location) {
    if (hasNetworkConnection()) {
      Point newOrigin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
      this.offRouteEvent.setValue(new OffRouteEvent(newOrigin, routeProgress));
      isOffRoute.setValue(true);
    }
  }

  /**
   * Listener used to play instructions and finish this activity
   * when the arrival milestone is triggered.
   * <p>
   * Added to {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}.
   *
   * @param routeProgress ignored in this scenario
   * @param instruction   to be voiced by the {@link InstructionPlayer}
   * @param milestone     the milestone being triggered
   * @since 0.8.0
   */
  @Override
  public void onMilestoneEvent(RouteProgress routeProgress, String instruction, Milestone milestone) {
    if (instructionPlayer.isPollyPlayer() && milestone instanceof VoiceInstructionMilestone) {
      instructionPlayer.play(((VoiceInstructionMilestone) milestone).getSsmlAnnouncement());
    } else {
      instructionPlayer.play(instruction);
    }
    updateBannerInstruction(routeProgress, milestone);
  }

  /**
   * Listener used to determine is navigation is running / not running.
   * <p>
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

  /**
   * Listener that will be fired if a faster {@link DirectionsRoute} is found
   * while navigating.
   *
   * @param directionsRoute faster route retrieved
   * @since 0.9.0
   */
  @Override
  public void fasterRouteFound(DirectionsRoute directionsRoute) {
    fasterRoute.setValue(directionsRoute);
  }

  public void setMuted(boolean isMuted) {
    instructionPlayer.setMuted(isMuted);
  }

  /**
   * Records a general feedback item with source
   */
  public void recordFeedback(@FeedbackEvent.FeedbackSource String feedbackSource) {
    feedbackId = navigation.recordFeedback(FeedbackEvent.FEEDBACK_TYPE_GENERAL_ISSUE, "", feedbackSource);
    shouldRecordScreenshot.setValue(true);
  }

  /**
   * Used to update an existing {@link FeedbackItem}
   * with a feedback type and description.
   * <p>
   * Uses cached feedbackId to ensure the proper item is updated.
   *
   * @param feedbackItem item to be updated
   * @since 0.7.0
   */
  public void updateFeedback(FeedbackItem feedbackItem) {
    if (!TextUtils.isEmpty(feedbackId)) {
      navigation.updateFeedback(feedbackId, feedbackItem.getFeedbackType(), feedbackItem.getDescription(), screenshot);
      selectedFeedbackItem.setValue(feedbackItem);
      feedbackId = null;
      screenshot = null;
    }
  }

  /**
   * Used to cancel an existing {@link FeedbackItem}.
   * <p>
   * Uses cached feedbackId to ensure the proper item is cancelled.
   *
   * @since 0.7.0
   */
  public void cancelFeedback() {
    if (!TextUtils.isEmpty(feedbackId)) {
      isFeedbackShowing.setValue(false);
      navigation.cancelFeedback(feedbackId);
      feedbackId = null;
    }
  }

  /**
   * Returns the current instance of {@link MapboxNavigation}.
   *
   * @since 0.6.1
   */
  public MapboxNavigation getNavigation() {
    return navigation;
  }

  /**
   * This method will pass {@link MapboxNavigationOptions} from the {@link NavigationViewOptions}
   * to this view model to be used to initialize {@link MapboxNavigation}.
   *
   * @param options to init MapboxNavigation
   */
  void initializeNavigationOptions(Context context, MapboxNavigationOptions options) {
    initLocaleInfo(options);
    initVoiceInstructions();
    initNavigation(context, options);
  }

  void updateRoute(DirectionsRoute route) {
    startNavigation(route);
    isOffRoute.setValue(false);
  }

  void updateFeedbackScreenshot(String screenshot) {
    if (!TextUtils.isEmpty(feedbackId)) {
      this.screenshot = screenshot;
    }
    shouldRecordScreenshot.setValue(false);
  }

  void updateLocationEngine(LocationEngine locationEngine) {
    navigation.setLocationEngine(locationEngine);
  }

  /**
   * Initializes {@link MapboxNavigation} and adds all views that implement listeners.
   */
  private void initNavigation(Context context, MapboxNavigationOptions options) {
    navigation = new MapboxNavigation(context, Mapbox.getAccessToken(), options);
    addNavigationListeners();
  }

  private void initLocaleInfo(MapboxNavigationOptions options) {
    locale = LocaleUtils.getNonNullLocale(this.getApplication(), options.locale());
    unitType = options.unitType();
  }

  /**
   * Initializes the {@link InstructionPlayer}.
   */
  private void initVoiceInstructions() {
    instructionPlayer = new NavigationInstructionPlayer(this.getApplication().getBaseContext(),
      preferences.getString(NavigationConstants.NAVIGATION_VIEW_AWS_POOL_ID, null), locale);
  }

  /**
   * Initializes the {@link ConnectivityManager}.
   */
  private void initConnectivityManager(Application application) {
    connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
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
      navigation.addFasterRouteListener(this);
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

  /**
   * Checks for active network connection.
   *
   * @return true if connected, false otherwise
   */
  @SuppressWarnings( {"MissingPermission"})
  private boolean hasNetworkConnection() {
    if (connectivityManager == null) {
      return false;
    }

    NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
  }

  private void updateBannerInstruction(RouteProgress routeProgress, Milestone milestone) {
    if (milestone instanceof BannerInstructionMilestone) {
      bannerInstructionModel.setValue(
        new BannerInstructionModel(
          getApplication(), (BannerInstructionMilestone) milestone, routeProgress, locale, unitType));
    }
  }
}
