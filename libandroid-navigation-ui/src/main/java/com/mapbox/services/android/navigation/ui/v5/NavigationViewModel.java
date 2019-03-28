package com.mapbox.services.android.navigation.ui.v5;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.camera.DynamicCamera;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackItem;
import com.mapbox.services.android.navigation.ui.v5.instruction.BannerInstructionModel;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionModel;
import com.mapbox.services.android.navigation.ui.v5.summary.SummaryModel;
import com.mapbox.services.android.navigation.ui.v5.voice.NavigationSpeechPlayer;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechPlayer;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechPlayerProvider;
import com.mapbox.services.android.navigation.ui.v5.voice.VoiceInstructionLoader;
import com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationTimeFormat;
import com.mapbox.services.android.navigation.v5.navigation.camera.Camera;
import com.mapbox.services.android.navigation.v5.navigation.metrics.FeedbackEvent;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.route.FasterRouteListener;
import com.mapbox.services.android.navigation.v5.route.RouteFetcher;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.io.File;
import java.util.List;

import okhttp3.Cache;

public class NavigationViewModel extends AndroidViewModel {

  private static final String EMPTY_STRING = "";
  private static final String OKHTTP_INSTRUCTION_CACHE = "okhttp-instruction-cache";
  private static final long TEN_MEGABYTE_CACHE_SIZE = 10 * 1024 * 1024;

  public final MutableLiveData<InstructionModel> instructionModel = new MutableLiveData<>();
  public final MutableLiveData<BannerInstructionModel> bannerInstructionModel = new MutableLiveData<>();
  public final MutableLiveData<SummaryModel> summaryModel = new MutableLiveData<>();
  public final MutableLiveData<Boolean> isOffRoute = new MutableLiveData<>();
  final MutableLiveData<Location> navigationLocation = new MutableLiveData<>();
  final MutableLiveData<DirectionsRoute> route = new MutableLiveData<>();
  final MutableLiveData<Boolean> shouldRecordScreenshot = new MutableLiveData<>();
  private final MutableLiveData<Point> destination = new MutableLiveData<>();

  private MapboxNavigation navigation;
  private NavigationViewRouter router;
  private LocationEngineConductor locationEngineConductor;
  private NavigationViewEventDispatcher navigationViewEventDispatcher;
  private SpeechPlayer speechPlayer;
  private VoiceInstructionLoader voiceInstructionLoader;
  private VoiceInstructionCache voiceInstructionCache;
  private int voiceInstructionsToAnnounce = 0;
  private RouteProgress routeProgress;
  private String feedbackId;
  private String screenshot;
  private String language;
  private RouteUtils routeUtils;
  private LocaleUtils localeUtils;
  private DistanceFormatter distanceFormatter;
  private String accessToken;
  @NavigationTimeFormat.Type
  private int timeFormatType;
  private boolean isRunning;
  private boolean isChangingConfigurations;

  public NavigationViewModel(Application application) {
    super(application);
    this.accessToken = Mapbox.getAccessToken();
    initializeLocationEngine();
    initializeRouter();
    routeUtils = new RouteUtils();
    localeUtils = new LocaleUtils();
  }

  // Package private (no modifier) for testing purposes
  NavigationViewModel(Application application, MapboxNavigation navigation) {
    super(application);
    this.navigation = navigation;
  }

  // Package private (no modifier) for testing purposes
  NavigationViewModel(Application application, MapboxNavigation navigation,
                      LocationEngineConductor conductor, NavigationViewEventDispatcher dispatcher,
                      VoiceInstructionCache cache, SpeechPlayer speechPlayer) {
    super(application);
    this.navigation = navigation;
    this.locationEngineConductor = conductor;
    this.navigationViewEventDispatcher = dispatcher;
    this.voiceInstructionCache = cache;
    this.speechPlayer = speechPlayer;
  }

  public void onDestroy(boolean isChangingConfigurations) {
    this.isChangingConfigurations = isChangingConfigurations;
    if (!isChangingConfigurations) {
      router.onDestroy();
      endNavigation();
      deactivateInstructionPlayer();
      isRunning = false;
    }
    clearDynamicCameraMap();
    navigationViewEventDispatcher = null;
  }

