package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElectronicHorizonRequestBuilder {
  private Gson gson = new Gson();

  public String build(Expansion expansion, List<Location> locations) {
    List<Position> positions = new ArrayList<>();
    for (Location location : locations) {
      positions.add(new Position(location.getLatitude(), location.getLongitude()));
    }

    Map<String, Object> options = new HashMap<>();
    options.put("expansion", expansion.value);

    return gson.toJson(new ElectronicHorizonRequest(positions, options));
  }

  public enum Expansion {
    _1D("1D"),
    _1_5D("1.5D"),
    _2D("2D");

    private String value;

    Expansion(String expansion) {
      this.value = expansion;
    }
  }

  private class Position {
    private double lat;
    private double lon;

    Position(double lat, double lon) {
      this.lat = lat;
      this.lon = lon;
    }
  }

  private class ElectronicHorizonRequest {
    private List<Position> shape;
    @SerializedName("eh_options")
    private Map<String, Object> options;

    ElectronicHorizonRequest(List<Position> shape, Map<String, Object> options) {
      this.shape = shape;
      this.options = options;
    }
  }
}
