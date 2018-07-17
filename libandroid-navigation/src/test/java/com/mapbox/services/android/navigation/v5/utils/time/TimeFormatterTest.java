package com.mapbox.services.android.navigation.v5.utils.time;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;

public class TimeFormatterTest {

  @Test
  public void checksTwelveHoursTimeFormat() throws Exception {
    Calendar time = Calendar.getInstance();
    int anyYear = 2018;
    int anyMonth = 3;
    int anyDay = 26;
    int sixPm = 18;
    int eighteenMinutes = 18;
    int zeroSeconds = 0;
    time.set(anyYear, anyMonth, anyDay, sixPm, eighteenMinutes, zeroSeconds);
    double elevenMinutes = 663.7;
    int twelveHoursTimeFormatType = 0;
    boolean indifferentDeviceTwentyFourHourFormat = true;

    String formattedTime = TimeFormatter.formatTime(time, elevenMinutes, twelveHoursTimeFormatType,
      indifferentDeviceTwentyFourHourFormat);

    assertEquals("6:29 pm", formattedTime);
  }

  @Test
  public void checksTwentyFourHoursTimeFormat() throws Exception {
    Calendar time = Calendar.getInstance();
    int anyYear = 2018;
    int anyMonth = 3;
    int anyDay = 26;
    int sixPm = 18;
    int eighteenMinutes = 18;
    int zeroSeconds = 0;
    time.set(anyYear, anyMonth, anyDay, sixPm, eighteenMinutes, zeroSeconds);
    double elevenMinutes = 663.7;
    int twentyFourHoursTimeFormatType = 1;
    boolean indifferentDeviceTwentyFourHourFormat = false;

    String formattedTime = TimeFormatter.formatTime(time, elevenMinutes, twentyFourHoursTimeFormatType,
      indifferentDeviceTwentyFourHourFormat);

    assertEquals("18:29", formattedTime);
  }

  @Test
  public void checksDefaultTwelveHoursTimeFormat() throws Exception {
    Calendar time = Calendar.getInstance();
    int anyYear = 2018;
    int anyMonth = 3;
    int anyDay = 26;
    int sixPm = 18;
    int eighteenMinutes = 18;
    int zeroSeconds = 0;
    time.set(anyYear, anyMonth, anyDay, sixPm, eighteenMinutes, zeroSeconds);
    double elevenMinutes = 663.7;
    int noneSpecifiedTimeFormatType = -1;
    boolean deviceTwelveHourFormat = false;

    String formattedTime = TimeFormatter.formatTime(time, elevenMinutes, noneSpecifiedTimeFormatType,
      deviceTwelveHourFormat);

    assertEquals("6:29 pm", formattedTime);
  }

  @Test
  public void checksDefaultTwentyFourHoursTimeFormat() throws Exception {
    Calendar time = Calendar.getInstance();
    int anyYear = 2018;
    int anyMonth = 3;
    int anyDay = 26;
    int sixPm = 18;
    int eighteenMinutes = 18;
    int zeroSeconds = 0;
    time.set(anyYear, anyMonth, anyDay, sixPm, eighteenMinutes, zeroSeconds);
    double elevenMinutes = 663.7;
    int noneSpecifiedTimeFormatType = -1;
    boolean deviceTwentyFourHourFormat = true;

    String formattedTime = TimeFormatter.formatTime(time, elevenMinutes, noneSpecifiedTimeFormatType,
      deviceTwentyFourHourFormat);

    assertEquals("18:29", formattedTime);
  }
}