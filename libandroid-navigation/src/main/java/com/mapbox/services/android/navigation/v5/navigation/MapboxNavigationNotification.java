package com.mapbox.services.android.navigation.v5.navigation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.api.directions.v5.models.StepManeuver;
import com.mapbox.navigator.BannerInstruction;
import com.mapbox.services.android.navigation.R;
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;

import java.util.Calendar;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_CHANNEL;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_ID;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_UTURN;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_CONTINUE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_DEPART;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_END_OF_ROAD;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_FORK;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_MERGE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_NEW_NAME;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_NOTIFICATION;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ON_RAMP;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROTARY;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT_TURN;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_TURN;
import static com.mapbox.services.android.navigation.v5.utils.time.TimeFormatter.formatTime;

/**
 * This is in charge of creating the persistent navigation session notification and updating it.
 */
class MapboxNavigationNotification implements NavigationNotification {

  private static final String END_NAVIGATION_ACTION = "com.mapbox.intent.action.END_NAVIGATION";
  private static final String SET_BACKGROUND_COLOR = "setBackgroundColor";
  private NotificationManager notificationManager;
  private Notification notification;
  private RemoteViews collapsedNotificationRemoteViews;
  private RemoteViews expandedNotificationRemoteViews;
  private MapboxNavigation mapboxNavigation;
  private SpannableString currentDistanceText;
  private DistanceFormatter distanceFormatter;
  private String instructionText;
  private int currentManeuverId;
  private boolean isTwentyFourHourFormat;
  private String etaFormat;
  private final Context applicationContext;
  private PendingIntent pendingOpenIntent;
  private PendingIntent pendingCloseIntent;

