package com.mapbox.navigation.ui;

import android.annotation.SuppressLint;
import android.app.Application;
import android.location.Location;
import android.util.Log;
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
import com.mapbox.core.utils.TextUtils;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.internal.extensions.ContextEx;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.base.trip.model.RouteProgressState;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesObserver;
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter;
import com.mapbox.navigation.core.replay.MapboxReplayer;
import com.mapbox.navigation.core.replay.ReplayLocationEngine;
import com.mapbox.navigation.core.replay.history.ReplayEventBase;
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver;
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper;
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver;
import com.mapbox.navigation.core.trip.session.OffRouteObserver;
import com.mapbox.navigation.core.trip.session.TripSessionState;
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver;
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver;
import com.mapbox.navigation.ui.camera.Camera;
import com.mapbox.navigation.ui.camera.DynamicCamera;
import com.mapbox.navigation.ui.feedback.FeedbackItem;
import com.mapbox.navigation.ui.feedback.FeedbackItemCache;
import com.mapbox.navigation.ui.internal.ConnectivityStatusProvider;
import com.mapbox.navigation.ui.voice.NavigationSpeechPlayer;
import com.mapbox.navigation.ui.voice.SpeechPlayer;
import com.mapbox.navigation.ui.voice.SpeechPlayerProvider;
import com.mapbox.navigation.ui.voice.VoiceInstructionLoader;
import okhttp3.Cache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.mapbox.navigation.base.internal.extensions.LocaleEx.getLocaleDirectionsRoute;
import static com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale;
import static com.mapbox.navigation.core.telemetry.events.FeedbackEvent.UI;

public class NavigationViewModel extends AndroidViewModel {

  private static final String OKHTTP_INSTRUCTION_CACHE = "okhttp-instruction-cache";
  private static final long TEN_MEGABYTE_CACHE_SIZE = 10 * 1024 * 1024;

  private final MutableLiveData<RouteProgress> routeProgress = new MutableLiveData<>();
  private final MutableLiveData<BannerInstructions> bannerInstructions = new MutableLiveData<>();
  private final MutableLiveData<Boolean> isOffRoute = new MutableLiveData<>();
  private final MutableLiveData<Location> navigationLocation = new MutableLiveData<>();
  private final MutableLiveData<DirectionsRoute> route = new MutableLiveData<>();
  private final MutableLiveData<Boolean> shouldRecordScreenshot = new MutableLiveData<>();
  private final MutableLiveData<Boolean> isFeedbackSentSuccess = new MutableLiveData<>();
  private final MutableLiveData<Point> destination = new MutableLiveData<>();
  private final MutableLiveData<Boolean> onFinalDestinationArrival = new MutableLiveData<>();
  private final MutableLiveData<FeedbackItem> latestIncomingFeedbackItem = new MutableLiveData<>();

  private MapboxNavigation navigation;
  @Nullable
  private NavigationViewEventDispatcher navigationViewEventDispatcher;
  @Nullable
  private SpeechPlayer speechPlayer;
  @Nullable
  private VoiceInstructionLoader voiceInstructionLoader;
  @Nullable
  private VoiceInstructionCache voiceInstructionCache;
  private int voiceInstructionsToAnnounce = 0;
  @Nullable
  private FeedbackItem feedbackItem;
  @Nullable
  private String feedbackEncodedScreenShot;
  @Nullable
  private String language;
  @Nullable
  private DistanceFormatter distanceFormatter;
  @Nullable
  private String accessToken;
  private boolean isRunning;
  @NonNull
  private NavigationViewModelProgressObserver navigationProgressObserver =
      new NavigationViewModelProgressObserver(this);

  private NavigationViewOptions navigationViewOptions;
  @NonNull
  private MapboxReplayer mapboxReplayer = new MapboxReplayer();
  private FeedbackItemCache feedbackItemCache;

  public NavigationViewModel(@NonNull Application application) {
    super(application);
    this.accessToken = Mapbox.getAccessToken();
  }

  @TestOnly NavigationViewModel(@NonNull Application application, MapboxNavigation navigation,
                                NavigationViewOptions navigationViewOptions) {
    super(application);
    this.navigation = navigation;
    this.navigationViewOptions = navigationViewOptions;
  }

  @TestOnly NavigationViewModel(@NonNull Application application, MapboxNavigation navigation,
                                NavigationViewEventDispatcher dispatcher,
                                VoiceInstructionCache cache, SpeechPlayer speechPlayer) {
    super(application);
    this.navigation = navigation;
    this.navigationViewEventDispatcher = dispatcher;
    this.voiceInstructionCache = cache;
    this.speechPlayer = speechPlayer;
  }

