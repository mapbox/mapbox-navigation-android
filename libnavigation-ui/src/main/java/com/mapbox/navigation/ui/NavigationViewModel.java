package com.mapbox.navigation.ui;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.navigation.base.extensions.LocaleEx;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.internal.util.RouteUtils;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.route.Router;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.base.typedef.TimeFormatType;
import com.mapbox.navigation.core.MapboxDistanceFormatter;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.trip.session.OffRouteObserver;
import com.mapbox.navigation.ui.camera.Camera;
import com.mapbox.navigation.ui.camera.DynamicCamera;
import com.mapbox.navigation.ui.feedback.FeedbackItem;
import com.mapbox.navigation.ui.instruction.BannerInstructionModel;
import com.mapbox.navigation.ui.instruction.InstructionModel;
import com.mapbox.navigation.ui.summary.SummaryModel;
import com.mapbox.navigation.ui.voice.NavigationSpeechPlayer;
import com.mapbox.navigation.ui.voice.SpeechPlayer;
import com.mapbox.navigation.ui.voice.SpeechPlayerProvider;
import com.mapbox.navigation.ui.voice.VoiceInstructionLoader;
import com.mapbox.navigation.utils.extensions.ContextEx;

import org.jetbrains.annotations.TestOnly;

import java.io.File;

import okhttp3.Cache;

public class NavigationViewModel extends AndroidViewModel {

  private static final String EMPTY_STRING = "";
  private static final String OKHTTP_INSTRUCTION_CACHE = "okhttp-instruction-cache";
  private static final long TEN_MEGABYTE_CACHE_SIZE = 10 * 1024 * 1024;

  public final MutableLiveData<InstructionModel> instructionModel = new MutableLiveData<>();
  public final MutableLiveData<BannerInstructionModel> bannerInstructionModel = new MutableLiveData<>();
  public final MutableLiveData<SummaryModel> summaryModel = new MutableLiveData<>();
  public final MutableLiveData<Boolean> isOffRoute = new MutableLiveData<>();
  private final MutableLiveData<Location> navigationLocation = new MutableLiveData<>();
  private final MutableLiveData<DirectionsRoute> route = new MutableLiveData<>();
  private final MutableLiveData<Boolean> shouldRecordScreenshot = new MutableLiveData<>();
  private final MutableLiveData<Point> destination = new MutableLiveData<>();

  private MapboxNavigation navigation;
  private Router router;
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
  private DistanceFormatter distanceFormatter;
  private String accessToken;
  @TimeFormatType
  private int timeFormatType;
  private boolean isRunning;
  private boolean isChangingConfigurations;
  private MapConnectivityController connectivityController;
  private MapOfflineManager mapOfflineManager;
  private NavigationViewModelProgressChangeListener navigationViewVm =
          new NavigationViewModelProgressChangeListener(this);

  private NavigationViewOptions navigationViewOptions;

  public NavigationViewModel(Application application) {
    super(application);
    this.accessToken = Mapbox.getAccessToken();
    initializeLocationEngine();
    this.routeUtils = new RouteUtils();
    this.connectivityController = new MapConnectivityController();
  }

  @TestOnly
  NavigationViewModel(Application application, MapboxNavigation navigation,
                      MapConnectivityController connectivityController, MapOfflineManager mapOfflineManager,
                      Router router) {
    super(application);
    this.navigation = navigation;
    this.router = router;
    this.connectivityController = connectivityController;
    this.mapOfflineManager = mapOfflineManager;
  }

  @TestOnly
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

  @Override
  protected void onCleared() {
    super.onCleared();
    destroyRouter();
  }

  public void onDestroy(boolean isChangingConfigurations) {
    this.isChangingConfigurations = isChangingConfigurations;
    if (!isChangingConfigurations) {
      endNavigation();
      destroyMapOffline();
      deactivateInstructionPlayer();
      isRunning = false;
    }
    clearDynamicCameraMap();
    navigationViewEventDispatcher = null;
  }

  public void setMuted(boolean isMuted) {
    speechPlayer.setMuted(isMuted);
  }

