package com.mapbox.services.android.navigation.v5.location.replay;

import android.location.Location;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

class ParseGpxTask extends AsyncTask<InputStream, Void, List<Location>> {

  private static final int FIRST_INPUT_STREAM = 0;

  private Listener listener;
  private GpxParser parser;

  ParseGpxTask(GpxParser parser, Listener listener) {
    this.parser = parser;
    this.listener = listener;
  }

  @Override
  protected List<Location> doInBackground(InputStream... inputStreams) {
    InputStream inputStream = inputStreams[FIRST_INPUT_STREAM];
    try {
      return parseGpxStream(inputStream);
    } catch (IOException exception) {
      listener.onParseError(exception);
      return null;
    }
  }

  @Override
  protected void onPostExecute(List<Location> locationList) {
    if (locationList != null && !locationList.isEmpty()) {
      listener.onParseComplete(locationList);
    } else {
      listener.onParseError(new RuntimeException("An error occurred parsing the GPX Xml."));
    }
  }

  @Nullable
  private List<Location> parseGpxStream(InputStream inputStream) throws IOException {
    try {
      return parser.parseGpx(inputStream);
    } catch (ParserConfigurationException | ParseException | SAXException | IOException exception) {
      exception.printStackTrace();
      listener.onParseError(exception);
      return null;
    } finally {
      inputStream.close();
    }
  }

  public interface Listener {

    void onParseComplete(@NonNull List<Location> gpxLocationList);

    void onParseError(Exception exception);
  }
}
