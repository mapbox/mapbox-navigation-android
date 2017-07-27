package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

public class NavigationEngine extends HandlerThread implements Handler.Callback {

  static final int MSG_LOCATION_UPDATED = 100;

  private Handler handler;
  private Handler callback;

  public NavigationEngine(String name) {
    super(name);
  }

  public NavigationEngine(String name, int priority) {
    super(name, priority);
  }

  void setCallback(Handler callback) {
    this.callback = callback;
  }

  @Override
  protected void onLooperPrepared() {
    handler = new Handler(getLooper(), this);
  }

  public synchronized void registerNewLocation(Location location) {
    Message msg = Message.obtain(null, MSG_LOCATION_UPDATED, location);
    handler.sendMessage(msg);
  }

  @Override
  public boolean handleMessage(Message msg) {

    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    if (msg.what == MSG_LOCATION_UPDATED) {
      callback.sendMessage(Message.obtain(null, msg.what));
    }

    return true;
  }

}
