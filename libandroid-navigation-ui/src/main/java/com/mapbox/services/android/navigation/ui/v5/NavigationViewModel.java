package com.mapbox.services.android.navigation.ui.v5;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackItem;
import com.mapbox.services.android.navigation.ui.v5.instruction.BannerInstructionModel;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionModel;
import com.mapbox.services.android.navigation.ui.v5.location.LocationEngineConductor;
import com.mapbox.services.android.navigation.ui.v5.location.LocationEngineConductorListener;
import com.mapbox.services.android.navigation.ui.v5.route.OffRouteEvent;
import com.mapbox.services.android.navigation.ui.v5.route.ViewRouteFetcher;
import com.mapbox.services.android.navigation.ui.v5.route.ViewRouteListener;
import com.mapbox.services.android.navigation.ui.v5.summary.SummaryModel;
import com.mapbox.services.android.navigation.ui.v5.voice.NavigationSpeechPlayer;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechPlayerProvider;
import com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationTimeFormat;
import com.mapbox.services.android.navigation.v5.navigation.metrics.FeedbackEvent;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.route.FasterRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.util.List;

public class NavigationViewModel extends AndroidViewModel {

  private static final String EMPTY_STRING = "";

  public final MutableLiveData<InstructionModel> instructionModel = new MutableLiveData<>();
  public final MutableLiveData<BannerInstructionModel> bannerInstructionModel = new MutableLiveData<>();
  public final MutableLiveData<SummaryModel> summaryModel = new MutableLiveData<>();
  public final MutableLiveData<Boolean> isOffRoute = new MutableLiveData<>();
  public final MutableLiveData<Boolean> isFeedbackShowing = new MutableLiveData<>();
  final MutableLiveData<Location> navigationLocation = new MutableLiveData<>();
  final MutableLiveData<DirectionsRoute> route = new MutableLiveData<>();
  final MutableLiveData<Point> destination = new MutableLiveData<>();
  final MutableLiveData<Boolean> shouldRecordScreenshot = new MutableLiveData<>();

  private MapboxNavigation navigation;
  private ViewRouteFetcher navigationViewRouteEngine;
  private LocationEngineConductor locationEngineConductor;
  private NavigationViewEventDispatcher navigationViewEventDispatcher;
  private NavigationSpeechPlayer instructionPlayer;
  private ConnectivityManager connectivityManager;
  private RouteProgress routeProgress;
  private String feedbackId;
  private String screenshot;
  private String language;
  @DirectionsCriteria.VoiceUnitCriteria
  private String unitType;
  @NavigationTimeFormat.Type
  private int timeFormatType;
  private boolean isRunning;
  private RouteUtils routeUtils;
  private String accessToken;

  public NavigationViewModel(Application application) {
    super(application);
    this.accessToken = Mapbox.getAccessToken();
    initConnectivityManager(application);
    initNavigationRouteEngine();
    initNavigationLocationEngine();
    routeUtils = new RouteUtils();
  }

  public void onCreate() {
    if (!isRunning) {
      locationEngineConductor.onCreate();
    }
  }

  public void onDestroy(boolean isChangingConfigurations) {
    if (!isChangingConfigurations) {
      locationEngineConductor.onDestroy();
      deactivateInstructionPlayer();
      endNavigation();
    }
    navigationViewEventDispatcher = null;
  }

  public void setMuted(boolean isMuted) {
    instructionPlayer.setMuted(isMuted);
  }

