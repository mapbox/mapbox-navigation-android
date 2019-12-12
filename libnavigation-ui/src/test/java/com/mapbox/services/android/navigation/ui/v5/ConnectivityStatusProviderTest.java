package com.mapbox.services.android.navigation.ui.v5;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ConnectivityStatusProviderTest {

  @Test
  public void isConnected_returnsTrueWithConnectedInfo() {
    Context context = buildMockContextForMobile(true);
    ConnectivityStatusProvider statusProvider = new ConnectivityStatusProvider(context);

    boolean isConnected = statusProvider.isConnected();

    assertTrue(isConnected);
  }

  @Test
  public void isConnected_returnsFalseWithDisconnectedInfo() {
    Context context = buildMockContextForMobile(false);
    ConnectivityStatusProvider statusProvider = new ConnectivityStatusProvider(context);

    boolean isConnected = statusProvider.isConnected();

    assertFalse(isConnected);
  }

  @Test
  public void isConnectedFast_returnsTrueWithWifi() {
    Context context = buildMockContextForWifi(ConnectivityManager.TYPE_WIFI, -20);
    ConnectivityStatusProvider statusProvider = new ConnectivityStatusProvider(context);

    boolean isFast = statusProvider.isConnectedFast();

    assertTrue(isFast);
  }

  @Test
  public void isConnectedFast_returnsTrueWithLTE() {
    Context context = buildMockContextForMobile(ConnectivityManager.TYPE_MOBILE, TelephonyManager.NETWORK_TYPE_LTE);
    ConnectivityStatusProvider statusProvider = new ConnectivityStatusProvider(context);

    boolean isFast = statusProvider.isConnectedFast();

    assertTrue(isFast);
  }

  @Test
  public void isConnectedFast_returnsTrueWithEDGE() {
    Context context = buildMockContextForMobile(ConnectivityManager.TYPE_MOBILE, TelephonyManager.NETWORK_TYPE_EDGE);
    ConnectivityStatusProvider statusProvider = new ConnectivityStatusProvider(context);

    boolean isFast = statusProvider.isConnectedFast();

    assertFalse(isFast);
  }

  private Context buildMockContextForMobile(boolean isConnected) {
    Context context = mock(Context.class);
    ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
    NetworkInfo networkInfo = mock(NetworkInfo.class);
    when(networkInfo.isConnected()).thenReturn(isConnected);
    when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
    when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
    WifiManager wifiManager = mock(WifiManager.class);
    WifiInfo wifiInfo = mock(WifiInfo.class);
    when(wifiInfo.getRssi()).thenReturn(-20);
    when(wifiManager.getConnectionInfo()).thenReturn(wifiInfo);
    when(context.getSystemService(Context.WIFI_SERVICE)).thenReturn(wifiManager);
    return context;
  }

  private Context buildMockContextForWifi(int type, int rssiLevel) {
    Context context = mock(Context.class);
    ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
    NetworkInfo networkInfo = mock(NetworkInfo.class);
    when(networkInfo.isConnected()).thenReturn(true);
    when(networkInfo.getType()).thenReturn(type);
    when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
    when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
    WifiManager wifiManager = mock(WifiManager.class);
    WifiInfo wifiInfo = mock(WifiInfo.class);
    when(wifiInfo.getRssi()).thenReturn(rssiLevel);
    when(wifiManager.getConnectionInfo()).thenReturn(wifiInfo);
    when(context.getSystemService(Context.WIFI_SERVICE)).thenReturn(wifiManager);
    return context;
  }

  private Context buildMockContextForMobile(int type, int subType) {
    Context context = mock(Context.class);
    ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
    NetworkInfo networkInfo = mock(NetworkInfo.class);
    when(networkInfo.isConnected()).thenReturn(true);
    when(networkInfo.getType()).thenReturn(type);
    when(networkInfo.getSubtype()).thenReturn(subType);
    when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
    when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager);
    WifiManager wifiManager = mock(WifiManager.class);
    WifiInfo wifiInfo = mock(WifiInfo.class);
    when(wifiInfo.getRssi()).thenReturn(-20);
    when(wifiManager.getConnectionInfo()).thenReturn(wifiInfo);
    when(context.getSystemService(Context.WIFI_SERVICE)).thenReturn(wifiManager);
    return context;
  }
}