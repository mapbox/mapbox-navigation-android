package com.mapbox.services.android.navigation.v5.internal.navigation;

import org.junit.Test;

import java.util.Timer;
import java.util.TimerTask;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BatteryChargeReporterTest {

  @Test
  public void checksTimerIsScheduled() {
    Timer aTimer = mock(Timer.class);
    TimerTask anyTask = null;
    BatteryChargeReporter theBatteryChargeReporter = new BatteryChargeReporter(aTimer, anyTask);
    long anyPeriodInMilloseconds = 1 * 60 * 1000;

    theBatteryChargeReporter.scheduleAt(anyPeriodInMilloseconds);

    verify(aTimer).scheduleAtFixedRate(eq(anyTask), eq(0L), eq(anyPeriodInMilloseconds));
  }

  @Test
  public void checksTimerIsStopped() {
    Timer aTimer = mock(Timer.class);
    TimerTask anyTask = null;
    BatteryChargeReporter theBatteryChargeReporter = new BatteryChargeReporter(aTimer, anyTask);

    theBatteryChargeReporter.stop();

    verify(aTimer).cancel();
  }
}