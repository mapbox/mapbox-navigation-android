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
import android.widget.RemoteViews;

import com.mapbox.services.android.navigation.R;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;
import com.mapbox.services.android.navigation.v5.utils.ManeuverUtils;
import com.mapbox.services.android.navigation.v5.utils.abbreviation.StringAbbreviator;
import com.mapbox.services.api.directions.v5.models.LegStep;

import java.util.Calendar;
import java.util.Locale;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_ID;

/**
 * This is in charge of creating the persistent navigation session notification and updating it.
 */
class NavigationNotification {

  private static final String ROUTE_PROGRESS_STRING_FORMAT = "%tl:%tM %tp%n";
  private static final String END_NAVIGATION_BUTTON_TAG = "endNavigationButtonTag";

  private NotificationCompat.Builder notificationBuilder;
  private NotificationManager notificationManager;
  private MapboxNavigation mapboxNavigation;
  private RemoteViews remoteViewsBig;
  private EndNavigationReceiver receiver;
  private RemoteViews remoteViews;
  private Context context;

  NavigationNotification(Context context, MapboxNavigation mapboxNavigation) {
    this.context = context;
    this.mapboxNavigation = mapboxNavigation;
    initialize();
  }

  private void initialize() {
    notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
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
    remoteViews.setTextViewText(
      R.id.notificationStreetNameTextView,
      StringAbbreviator.deliminator(
        StringAbbreviator.abbreviate(routeProgress.currentLegProgress().currentStep().getName()))
    );
    remoteViewsBig.setTextViewText(
      R.id.notificationStreetNameTextView,
      StringAbbreviator.deliminator(
        StringAbbreviator.abbreviate(routeProgress.currentLegProgress().currentStep().getName()))
    );

    // Distance
    SpannableStringBuilder distanceSpannableStringBuilder = DistanceUtils.distanceFormatterBold(
      routeProgress.currentLegProgress().currentStepProgress().distanceRemaining()
    );

    remoteViewsBig.setTextViewText(R.id.notificationStepDistanceTextView, DistanceUtils.distanceFormatterBold(
      routeProgress.currentLegProgress().currentStepProgress().distanceRemaining()));

    if (routeProgress.currentLegProgress().currentStep().getName() != null) {
      if (!routeProgress.currentLegProgress().currentStep().getName().isEmpty()) {
        distanceSpannableStringBuilder.append(" - ");
      }
    }
    remoteViews.setTextViewText(R.id.notificationStepDistanceTextView, distanceSpannableStringBuilder);

    String arrivalTime = String.format(Locale.US, "Arrive at %s",
      formatArrivalTime(routeProgress.durationRemaining()));
    remoteViews.setTextViewText(R.id.estimatedArrivalTimeTextView, arrivalTime);
    remoteViewsBig.setTextViewText(R.id.estimatedArrivalTimeTextView, arrivalTime);

    LegStep step = routeProgress.currentLegProgress().upComingStep() == null
      ? routeProgress.currentLegProgress().currentStep() : routeProgress.currentLegProgress().upComingStep();

    remoteViews.setImageViewResource(R.id.maneuverSignal, ManeuverUtils.getManeuverResource(step));
    remoteViewsBig.setImageViewResource(R.id.maneuverSignal, ManeuverUtils.getManeuverResource(step));
    notificationManager.notify(NAVIGATION_NOTIFICATION_ID, notificationBuilder.build());
  }

  private static String formatArrivalTime(double routeDuration) {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.SECOND, (int) routeDuration);

    return String.format(Locale.US, ROUTE_PROGRESS_STRING_FORMAT,
      calendar, calendar, calendar);
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