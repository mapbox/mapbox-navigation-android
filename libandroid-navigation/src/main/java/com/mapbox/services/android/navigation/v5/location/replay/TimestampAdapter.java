package com.mapbox.services.android.navigation.v5.location.replay;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

class TimestampAdapter extends TypeAdapter<Date> {
  private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_PATTERN);
  private static final String UTC = "UTC";

  @Override
  public void write(JsonWriter out, Date value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      DATE_FORMAT.setTimeZone(TimeZone.getTimeZone(UTC));
      String date = DATE_FORMAT.format(value);
      out.value(date);
    }
  }

  @Override
  public Date read(JsonReader reader) throws IOException {
    if (reader.peek() == JsonToken.NULL) {
      reader.nextNull();
      return null;
    }

    String dateAsString = reader.nextString();
    try {
      return DATE_FORMAT.parse(dateAsString);
    } catch (ParseException exception) {
      exception.printStackTrace();
    }
    return null;
  }
}
