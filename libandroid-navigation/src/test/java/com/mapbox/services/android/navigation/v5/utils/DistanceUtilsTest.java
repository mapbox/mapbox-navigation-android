package com.mapbox.services.android.navigation.v5.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.mapbox.services.android.navigation.R;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class DistanceUtilsTest {
  private static final double LARGE_LARGE_UNIT = 18124.65;
  private static final double MEDIUM_LARGE_UNIT = 9812.33;
  private static final double SMALL_SMALL_UNIT = 13.71;
  private static final double LARGE_SMALL_UNIT = 109.73;

  @Mock
  private SharedPreferences sharedPreferences;
  @Mock
  private Context context;

  @Before
  public void setup() {
    sharedPreferences = Mockito.mock(SharedPreferences.class);
    context = Mockito.mock(Context.class);
    when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences);
    when(context.getString(R.string.kilometers)).thenReturn("km");
    when(context.getString(R.string.meters)).thenReturn("m");
    when(context.getString(R.string.miles)).thenReturn("mi");
    when(context.getString(R.string.feet)).thenReturn("ft");
  }

  private void setupSharedPreferences(Locale locale, int navigationUnitType) {
    when(sharedPreferences.getString(NavigationConstants.NAVIGATION_VIEW_LOCALE_LANGUAGE, Locale.getDefault().getLanguage())).thenReturn(locale.getLanguage());
    when(sharedPreferences.getString(NavigationConstants.NAVIGATION_VIEW_LOCALE_COUNTRY, Locale.getDefault().getCountry())).thenReturn(locale.getCountry());
    when(sharedPreferences.getInt(NavigationConstants.NAVIGATION_VIEW_UNIT_TYPE, -1)).thenReturn(navigationUnitType);
  }

  @Test
  public void testFormatDistance_noLocaleCountry() {
    setupSharedPreferences(new Locale(Locale.ENGLISH.getLanguage()), NavigationUnitType.TYPE_IMPERIAL);

    assertOutput("11 mi", LARGE_LARGE_UNIT);
  }

  @Test
  public void testFormatDistance_noLocale() {
    setupSharedPreferences(new Locale("", ""), NavigationUnitType.TYPE_IMPERIAL);

    assertOutput("11 mi", LARGE_LARGE_UNIT);
  }

  @Test
  public void testFormatDistance_unitTypeDifferentFromLocale() {
    setupSharedPreferences(Locale.US, NavigationUnitType.TYPE_METRIC);

    assertOutput("18 km", LARGE_LARGE_UNIT);
  }

  @Test
  public void testFormatDistance_largeMiles() {
    setupSharedPreferences(Locale.US, NavigationUnitType.TYPE_IMPERIAL);

    assertOutput("11 mi", LARGE_LARGE_UNIT);
  }

  @Test
  public void testFormatDistance_largeKilometers() {
    setupSharedPreferences(Locale.FRANCE, NavigationUnitType.TYPE_METRIC);

    assertOutput("18 km", LARGE_LARGE_UNIT);
  }

  @Test
  public void testFormatDistance_largeKilometerNoUnitTypeButMetricLocale() {
    setupSharedPreferences(Locale.FRANCE, -1);

    assertOutput("18 km", LARGE_LARGE_UNIT);
  }

  @Test
  public void testFormatDistance_mediumMiles() {
    setupSharedPreferences(Locale.US, NavigationUnitType.TYPE_IMPERIAL);

    assertOutput("6.1 mi", MEDIUM_LARGE_UNIT);
  }

  @Test
  public void testFormatDistance_mediumKilometers() {
    setupSharedPreferences(Locale.FRANCE, NavigationUnitType.TYPE_METRIC);

    assertOutput("9,8 km", MEDIUM_LARGE_UNIT);
  }

  @Test
  public void testFormatDistance_mediumKilometersUnitTypeDifferentFromLocale() {
    setupSharedPreferences(Locale.FRANCE, NavigationUnitType.TYPE_IMPERIAL);

    assertOutput("6,1 mi", MEDIUM_LARGE_UNIT);
  }

  @Test
  public void testFormatDistance_smallFeet() {
    setupSharedPreferences(Locale.US, NavigationUnitType.TYPE_IMPERIAL);

    assertOutput("50 ft", SMALL_SMALL_UNIT);
  }

  @Test
  public void testFormatDistance_smallMeters() {
    setupSharedPreferences(Locale.FRANCE, NavigationUnitType.TYPE_METRIC);

    assertOutput("50 m", SMALL_SMALL_UNIT);
  }

  @Test
  public void testFormatDistance_largeFeet() {
    setupSharedPreferences(Locale.US, NavigationUnitType.TYPE_IMPERIAL);

    assertOutput("350 ft", LARGE_SMALL_UNIT);
  }

  @Test
  public void testFormatDistance_largeMeters() {
    setupSharedPreferences(Locale.FRANCE, NavigationUnitType.TYPE_METRIC);

    assertOutput("100 m", LARGE_SMALL_UNIT);
  }

  private void assertOutput(String output, double distance) {
    Assert.assertEquals(output,
      new DistanceUtils(context).formatDistance(distance).toString());
  }
}
