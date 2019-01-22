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

import static com.mapbox.services.android.navigation.v5.navigation.EndNavigationBroadcastReceiver.END_NAVIGATION_ACTION;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_CHANNEL;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_ID;
import static com.mapbox.services.android.navigation.v5.utils.time.TimeFormatter.formatTime;

class MapboxNavigationNotification implements NavigationNotification {

  private NotificationManager notificationManager;
  private Notification notification;
  private RemoteViews notificationRemoteViews;
  private SpannableString currentDistanceText;
  private DistanceFormatter distanceFormatter;
  private String instructionText;
  private int currentManeuverId;
  private boolean isTwentyFourHourFormat;
  private String etaFormat;
  private PendingIntent pendingOpenIntent;
  private PendingIntent pendingCloseIntent;
  private final Context applicationContext;
  private final BroadcastReceiver endNavigationBroadcastReceiver;
  private final MapboxNavigationOptions options;

  MapboxNavigationNotification(Context applicationContext, MapboxNavigation navigation) {
    this.applicationContext = applicationContext;
    this.endNavigationBroadcastReceiver = new EndNavigationBroadcastReceiver(navigation);
    this.options = navigation.options();
    initialize(applicationContext, navigation);
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
  }

  @Override
  public void onNavigationStopped(Context applicationContext) {
    unregisterReceiver(applicationContext);
  }

  private void initialize(Context applicationContext, MapboxNavigation navigation) {
    etaFormat = applicationContext.getString(R.string.eta_format);
    initializeDistanceFormatter(applicationContext, navigation);
    notificationManager = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
    isTwentyFourHourFormat = DateFormat.is24HourFormat(applicationContext);

    pendingOpenIntent = createPendingOpenIntent(applicationContext);
    pendingCloseIntent = createPendingCloseIntent(applicationContext);
    initializeRemoteView();

    registerReceiver(applicationContext);
    createNotificationChannel(applicationContext);
    notification = buildNotification(applicationContext);
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

  private void initializeRemoteView() {
    notificationRemoteViews = new RemoteViews(applicationContext.getPackageName(),
      R.layout.navigation_notification_layout);
    notificationRemoteViews.setOnClickPendingIntent(R.id.endNavigationBtn, pendingCloseIntent);
  }

  private void registerReceiver(Context applicationContext) {
    if (applicationContext != null) {
      applicationContext.registerReceiver(endNavigationBroadcastReceiver, new IntentFilter(END_NAVIGATION_ACTION));
    }
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
      .setCustomContentView(notificationRemoteViews)
      .setOngoing(true);

    if (pendingOpenIntent != null) {
      builder.setContentIntent(pendingOpenIntent);
    }
    return builder.build();
  }

  private void updateNotificationViews(RouteProgress routeProgress) {
    notificationRemoteViews = notificationRemoteViews.clone();
    updateInstructionText(routeProgress.currentLegProgress().currentStep());
    updateDistanceText(routeProgress);
    updateArrivalTime(routeProgress);
    updateManeuverImage(routeProgress);
    notification = buildNotification(applicationContext);

    try {
      notificationManager.notify(NAVIGATION_NOTIFICATION_ID, notification);
    } catch (Exception exception) {
      notificationManager.cancel(NAVIGATION_NOTIFICATION_ID);
    }
  }

  private void unregisterReceiver(Context applicationContext) {
    if (applicationContext != null) {
      applicationContext.unregisterReceiver(endNavigationBroadcastReceiver);
    }
    if (notificationManager != null) {
      notificationManager.cancel(NAVIGATION_NOTIFICATION_ID);
    }
  }

  private void updateInstructionText(LegStep step) {
    if (hasInstructions(step) && (instructionText == null || newInstructionText(step))) {
      instructionText = step.bannerInstructions().get(0).primary().text();
      notificationRemoteViews.setTextViewText(R.id.notificationInstructionText, instructionText);
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
      notificationRemoteViews.setTextViewText(R.id.notificationDistanceText, currentDistanceText);
    }
  }

  private boolean newDistanceText(RouteProgress routeProgress) {
    return currentDistanceText != null
      && !currentDistanceText.toString().equals(distanceFormatter.formatDistance(
      routeProgress.currentLegProgress().currentStepProgress().distanceRemaining()).toString());
  }

  private void updateArrivalTime(RouteProgress routeProgress) {
    Calendar time = Calendar.getInstance();
    double durationRemaining = routeProgress.durationRemaining();
    int timeFormatType = options.timeFormatType();
    String arrivalTime = formatTime(time, durationRemaining, timeFormatType, isTwentyFourHourFormat);
    String formattedArrivalTime = String.format(etaFormat, arrivalTime);
    notificationRemoteViews.setTextViewText(R.id.notificationArrivalText, formattedArrivalTime);
  }

  private void updateManeuverImage(RouteProgress routeProgress) {
    LegStep step = routeProgress.currentLegProgress().upComingStep() != null
      ? routeProgress.currentLegProgress().upComingStep()
      : routeProgress.currentLegProgress().currentStep();
    if (newManeuverId(step)) {
      int maneuverResource = ManeuverUtils.getManeuverResource(step);
      currentManeuverId = maneuverResource;
      notificationRemoteViews.setImageViewResource(R.id.maneuverImage, maneuverResource);
    }
  }

  private boolean newManeuverId(LegStep step) {
    return currentManeuverId != ManeuverUtils.getManeuverResource(step);
  }
}