  public void onDestroy(boolean isChangingConfigurations) {
    if (!isChangingConfigurations) {
      endNavigation();
      deactivateInstructionPlayer();
      isRunning = false;
    }
    clearDynamicCameraMap();
    navigationViewEventDispatcher = null;
  }

  public void setMuted(boolean isMuted) {
    if (speechPlayer != null) {
      speechPlayer.setMuted(isMuted);
    }
  }

  /**
   * Used to update an existing {@link FeedbackItem}
   * with a feedback type and description.
   * <p>
   * @param feedbackItem item to be sent
   */
  public void sendFeedback(FeedbackItem feedbackItem) {
    this.feedbackItem = feedbackItem;
    sendFeedback();
  }

  /**
   * Used to queue a {@link FeedbackItem}
   * with a feedback type and description.
   * <p>
   * @param feedbackItem item to be queued
   */
  public void cacheFeedback(FeedbackItem feedbackItem) {
    this.feedbackItem = feedbackItem;
    cacheFeedback();
  }

  /**
   * Returns the current instance of {@link MapboxNavigation}.
   * <p>
   * Will be null if navigation has not been initialized.
   */
  @Nullable
  MapboxNavigation retrieveNavigation() {
    return navigation;
  }

  @Override
  protected void onCleared() {
    mapboxReplayer.finish();
    super.onCleared();
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
  void initialize(@NonNull NavigationViewOptions options) {
    this.navigationViewOptions = options;

    initializeDistanceFormatter(options);
    initializeLanguage(options);

    if (!isRunning()) {
      NavigationOptions.Builder updatedOptionsBuilder = options.navigationOptions()
              .toBuilder()
              .accessToken(accessToken)
              .isFromNavigationUi(true)
              .distanceFormatter(distanceFormatter);

      LocationEngine locationEngine = initializeLocationEngineFrom(options);
      if (locationEngine != null) {
        updatedOptionsBuilder.locationEngine(locationEngine);
      }

      NavigationOptions updatedOptions = updatedOptionsBuilder.build();
      initializeNavigation(updatedOptions);
      initializeVoiceInstructionLoader();
      initializeVoiceInstructionCache();
      initializeNavigationSpeechPlayer(options);
      initializeFeedbackItemCache();
    }
    navigation.setRoutes(Arrays.asList(options.directionsRoute()));
    navigation.startTripSession();
    voiceInstructionCache.initCache(options.directionsRoute());
  }

  void updateFeedbackScreenshot(String screenshot) {
    feedbackEncodedScreenShot = screenshot;
    sendFeedback();
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
    if (navigation != null) {
      navigation.unregisterRouteProgressObserver(navigationProgressObserver);
      navigation.unregisterLocationObserver(navigationProgressObserver);
      navigation.unregisterRoutesObserver(routesObserver);
      navigation.unregisterOffRouteObserver(offRouteObserver);
      navigation.unregisterBannerInstructionsObserver(bannerInstructionsObserver);
      navigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver);
      navigation.unregisterTripSessionStateObserver(tripSessionStateObserver);
      navigation.stopTripSession();
    }
  }

  void updateRouteProgress(RouteProgress routeProgress) {
    // TODO: Refactor this part and use ArrivalObserver
    if (routeProgress.getCurrentState() == RouteProgressState.ROUTE_COMPLETE) {
      this.onFinalDestinationArrival.setValue(true);
    }
    this.routeProgress.setValue(routeProgress);
  }

  void updateLocation(Location location) {
    navigationLocation.setValue(location);
  }

  void takeScreenshot() {
    clearFeedback();

    shouldRecordScreenshot.setValue(true);
  }

  @NonNull
  LiveData<Location> retrieveNavigationLocation() {
    return navigationLocation;
  }

  @NonNull
  LiveData<DirectionsRoute> retrieveRoute() {
    return route;
  }

  @NonNull
  LiveData<Point> retrieveDestination() {
    return destination;
  }

  @NonNull
  LiveData<Boolean> retrieveShouldRecordScreenshot() {
    return shouldRecordScreenshot;
  }

  @NonNull
  LiveData<Boolean> retrieveIsFeedbackSentSuccess() {
    return isFeedbackSentSuccess;
  }

  @NonNull
  public LiveData<RouteProgress> retrieveRouteProgress() {
    return routeProgress;
  }

  @NonNull
  public LiveData<BannerInstructions> retrieveBannerInstructions() {
    return bannerInstructions;
  }

  @NonNull
  public LiveData<Boolean> retrieveIsOffRoute() {
    return isOffRoute;
  }