  /**
   * Records a general feedback item with source
   */
  public void recordFeedback(@FeedbackEvent.FeedbackSource String feedbackSource) {
    feedbackId = navigation.recordFeedback(FeedbackEvent.FEEDBACK_TYPE_GENERAL_ISSUE, EMPTY_STRING, feedbackSource);
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
      sendEventFeedback(feedbackItem);
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

  void initializeEventDispatcher(NavigationViewEventDispatcher navigationViewEventDispatcher) {
    this.navigationViewEventDispatcher = navigationViewEventDispatcher;
  }

  /**
   * This method will pass {@link MapboxNavigationOptions} from the {@link NavigationViewOptions}
   * to this view model to be used to initialize {@link MapboxNavigation}.
   *
   * @param options to init MapboxNavigation
   */
  MapboxNavigation initializeNavigation(NavigationViewOptions options) {
    MapboxNavigationOptions navigationOptions = options.navigationOptions();
    navigationOptions = navigationOptions.toBuilder().isFromNavigationUi(true).build();
    initLanguage(options);
    initUnitType(options);
    initTimeFormat(navigationOptions);
    initVoiceInstructions(options);
    if (!isRunning) {
      locationEngineConductor.initializeLocationEngine(getApplication(), options.shouldSimulateRoute());
      initNavigation(getApplication(), navigationOptions);
      addMilestones(options);
      navigationViewRouteEngine.extractRouteOptions(options);
    }
    return navigation;
  }

  void updateNavigation(NavigationViewOptions options) {
    navigationViewRouteEngine.extractRouteOptions(options);
  }

  void updateFeedbackScreenshot(String screenshot) {
    if (!TextUtils.isEmpty(feedbackId)) {
      this.screenshot = screenshot;
    }
    shouldRecordScreenshot.setValue(false);
  }

  boolean isRunning() {
    return isRunning;
  }

  void stopNavigation() {
    navigation.stopNavigation();
  }

  private void initConnectivityManager(Application application) {
    connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
  }

  private void initNavigationRouteEngine() {
    navigationViewRouteEngine = new ViewRouteFetcher(getApplication(), accessToken, routeEngineListener);
  }

  private void initNavigationLocationEngine() {
    locationEngineConductor = new LocationEngineConductor(locationEngineCallback);
  }

  private void initLanguage(NavigationUiOptions options) {
    language = options.directionsRoute().routeOptions().language();
  }

  private void initUnitType(NavigationUiOptions options) {
    unitType = options.directionsRoute().routeOptions().voiceUnits();
  }

  private void initTimeFormat(MapboxNavigationOptions options) {
    timeFormatType = options.timeFormatType();
  }

  private void initVoiceInstructions(NavigationViewOptions options) {
    boolean isVoiceLanguageSupported = options.directionsRoute().voiceLanguage() != null;
    SpeechPlayerProvider speechPlayerProvider = initializeSpeechPlayerProvider(isVoiceLanguageSupported);
    instructionPlayer = new NavigationSpeechPlayer(speechPlayerProvider);
  }

  @NonNull
  private SpeechPlayerProvider initializeSpeechPlayerProvider(boolean voiceLanguageSupported) {
    return new SpeechPlayerProvider(getApplication(), language, voiceLanguageSupported, accessToken);
  }

  private void initNavigation(Context context, MapboxNavigationOptions options) {
    navigation = new MapboxNavigation(context, accessToken, options);
    navigation.setLocationEngine(locationEngineConductor.obtainLocationEngine());
    addNavigationListeners();
  }

  private void addNavigationListeners() {
    navigation.addProgressChangeListener(progressChangeListener);
    navigation.addOffRouteListener(offRouteListener);
    navigation.addMilestoneEventListener(milestoneEventListener);
    navigation.addNavigationEventListener(navigationEventListener);
    navigation.addFasterRouteListener(fasterRouteListener);
  }

  private void addMilestones(NavigationViewOptions options) {
    List<Milestone> milestones = options.milestones();
    if (milestones != null && !milestones.isEmpty()) {
      navigation.addMilestones(milestones);
    }
  }

  private ProgressChangeListener progressChangeListener = new ProgressChangeListener() {
    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
      NavigationViewModel.this.routeProgress = routeProgress;
      instructionModel.setValue(new InstructionModel(getApplication(), routeProgress, language, unitType));
      summaryModel.setValue(new SummaryModel(getApplication(), routeProgress, language, unitType, timeFormatType));
      navigationLocation.setValue(location);
    }
  };

  private OffRouteListener offRouteListener = new OffRouteListener() {
    @Override
    public void userOffRoute(Location location) {
      if (hasNetworkConnection()) {
        instructionPlayer.onOffRoute();
        Point newOrigin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
        sendEventOffRoute(newOrigin);
      }
    }
  };

  private MilestoneEventListener milestoneEventListener = new MilestoneEventListener() {
    @Override
    public void onMilestoneEvent(RouteProgress routeProgress, String instruction, Milestone milestone) {
      playVoiceAnnouncement(milestone);
      updateBannerInstruction(routeProgress, milestone);
      sendEventArrival(routeProgress, milestone);
    }
  };

  private NavigationEventListener navigationEventListener = new NavigationEventListener() {
    @Override
    public void onRunning(boolean isRunning) {
      NavigationViewModel.this.isRunning = isRunning;
      sendNavigationStatusEvent(isRunning);
    }
  };

