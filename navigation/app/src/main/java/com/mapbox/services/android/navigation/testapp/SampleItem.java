package com.mapbox.services.android.navigation.testapp;

public class SampleItem {

  private String name;
  private String description;
  private Class activity;

  public SampleItem(String name, String description, Class activity) {
    this.name = name;
    this.description = description;
    this.activity = activity;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Class getActivity() {
    return activity;
  }

}
