package com.mapbox.navigation.ui;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.navigation.base.internal.extensions.ContextEx;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.options.MapboxOnboardRouterConfig;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.base.TimeFormat;
import com.mapbox.navigation.core.fasterroute.FasterRouteObserver;
import com.mapbox.navigation.core.internal.MapboxDistanceFormatter;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesObserver;
import com.mapbox.navigation.core.replay.route.ReplayRouteLocationEngine;
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver;
import com.mapbox.navigation.core.trip.session.OffRouteObserver;
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver;
import com.mapbox.navigation.ui.camera.Camera;
import com.mapbox.navigation.ui.camera.DynamicCamera;
import com.mapbox.navigation.ui.feedback.FeedbackItem;
import com.mapbox.navigation.ui.instruction.BannerInstructionModel;
import com.mapbox.navigation.ui.instruction.InstructionModel;
import com.mapbox.navigation.ui.junction.RouteJunctionModel;
import com.mapbox.navigation.ui.legacy.RouteUtils;
import com.mapbox.navigation.ui.summary.SummaryModel;
import com.mapbox.navigation.ui.voice.NavigationSpeechPlayer;
import com.mapbox.navigation.ui.voice.SpeechPlayer;
import com.mapbox.navigation.ui.voice.SpeechPlayerProvider;
import com.mapbox.navigation.ui.voice.VoiceInstructionLoader;
import okhttp3.Cache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.mapbox.navigation.base.internal.extensions.LocaleEx.getLocaleDirectionsRoute;
import static com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale;
import static com.mapbox.navigation.core.telemetry.events.FeedbackEvent.UI;

public class NavigationViewModel extends AndroidViewModel {

  private static final String EMPTY_STRING = "";
  private static final String OKHTTP_INSTRUCTION_CACHE = "okhttp-instruction-cache";
  private static final long TEN_MEGABYTE_CACHE_SIZE = 10 * 1024 * 1024;

  private final MutableLiveData<InstructionModel> instructionModel = new MutableLiveData<>();
  private final MutableLiveData<BannerInstructionModel> bannerInstructionModel = new MutableLiveData<>();
  private final MutableLiveData<SummaryModel> summaryModel = new MutableLiveData<>();
  private final MutableLiveData<Boolean> isOffRoute = new MutableLiveData<>();
  private final MutableLiveData<RouteJunctionModel> routeJunctionModel = new MutableLiveData<>();
  private final MutableLiveData<Location> navigationLocation = new MutableLiveData<>();
  private final MutableLiveData<DirectionsRoute> route = new MutableLiveData<>();
  private final MutableLiveData<Boolean> shouldRecordScreenshot = new MutableLiveData<>();
  private final MutableLiveData<Boolean> isFeedbackSentSuccess = new MutableLiveData<>();
  private final MutableLiveData<Point> destination = new MutableLiveData<>();
  private final MutableLiveData<Location> locationUpdates = new MutableLiveData<>();
  private final MutableLiveData<RouteProgress> routeProgressUpdates = new MutableLiveData<>();

  private MapboxNavigation navigation;
  private LocationEngineConductor locationEngineConductor;
  private NavigationViewEventDispatcher navigationViewEventDispatcher;
  private SpeechPlayer speechPlayer;
  private VoiceInstructionLoader voiceInstructionLoader;
  private VoiceInstructionCache voiceInstructionCache;
  private int voiceInstructionsToAnnounce = 0;
  private RouteProgress routeProgress;
  private FeedbackItem feedbackItem;
  private String language;
  private RouteUtils routeUtils;
  private DistanceFormatter distanceFormatter;
  private String accessToken;
  @TimeFormat.Type
  private int timeFormatType;
  private boolean isRunning;
  private boolean isChangingConfigurations;
  private boolean arrivedAtFinalDestination = false;
  private MapConnectivityController connectivityController;
  private MapOfflineManager mapOfflineManager;
  private NavigationViewModelProgressObserver navigationProgressObserver =
      new NavigationViewModelProgressObserver(this);

