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
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.services.android.navigation.R;
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;
import com.mapbox.services.android.navigation.v5.utils.ManeuverUtils;

import java.util.Calendar;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_CHANNEL;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_ID;
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

  private void updateNotificationViews(RouteProgress routeProgress) {
    buildRemoteViews();
    updateInstructionText(routeProgress.currentLegProgress().currentStep());
    updateDistanceText(routeProgress);
    Calendar time = Calendar.getInstance();
    String formattedArrivalTime = generateArrivalTime(routeProgress, time);
    updateViewsWith(formattedArrivalTime);
    LegStep step = routeProgress.currentLegProgress().upComingStep() != null
      ? routeProgress.currentLegProgress().upComingStep()
      : routeProgress.currentLegProgress().currentStep();
    updateManeuverImage(step);
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

  private void updateInstructionText(LegStep step) {
    if (hasInstructions(step) && (instructionText == null || newInstructionText(step))) {
      instructionText = step.bannerInstructions().get(0).primary().text();
      collapsedNotificationRemoteViews.setTextViewText(R.id.notificationInstructionText, instructionText);
      expandedNotificationRemoteViews.setTextViewText(R.id.notificationInstructionText, instructionText);
    }
  }

  private boolean hasInstructions(LegStep step) {
    return step.bannerInstructions() != null && !step.bannerInstructions().isEmpty();
  }

  private boolean newInstructionText(LegStep step) {
    return !instructionText.equals(step.bannerInstructions().get(0).primary().text());
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
    return currentDistanceText != null
      && !currentDistanceText.toString().equals(distanceFormatter.formatDistance(
      routeProgress.currentLegProgress().currentStepProgress().distanceRemaining()).toString());
  }

  private void updateViewsWith(String formattedArrivalTime) {
    collapsedNotificationRemoteViews.setTextViewText(R.id.notificationArrivalText, formattedArrivalTime);
    expandedNotificationRemoteViews.setTextViewText(R.id.notificationArrivalText, formattedArrivalTime);
  }

  private void updateManeuverImage(LegStep step) {
    if (newManeuverId(step)) {
      int maneuverResource = ManeuverUtils.getManeuverResource(step);
      currentManeuverId = maneuverResource;
      collapsedNotificationRemoteViews.setImageViewResource(R.id.maneuverImage, maneuverResource);
      expandedNotificationRemoteViews.setImageViewResource(R.id.maneuverImage, maneuverResource);
    }
  }

  private boolean newManeuverId(LegStep step) {
    return currentManeuverId != ManeuverUtils.getManeuverResource(step);
  }

  private void onEndNavigationBtnClick() {
    if (mapboxNavigation != null) {
      mapboxNavigation.stopNavigation();
    }
  }
}