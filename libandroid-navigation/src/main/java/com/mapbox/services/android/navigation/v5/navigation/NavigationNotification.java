package com.mapbox.services.android.navigation.v5.navigation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.LayoutRes;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.R;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;
import com.mapbox.services.android.navigation.v5.utils.ManeuverUtils;
import com.mapbox.services.android.navigation.v5.utils.abbreviation.StringAbbreviator;
import com.mapbox.services.android.navigation.v5.utils.time.TimeUtils;

import java.text.DecimalFormat;
import java.util.Locale;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_ID;

/**
 * This is in charge of creating the persistent navigation session notification and updating it.
 */
class NavigationNotification {

  private static final String NAVIGATION_NOTIFICATION_CHANNEL = "NAVIGATION_NOTIFICATION_CHANNEL";
  private static final String END_NAVIGATION_ACTION = "com.mapbox.intent.action.END_NAVIGATION";

  private NotificationCompat.Builder notificationBuilder;
  private SpannableStringBuilder currentDistanceText;
  private NotificationManager notificationManager;
  private MapboxNavigation mapboxNavigation;
  private DecimalFormat decimalFormat;
  private RemoteViews remoteViewsBig;
  private String currentArrivalTime;
  private RemoteViews remoteViews;
  private String currentStepName;
  private int currentManeuverId;
  private Context context;

  NavigationNotification(Context context, MapboxNavigation mapboxNavigation) {
    this.context = context;
    this.mapboxNavigation = mapboxNavigation;
    initialize();
  }

  Notification buildPersistentNotification(@LayoutRes int layout, @LayoutRes int bigLayout) {
    remoteViewsBig = new RemoteViews(context.getPackageName(), bigLayout);
    remoteViews = new RemoteViews(context.getPackageName(), layout);

    // Will trigger endNavigationBtnReceiver when clicked
    PendingIntent pendingCloseIntent = createPendingCloseIntent();
    remoteViewsBig.setOnClickPendingIntent(R.id.endNavigationButton, pendingCloseIntent);

    // Sets up the top bar notification
    notificationBuilder = new NotificationCompat.Builder(context, NAVIGATION_NOTIFICATION_CHANNEL)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setTicker("Navigation notification")
      .setContent(remoteViews)
      .setCustomBigContentView(remoteViewsBig)
      .setSmallIcon(R.drawable.ic_navigation)
      .setContentIntent(PendingIntent.getActivity(context, 0,
        new Intent(context, NavigationService.class), 0));

    return notificationBuilder.build();
  }

  /**
   * With each location update and new routeProgress, the notification is checked and updated if any
   * information has changed.
   *
   * @param routeProgress the latest RouteProgress object
   */
  void updateDefaultNotification(RouteProgress routeProgress) {
    // Street name
    if (newStepName(routeProgress) || currentStepName == null) {
      addStepName(routeProgress);
    }

    // Distance
    if (newDistanceText(routeProgress) || currentDistanceText == null) {
      addDistanceText(routeProgress);
    }

    // Arrival Time
    if (newArrivalTime(routeProgress) || currentArrivalTime == null) {
      addArrivalTime(routeProgress);
    }

    // Maneuver Image
    LegStep step = routeProgress.currentLegProgress().upComingStep() == null
      ? routeProgress.currentLegProgress().currentStep()
      : routeProgress.currentLegProgress().upComingStep();
    addManeuverImage(step);

    notificationManager.notify(NAVIGATION_NOTIFICATION_ID, notificationBuilder.build());
  }

  void unregisterReceiver() {
    if (context != null) {
      context.unregisterReceiver(endNavigationBtnReceiver);
    }
  }

  private boolean newStepName(RouteProgress routeProgress) {
    return currentStepName != null
      && !currentStepName.contentEquals(routeProgress.currentLegProgress().currentStep().name());
  }

