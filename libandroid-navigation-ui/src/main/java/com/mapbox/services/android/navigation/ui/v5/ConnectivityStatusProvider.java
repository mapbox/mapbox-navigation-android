package com.mapbox.services.android.navigation.ui.v5;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.util.HashMap;

/**
 * TODO internal package
 */
public class ConnectivityStatusProvider {

  private final Context context;
  private final WifiNetworkChecker wifiNetworkChecker;
  private final MobileNetworkChecker mobileNetworkChecker;

  public ConnectivityStatusProvider(Context applicationContext) {
    this.context = applicationContext;
    this.wifiNetworkChecker = new WifiNetworkChecker(new HashMap<Integer, Boolean>());
    this.mobileNetworkChecker = new MobileNetworkChecker(new HashMap<Integer, Boolean>());
  }

  public boolean isConnectedFast() {
    NetworkInfo info = getNetworkInfo(context);
    int wifiLevel = getWifiLevel(context);
    return (info != null
      && info.isConnected()
      && isConnectionFast(info.getType(), info.getSubtype(), wifiLevel));
  }

  public boolean isConnected() {
    NetworkInfo info = getNetworkInfo(context);
    return (info != null && info.isConnected());
  }

  @SuppressLint("MissingPermission")
  private NetworkInfo getNetworkInfo(Context context) {
    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    return cm.getActiveNetworkInfo();
  }

  @SuppressLint({"MissingPermission", "WifiManagerPotentialLeak"})
  private int getWifiLevel(Context context) {
    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    int numberOfLevels = 5;
    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    return WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels);
  }

  private boolean isConnectionFast(int type, int networkType, int wifiLevel) {
    if (type == ConnectivityManager.TYPE_WIFI) {
      return wifiNetworkChecker.isFast(wifiLevel);
    } else if (type == ConnectivityManager.TYPE_MOBILE) {
      return mobileNetworkChecker.isFast(networkType);
    } else {
      return false;
    }
  }
}