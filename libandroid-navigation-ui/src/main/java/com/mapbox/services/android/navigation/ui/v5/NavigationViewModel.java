package com.mapbox.services.android.navigation.ui.v5;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackItem;
import com.mapbox.services.android.navigation.ui.v5.instruction.BannerInstructionModel;
import com.mapbox.services.android.navigation.ui.v5.instruction.InstructionModel;
import com.mapbox.services.android.navigation.ui.v5.location.LocationEngineConductor;
import com.mapbox.services.android.navigation.ui.v5.location.LocationEngineConductorListener;
import com.mapbox.services.android.navigation.ui.v5.route.ViewRouteFetcher;
import com.mapbox.services.android.navigation.ui.v5.route.ViewRouteListener;
import com.mapbox.services.android.navigation.ui.v5.route.OffRouteEvent;
import com.mapbox.services.android.navigation.ui.v5.summary.SummaryModel;
import com.mapbox.services.android.navigation.ui.v5.voice.NavigationInstructionPlayer;
import com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationTimeFormat;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.navigation.metrics.FeedbackEvent;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.route.FasterRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;

import java.util.Locale;

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
  private NavigationInstructionPlayer instructionPlayer;
  private ConnectivityManager connectivityManager;
  private RouteProgress routeProgress;
  private String feedbackId;
  private String screenshot;
  private Locale locale;
  @NavigationUnitType.UnitType
  private int unitType;
  @NavigationTimeFormat.Type
  private int timeFormatType;
  private boolean isRunning;
  private String accessToken;

  public NavigationViewModel(Application application) {
    super(application);
    this.accessToken = Mapbox.getAccessToken();
    initConnectivityManager(application);
    initNavigationRouteEngine();
    initNavigationLocationEngine();
  }

  public void onCreate() {
    if (!isRunning) {
      locationEngineConductor.onCreate();
    }
  }

  public void onDestroy(boolean isChangingConfigurations) {
    if (!isChangingConfigurations) {
      locationEngineConductor.onDestroy();
      endNavigation();
      deactivateInstructionPlayer();
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
      navigationViewEventDispatcher.onFeedbackSent(feedbackItem);
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
  void initializeNavigation(NavigationViewOptions options) {
    MapboxNavigationOptions navigationOptions = options.navigationOptions();
    navigationOptions = navigationOptions.toBuilder().isFromNavigationUi(true).build();
    initLocaleInfo(navigationOptions);
    initTimeFormat(navigationOptions);
    initVoiceInstructions();
    if (!isRunning) {
      locationEngineConductor.initializeLocationEngine(getApplication(), options.shouldSimulateRoute());
      initNavigation(getApplication(), navigationOptions);
      navigationViewRouteEngine.extractRouteOptions(getApplication(), options);
    }
  }

  void updateNavigation(NavigationViewOptions options) {
    navigationViewRouteEngine.extractRouteOptions(getApplication(), options);
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

  private void initConnectivityManager(Application application) {
    connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
  }

  private void initNavigationRouteEngine() {
    navigationViewRouteEngine = new ViewRouteFetcher(routeEngineCallback);
    navigationViewRouteEngine.updateAccessToken(accessToken);
  }

  private void initNavigationLocationEngine() {
    locationEngineConductor = new LocationEngineConductor(locationEngineCallback);
  }

  private void initLocaleInfo(MapboxNavigationOptions options) {
    locale = LocaleUtils.getNonNullLocale(getApplication(), options.locale());
    unitType = options.unitType();
  }

  private void initVoiceInstructions() {
    instructionPlayer = new NavigationInstructionPlayer(getApplication(), locale, accessToken);
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

  private void initTimeFormat(MapboxNavigationOptions options) {
    timeFormatType = options.timeFormatType();
  }

  private ProgressChangeListener progressChangeListener = new ProgressChangeListener() {
    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
      NavigationViewModel.this.routeProgress = routeProgress;
      instructionModel.setValue(new InstructionModel(getApplication(), routeProgress, locale, unitType));
      summaryModel.setValue(new SummaryModel(getApplication(), routeProgress, locale, unitType, timeFormatType));
      navigationLocation.setValue(location);
    }
  };

  private OffRouteListener offRouteListener = new OffRouteListener() {
    @Override
    public void userOffRoute(Location location) {
      if (hasNetworkConnection()) {
        Point newOrigin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
        if (navigationViewEventDispatcher.allowRerouteFrom(newOrigin)) {
          navigationViewEventDispatcher.onOffRoute(newOrigin);
          OffRouteEvent event = new OffRouteEvent(newOrigin, routeProgress);
          navigationViewRouteEngine.fetchRouteFromOffRouteEvent(event);
          isOffRoute.setValue(true);
        }
      }
    }
  };

  private MilestoneEventListener milestoneEventListener = new MilestoneEventListener() {
    @Override
    public void onMilestoneEvent(RouteProgress routeProgress, String instruction, Milestone milestone) {
      if (milestone instanceof VoiceInstructionMilestone) {
        instructionPlayer.play((VoiceInstructionMilestone) milestone);
      }
      updateBannerInstruction(routeProgress, milestone);
    }
  };

  private NavigationEventListener navigationEventListener = new NavigationEventListener() {
    @Override
    public void onRunning(boolean isRunning) {
      NavigationViewModel.this.isRunning = isRunning;
      if (isRunning) {
        navigationViewEventDispatcher.onNavigationRunning();
      } else {
        navigationViewEventDispatcher.onNavigationFinished();
      }
    }
  };

  private FasterRouteListener fasterRouteListener = new FasterRouteListener() {
    @Override
    public void fasterRouteFound(DirectionsRoute directionsRoute) {
      updateRoute(directionsRoute);
    }
  };

  private ViewRouteListener routeEngineCallback = new ViewRouteListener() {
    @Override
    public void onRouteUpdate(DirectionsRoute directionsRoute) {
      updateRoute(directionsRoute);
    }

    @Override
    public void onRouteRequestError(Throwable throwable) {
      if (isOffRoute() && navigationViewEventDispatcher != null) {
        String errorMessage = throwable.getMessage();
        navigationViewEventDispatcher.onFailedReroute(errorMessage);
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
    if (isOffRoute()) {
      navigationViewEventDispatcher.onRerouteAlong(route);
    }
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

  private void updateBannerInstruction(RouteProgress routeProgress, Milestone milestone) {
    if (milestone instanceof BannerInstructionMilestone) {
      BannerInstructionMilestone bannerInstructionMilestone = (BannerInstructionMilestone) milestone;
      BannerInstructionModel model = new BannerInstructionModel(
        getApplication(), bannerInstructionMilestone, routeProgress, locale, unitType
      );
      bannerInstructionModel.setValue(model);
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
}
