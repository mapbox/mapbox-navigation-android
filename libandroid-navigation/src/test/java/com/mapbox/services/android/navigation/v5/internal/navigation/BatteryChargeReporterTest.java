package com.mapbox.services.android.navigation.v5.internal.navigation;

import org.junit.Test;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BatteryChargeReporterTest {

  @Test
  public void checksTimerIsScheduled() {
    Timer aTimer = mock(Timer.class);
    TimerTask anyTask = mock(TimerTask.class);
    BatteryChargeReporter theBatteryChargeReporter = new BatteryChargeReporter(aTimer, anyTask);
    long anyPeriodInMilliseconds = TimeUnit.MINUTES.toMillis(1);

    theBatteryChargeReporter.scheduleAt(anyPeriodInMilliseconds);

    verify(aTimer).scheduleAtFixedRate(eq(anyTask), eq(0L), eq(anyPeriodInMilliseconds));
  }

  @Test
  public void checksTimerIsStopped() {
    Timer aTimer = mock(Timer.class);
    TimerTask anyTask = mock(TimerTask.class);
    BatteryChargeReporter theBatteryChargeReporter = new BatteryChargeReporter(aTimer, anyTask);

    theBatteryChargeReporter.stop();

    verify(aTimer).cancel();
  }
}