  // TODO need Telemetry impl
//  /**
//   * Records a general feedback item with source
//   */
//  public void recordFeedback(@FeedbackEvent.FeedbackSource String feedbackSource) {
//    feedbackId = navigation.recordFeedback(FeedbackEvent.FEEDBACK_TYPE_GENERAL_ISSUE, EMPTY_STRING, feedbackSource);
//    shouldRecordScreenshot.setValue(true);
//  }

//  /**
//   * Used to update an existing {@link FeedbackItem}
//   * with a feedback type and description.
//   * <p>
//   * Uses cached feedbackId to ensure the proper item is updated.
//   *
//   * @param feedbackItem item to be updated
//   * @since 0.7.0
//   */
//  public void updateFeedback(FeedbackItem feedbackItem) {
//    if (!TextUtils.isEmpty(feedbackId)) {
//      navigation.updateFeedback(feedbackId, feedbackItem.getFeedbackType(), feedbackItem.getDescription(), screenshot);
//      sendEventFeedback(feedbackItem);
//      feedbackId = null;
//      screenshot = null;
//    }
//  }

//  /**
//   * Used to cancel an existing {@link FeedbackItem}.
//   * <p>
//   * Uses cached feedbackId to ensure the proper item is cancelled.
//   *
//   * @since 0.7.0
//   */
//  public void cancelFeedback() {
//    if (!TextUtils.isEmpty(feedbackId)) {
//      navigation.cancelFeedback(feedbackId);
//      feedbackId = null;
//    }
//  }

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
   * This method will pass {@link NavigationOptions} from the {@link NavigationViewOptions}
   * to this view model to be used to initialize {@link MapboxNavigation}.
   *
   * @param options to init MapboxNavigation
   */
  void initialize(NavigationViewOptions options, Router router) {
    NavigationOptions navigationOptions = options.navigationOptions();
    navigationOptions = navigationOptions.toBuilder().build();
    initializeLanguage(options);
    initializeTimeFormat(navigationOptions);
    initializeDistanceFormatter(options);
    if (!isRunning()) {
      LocationEngine locationEngine = initializeLocationEngineFrom(options);
      initializeNavigation(getApplication(), navigationOptions, locationEngine);
      initializeVoiceInstructionLoader();
      initializeVoiceInstructionCache();
      initializeNavigationSpeechPlayer(options);
      initializeMapOfflineManager(options);
    }
    this.router = router;
    this.navigationViewOptions = options;
    router.getRoute(options.directionsRoute().routeOptions(), routeEngineCallback);
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
    navigation.unregisterRouteProgressObserver(navigationViewVm);
    navigation.unregisterLocationObserver(navigationViewVm);
    navigation.stopTripSession();
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

  void updateRouteProgress(RouteProgress routeProgress) {
    this.routeProgress = routeProgress;
    sendEventArrival(routeProgress);
    instructionModel.setValue(new InstructionModel(distanceFormatter, routeProgress));
    summaryModel.setValue(new SummaryModel(getApplication(), distanceFormatter, routeProgress, timeFormatType));
  }

  void updateLocation(Location location) {
    navigationLocation.setValue(location);
  }

  void sendEventFailedReroute(String errorMessage) {
    if (navigationViewEventDispatcher != null) {
      navigationViewEventDispatcher.onFailedReroute(errorMessage);
    }
  }

  MutableLiveData<Location> retrieveNavigationLocation() {
    return navigationLocation;
  }

  MutableLiveData<DirectionsRoute> retrieveRoute() {
    return route;
  }

  MutableLiveData<Point> retrieveDestination() {
    return destination;
  }

  MutableLiveData<Boolean> retrieveShouldRecordScreenshot() {
    return shouldRecordScreenshot;
  }

  private void initializeLocationEngine() {
    locationEngineConductor = new LocationEngineConductor();
  }

  private void initializeLanguage(NavigationUiOptions options) {
    RouteOptions routeOptions = options.directionsRoute().routeOptions();
    language = ContextEx.inferDeviceLanguage(getApplication());
    if (routeOptions != null) {
      language = routeOptions.language();
    }
  }

  private String initializeUnitType(NavigationUiOptions options) {
    RouteOptions routeOptions = options.directionsRoute().routeOptions();
    String unitType = LocaleEx.getUnitTypeForLocale(ContextEx.inferDeviceLocale(getApplication()));
    if (routeOptions != null) {
      unitType = routeOptions.voiceUnits();
    }
    return unitType;
  }

  private void initializeTimeFormat(NavigationOptions options) {
    timeFormatType = options.timeFormatType();
  }

  private int initializeRoundingIncrement(NavigationViewOptions options) {
    NavigationOptions navigationOptions = options.navigationOptions();
    return navigationOptions.roundingIncrement();
  }

  private void initializeDistanceFormatter(NavigationViewOptions options) {
    String unitType = initializeUnitType(options);
    int roundingIncrement = initializeRoundingIncrement(options);
    distanceFormatter = new MapboxDistanceFormatter(getApplication(), language, unitType, roundingIncrement);
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

  private void initializeMapOfflineManager(NavigationViewOptions options) {
    MapOfflineOptions mapOfflineOptions = options.offlineMapOptions();
    if (mapOfflineOptions == null) {
      return;
    }
    String mapStyleUrl = mapOfflineOptions.getStyleUrl();
    Context applicationContext = getApplication().getApplicationContext();
    OfflineManager offlineManager = OfflineManager.getInstance(applicationContext);
    float pixelRatio = applicationContext.getResources().getDisplayMetrics().density;
    OfflineRegionDefinitionProvider definitionProvider = new OfflineRegionDefinitionProvider(mapStyleUrl, pixelRatio);
    OfflineMetadataProvider metadataProvider = new OfflineMetadataProvider();
    RegionDownloadCallback regionDownloadCallback = new RegionDownloadCallback(connectivityController);
    mapOfflineManager = new MapOfflineManager(offlineManager, definitionProvider, metadataProvider,
            connectivityController, regionDownloadCallback);
    navigation.registerRouteProgressObserver(mapOfflineManager);
  }

  private void initializeVoiceInstructionLoader() {
    Cache cache = new Cache(new File(getApplication().getCacheDir(), OKHTTP_INSTRUCTION_CACHE),
            TEN_MEGABYTE_CACHE_SIZE);
    voiceInstructionLoader = new VoiceInstructionLoader(getApplication(), accessToken, cache);
  }

  private void initializeVoiceInstructionCache() {
    ConnectivityStatusProvider connectivityStatus = new ConnectivityStatusProvider(getApplication());
    voiceInstructionCache = new VoiceInstructionCache(navigation, voiceInstructionLoader, connectivityStatus);
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

  private void initializeNavigation(Context context, NavigationOptions options, LocationEngine locationEngine) {
    navigation = new MapboxNavigation(context, accessToken, options, locationEngine);
    addNavigationListeners();
  }

  private void addNavigationListeners() {
    navigation.registerRouteProgressObserver(navigationViewVm);
    navigation.unregisterLocationObserver(navigationViewVm);
    navigation.registerOffRouteObserver(offRouteListener);
//    navigation.addFasterRouteListener(fasterRouteListener); TODO waiting for implementation
  }

  private OffRouteObserver offRouteListener = new OffRouteObserver() {
    @Override
    public void onOffRouteStateChanged(boolean offRoute) {
      if (offRoute) {
        speechPlayer.onOffRoute();
      }
    }

//    @Override
//    public void userOffRoute(Location location) {
//
//      Point newOrigin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
//      handleOffRouteEvent(newOrigin);
//    }
  };

  // TODO Faster route
//  private FasterRouteListener fasterRouteListener = new FasterRouteListener() {
//    @Override
//    public void fasterRouteFound(DirectionsRoute directionsRoute) {
//      updateRoute(directionsRoute);
//    }
//  };

  private Router.Callback routeEngineCallback = new NavigationViewRouteEngineListener(this);

  @SuppressLint("MissingPermission")
  private void startNavigation(DirectionsRoute route) {
    if (route != null) {
      navigation.startTripSession(route);
      voiceInstructionsToAnnounce = 0;
      voiceInstructionCache.preCache(route);
    }
  }

  private void updateReplayEngine(DirectionsRoute route) {
    locationEngineConductor.updateSimulatedRoute(route);
  }

  private void destroyRouter() {
    if (router != null) {
      router.cancel();
    }
  }

  private void endNavigation() {
    if (navigation != null) {
      navigation.onDestroy();
    }
  }

  private void clearDynamicCameraMap() {
    if (navigation != null) {
      Camera cameraEngine = navigationViewOptions.camera();
      boolean isDynamicCamera = cameraEngine instanceof DynamicCamera;
      if (isDynamicCamera) {
        ((DynamicCamera) cameraEngine).clearMap();
      }
    }
  }

  private void destroyMapOffline() {
    if (mapOfflineManager != null) {
      mapOfflineManager.onDestroy();
    }
    connectivityController.assign(null);
  }

  private void deactivateInstructionPlayer() {
    if (speechPlayer != null) {
      speechPlayer.onDestroy();
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

  // TODO find route based on location and route progress
  private void handleOffRouteEvent(Point newOrigin) {
    if (navigationViewEventDispatcher != null && navigationViewEventDispatcher.allowRerouteFrom(newOrigin)) {
      navigationViewEventDispatcher.onOffRoute(newOrigin);
//      route.findRouteFrom(routeProgress);
      isOffRoute.setValue(true);
    }
  }

  // TODO NavigationEvents wait for implementation
//  private void sendNavigationStatusEvent(boolean isRunning) {
//    if (navigationViewEventDispatcher != null) {
//      if (isRunning) {
//        navigationViewEventDispatcher.onNavigationRunning();
//      } else {
//        navigationViewEventDispatcher.onNavigationFinished();
//      }
//    }
//  }

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

  private VoiceInstructions retrieveAnnouncementFromSpeechEvent(VoiceInstructions announcement) {
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