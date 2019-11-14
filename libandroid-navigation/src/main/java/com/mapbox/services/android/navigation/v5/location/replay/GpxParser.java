package com.mapbox.services.android.navigation.v5.location.replay;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

class GpxParser {

  private static final String TAG_TRACK_POINT = "trkpt";
  private static final String TAG_TIME = "time";
  private static final String ATTR_LATITUDE = "lat";
  private static final String ATTR_LONGITUDE = "lon";
  private static final String GPX_LOCATION_NAME = "GPX Generated Location";
  private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN);

  @Nullable
  List<Location> parseGpx(InputStream inputStream) throws ParserConfigurationException,
    SAXException, IOException, ParseException {

    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    Document document = documentBuilder.parse(inputStream);
    Element elementRoot = document.getDocumentElement();

    NodeList trackPointNodes = elementRoot.getElementsByTagName(TAG_TRACK_POINT);
    if (trackPointNodes == null || trackPointNodes.getLength() == 0) {
      return null; // Gpx trace did not contain correct tagging
    }
    return createGpxLocationList(trackPointNodes);
  }

  @NonNull
  private List<Location> createGpxLocationList(NodeList trackPointNodes) throws ParseException {
    List<Location> gpxLocations = new ArrayList<>();

    for (int i = 0; i < trackPointNodes.getLength(); i++) {
      Node node = trackPointNodes.item(i);
      NamedNodeMap attributes = node.getAttributes();

      Double latitude = createCoordinate(attributes, ATTR_LATITUDE);
      Double longitude = createCoordinate(attributes, ATTR_LONGITUDE);
      Long time = createTime(node);

      gpxLocations.add(buildGpxLocation(latitude, longitude, time));
    }
    return gpxLocations;
  }

  @NonNull
  private Double createCoordinate(NamedNodeMap attributes, String attributeName) {
    String coordinateTextContent = attributes.getNamedItem(attributeName).getTextContent();
    return Double.parseDouble(coordinateTextContent);
  }

  @NonNull
  private Long createTime(Node trackPoint) throws ParseException {
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    NodeList trackPointChildren = trackPoint.getChildNodes();
    for (int i = 0; i < trackPointChildren.getLength(); i++) {
      Node node = trackPointChildren.item(i);
      if (node.getNodeName().contains(TAG_TIME)) {
        Date date = dateFormat.parse(node.getTextContent());
        return date.getTime();
      }
    }
    return 0L;
  }

  @NonNull
  private Location buildGpxLocation(Double latitude, Double longitude, Long time) {
    Location gpxLocation = new Location(GPX_LOCATION_NAME);
    gpxLocation.setTime(time);
    gpxLocation.setLatitude(latitude);
    gpxLocation.setLongitude(longitude);
    return gpxLocation;
  }
}
