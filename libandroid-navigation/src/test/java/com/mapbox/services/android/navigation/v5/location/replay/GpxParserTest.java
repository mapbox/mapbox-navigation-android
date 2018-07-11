package com.mapbox.services.android.navigation.v5.location.replay;

import android.location.Location;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class GpxParserTest {

  private static final double DELTA = 1E-10;
  private static final String TEST_GPX = "test.gpx";
  private static final String TEST_INVALID_GPX = "test_invalid.gpx";
  private static final double FIRST_TEST_GPS_LATITUDE = 47.644548;
  private static final double FIRST_TEST_GPS_LONGITUDE = -122.326897;
  private static final long FIRST_TEST_GPS_TIME = 1255804646000L;
  private static final int FIRST_LOCATION = 0;

  @Test
  public void sanity() {
    GpxParser parser = new GpxParser();

    assertNotNull(parser);
  }

  @Test
  public void invalidGpxTags_returnsNullList() throws ParserConfigurationException, SAXException,
    ParseException, IOException {
    GpxParser parser = new GpxParser();
    InputStream inputStream = buildTestGpxInputStream(TEST_INVALID_GPX);

    List<Location> parsedLocations = parser.parseGpx(inputStream);

    assertNull(parsedLocations);
  }

  @Test
  public void validGpxFile_returnsPopulatedLocationList() throws ParserConfigurationException, SAXException,
    ParseException, IOException {
    GpxParser parser = new GpxParser();
    InputStream inputStream = buildTestGpxInputStream(TEST_GPX);

    List<Location> parsedLocations = parser.parseGpx(inputStream);

    assertTrue(!parsedLocations.isEmpty());
  }

  @Test
  public void validGpxFile_returnsCorrectAmountOfLocations() throws ParserConfigurationException, SAXException,
    ParseException, IOException {
    GpxParser parser = new GpxParser();
    InputStream inputStream = buildTestGpxInputStream(TEST_GPX);

    List<Location> parsedLocations = parser.parseGpx(inputStream);

    assertEquals(3, parsedLocations.size());
  }

  @Test
  public void firstLocationUpdate_returnsCorrectLatitude() throws ParserConfigurationException, SAXException,
    ParseException, IOException {
    GpxParser parser = new GpxParser();
    InputStream inputStream = buildTestGpxInputStream(TEST_GPX);

    List<Location> parsedLocations = parser.parseGpx(inputStream);

    double actualFirstLatitude = parsedLocations.get(FIRST_LOCATION).getLatitude();
    assertEquals(FIRST_TEST_GPS_LATITUDE, actualFirstLatitude);
  }

  @Test
  public void firstLocationUpdate_returnsCorrectLongitude() throws ParserConfigurationException, SAXException,
    ParseException, IOException {
    GpxParser parser = new GpxParser();
    InputStream inputStream = buildTestGpxInputStream(TEST_GPX);

    List<Location> parsedLocations = parser.parseGpx(inputStream);

    double actualFirstLongitude = parsedLocations.get(FIRST_LOCATION).getLongitude();
    assertEquals(FIRST_TEST_GPS_LONGITUDE, actualFirstLongitude);
  }

  @Test
  public void firstLocationUpdate_returnsCorrectTimeInMillis() throws ParserConfigurationException, SAXException,
    ParseException, IOException {
    GpxParser parser = new GpxParser();
    InputStream inputStream = buildTestGpxInputStream(TEST_GPX);

    List<Location> parsedLocations = parser.parseGpx(inputStream);

    long actualFirstTime = parsedLocations.get(FIRST_LOCATION).getTime();
    assertEquals(FIRST_TEST_GPS_TIME, actualFirstTime, DELTA);
  }

  private InputStream buildTestGpxInputStream(String gpxFileName) {
    ClassLoader classLoader = getClass().getClassLoader();
    return classLoader.getResourceAsStream(gpxFileName);
  }
}
