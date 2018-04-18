package com.mapbox.services.android.navigation.ui.v5;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import static okhttp3.internal.Util.UTF_8;

class TestRouteBuilder {

  String loadJsonFixture(String filename) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(filename);
    Scanner scanner = new Scanner(inputStream, UTF_8.name()).useDelimiter("\\A");
    return scanner.hasNext() ? scanner.next() : "";
  }
}
