package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.services.android.navigation.v5.internal.navigation.SdkVersionChecker;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class UrlDensityMapTest {

  @Test
  public void checksDensityLowReturnsOneXPngUrl() {
    int lowDensityDpi = 120;
    SdkVersionChecker anySdkVersionChecker = new SdkVersionChecker(18);
    UrlDensityMap urlDensityMap = new UrlDensityMap(lowDensityDpi, anySdkVersionChecker);
    String anyUrl = "any.url";

    String threeXPng = urlDensityMap.get(anyUrl);

    assertEquals(anyUrl + "@1x.png", threeXPng);
  }

  @Test
  public void checksDensityMediumReturnsOneXPngUrl() {
    int mediumDensityDpi = 160;
    SdkVersionChecker anySdkVersionChecker = new SdkVersionChecker(14);
    UrlDensityMap urlDensityMap = new UrlDensityMap(mediumDensityDpi, anySdkVersionChecker);
    String anyUrl = "any.url";

    String threeXPng = urlDensityMap.get(anyUrl);

    assertEquals(anyUrl + "@1x.png", threeXPng);
  }

  @Test
  public void checksDensityHighReturnsTwoXPngUrl() {
    int highDensityDpi = 240;
    SdkVersionChecker anySdkVersionChecker = new SdkVersionChecker(15);
    UrlDensityMap urlDensityMap = new UrlDensityMap(highDensityDpi, anySdkVersionChecker);
    String anyUrl = "any.url";

    String threeXPng = urlDensityMap.get(anyUrl);

    assertEquals(anyUrl + "@2x.png", threeXPng);
  }

  @Test
  public void checksDensityXHighReturnsThreeXPngUrl() {
    int xhighDensityDpi = 320;
    SdkVersionChecker anySdkVersionChecker = new SdkVersionChecker(21);
    UrlDensityMap urlDensityMap = new UrlDensityMap(xhighDensityDpi, anySdkVersionChecker);
    String anyUrl = "any.url";

    String threeXPng = urlDensityMap.get(anyUrl);

    assertEquals(anyUrl + "@3x.png", threeXPng);
  }

  @Test
  public void checksAndroidJellyBeanAndDensityXxhighReturnsThreeXPngUrl() {
    int xxhighDensityDpi = 480;
    SdkVersionChecker jellyBeanChecker = new SdkVersionChecker(16);
    UrlDensityMap urlDensityMap = new UrlDensityMap(xxhighDensityDpi, jellyBeanChecker);
    String anyUrl = "any.url";

    String threeXPng = urlDensityMap.get(anyUrl);

    assertEquals(anyUrl + "@3x.png", threeXPng);
  }

  @Test
  public void checksAndroidJellyBeanMr2AndDensityXxxhighReturnsFourXPngUrl() {
    int xxxhighDensityDpi = 640;
    SdkVersionChecker jellyBeanMr2Checker = new SdkVersionChecker(18);
    UrlDensityMap urlDensityMap = new UrlDensityMap(xxxhighDensityDpi, jellyBeanMr2Checker);
    String anyUrl = "any.url";

    String threeXPng = urlDensityMap.get(anyUrl);

    assertEquals(anyUrl + "@4x.png", threeXPng);
  }

  @Test
  public void checksAndroidKitkatAndFourHundredDensityReturnsThreeXPngUrl() {
    int fourHundredDensityDpi = 400;
    SdkVersionChecker kitkatChecker = new SdkVersionChecker(19);
    UrlDensityMap urlDensityMap = new UrlDensityMap(fourHundredDensityDpi, kitkatChecker);
    String anyUrl = "any.url";

    String threeXPng = urlDensityMap.get(anyUrl);

    assertEquals(anyUrl + "@3x.png", threeXPng);
  }

  @Test
  public void checksAndroidLollipopAndFiveHundredAndSixtyDensityReturnsFourXPngUrl() {
    int fiveHundredAndSixtyDensityDpi = 560;
    SdkVersionChecker lollipopChecker = new SdkVersionChecker(21);
    UrlDensityMap urlDensityMap = new UrlDensityMap(fiveHundredAndSixtyDensityDpi, lollipopChecker);
    String anyUrl = "any.url";

    String threeXPng = urlDensityMap.get(anyUrl);

    assertEquals(anyUrl + "@4x.png", threeXPng);
  }

  @Test
  public void checksAndroidLollipopMr1AndTwoHundredAndEightyDensityReturnsTwoXPngUrl() {
    int twoHundredAndEightyDensityDpi = 280;
    SdkVersionChecker lollipopMr1Checker = new SdkVersionChecker(22);
    UrlDensityMap urlDensityMap = new UrlDensityMap(twoHundredAndEightyDensityDpi, lollipopMr1Checker);
    String anyUrl = "any.url";

    String threeXPng = urlDensityMap.get(anyUrl);

    assertEquals(anyUrl + "@2x.png", threeXPng);
  }

  @Test
  public void checksAndroidMAndThreeHundredAndSixtyDensityReturnsThreeXPngUrl() {
    int threeHundredAndSixtyDensityDpi = 360;
    SdkVersionChecker mChecker = new SdkVersionChecker(23);
    UrlDensityMap urlDensityMap = new UrlDensityMap(threeHundredAndSixtyDensityDpi, mChecker);
    String anyUrl = "any.url";

    String threeXPng = urlDensityMap.get(anyUrl);

    assertEquals(anyUrl + "@3x.png", threeXPng);
  }

  @Test
  public void checksAndroidMAndFourHundredAndTwentyDensityReturnsThreeXPngUrl() {
    int fourHundredAndTwentyDensityDpi = 420;
    SdkVersionChecker mChecker = new SdkVersionChecker(23);
    UrlDensityMap urlDensityMap = new UrlDensityMap(fourHundredAndTwentyDensityDpi, mChecker);
    String anyUrl = "any.url";

    String threeXPng = urlDensityMap.get(anyUrl);

    assertEquals(anyUrl + "@3x.png", threeXPng);
  }

  @Test
  public void checksAndroidNMr1AndTwoHundredAndSixtyDensityReturnsTwoXPngUrl() {
    int twoHundredAndSixtyDensityDpi = 260;
    SdkVersionChecker nMr1Checker = new SdkVersionChecker(25);
    UrlDensityMap urlDensityMap = new UrlDensityMap(twoHundredAndSixtyDensityDpi, nMr1Checker);
    String anyUrl = "any.url";

    String threeXPng = urlDensityMap.get(anyUrl);

    assertEquals(anyUrl + "@2x.png", threeXPng);
  }

  @Test
  public void checksAndroidNMr1AndThreeHundredDensityReturnsTwoXPngUrl() {
    int threeHundredDensityDpi = 300;
    SdkVersionChecker nMr1Checker = new SdkVersionChecker(25);
    UrlDensityMap urlDensityMap = new UrlDensityMap(threeHundredDensityDpi, nMr1Checker);
    String anyUrl = "any.url";

    String threeXPng = urlDensityMap.get(anyUrl);

    assertEquals(anyUrl + "@2x.png", threeXPng);
  }

  @Test
  public void checksAndroidNMr1AndThreeHundredAndFortyDensityReturnsThreeXPngUrl() {
    int threeHundredAndFortyDensityDpi = 340;
    SdkVersionChecker nMr1Checker = new SdkVersionChecker(25);
    UrlDensityMap urlDensityMap = new UrlDensityMap(threeHundredAndFortyDensityDpi, nMr1Checker);
    String anyUrl = "any.url";

    String threeXPng = urlDensityMap.get(anyUrl);

    assertEquals(anyUrl + "@3x.png", threeXPng);
  }

  @Test
  public void checksAndroidPAndFourHundredAndFortyDensityReturnsThreeXPngUrl() {
    int fourHundredAndFortyDensityDpi = 440;
    SdkVersionChecker androidPChecker = new SdkVersionChecker(28);
    UrlDensityMap urlDensityMap = new UrlDensityMap(fourHundredAndFortyDensityDpi, androidPChecker);
    String anyUrl = "any.url";

    String threeXPng = urlDensityMap.get(anyUrl);

    assertEquals(anyUrl + "@3x.png", threeXPng);
  }
}