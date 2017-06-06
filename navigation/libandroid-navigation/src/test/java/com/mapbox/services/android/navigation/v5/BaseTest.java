package com.mapbox.services.android.navigation.v5;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class BaseTest {
  public static final double DELTA = 1E-10;
  private static final String BASE_PATH = "/res/";

  private static final String LOG_TAG = BaseTest.class.getSimpleName();

  protected String readPath(String path) {
    try {
      InputStream is = getClass().getResourceAsStream(BASE_PATH + path);
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      String content = IOUtils.toString(is, Charset.forName("utf-8"));
      IOUtils.closeQuietly(is);
      return content;
    } catch (IOException exception) {
      Log.e(LOG_TAG, String.format("Failed to read fixture (%s): %s", path, exception.getMessage()));
      exception.printStackTrace();
    }

    return null;
  }
}
