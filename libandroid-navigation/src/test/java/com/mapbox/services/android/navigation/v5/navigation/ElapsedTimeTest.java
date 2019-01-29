package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.services.android.navigation.v5.exception.NavigationException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ElapsedTimeTest {

  @Test(expected = NavigationException.class)
  public void errorThrownIfEndCalledBeforeStart() {
    ElapsedTime.builder().end();
  }

  @Test
  public void elapsedTime() {
    ElapsedTime.Builder builder = ElapsedTime.builder();
    builder.start();
    builder.end();
    long start = builder.getStart();
    long end = builder.getEnd();

    assertEquals(builder.build().getElapsedTime(), end - start);
  }
}