  private NavigationViewOptions navigationViewOptions;

  public NavigationViewModel(Application application) {
    super(application);
    this.accessToken = Mapbox.getAccessToken();
    initializeLocationEngine();
    this.routeUtils = new RouteUtils();
    this.connectivityController = new MapConnectivityController();
  }

  @TestOnly NavigationViewModel(Application application, MapboxNavigation navigation,
      MapConnectivityController connectivityController, MapOfflineManager mapOfflineManager) {
    super(application);
    this.navigation = navigation;
    this.connectivityController = connectivityController;
    this.mapOfflineManager = mapOfflineManager;
  }

  @TestOnly NavigationViewModel(Application application, MapboxNavigation navigation,
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
    this.feedbackItem = feedbackItem;
    isFeedbackSentSuccess.setValue(false);
    shouldRecordScreenshot.setValue(true);
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
   * This method will pass {@link NavigationOptions} from the {@link NavigationViewOptions}
   * to this view model to be used to initialize {@link MapboxNavigation}.
   *
   * @param options to init MapboxNavigation
   */
  @SuppressLint("MissingPermission")
  void initialize(NavigationViewOptions options) {
    initializeLanguage(options);
    NavigationOptions.Builder updatedOptionsBuilder = options.navigationOptions()
      .toBuilder()
      .isFromNavigationUi(true);

    if (options.navigationOptions().getDistanceFormatter() == null) {
      this.distanceFormatter = buildDistanceFormatter(options);
      updatedOptionsBuilder.distanceFormatter(distanceFormatter);
    } else {
      this.distanceFormatter = options.navigationOptions().getDistanceFormatter();
    }

    if (options.navigationOptions().getOnboardRouterConfig() == null) {
      MapboxOnboardRouterConfig routerConfig =
        MapboxNavigation.defaultNavigationOptions(getApplication(), accessToken).getOnboardRouterConfig();
      updatedOptionsBuilder.onboardRouterConfig(routerConfig);
    }

    NavigationOptions updatedOptions = updatedOptionsBuilder.build();
    initializeTimeFormat(updatedOptions);
    if (!isRunning()) {
      LocationEngine locationEngine = initializeLocationEngineFrom(options);
      initializeNavigation(getApplication(), updatedOptions, locationEngine);
      initializeVoiceInstructionLoader();
      initializeVoiceInstructionCache();
      initializeNavigationSpeechPlayer(options);
      initializeMapOfflineManager(options);
    }
    this.navigationViewOptions = options;
    navigation.setRoutes(Arrays.asList(options.directionsRoute()));
    navigation.startTripSession();
    voiceInstructionCache.initCache(options.directionsRoute());
  }

  void updateFeedbackScreenshot(String screenshot) {
    if (feedbackItem != null) {
      MapboxNavigation.postUserFeedback(feedbackItem.getFeedbackType(),
        feedbackItem.getDescription(), UI, screenshot);
      sendEventFeedback(feedbackItem);
      isFeedbackSentSuccess.setValue(true);
      feedbackItem = null;
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
    navigation.unregisterRouteProgressObserver(navigationProgressObserver);
    navigation.unregisterLocationObserver(navigationProgressObserver);
    navigation.unregisterRoutesObserver(routesObserver);
    navigation.unregisterOffRouteObserver(offRouteObserver);
    navigation.unregisterBannerInstructionsObserver(bannerInstructionsObserver);
    navigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver);
    navigation.detachFasterRouteObserver();
    navigation.stopTripSession();
  }

  boolean isOffRoute() {
    try {
      return isOffRoute.getValue();
    } catch (NullPointerException exception) {
      return false;
    }
  }

  void updateRouteProgress(RouteProgress routeProgress) {
    this.routeProgress = routeProgress;
    sendEventArrival(routeProgress);
    if (routeUtils.deviceCloseEnoughToFinalDestination(routeProgress,
        navigationViewOptions.maxMetersToTriggerDestinationArrival())
        && !arrivedAtFinalDestination) {
      sendEventFinalDestinationArrival();
    }
    instructionModel.setValue(new InstructionModel(distanceFormatter, routeProgress));
    summaryModel.setValue(new SummaryModel(getApplication(), distanceFormatter, routeProgress, timeFormatType));
    routeJunctionModel.setValue(new RouteJunctionModel(routeProgress));
    routeProgressUpdates.setValue(routeProgress);
  }

  void updateLocation(Location location) {
    navigationLocation.setValue(location);
  }

  private void updateBannerInstruction(BannerInstructions bannerInstructions) {
    BannerInstructions instructions = retrieveInstructionsFromBannerEvent(bannerInstructions);
    if (instructions != null) {
      BannerInstructionModel model = new BannerInstructionModel(distanceFormatter, routeProgress, instructions);
      bannerInstructionModel.setValue(model);
    }
  }

  void sendEventFailedReroute(String errorMessage) {
    if (navigationViewEventDispatcher != null) {
      navigationViewEventDispatcher.onFailedReroute(errorMessage);
    }
  }

  LiveData<Location> retrieveNavigationLocation() {
    return navigationLocation;
  }

  LiveData<DirectionsRoute> retrieveRoute() {
    return route;
  }

  LiveData<Point> retrieveDestination() {
    return destination;
  }

  LiveData<Boolean> retrieveShouldRecordScreenshot() {
    return shouldRecordScreenshot;
  }

  LiveData<Boolean> retrieveIsFeedbackSentSuccess() {
    return isFeedbackSentSuccess;
  }

  LiveData<Location> retrieveLocationUpdates() {
    return locationUpdates;
  }

  LiveData<RouteProgress> retrieveRouteProgressUpdates() {
    return routeProgressUpdates;
  }

  public LiveData<InstructionModel> retrieveInstructionModel() {
    return instructionModel;
  }

  public LiveData<RouteJunctionModel> retrieveRouteJunctionModelUpdates() {
    return routeJunctionModel;
  }

  public LiveData<BannerInstructionModel> retrieveBannerInstructionModel() {
    return bannerInstructionModel;
  }

  public LiveData<Boolean> retrieveIsOffRoute() {
    return isOffRoute;
  }

  public LiveData<SummaryModel> retrieveSummaryModel() {
    return summaryModel;
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
    String unitType = getUnitTypeForLocale(ContextEx.inferDeviceLocale(getApplication()));
    if (routeOptions != null) {
      unitType = routeOptions.voiceUnits();
    }
    return unitType;
  }

  private void initializeTimeFormat(NavigationOptions options) {
    timeFormatType = options.getTimeFormatType();
  }

  private DistanceFormatter buildDistanceFormatter(final NavigationViewOptions options) {
    final String unitType = initializeUnitType(options);
    final int roundingIncrement = options.roundingIncrement();
    final Locale locale = getLocaleDirectionsRoute(options.directionsRoute(), getApplication());
    return new MapboxDistanceFormatter.Builder(getApplication())
            .withUnitType(unitType)
            .withRoundingIncrement(roundingIncrement)
            .withLocale(locale)
            .build();
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

  private LocationEngine initializeLocationEngineFrom(final NavigationViewOptions options) {
    final LocationEngine locationEngine = options.locationEngine();
    final boolean shouldReplayRoute = options.shouldSimulateRoute();
    locationEngineConductor.initializeLocationEngine(getApplication(), locationEngine, shouldReplayRoute);

    final LocationEngine locationEngineToReturn = locationEngineConductor.obtainLocationEngine();
    if (locationEngineToReturn instanceof ReplayRouteLocationEngine) {
      final Point lastLocation = getOriginOfRoute(options.directionsRoute());
      ((ReplayRouteLocationEngine) locationEngineToReturn).assignLastLocation(lastLocation);
      ((ReplayRouteLocationEngine) locationEngineToReturn).assign(options.directionsRoute());
    }
    return locationEngineToReturn;
  }

  private void initializeNavigation(Context context, NavigationOptions options, LocationEngine locationEngine) {
    navigation = new MapboxNavigation(context, options, locationEngine);
    addNavigationListeners();
  }

  private void addNavigationListeners() {
    navigation.registerRouteProgressObserver(navigationProgressObserver);
    navigation.registerLocationObserver(navigationProgressObserver);
    navigation.registerRoutesObserver(routesObserver);
    navigation.registerOffRouteObserver(offRouteObserver);
    navigation.registerBannerInstructionsObserver(bannerInstructionsObserver);
    navigation.registerVoiceInstructionsObserver(voiceInstructionsObserver);
    navigation.attachFasterRouteObserver(fasterRouteObserver);
  }

  private VoiceInstructionsObserver voiceInstructionsObserver = new VoiceInstructionsObserver() {
    @Override
    public void onNewVoiceInstructions(@NotNull VoiceInstructions voiceInstructions) {
      voiceInstructionCache.cache();
      voiceInstructionsToAnnounce++;
      voiceInstructionCache.update(voiceInstructionsToAnnounce);
      speechPlayer.play(retrieveAnnouncementFromSpeechEvent(voiceInstructions));
    }
  };

  private OffRouteObserver offRouteObserver = new OffRouteObserver() {
    @Override
    public void onOffRouteStateChanged(boolean offRoute) {
      if (offRoute) {
        speechPlayer.onOffRoute();
      }
      isOffRoute.setValue(offRoute);
    }
  };

  private BannerInstructionsObserver bannerInstructionsObserver = new BannerInstructionsObserver() {
    @Override
    public void onNewBannerInstructions(@NotNull BannerInstructions bannerInstructions) {
      updateBannerInstruction(bannerInstructions);
    }
  };

  private void updateRoute(DirectionsRoute route) {
    this.route.setValue(route);
    if (!isChangingConfigurations) {
      startNavigation(route);
      updateReplayEngine(route);
      sendEventOnRerouteAlong(route);
      isOffRoute.setValue(false);
    }
    resetConfigurationFlag();
  }

  private FasterRouteObserver fasterRouteObserver = new FasterRouteObserver() {
    @Override
    public void onFasterRoute(
            @NotNull DirectionsRoute currentRoute,
            @NotNull DirectionsRoute alternativeRoute,
            boolean isAlternativeFaster
    ) {
      if (isAlternativeFaster) {
        updateRoute(alternativeRoute);
      }
    }

    @Override
    public long restartAfterMillis() {
      return TimeUnit.MINUTES.toMillis(2);
    }
  };

  private RoutesObserver routesObserver = new RoutesObserver() {
    @Override
    public void onRoutesChanged(@NotNull List<? extends DirectionsRoute> routes) {
      if (routes.size() > 0) {
        route.setValue(routes.get(0));
      }
    }
  };

  @SuppressLint("MissingPermission")
  private void startNavigation(DirectionsRoute route) {
    if (route != null) {
      navigation.setRoutes(Arrays.asList(route));
      navigation.startTripSession();
      voiceInstructionsToAnnounce = 0;
      voiceInstructionCache.initCache(route);
    }
  }

  private void updateReplayEngine(DirectionsRoute route) {
    locationEngineConductor.updateSimulatedRoute(route);
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

  private void sendEventFinalDestinationArrival() {
    if (navigationViewEventDispatcher != null) {
      navigationViewEventDispatcher.onFinalDestinationArrival();
      arrivedAtFinalDestination = true;
    }
  }

  // TODO NavigationEvents wait for implementation
  /*private void sendNavigationStatusEvent(boolean isRunning) {
    if (navigationViewEventDispatcher != null) {
      if (isRunning) {
        navigationViewEventDispatcher.onNavigationRunning();
      } else {
        navigationViewEventDispatcher.onNavigationFinished();
      }
    }
  }*/

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

  private Point getOriginOfRoute(final DirectionsRoute directionsRoute) {
    return PolylineUtils.decode(directionsRoute.geometry(), 6).get(0);
  }
}