  @NonNull
  public LiveData<Boolean> retrieveOnFinalDestinationArrival() {
    return onFinalDestinationArrival;
  }

  @NonNull
  public LiveData<FeedbackItem> retrieveLatestIncomingFeedbackItem() {
    return latestIncomingFeedbackItem;
  }

  NavigationViewOptions getNavigationViewOptions() {
    return navigationViewOptions;
  }

  @Nullable
  DistanceFormatter getDistanceFormatter() {
    if (distanceFormatter == null && navigationViewOptions != null) {
      initializeDistanceFormatter(navigationViewOptions);
    }
    return distanceFormatter;
  }

  List<FeedbackItem> getCachedFeedbackItems() {
    return feedbackItemCache.getFeedbackItems();
  }

  private void initializeDistanceFormatter(@NonNull NavigationViewOptions options) {
    RouteOptions routeOptions = options.directionsRoute().routeOptions();
    if ((routeOptions == null || TextUtils.isEmpty(routeOptions.voiceUnits()))
      && options.navigationOptions().getDistanceFormatter() != null) {
      distanceFormatter = options.navigationOptions().getDistanceFormatter();
    } else {
      distanceFormatter = buildDistanceFormatter(options);
    }
  }

  private void initializeLanguage(@NonNull NavigationViewOptions options) {
    RouteOptions routeOptions = options.directionsRoute().routeOptions();
    if (routeOptions != null) {
      language = routeOptions.language();
    } else {
      language = ContextEx.inferDeviceLanguage(getApplication());
    }
  }

  @Nullable
  private String initializeUnitType(@NonNull NavigationViewOptions options) {
    RouteOptions routeOptions = options.directionsRoute().routeOptions();
    String unitType = getUnitTypeForLocale(ContextEx.inferDeviceLocale(getApplication()));
    if (routeOptions != null) {
      unitType = routeOptions.voiceUnits();
    }
    return unitType;
  }

  @NonNull
  private DistanceFormatter buildDistanceFormatter(@NonNull final NavigationViewOptions options) {
    final String unitType = initializeUnitType(options);
    final int roundingIncrement = options.roundingIncrement();
    final Locale locale = getLocaleDirectionsRoute(options.directionsRoute(), getApplication());
    return new MapboxDistanceFormatter.Builder(getApplication())
        .unitType(unitType)
        .roundingIncrement(roundingIncrement)
        .locale(locale)
        .build();
  }

