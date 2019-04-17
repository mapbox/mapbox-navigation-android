package com.mapbox.services.android.navigation.ui.v5;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RouteCallStatusTest {

  @Test
  public void setResponseReceived_isNoLongerRouting() {
    Date callDate = mock(Date.class);
    RouteCallStatus callStatus = new RouteCallStatus(callDate);

    callStatus.setResponseReceived();

    assertFalse(callStatus.isRouting(mock(Date.class)));
  }

  @Test
  public void isRouting_returnsTrueUnderTwoSeconds() {
    Date callDate = mock(Date.class);
    when(callDate.getTime()).thenReturn(0L);
    Date currentDate = mock(Date.class);
    when((currentDate.getTime())).thenReturn(1000L);
    RouteCallStatus callStatus = new RouteCallStatus(callDate);

    boolean isRouting = callStatus.isRouting(currentDate);

    assertTrue(isRouting);
  }

  @Test
  public void isRouting_returnsFalseOverFiveSeconds() {
    Date callDate = mock(Date.class);
    when(callDate.getTime()).thenReturn(0L);
    Date currentDate = mock(Date.class);
    when((currentDate.getTime())).thenReturn(5100L);
    RouteCallStatus callStatus = new RouteCallStatus(callDate);

    boolean isRouting = callStatus.isRouting(currentDate);

    assertFalse(isRouting);
  }
}