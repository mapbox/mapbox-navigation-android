package com.mapbox.services.android.navigation.v5.navigation;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.LayoutRes;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.mapbox.services.android.navigation.R;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;
import com.mapbox.services.android.navigation.v5.utils.ManeuverUtils;
import com.mapbox.services.android.navigation.v5.utils.abbreviation.StringAbbreviator;
import com.mapbox.services.android.navigation.v5.utils.time.TimeUtils;
import com.mapbox.services.api.directions.v5.models.LegStep;

import java.text.DecimalFormat;
import java.util.Locale;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_ID;

/**
 * This is in charge of creating the persistent navigation session notification and updating it.
 */
class NavigationNotification {

  private static final String END_NAVIGATION_BUTTON_TAG = "endNavigationButtonTag";

  private NotificationCompat.Builder notificationBuilder;
  private NotificationManager notificationManager;
  private MapboxNavigation mapboxNavigation;
  private RemoteViews remoteViewsBig;
  private EndNavigationReceiver receiver;
  private RemoteViews remoteViews;
  private Context context;
  private DecimalFormat decimalFormat;
  private String currentStepName;
  private String currentArrivalTime;
  private SpannableStringBuilder currentDistanceText;
  private int currentManeuverId;

  NavigationNotification(Context context, MapboxNavigation mapboxNavigation) {
    this.context = context;
    this.mapboxNavigation = mapboxNavigation;
    initialize();
  }

  private void initialize() {
    notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    decimalFormat = new DecimalFormat(NavigationConstants.DECIMAL_FORMAT);
  }

  Notification buildPersistentNotification(@LayoutRes int layout, @LayoutRes int bigLayout) {
    remoteViewsBig = new RemoteViews(context.getPackageName(), bigLayout);
    remoteViews = new RemoteViews(context.getPackageName(), layout);

    remoteViewsBig.setOnClickPendingIntent(R.id.endNavigationButton,
      getPendingSelfIntent(context, END_NAVIGATION_BUTTON_TAG));

    // Sets up the top bar notification
    notificationBuilder = new NotificationCompat.Builder(context)
      .setContent(remoteViews)
      .setCustomBigContentView(remoteViewsBig)
      .setSmallIcon(R.drawable.ic_navigation)
      .setContentIntent(PendingIntent.getActivity(context, 0,
        new Intent(context, NavigationService.class), 0));

    IntentFilter filter = new IntentFilter(END_NAVIGATION_BUTTON_TAG);
    filter.addCategory(Intent.CATEGORY_DEFAULT);
    receiver = new EndNavigationReceiver();
    context.registerReceiver(receiver, filter);

    return notificationBuilder.build();
  }

  void onDestroy() {
    try {
      context.unregisterReceiver(receiver);
    } catch (Exception exception) {
      // Empty
    }
  }

  void updateDefaultNotification(RouteProgress routeProgress) {
    // Street name
    if (newStepName(routeProgress)) {
      addStepName(routeProgress);
    } else if (currentStepName == null) {
      addStepName(routeProgress);
    }

    // Distance
    if (newDistanceText(routeProgress)) {
      addDistanceText(routeProgress);
    } else if (currentDistanceText == null) {
      addDistanceText(routeProgress);
    }

    // Arrival Time
    if (newArrivalTime(routeProgress)) {
      addArrivalTime(routeProgress);
    } else if (currentArrivalTime == null) {
      addArrivalTime(routeProgress);
    }

    // Maneuver Image
    LegStep step = routeProgress.currentLegProgress().upComingStep() == null
      ? routeProgress.currentLegProgress().currentStep() : routeProgress.currentLegProgress().upComingStep();
    addManeuverImage(step);

    notificationManager.notify(NAVIGATION_NOTIFICATION_ID, notificationBuilder.build());
  }

  private boolean newStepName(RouteProgress routeProgress) {
    return currentStepName != null
      && !currentStepName.contentEquals(routeProgress.currentLegProgress().currentStep().getName());
  }

  private void addStepName(RouteProgress routeProgress) {
    currentStepName = routeProgress.currentLegProgress().currentStep().getName();
    String formattedStepName = StringAbbreviator.deliminator(StringAbbreviator.abbreviate(currentStepName));
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
      && !currentDistanceText.toString().contentEquals(DistanceUtils.distanceFormatterBold(routeProgress
        .currentLegProgress().currentStepProgress().distanceRemaining(), decimalFormat).toString());
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
      String.format(Locale.getDefault(), "Arrive at %s", currentArrivalTime));
    remoteViewsBig.setTextViewText(R.id.estimatedArrivalTimeTextView,
      String.format(Locale.getDefault(), "Arrive at %s", currentArrivalTime));
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

  private PendingIntent getPendingSelfIntent(Context context, String action) {
    Intent intent = new Intent(END_NAVIGATION_BUTTON_TAG);
    intent.setAction(action);
    return PendingIntent.getBroadcast(context, 0, intent, 0);
  }

  public class EndNavigationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      mapboxNavigation.endNavigation();
    }
  }
}