  public void setMuted(boolean isMuted) {
    speechPlayer.setMuted(isMuted);
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
      navigation.cancelFeedback(feedbackId);
      feedbackId = null;
    }
  }

  /**
   * Returns the current instance of {@link MapboxNavigation}.
   * <p>
   * Will be null if navigation has not been initialized.
   */
  @Nullable
  public MapboxNavigation retrieveNavigation() {
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
  void initialize(NavigationViewOptions options) {
    MapboxNavigationOptions navigationOptions = options.navigationOptions();
    navigationOptions = navigationOptions.toBuilder().isFromNavigationUi(true).build();
    initializeLanguage(options);
    initializeTimeFormat(navigationOptions);
    initializeDistanceFormatter(options);
    if (!isRunning()) {
      LocationEngine locationEngine = initializeLocationEngineFrom(options);
      initializeNavigation(getApplication(), navigationOptions, locationEngine);
      addMilestones(options);
      initializeVoiceInstructionLoader();
      initializeVoiceInstructionCache();
      initializeNavigationSpeechPlayer(options);
    }
    router.extractRouteOptions(options);
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

  boolean isMuted() {
    if (speechPlayer == null) {
      return false;
    }
    return speechPlayer.isMuted();
  }

  void stopNavigation() {
    navigation.removeProgressChangeListener(null);
    navigation.removeMilestoneEventListener(null);
    navigation.stopNavigation();
  }

  boolean isOffRoute() {
    try {
      return isOffRoute.getValue();
    } catch (NullPointerException exception) {
      return false;
    }
  }

  void updateRoute(DirectionsRoute route) {
    this.route.setValue(route);
    if (!isChangingConfigurations) {
      startNavigation(route);
      updateReplayEngine(route);
      sendEventOnRerouteAlong(route);
      isOffRoute.setValue(false);
    }
    resetConfigurationFlag();
  }

  void sendEventFailedReroute(String errorMessage) {
    if (navigationViewEventDispatcher != null) {
      navigationViewEventDispatcher.onFailedReroute(errorMessage);
    }
  }

  MutableLiveData<Point> retrieveDestination() {
    return destination;
  }

  private void initializeRouter() {
    RouteFetcher onlineRouter = new RouteFetcher(getApplication(), accessToken);
    Context applicationContext = getApplication().getApplicationContext();
    ConnectivityStatusProvider connectivityStatus = new ConnectivityStatusProvider(applicationContext);
    router = new NavigationViewRouter(onlineRouter, connectivityStatus, routeEngineListener);
  }

  private void initializeLocationEngine() {
    locationEngineConductor = new LocationEngineConductor();
  }

  private void initializeLanguage(NavigationUiOptions options) {
    RouteOptions routeOptions = options.directionsRoute().routeOptions();
    language = localeUtils.inferDeviceLanguage(getApplication());
    if (routeOptions != null) {
      language = routeOptions.language();
    }
  }

  private String initializeUnitType(NavigationUiOptions options) {
    RouteOptions routeOptions = options.directionsRoute().routeOptions();
    String unitType = localeUtils.getUnitTypeForDeviceLocale(getApplication());
    if (routeOptions != null) {
      unitType = routeOptions.voiceUnits();
    }
    return unitType;
  }

  private void initializeTimeFormat(MapboxNavigationOptions options) {
    timeFormatType = options.timeFormatType();
  }

  private int initializeRoundingIncrement(NavigationViewOptions options) {
    MapboxNavigationOptions navigationOptions = options.navigationOptions();
    return navigationOptions.roundingIncrement();
  }

  private void initializeDistanceFormatter(NavigationViewOptions options) {
    String unitType = initializeUnitType(options);
    int roundingIncrement = initializeRoundingIncrement(options);
    distanceFormatter = new DistanceFormatter(getApplication(), language, unitType, roundingIncrement);
  }

  private void initializeNavigationSpeechPlayer(NavigationViewOptions options) {
    SpeechPlayer speechPlayer = options.speechPlayer();
    if (speechPlayer != null) {
      this.speechPlayer = speechPlayer;
      return;
    }
    boolean isVoiceLanguageSupported = options.directionsRoute().voiceLanguage() != null;
    SpeechPlayerProvider speechPlayerProvider = initializeSpeechPlayerProvider(isVoiceLanguageSupported);
    this.speechPlayer = new NavigationSpeechPlayer(speechPlayerProvider);
  }

  private void initializeVoiceInstructionLoader() {
    Cache cache = new Cache(new File(getApplication().getCacheDir(), OKHTTP_INSTRUCTION_CACHE),
      TEN_MEGABYTE_CACHE_SIZE);
    voiceInstructionLoader = new VoiceInstructionLoader(getApplication(), accessToken, cache);
  }

  private void initializeVoiceInstructionCache() {
    voiceInstructionCache = new VoiceInstructionCache(navigation, voiceInstructionLoader);
  }

  @NonNull
  private SpeechPlayerProvider initializeSpeechPlayerProvider(boolean voiceLanguageSupported) {
    return new SpeechPlayerProvider(getApplication(), language, voiceLanguageSupported, voiceInstructionLoader);
  }

  private LocationEngine initializeLocationEngineFrom(NavigationViewOptions options) {
    LocationEngine locationEngine = options.locationEngine();
    boolean shouldReplayRoute = options.shouldSimulateRoute();
    locationEngineConductor.initializeLocationEngine(getApplication(), locationEngine, shouldReplayRoute);
    return locationEngineConductor.obtainLocationEngine();
  }

  private void initializeNavigation(Context context, MapboxNavigationOptions options, LocationEngine locationEngine) {
    navigation = new MapboxNavigation(context, accessToken, options, locationEngine);
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
      router.updateLocation(location);
      instructionModel.setValue(new InstructionModel(distanceFormatter, routeProgress));
      summaryModel.setValue(new SummaryModel(getApplication(), distanceFormatter, routeProgress, timeFormatType));
      navigationLocation.setValue(location);
      sendEventArrival(routeProgress);
    }
  };

  private OffRouteListener offRouteListener = new OffRouteListener() {
    @Override
    public void userOffRoute(Location location) {
      speechPlayer.onOffRoute();
      Point newOrigin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
      handleOffRouteEvent(newOrigin);
    }
  };

  private MilestoneEventListener milestoneEventListener = new MilestoneEventListener() {
    @Override
    public void onMilestoneEvent(RouteProgress routeProgress, String instruction, Milestone milestone) {
      voiceInstructionCache.cache();
      playVoiceAnnouncement(milestone);
      updateBannerInstruction(routeProgress, milestone);
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

  private ViewRouteListener routeEngineListener = new NavigationViewRouteEngineListener(this);

  private void startNavigation(DirectionsRoute route) {
    if (route != null) {
      navigation.startNavigation(route);
      voiceInstructionsToAnnounce = 0;
      voiceInstructionCache.preCache(route);
    }
  }

  private void updateReplayEngine(DirectionsRoute route) {
    if (locationEngineConductor.updateSimulatedRoute(route)) {
      LocationEngine replayEngine = locationEngineConductor.obtainLocationEngine();
      navigation.setLocationEngine(replayEngine);
    }
  }

  private void endNavigation() {
    if (navigation != null) {
      navigation.onDestroy();
    }
  }

  private void clearDynamicCameraMap() {
    if (navigation != null) {
      Camera cameraEngine = navigation.getCameraEngine();
      boolean isDynamicCamera = cameraEngine instanceof DynamicCamera;
      if (isDynamicCamera) {
        ((DynamicCamera) cameraEngine).clearMap();
      }
    }
  }

  private void deactivateInstructionPlayer() {
    if (speechPlayer != null) {
      speechPlayer.onDestroy();
    }
  }

  private void playVoiceAnnouncement(Milestone milestone) {
    if (milestone instanceof VoiceInstructionMilestone) {
      voiceInstructionsToAnnounce++;
      voiceInstructionCache.update(voiceInstructionsToAnnounce);
      SpeechAnnouncement announcement = SpeechAnnouncement.builder()
        .voiceInstructionMilestone((VoiceInstructionMilestone) milestone).build();
      announcement = retrieveAnnouncementFromSpeechEvent(announcement);
      speechPlayer.play(announcement);
    }
  }

  private void updateBannerInstruction(RouteProgress routeProgress, Milestone milestone) {
    if (milestone instanceof BannerInstructionMilestone) {
      BannerInstructions instructions = ((BannerInstructionMilestone) milestone).getBannerInstructions();
      instructions = retrieveInstructionsFromBannerEvent(instructions);
      if (instructions != null) {
        BannerInstructionModel model = new BannerInstructionModel(distanceFormatter, routeProgress, instructions);
        bannerInstructionModel.setValue(model);
      }
    }
  }

  private void sendEventFeedback(FeedbackItem feedbackItem) {
    if (navigationViewEventDispatcher != null) {
      navigationViewEventDispatcher.onFeedbackSent(feedbackItem);
    }
  }

  private void sendEventArrival(RouteProgress routeProgress) {
    if (navigationViewEventDispatcher != null && routeUtils.isArrivalEvent(routeProgress)) {
      navigationViewEventDispatcher.onArrival();
    }
  }

  private void handleOffRouteEvent(Point newOrigin) {
    if (navigationViewEventDispatcher != null && navigationViewEventDispatcher.allowRerouteFrom(newOrigin)) {
      navigationViewEventDispatcher.onOffRoute(newOrigin);
      router.findRouteFrom(routeProgress);
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

  private void sendEventOnRerouteAlong(DirectionsRoute route) {
    if (navigationViewEventDispatcher != null && isOffRoute()) {
      navigationViewEventDispatcher.onRerouteAlong(route);
    }
  }

  private void resetConfigurationFlag() {
    if (isChangingConfigurations) {
      isChangingConfigurations = false;
    }
  }

  private SpeechAnnouncement retrieveAnnouncementFromSpeechEvent(SpeechAnnouncement announcement) {
    if (navigationViewEventDispatcher != null) {
      announcement = navigationViewEventDispatcher.onAnnouncement(announcement);
    }
    return announcement;
  }

  private BannerInstructions retrieveInstructionsFromBannerEvent(BannerInstructions instructions) {
    if (navigationViewEventDispatcher != null) {
      instructions = navigationViewEventDispatcher.onBannerDisplay(instructions);
    }
    return instructions;
  }
}