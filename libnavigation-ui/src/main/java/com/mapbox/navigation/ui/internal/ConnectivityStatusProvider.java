package com.mapbox.navigation.ui.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;

/**
 * TODO: can be merged with NetworkStatusService in V2.0
 */
public class ConnectivityStatusProvider {

  private final Context context;
  @NonNull
  private final WifiNetworkChecker wifiNetworkChecker;
  @NonNull
  private final MobileNetworkChecker mobileNetworkChecker;

  public ConnectivityStatusProvider(Context applicationContext) {
    this.context = applicationContext;
    this.wifiNetworkChecker = new WifiNetworkChecker(new HashMap<>());
    this.mobileNetworkChecker = new MobileNetworkChecker(new HashMap<>());
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

  @Nullable
  @SuppressLint("MissingPermission")
  private NetworkInfo getNetworkInfo(@NonNull Context context) {
    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    return cm.getActiveNetworkInfo();
  }

  @SuppressLint({"MissingPermission", "WifiManagerPotentialLeak"})
  private int getWifiLevel(@NonNull Context context) {
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