  private FasterRouteListener fasterRouteListener = new FasterRouteListener() {
    @Override
    public void fasterRouteFound(DirectionsRoute directionsRoute) {
      updateRoute(directionsRoute);
    }
  };

  private ViewRouteListener routeEngineListener = new ViewRouteListener() {
    @Override
    public void onRouteUpdate(DirectionsRoute directionsRoute) {
      updateRoute(directionsRoute);
    }

    @Override
    public void onRouteRequestError(Throwable throwable) {
      if (isOffRoute()) {
        String errorMessage = throwable.getMessage();
        sendEventFailedReroute(errorMessage);
      }
    }

    @Override
    public void onDestinationSet(Point destination) {
      NavigationViewModel.this.destination.setValue(destination);
    }
  };

  private LocationEngineConductorListener locationEngineCallback = new LocationEngineConductorListener() {
    @Override
    public void onLocationUpdate(Location location) {
      navigationViewRouteEngine.updateRawLocation(location);
    }
  };

  private void updateRoute(DirectionsRoute route) {
    this.route.setValue(route);
    startNavigation(route);
    locationEngineConductor.updateRoute(route);
    sendEventOnRerouteAlong(route);
    isOffRoute.setValue(false);
  }

  private boolean isOffRoute() {
    try {
      return isOffRoute.getValue();
    } catch (NullPointerException exception) {
      return false;
    }
  }

  private void startNavigation(DirectionsRoute route) {
    if (route != null) {
      navigation.startNavigation(route);
    }
  }

  private void endNavigation() {
    if (navigation != null) {
      navigation.onDestroy();
    }
  }

  private void deactivateInstructionPlayer() {
    if (instructionPlayer != null) {
      instructionPlayer.onDestroy();
    }
  }

  private void playVoiceAnnouncement(Milestone milestone) {
    if (milestone instanceof VoiceInstructionMilestone) {
      SpeechAnnouncement announcement = SpeechAnnouncement.builder()
        .voiceInstructionMilestone((VoiceInstructionMilestone) milestone).build();
      announcement = navigationViewEventDispatcher.onAnnouncement(announcement);
      instructionPlayer.play(announcement);
    }
  }

  private void updateBannerInstruction(RouteProgress routeProgress, Milestone milestone) {
    if (milestone instanceof BannerInstructionMilestone) {
      BannerInstructions instructions = ((BannerInstructionMilestone) milestone).getBannerInstructions();
      instructions = navigationViewEventDispatcher.onBannerDisplay(instructions);
      if (instructions != null) {
        BannerInstructionModel model = new BannerInstructionModel(getApplication(), instructions,
          routeProgress, language, unitType);
        bannerInstructionModel.setValue(model);
      }
    }
  }

  @SuppressWarnings( {"MissingPermission"})
  private boolean hasNetworkConnection() {
    if (connectivityManager == null) {
      return false;
    }
    NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
  }

  private void sendEventFeedback(FeedbackItem feedbackItem) {
    if (navigationViewEventDispatcher != null) {
      navigationViewEventDispatcher.onFeedbackSent(feedbackItem);
    }
  }

  private void sendEventArrival(RouteProgress routeProgress, Milestone milestone) {
    if (navigationViewEventDispatcher != null && routeUtils.isArrivalEvent(routeProgress, milestone)) {
      navigationViewEventDispatcher.onArrival();
    }
  }

  private void sendEventOffRoute(Point newOrigin) {
    if (navigationViewEventDispatcher != null && navigationViewEventDispatcher.allowRerouteFrom(newOrigin)) {
      navigationViewEventDispatcher.onOffRoute(newOrigin);
      OffRouteEvent event = new OffRouteEvent(newOrigin, routeProgress);
      navigationViewRouteEngine.fetchRouteFromOffRouteEvent(event);
      isOffRoute.setValue(true);
    }
  }

  private void sendNavigationStatusEvent(boolean isRunning) {
    if (navigationViewEventDispatcher != null) {
      if (isRunning) {
        navigationViewEventDispatcher.onNavigationRunning();
      } else {
        navigationViewEventDispatcher.onNavigationFinished();
      }
    }
  }

  private void sendEventFailedReroute(String errorMessage) {
    if (navigationViewEventDispatcher != null) {
      navigationViewEventDispatcher.onFailedReroute(errorMessage);
    }
  }

  private void sendEventOnRerouteAlong(DirectionsRoute route) {
    if (navigationViewEventDispatcher != null && isOffRoute()) {
      navigationViewEventDispatcher.onRerouteAlong(route);
    }
  }
}
