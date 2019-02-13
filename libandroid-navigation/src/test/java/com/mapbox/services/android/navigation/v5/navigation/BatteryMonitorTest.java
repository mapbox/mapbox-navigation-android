package com.mapbox.services.android.navigation.v5.navigation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatteryMonitorTest {

  @Test
  public void checksBatteryPercentageIsReturned() {
    BatteryMonitor theBatteryMonitor = new BatteryMonitor();
    Context mockedContext = mock(Context.class);
    Intent mockedIntent = mock(Intent.class);
    BroadcastReceiver mockedBroadcastReceiver = null;
    when(mockedContext.registerReceiver(eq(mockedBroadcastReceiver), any(IntentFilter.class))).thenReturn(mockedIntent);
    when(mockedIntent.getIntExtra(eq(BatteryManager.EXTRA_LEVEL), eq(-1))).thenReturn(25);
    when(mockedIntent.getIntExtra(eq(BatteryManager.EXTRA_SCALE), eq(100))).thenReturn(100);

    float batteryLevelPercentage = theBatteryMonitor.obtainPercentage(mockedContext);

    assertEquals(25.0f, batteryLevelPercentage, 0.1);
  }

  @Test
  public void checksBatteryPercentageUnavailable() {
    BatteryMonitor theBatteryMonitor = new BatteryMonitor();
    Context mockedContext = mock(Context.class);
    Intent nullIntent = null;
    BroadcastReceiver mockedBroadcastReceiver = null;
    when(mockedContext.registerReceiver(eq(mockedBroadcastReceiver), any(IntentFilter.class))).thenReturn(nullIntent);

    float batteryLevelPercentage = theBatteryMonitor.obtainPercentage(mockedContext);

    assertEquals(-1.0f, batteryLevelPercentage, 0.1);
  }

  @Test
  public void checksBatteryIsUsbPlugged() {
    BatteryMonitor theBatteryMonitor = new BatteryMonitor();
    Context mockedContext = mock(Context.class);
    Intent mockedIntent = mock(Intent.class);
    BroadcastReceiver mockedBroadcastReceiver = null;
    when(mockedContext.registerReceiver(eq(mockedBroadcastReceiver), any(IntentFilter.class))).thenReturn(mockedIntent);
    when(mockedIntent.getIntExtra(eq(BatteryManager.EXTRA_PLUGGED), eq(-1))).thenReturn(2);

    boolean isPlugged = theBatteryMonitor.isPluggedIn(mockedContext);

    assertTrue(isPlugged);
  }

  @Test
  public void checksBatteryIsAcPlugged() {
    BatteryMonitor theBatteryMonitor = new BatteryMonitor();
    Context mockedContext = mock(Context.class);
    Intent mockedIntent = mock(Intent.class);
    BroadcastReceiver mockedBroadcastReceiver = null;
    when(mockedContext.registerReceiver(eq(mockedBroadcastReceiver), any(IntentFilter.class))).thenReturn(mockedIntent);
    when(mockedIntent.getIntExtra(eq(BatteryManager.EXTRA_PLUGGED), eq(-1))).thenReturn(1);

    boolean isPlugged = theBatteryMonitor.isPluggedIn(mockedContext);

    assertTrue(isPlugged);
  }

  @Test
  public void checksBatteryIsWirelessPlugged() {
    int anySdkVersionGreaterThanJellyBean = 24;
    SdkVersionChecker jellyBeanSdkVersionChecker = new SdkVersionChecker(anySdkVersionGreaterThanJellyBean);
    BatteryMonitor theBatteryMonitor = new BatteryMonitor(jellyBeanSdkVersionChecker);
    Context mockedContext = mock(Context.class);
    Intent mockedIntent = mock(Intent.class);
    BroadcastReceiver mockedBroadcastReceiver = null;
    when(mockedContext.registerReceiver(eq(mockedBroadcastReceiver), any(IntentFilter.class))).thenReturn(mockedIntent);
    when(mockedIntent.getIntExtra(eq(BatteryManager.EXTRA_PLUGGED), eq(-1))).thenReturn(4);

    boolean isPlugged = theBatteryMonitor.isPluggedIn(mockedContext);

    assertTrue(isPlugged);
  }

  @Test
  public void checksBatteryPluggedUnavailable() {
    BatteryMonitor theBatteryMonitor = new BatteryMonitor();
    Context mockedContext = mock(Context.class);
    Intent nullIntent = null;
    BroadcastReceiver mockedBroadcastReceiver = null;
    when(mockedContext.registerReceiver(eq(mockedBroadcastReceiver), any(IntentFilter.class))).thenReturn(nullIntent);

    boolean isPlugged = theBatteryMonitor.isPluggedIn(mockedContext);

    assertFalse(isPlugged);
  }
}