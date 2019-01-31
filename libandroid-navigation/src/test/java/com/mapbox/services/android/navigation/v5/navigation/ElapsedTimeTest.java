package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.services.android.navigation.v5.exception.NavigationException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ElapsedTimeTest {

  @Test(expected = NavigationException.class)
  public void errorThrownIfEndCalledBeforeStart() {
    new ElapsedTime().end();
  }

  @Test
  public void elapsedTime() {
    ElapsedTime elapsedTime = new ElapsedTime();
    elapsedTime.start();
    elapsedTime.end();
    long start = elapsedTime.getStart();
    long end = elapsedTime.getEnd();

    assertEquals(elapsedTime.getElapsedTime(), end - start);
  }
}
