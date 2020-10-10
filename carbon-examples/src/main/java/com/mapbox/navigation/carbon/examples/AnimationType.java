package com.mapbox.navigation.carbon.examples;

public enum AnimationType {
  Following("Following"),
  Overview("Overview"),
  Recenter("Recenter");

  private final String name;

  AnimationType(String s) {
    name = s;
  }
}
