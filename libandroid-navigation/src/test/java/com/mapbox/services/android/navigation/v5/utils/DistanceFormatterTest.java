package com.mapbox.services.android.navigation.v5.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.android.navigation.R;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Locale;

import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class DistanceFormatterTest {
  private static final double LARGE_LARGE_UNIT = 18124.65;
  private static final double MEDIUM_LARGE_UNIT = 9812.33;
  private static final double SMALL_SMALL_UNIT = 13.71;
  private static final double LARGE_SMALL_UNIT = 109.73;

  @Mock
  private Context context;
  @Mock
  private Resources resources;
  @Mock
  private Configuration configuration;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(context.getResources()).thenReturn(resources);
    when(resources.getConfiguration()).thenReturn(configuration);
    when(configuration.getLocales()).thenReturn(LocaleList.getDefault());
    when(context.getString(R.string.kilometers)).thenReturn("km");
    when(context.getString(R.string.meters)).thenReturn("m");
    when(context.getString(R.string.miles)).thenReturn("mi");
    when(context.getString(R.string.feet)).thenReturn("ft");
  }

  @Test
  public void formatDistance_noLocaleCountry() {
    assertOutput(LARGE_LARGE_UNIT, new Locale(Locale.ENGLISH.getLanguage()), DirectionsCriteria.IMPERIAL, "11 mi");

  }

  @Test
  public void formatDistance_noLocale() {
    assertOutput(LARGE_LARGE_UNIT, new Locale("", ""), DirectionsCriteria.IMPERIAL, "11 mi");
  }

  @Test
  public void formatDistance_unitTypeDifferentFromLocale() {
    assertOutput(LARGE_LARGE_UNIT, Locale.US, DirectionsCriteria.METRIC, "18 km");
  }

  @Test
  public void formatDistance_largeMiles() {
    assertOutput(LARGE_LARGE_UNIT, Locale.US, DirectionsCriteria.IMPERIAL,"11 mi");
  }

  @Test
  public void formatDistance_largeKilometers() {
    assertOutput(LARGE_LARGE_UNIT, Locale.FRANCE, DirectionsCriteria.METRIC, "18 km");
  }

  @Test
  public void formatDistance_largeKilometerNoUnitTypeButMetricLocale() {
    assertOutput(LARGE_LARGE_UNIT, Locale.FRANCE, DirectionsCriteria.METRIC,"18 km");
  }

  @Test
  public void formatDistance_mediumMiles() {
    assertOutput(MEDIUM_LARGE_UNIT, Locale.US, DirectionsCriteria.IMPERIAL, "6.1 mi");
  }

  @Test
  public void formatDistance_mediumKilometers() {
    assertOutput(MEDIUM_LARGE_UNIT, Locale.FRANCE, DirectionsCriteria.METRIC, "9,8 km");
  }

  @Test
  public void formatDistance_mediumKilometersUnitTypeDifferentFromLocale() {
    assertOutput(MEDIUM_LARGE_UNIT, Locale.FRANCE, DirectionsCriteria.IMPERIAL, "6,1 mi");
  }

  @Test
  public void formatDistance_smallFeet() {
    assertOutput(SMALL_SMALL_UNIT, Locale.US, DirectionsCriteria.IMPERIAL, "50 ft");
  }

  @Test
  public void formatDistance_smallMeters() {
    assertOutput(SMALL_SMALL_UNIT, Locale.FRANCE, DirectionsCriteria.METRIC, "50 m");
  }

  @Test
  public void formatDistance_largeFeet() {
    assertOutput(LARGE_SMALL_UNIT, Locale.US, DirectionsCriteria.IMPERIAL,"350 ft");
  }

  @Test
  public void formatDistance_largeMeters() {
    assertOutput(LARGE_SMALL_UNIT, Locale.FRANCE, DirectionsCriteria.METRIC, "100 m");
  }

  private void assertOutput(double distance, Locale locale, String unitType, String output) {
    Assert.assertEquals(output,
      new DistanceFormatter(context, locale.getLanguage(), unitType).formatDistance(distance).toString());
  }
}
