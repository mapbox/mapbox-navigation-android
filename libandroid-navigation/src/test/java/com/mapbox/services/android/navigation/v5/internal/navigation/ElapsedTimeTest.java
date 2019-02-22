package com.mapbox.services.android.navigation.v5.internal.navigation;

import com.mapbox.services.android.navigation.v5.internal.exception.NavigationException;
import com.mapbox.services.android.navigation.v5.internal.navigation.ElapsedTime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ElapsedTimeTest {

  private static final double DELTA = 1E-2;

  @Test(expected = NavigationException.class)
  public void errorThrownIfEndCalledBeforeStart() {
    new ElapsedTime().end();
  }

  @Test
  public void elapsedTime_returnsCorrectTimeInSeconds() {
    ElapsedTime elapsedTime = new ElapsedTime();

    elapsedTime.start();
    someOperation();
    elapsedTime.end();

    long start = elapsedTime.getStart();
    long end = elapsedTime.getEnd();
    long elapsedTimeInNanoseconds = end - start;
    double elapsedTimeInSeconds = elapsedTimeInNanoseconds / 1e+9;
    double roundedTime =  Math.round(elapsedTimeInSeconds * 100d) / 100d;
    assertEquals(roundedTime, elapsedTime.getElapsedTime(), DELTA);
  }

  private void someOperation() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException exception) {
      exception.printStackTrace();
    }
  }
}
