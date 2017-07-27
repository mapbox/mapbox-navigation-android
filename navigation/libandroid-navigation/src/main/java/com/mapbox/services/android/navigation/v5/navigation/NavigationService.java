package com.mapbox.services.android.navigation.v5.navigation;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;

import java.util.List;

import timber.log.Timber;

public class NavigationService extends Service implements LocationEngineListener, Handler.Callback {

  private final IBinder localBinder = new LocalBinder();

  private Notification notifyBuilder;
  private static final int ONGOING_NOTIFICATION_ID = 1;

  private NavigationEngine thread;

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return localBinder;
  }

  @Override
  public void onCreate() {
    thread = new NavigationEngine("NavigationThread", Process.THREAD_PRIORITY_BACKGROUND);
    thread.setCallback(new Handler(this));
    thread.start();

    NotificationManager notificationManager = new NotificationManager(this);
    notifyBuilder = notificationManager.buildPersistentNotification();
    startForeground(ONGOING_NOTIFICATION_ID, notifyBuilder);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Toast.makeText(this, "onStartCommand", Toast.LENGTH_SHORT).show();
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      thread.quitSafely();
    } else {
      thread.quit();
    }
    stopSelf();
    super.onDestroy();
  }

  @Override
  public boolean handleMessage(Message msg) {
    System.out.println("message: " + msg.what);
    return false;
  }

  void setNavigationEventDispatcher(NavigationEventDispatcher navigationEventDispatcher) {

  }

  void setMilestones(List<Milestone> milestones) {

  }

  @Override
  public void onConnected() {

  }

  @Override
  public void onLocationChanged(Location location) {
    thread.registerNewLocation(location);
  }

  public class LocalBinder extends Binder {
    public NavigationService getService() {
      Timber.d("Local binder called.");
      return NavigationService.this;
    }
  }
}