  private void initializeNavigationSpeechPlayer(@NonNull NavigationViewOptions options) {
    SpeechPlayer speechPlayer = options.speechPlayer();
    if (speechPlayer != null) {
      this.speechPlayer = speechPlayer;
      return;
    }
    boolean isVoiceLanguageSupported = options.directionsRoute().voiceLanguage() != null;
    SpeechPlayerProvider speechPlayerProvider = initializeSpeechPlayerProvider(isVoiceLanguageSupported);
    speechPlayerProvider.setIsFallbackAlwaysEnabled(options.isFallbackAlwaysEnabled());
    this.speechPlayer = new NavigationSpeechPlayer(speechPlayerProvider);
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

  @Nullable
  private LocationEngine initializeLocationEngineFrom(@NonNull final NavigationViewOptions options) {
    if (options.locationEngine() != null) {
      return options.locationEngine();
    } else if (options.shouldSimulateRoute()) {
      ReplayLocationEngine replayLocationEngine = new ReplayLocationEngine(mapboxReplayer);
      final Point lastLocation = getOriginOfRoute(options.directionsRoute());
      ReplayEventBase replayEventOrigin = ReplayRouteMapper.mapToUpdateLocation(0.0, lastLocation);
      mapboxReplayer.pushEvents(Collections.singletonList(replayEventOrigin));
      mapboxReplayer.play();
      return replayLocationEngine;
    } else {
      return null;
    }
  }

  private void initializeNavigation(@NonNull NavigationOptions options) {
    navigation = new MapboxNavigation(options);
    addNavigationListeners();
  }

  private void initializeFeedbackItemCache() {
    feedbackItemCache = FeedbackItemCache.Companion.newInstance();
  }

  private void addNavigationListeners() {
    navigation.registerRouteProgressObserver(navigationProgressObserver);
    navigation.registerLocationObserver(navigationProgressObserver);
    navigation.registerRoutesObserver(routesObserver);
    navigation.registerOffRouteObserver(offRouteObserver);
    navigation.registerBannerInstructionsObserver(bannerInstructionsObserver);
    navigation.registerVoiceInstructionsObserver(voiceInstructionsObserver);
    navigation.registerTripSessionStateObserver(tripSessionStateObserver);
    if (navigationViewOptions.shouldSimulateRoute()) {
      navigation.registerRouteProgressObserver(new ReplayProgressObserver(mapboxReplayer));
    }
  }

  public void setLatestIncomingFeedbackItem(FeedbackItem feedbackItem) {
    latestIncomingFeedbackItem.setValue(feedbackItem);
  }

  private synchronized void sendFeedback() {
    if (feedbackItem != null && !TextUtils.isEmpty(feedbackEncodedScreenShot)) {
      navigation.postUserFeedback(feedbackItem.getFeedbackType(),
              feedbackItem.getDescription(), UI, feedbackEncodedScreenShot,
              feedbackItem.getFeedbackSubType().toArray(new String[0]), null);

      onFeedbackSubmitted(feedbackItem);
      isFeedbackSentSuccess.setValue(true);

      clearFeedback();
    }
  }

  /**
   * Cache a feedback event so that it can be sent at a later time
   */
  private synchronized void cacheFeedback() {
    if (feedbackItem != null && !TextUtils.isEmpty(feedbackEncodedScreenShot)) {
      feedbackItem.setEncodedScreenshot(feedbackEncodedScreenShot);
      feedbackItemCache.addNewFeedbackItem(feedbackItem);
      clearFeedback();
    }
  }

  /**
   * Queue a feedback event so that it can be sent at a later time
   */
  public synchronized void sendCachedFeedback() {
    if (getCachedFeedbackItems() != null) {
      for (FeedbackItem singleFeedbackItem : getCachedFeedbackItems()) {
        navigation.queueUserFeedback(singleFeedbackItem.getFeedbackType(),
                singleFeedbackItem.getDescription(), UI, singleFeedbackItem.getEncodedScreenshot(),
                singleFeedbackItem.getFeedbackSubType().toArray(new String[0]), null);
      }
      navigation.sendQueuedUserFeedback();
      feedbackItemCache.removeAllItems();
    }
  }

  private void clearFeedback() {
    feedbackItem = null;
    feedbackEncodedScreenShot = null;
    isFeedbackSentSuccess.setValue(false);
    shouldRecordScreenshot.setValue(false);
  }

  @NonNull
  private VoiceInstructionsObserver voiceInstructionsObserver = new VoiceInstructionsObserver() {
    @Override
    public void onNewVoiceInstructions(@NotNull VoiceInstructions voiceInstructions) {
      voiceInstructionCache.cache();
      voiceInstructionsToAnnounce++;
      voiceInstructionCache.update(voiceInstructionsToAnnounce);
      speechPlayer.play(retrieveAnnouncementFromSpeechEvent(voiceInstructions));
    }
  };

  @NonNull
  private OffRouteObserver offRouteObserver = new OffRouteObserver() {
    @Override
    public void onOffRouteStateChanged(boolean offRoute) {
      if (offRoute) {
        speechPlayer.onOffRoute();
      }
      isOffRoute.setValue(offRoute);
    }
  };

  @NonNull
  private BannerInstructionsObserver bannerInstructionsObserver = bannerInstructions ->
          this.bannerInstructions.setValue(retrieveInstructionsFromBannerEvent(bannerInstructions));

  @NonNull
  private TripSessionStateObserver tripSessionStateObserver = new TripSessionStateObserver() {
    @Override
    public void onSessionStateChanged(@NotNull TripSessionState tripSessionState) {
      if (tripSessionState == TripSessionState.STOPPED) {
        navigationViewEventDispatcher.onNavigationFinished();
      } else if (tripSessionState == TripSessionState.STARTED) {
        navigationViewEventDispatcher.onNavigationRunning();
        isRunning = true;
      }
    }
  };

  @NonNull
  private RoutesObserver routesObserver = routes -> {
    if (routes.size() > 0) {
      route.setValue(routes.get(0));
      RouteOptions routeOptions = routes.get(0).routeOptions();
      if (routeOptions != null) {
        destination.setValue(routeOptions.coordinates().get(routes.get(0).routeOptions().coordinates().size() - 1));
      }
    }
  };

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

  private void deactivateInstructionPlayer() {
    if (speechPlayer != null) {
      speechPlayer.onDestroy();
    }
  }

  private void onFeedbackSubmitted(FeedbackItem feedbackItem) {
    if (navigationViewEventDispatcher != null) {
      navigationViewEventDispatcher.onFeedbackSent(feedbackItem);
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

  private Point getOriginOfRoute(@NonNull final DirectionsRoute directionsRoute) {
    return PolylineUtils.decode(directionsRoute.geometry(), 6).get(0);
  }
}