  private BroadcastReceiver endNavigationBtnReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(final Context applicationContext, final Intent intent) {
      MapboxNavigationNotification.this.onEndNavigationBtnClick();
    }
  };

  MapboxNavigationNotification(Context applicationContext, MapboxNavigation mapboxNavigation) {
    this.applicationContext = applicationContext;
    initialize(applicationContext, mapboxNavigation);
  }

  // For testing only
  MapboxNavigationNotification(Context applicationContext, MapboxNavigation mapboxNavigation,
                               Notification notification) {
    this.applicationContext = applicationContext;
    this.notification = notification;
    initialize(applicationContext, mapboxNavigation);
  }

  @Override
  public Notification getNotification() {
    return notification;
  }

  @Override
  public int getNotificationId() {
    return NAVIGATION_NOTIFICATION_ID;
  }

  @Override
  public void updateNotification(RouteProgress routeProgress) {
    updateNotificationViews(routeProgress);
    rebuildNotification();
  }

  @Override
  public void onNavigationStopped(Context applicationContext) {
    unregisterReceiver(applicationContext);
  }

  // Package private (no modifier) for testing purposes
  String generateArrivalTime(RouteProgress routeProgress, Calendar time) {
    MapboxNavigationOptions options = mapboxNavigation.options();
    double legDurationRemaining = routeProgress.currentLegProgress().durationRemaining();
    int timeFormatType = options.timeFormatType();
    String arrivalTime = formatTime(time, legDurationRemaining, timeFormatType, isTwentyFourHourFormat);
    String formattedArrivalTime = String.format(etaFormat, arrivalTime);
    return formattedArrivalTime;
  }

  // Package private (no modifier) for testing purposes
  void updateNotificationViews(RouteProgress routeProgress) {
    buildRemoteViews();
    updateInstructionText(routeProgress.bannerInstruction());
    updateDistanceText(routeProgress);
    Calendar time = Calendar.getInstance();
    String formattedTime = generateArrivalTime(routeProgress, time);
    updateViewsWithArrival(formattedTime);
    LegStep step = routeProgress.currentLegProgress().upComingStep() != null
      ? routeProgress.currentLegProgress().upComingStep()
      : routeProgress.currentLegProgress().currentStep();
    updateManeuverImage(step);
  }

  // Package private (no modifier) for testing purposes
  String retrieveInstructionText() {
    return instructionText;
  }

  // Package private (no modifier) for testing purposes
  int retrieveCurrentManeuverId() {
    return currentManeuverId;
  }

  private void initialize(Context applicationContext, MapboxNavigation mapboxNavigation) {
    this.mapboxNavigation = mapboxNavigation;
    etaFormat = applicationContext.getString(R.string.eta_format);
    initializeDistanceFormatter(applicationContext, mapboxNavigation);
    notificationManager = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
    isTwentyFourHourFormat = DateFormat.is24HourFormat(applicationContext);

    pendingOpenIntent = createPendingOpenIntent(applicationContext);
    pendingCloseIntent = createPendingCloseIntent(applicationContext);

    registerReceiver(applicationContext);
    createNotificationChannel(applicationContext);
    if (notification == null) {
      notification = buildNotification(applicationContext);
    }
  }

  private void initializeDistanceFormatter(Context applicationContext, MapboxNavigation mapboxNavigation) {
    RouteOptions routeOptions = mapboxNavigation.getRoute().routeOptions();
    LocaleUtils localeUtils = new LocaleUtils();
    String language = localeUtils.inferDeviceLanguage(applicationContext);
    String unitType = localeUtils.getUnitTypeForDeviceLocale(applicationContext);
    if (routeOptions != null) {
      language = routeOptions.language();
      unitType = routeOptions.voiceUnits();
    }
    MapboxNavigationOptions mapboxNavigationOptions = mapboxNavigation.options();
    int roundingIncrement = mapboxNavigationOptions.roundingIncrement();
    distanceFormatter = new DistanceFormatter(applicationContext, language, unitType, roundingIncrement);
  }

  private void createNotificationChannel(Context applicationContext) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel notificationChannel = new NotificationChannel(
        NAVIGATION_NOTIFICATION_CHANNEL, applicationContext.getString(R.string.channel_name),
        NotificationManager.IMPORTANCE_LOW);
      notificationManager.createNotificationChannel(notificationChannel);
    }
  }

  private Notification buildNotification(Context applicationContext) {
    String channelId = NAVIGATION_NOTIFICATION_CHANNEL;
    NotificationCompat.Builder builder = new NotificationCompat.Builder(applicationContext, channelId)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setSmallIcon(R.drawable.ic_navigation)
      .setCustomContentView(collapsedNotificationRemoteViews)
      .setCustomBigContentView(expandedNotificationRemoteViews)
      .setOngoing(true);

    if (pendingOpenIntent != null) {
      builder.setContentIntent(pendingOpenIntent);
    }
    return builder.build();
  }

  private void buildRemoteViews() {
    int colorResId = mapboxNavigation.options().defaultNotificationColorId();
    int backgroundColor = ContextCompat.getColor(applicationContext, colorResId);

    int collapsedLayout = R.layout.collapsed_navigation_notification_layout;
    int collapsedLayoutId = R.id.navigationCollapsedNotificationLayout;
    collapsedNotificationRemoteViews = new RemoteViews(applicationContext.getPackageName(), collapsedLayout);
    collapsedNotificationRemoteViews.setInt(collapsedLayoutId, SET_BACKGROUND_COLOR, backgroundColor);

    int expandedLayout = R.layout.expanded_navigation_notification_layout;
    int expandedLayoutId = R.id.navigationExpandedNotificationLayout;
    expandedNotificationRemoteViews = new RemoteViews(applicationContext.getPackageName(), expandedLayout);
    expandedNotificationRemoteViews.setOnClickPendingIntent(R.id.endNavigationBtn, pendingCloseIntent);
    expandedNotificationRemoteViews.setInt(expandedLayoutId, SET_BACKGROUND_COLOR, backgroundColor);
  }

  @Nullable
  private PendingIntent createPendingOpenIntent(Context applicationContext) {
    PackageManager pm = applicationContext.getPackageManager();
    Intent intent = pm.getLaunchIntentForPackage(applicationContext.getPackageName());
    if (intent == null) {
      return null;
    }
    intent.setPackage(null);
    return PendingIntent.getActivity(applicationContext, 0, intent, 0);
  }

  private PendingIntent createPendingCloseIntent(Context applicationContext) {
    Intent endNavigationBtn = new Intent(END_NAVIGATION_ACTION);
    return PendingIntent.getBroadcast(applicationContext, 0, endNavigationBtn, 0);
  }

  private void registerReceiver(Context applicationContext) {
    if (applicationContext != null) {
      applicationContext.registerReceiver(endNavigationBtnReceiver, new IntentFilter(END_NAVIGATION_ACTION));
    }
  }

  private void rebuildNotification() {
    notification = buildNotification(applicationContext);
    notificationManager.notify(NAVIGATION_NOTIFICATION_ID, notification);
  }

  private void unregisterReceiver(Context applicationContext) {
    if (applicationContext != null) {
      applicationContext.unregisterReceiver(endNavigationBtnReceiver);
    }
    if (notificationManager != null) {
      notificationManager.cancel(NAVIGATION_NOTIFICATION_ID);
    }
  }

  private void updateInstructionText(BannerInstruction bannerInstruction) {
    if (bannerInstruction != null && (instructionText == null || newInstructionText(bannerInstruction))) {
      instructionText = bannerInstruction.getPrimary().getText();
      updateViewsWithInstruction(instructionText);
    }
  }

  private void updateViewsWithInstruction(String text) {
    collapsedNotificationRemoteViews.setTextViewText(R.id.notificationInstructionText, text);
    expandedNotificationRemoteViews.setTextViewText(R.id.notificationInstructionText, text);
  }

  private boolean newInstructionText(BannerInstruction bannerInstruction) {
    return !instructionText.equals(bannerInstruction.getPrimary().getText());
  }

  private void updateDistanceText(RouteProgress routeProgress) {
    if (currentDistanceText == null || newDistanceText(routeProgress)) {
      currentDistanceText = distanceFormatter.formatDistance(
        routeProgress.currentLegProgress().currentStepProgress().distanceRemaining());
      collapsedNotificationRemoteViews.setTextViewText(R.id.notificationDistanceText, currentDistanceText);
      expandedNotificationRemoteViews.setTextViewText(R.id.notificationDistanceText, currentDistanceText);
    }
  }

  private boolean newDistanceText(RouteProgress routeProgress) {
    String formattedDistance = distanceFormatter
      .formatDistance(routeProgress.currentLegProgress().currentStepProgress().distanceRemaining())
      .toString();
    String currentDistance = currentDistanceText.toString();
    return currentDistanceText != null && currentDistance != null && !currentDistance.equals(formattedDistance);
  }

  private void updateViewsWithArrival(String time) {
    collapsedNotificationRemoteViews.setTextViewText(R.id.notificationArrivalText, time);
    expandedNotificationRemoteViews.setTextViewText(R.id.notificationArrivalText, time);
  }

  private void updateManeuverImage(@NonNull LegStep step) {
    int maneuverResource = getManeuverResource(step);
    if (currentManeuverId != maneuverResource) {
      currentManeuverId = maneuverResource;
      collapsedNotificationRemoteViews.setImageViewResource(R.id.maneuverImage, maneuverResource);
      expandedNotificationRemoteViews.setImageViewResource(R.id.maneuverImage, maneuverResource);
    }
  }

  private int getManeuverResource(@NonNull LegStep step) {
    StepManeuver maneuver = step.maneuver();
    String maneuverType = maneuver.type();
    String maneuverModifier = maneuver.modifier();
    if (!TextUtils.isEmpty(maneuverModifier)) {
      String drivingSide = step.drivingSide();
      if (isLeftDrivingSideAndRoundaboutOrRotaryOrUturn(maneuverType, maneuverModifier, drivingSide)) {
        return obtainManeuverResourceFrom(maneuverType + maneuverModifier + drivingSide);
      }
      return obtainManeuverResourceFrom(maneuverType + maneuverModifier);
    }
    return obtainManeuverResourceFrom(maneuverType);
  }

  private int obtainManeuverResourceFrom(String maneuver) {
    switch (maneuver) {
      case STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_UTURN:
      case STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_UTURN:
        return R.drawable.ic_maneuver_turn_180;
      case STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_UTURN + STEP_MANEUVER_MODIFIER_LEFT:
      case STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_UTURN + STEP_MANEUVER_MODIFIER_LEFT:
        return R.drawable.ic_maneuver_turn_180_left_driving_side;

      case STEP_MANEUVER_TYPE_ARRIVE + STEP_MANEUVER_MODIFIER_LEFT:
        return R.drawable.ic_maneuver_arrive_left;
      case STEP_MANEUVER_TYPE_ARRIVE + STEP_MANEUVER_MODIFIER_RIGHT:
        return R.drawable.ic_maneuver_arrive_right;
      case STEP_MANEUVER_TYPE_ARRIVE:
        return R.drawable.ic_maneuver_arrive;

      case STEP_MANEUVER_TYPE_DEPART + STEP_MANEUVER_MODIFIER_LEFT:
        return R.drawable.ic_maneuver_depart_left;
      case STEP_MANEUVER_TYPE_DEPART + STEP_MANEUVER_MODIFIER_RIGHT:
        return R.drawable.ic_maneuver_depart_right;
      case STEP_MANEUVER_TYPE_DEPART:
        return R.drawable.ic_maneuver_depart;

      case STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SHARP_RIGHT:
      case STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SHARP_RIGHT:
      case STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SHARP_RIGHT:
        return R.drawable.ic_maneuver_turn_75;
      case STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_RIGHT:
      case STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_RIGHT:
      case STEP_MANEUVER_TYPE_ROUNDABOUT_TURN + STEP_MANEUVER_MODIFIER_RIGHT:
      case STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_RIGHT:
        return R.drawable.ic_maneuver_turn_45;
      case STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT:
      case STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT:
      case STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT:
        return R.drawable.ic_maneuver_turn_30;

      case STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SHARP_LEFT:
      case STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SHARP_LEFT:
      case STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SHARP_LEFT:
        return R.drawable.ic_maneuver_turn_75_left;
      case STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_LEFT:
      case STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_LEFT:
      case STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_LEFT:
      case STEP_MANEUVER_TYPE_ROUNDABOUT_TURN + STEP_MANEUVER_MODIFIER_LEFT:
        return R.drawable.ic_maneuver_turn_45_left;
      case STEP_MANEUVER_TYPE_TURN + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT:
      case STEP_MANEUVER_TYPE_ON_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT:
      case STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT:
        return R.drawable.ic_maneuver_turn_30_left;

      case STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_LEFT:
      case STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT:
        return R.drawable.ic_maneuver_merge_left;
      case STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_RIGHT:
      case STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT:
        return R.drawable.ic_maneuver_merge_right;

      case STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_LEFT:
        return R.drawable.ic_maneuver_off_ramp_left;
      case STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT:
        return R.drawable.ic_maneuver_off_ramp_slight_left;

      case STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_RIGHT:
        return R.drawable.ic_maneuver_off_ramp_right;
      case STEP_MANEUVER_TYPE_OFF_RAMP + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT:
        return R.drawable.ic_maneuver_off_ramp_slight_right;

      case STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_LEFT:
        return R.drawable.ic_maneuver_fork_left;
      case STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT:
        return R.drawable.ic_maneuver_fork_slight_left;
      case STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_RIGHT:
        return R.drawable.ic_maneuver_fork_right;
      case STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT:
        return R.drawable.ic_maneuver_fork_slight_right;
      case STEP_MANEUVER_TYPE_FORK + STEP_MANEUVER_MODIFIER_STRAIGHT:
        return R.drawable.ic_maneuver_fork_straight;
      case STEP_MANEUVER_TYPE_FORK:
        return R.drawable.ic_maneuver_fork;

      case STEP_MANEUVER_TYPE_END_OF_ROAD + STEP_MANEUVER_MODIFIER_LEFT:
        return R.drawable.ic_maneuver_end_of_road_left;
      case STEP_MANEUVER_TYPE_END_OF_ROAD + STEP_MANEUVER_MODIFIER_RIGHT:
        return R.drawable.ic_maneuver_end_of_road_right;

      case STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_LEFT:
      case STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_LEFT:
        return R.drawable.ic_maneuver_roundabout_left;
      case STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_LEFT:
      case STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_LEFT:
        return R.drawable.ic_maneuver_roundabout_sharp_left;
      case STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT:
      case STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT:
        return R.drawable.ic_maneuver_roundabout_slight_left;
      case STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_RIGHT:
      case STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_RIGHT:
        return R.drawable.ic_maneuver_roundabout_right;
      case STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_RIGHT:
      case STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_RIGHT:
        return R.drawable.ic_maneuver_roundabout_sharp_right;
      case STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT:
      case STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT:
        return R.drawable.ic_maneuver_roundabout_slight_right;
      case STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_STRAIGHT:
      case STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_STRAIGHT:
        return R.drawable.ic_maneuver_roundabout_straight;
      case STEP_MANEUVER_TYPE_ROUNDABOUT:
      case STEP_MANEUVER_TYPE_ROTARY:
        return R.drawable.ic_maneuver_roundabout;

      case STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_LEFT + STEP_MANEUVER_MODIFIER_LEFT:
      case STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_LEFT + STEP_MANEUVER_MODIFIER_LEFT:
        return R.drawable.ic_maneuver_roundabout_left_left_driving_side;
      case STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_LEFT + STEP_MANEUVER_MODIFIER_LEFT:
      case STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_LEFT + STEP_MANEUVER_MODIFIER_LEFT:
        return R.drawable.ic_maneuver_roundabout_sharp_left_left_driving_side;
      case STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT + STEP_MANEUVER_MODIFIER_LEFT:
      case STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_LEFT + STEP_MANEUVER_MODIFIER_LEFT:
        return R.drawable.ic_maneuver_roundabout_slight_left_left_driving_side;
      case STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_RIGHT + STEP_MANEUVER_MODIFIER_LEFT:
      case STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_RIGHT + STEP_MANEUVER_MODIFIER_LEFT:
        return R.drawable.ic_maneuver_roundabout_right_left_driving_side;
      case STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SHARP_RIGHT + STEP_MANEUVER_MODIFIER_LEFT:
      case STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SHARP_RIGHT + STEP_MANEUVER_MODIFIER_LEFT:
        return R.drawable.ic_maneuver_roundabout_sharp_right_left_driving_side;
      case STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT + STEP_MANEUVER_MODIFIER_LEFT:
      case STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT + STEP_MANEUVER_MODIFIER_LEFT:
        return R.drawable.ic_maneuver_roundabout_slight_right_left_driving_side;
      case STEP_MANEUVER_TYPE_ROUNDABOUT + STEP_MANEUVER_MODIFIER_STRAIGHT + STEP_MANEUVER_MODIFIER_LEFT:
      case STEP_MANEUVER_TYPE_ROTARY + STEP_MANEUVER_MODIFIER_STRAIGHT + STEP_MANEUVER_MODIFIER_LEFT:
        return R.drawable.ic_maneuver_roundabout_straight_left_driving_side;

      case STEP_MANEUVER_TYPE_MERGE + STEP_MANEUVER_MODIFIER_STRAIGHT:
      case STEP_MANEUVER_TYPE_NOTIFICATION + STEP_MANEUVER_MODIFIER_STRAIGHT:
      case STEP_MANEUVER_TYPE_CONTINUE + STEP_MANEUVER_MODIFIER_STRAIGHT:
      case STEP_MANEUVER_TYPE_NEW_NAME + STEP_MANEUVER_MODIFIER_STRAIGHT:
      default:
        return R.drawable.ic_maneuver_turn_0;
    }
  }

  private boolean isLeftDrivingSideAndRoundaboutOrRotaryOrUturn(String maneuverType, String maneuverModifier,
                                                                String drivingSide) {
    return STEP_MANEUVER_MODIFIER_LEFT.equals(drivingSide) && (STEP_MANEUVER_TYPE_ROUNDABOUT.equals(maneuverType)
      || STEP_MANEUVER_TYPE_ROTARY.equals(maneuverType) || STEP_MANEUVER_MODIFIER_UTURN.equals(maneuverModifier));
  }

  private void onEndNavigationBtnClick() {
    if (mapboxNavigation != null) {
      mapboxNavigation.stopNavigation();
    }
  }
}