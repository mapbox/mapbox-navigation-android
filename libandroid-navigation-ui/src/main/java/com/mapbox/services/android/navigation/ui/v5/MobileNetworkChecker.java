package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;

import java.util.HashMap;

class MobileNetworkChecker {

  private final HashMap<Integer, Boolean> statusMap;

  MobileNetworkChecker(HashMap<Integer, Boolean> statusMap) {
    this.statusMap = statusMap;
    initialize(statusMap);
  }

  @NonNull
  Boolean isFast(Integer networkType) {
    Boolean isConnectionFast = statusMap.get(networkType);
    if (isConnectionFast == null) {
      isConnectionFast = false;
    }
    return isConnectionFast;
  }

  private void initialize(HashMap<Integer, Boolean> statusMap) {
    statusMap.put(TelephonyManager.NETWORK_TYPE_1xRTT, false);
    statusMap.put(TelephonyManager.NETWORK_TYPE_GPRS, false);
    statusMap.put(TelephonyManager.NETWORK_TYPE_CDMA, false);
    statusMap.put(TelephonyManager.NETWORK_TYPE_EDGE, false);
    statusMap.put(TelephonyManager.NETWORK_TYPE_IDEN, false);
    statusMap.put(TelephonyManager.NETWORK_TYPE_UNKNOWN, false);

    statusMap.put(TelephonyManager.NETWORK_TYPE_EVDO_0, true);
    statusMap.put(TelephonyManager.NETWORK_TYPE_EVDO_A, true);
    statusMap.put(TelephonyManager.NETWORK_TYPE_HSDPA, true);
    statusMap.put(TelephonyManager.NETWORK_TYPE_HSPA, true);
    statusMap.put(TelephonyManager.NETWORK_TYPE_HSUPA, true);
    statusMap.put(TelephonyManager.NETWORK_TYPE_UMTS, true);
    statusMap.put(TelephonyManager.NETWORK_TYPE_EHRPD, true);
    statusMap.put(TelephonyManager.NETWORK_TYPE_EVDO_B, true);
    statusMap.put(TelephonyManager.NETWORK_TYPE_HSPAP, true);
    statusMap.put(TelephonyManager.NETWORK_TYPE_LTE, true);
  }
}