  private void addStepName(RouteProgress routeProgress) {
    currentStepName = routeProgress.currentLegProgress().currentStep().name();
    String formattedStepName = StringAbbreviator.deliminator(
      StringAbbreviator.abbreviate(currentStepName));
    remoteViews.setTextViewText(
      R.id.notificationStreetNameTextView,
      formattedStepName
    );
    remoteViewsBig.setTextViewText(
      R.id.notificationStreetNameTextView,
      formattedStepName
    );
  }

  private boolean newDistanceText(RouteProgress routeProgress) {
    return currentDistanceText != null
      && !currentDistanceText.toString().contentEquals(DistanceUtils.distanceFormatterBold(
      routeProgress.currentLegProgress().currentStepProgress().distanceRemaining(),
      decimalFormat).toString());
  }

  private void addDistanceText(RouteProgress routeProgress) {
    currentDistanceText = DistanceUtils.distanceFormatterBold(
      routeProgress.currentLegProgress().currentStepProgress().distanceRemaining(), decimalFormat);
    remoteViewsBig.setTextViewText(R.id.notificationStepDistanceTextView, currentDistanceText);
    if (!TextUtils.isEmpty(currentStepName)) {
      currentDistanceText.append(" - ");
    }
    remoteViews.setTextViewText(R.id.notificationStepDistanceTextView, currentDistanceText);
  }

  private boolean newArrivalTime(RouteProgress routeProgress) {
    return currentArrivalTime != null && currentArrivalTime.contentEquals(TimeUtils
      .formatArrivalTime(routeProgress.durationRemaining()));
  }

  private void addArrivalTime(RouteProgress routeProgress) {
    currentArrivalTime = TimeUtils.formatArrivalTime(routeProgress.durationRemaining());
    remoteViews.setTextViewText(R.id.estimatedArrivalTimeTextView,
      String.format(Locale.getDefault(),
        context.getString(R.string.notification_arrival_time_format), currentArrivalTime));
    remoteViewsBig.setTextViewText(R.id.estimatedArrivalTimeTextView,
      String.format(Locale.getDefault(),
        context.getString(R.string.notification_arrival_time_format), currentArrivalTime));
  }

  private boolean newManeuverId(LegStep step) {
    return currentManeuverId != ManeuverUtils.getManeuverResource(step);
  }

  private void addManeuverImage(LegStep step) {
    if (newManeuverId(step)) {
      int maneuverResource = ManeuverUtils.getManeuverResource(step);
      currentManeuverId = maneuverResource;
      remoteViews.setImageViewResource(R.id.maneuverSignal, maneuverResource);
      remoteViewsBig.setImageViewResource(R.id.maneuverSignal, maneuverResource);
    }
  }

  private void initialize() {
    notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    decimalFormat = new DecimalFormat(NavigationConstants.DECIMAL_FORMAT);
    createNotificationChannel();
    registerReceiver();
  }

  /**
   * Notification channel setup for devices running Android Oreo or later.
   */
  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel notificationChannel = new NotificationChannel(
        NAVIGATION_NOTIFICATION_CHANNEL, context.getString(R.string.channel_name),
        NotificationManager.IMPORTANCE_LOW);
      notificationManager.createNotificationChannel(notificationChannel);
    }
  }

  private void registerReceiver() {
    if (context != null) {
      context.registerReceiver(endNavigationBtnReceiver, new IntentFilter(END_NAVIGATION_ACTION));
    }
  }

  private PendingIntent createPendingCloseIntent() {
    Intent endNavigationBtn = new Intent(END_NAVIGATION_ACTION);
    return PendingIntent.getBroadcast(context, 0, endNavigationBtn, 0);
  }

  private BroadcastReceiver endNavigationBtnReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(final Context context, final Intent intent) {
      NavigationNotification.this.onEndNavigationBtnClick();
    }
  };

  private void onEndNavigationBtnClick() {
    if (notificationManager != null) {
      notificationManager.cancel(NAVIGATION_NOTIFICATION_ID);
    }
    if (mapboxNavigation != null) {
      mapboxNavigation.endNavigation();
    }
  }
}