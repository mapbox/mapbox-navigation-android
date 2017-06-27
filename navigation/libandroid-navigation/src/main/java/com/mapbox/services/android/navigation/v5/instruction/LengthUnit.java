package com.mapbox.services.android.navigation.v5.instruction;

public class LengthUnit {

  public static double convert(String originalUnit, String newUnit, double input) {
    double output = 0.0d;

    originalUnit = originalUnit.toLowerCase();
    newUnit = newUnit.toLowerCase();

    switch (originalUnit) {
      case "feet":
        switch (newUnit) {
          case "feet":
            output = input;
            break;
          case "miles":
            output = input / 5280.0d;
            break;
          case "meters":
            output = input * 0.3048d;
            break;
          case "kilometers":
            output = input * 0.0003048d;
            break;
          default:
            break;
        }
        break;
      case "miles":
        switch (newUnit) {
          case "feet":
            output = input * 5280.0d;
            break;
          case "miles":
            output = input;
            break;
          case "meters":
            output = input * 1609.34d;
            break;
          case "kilometers":
            output = input * 1.60934d;
            break;
          default:
            break;
        }
        break;
      case "meters":
        switch (newUnit) {
          case "feet":
            output = input * 3.28084d;
            break;
          case "miles":
            output = input / 1609.34d;
            break;
          case "meters":
            output = input;
            break;
          case "kilometers":
            // TODO
            break;
          default:
            break;
        }
        break;
      case "kilometers":
        switch (newUnit) {
          case "feet":
            output = input * 3280.84d;
            break;
          case "miles":
            output = input / 1.60934d;
            break;
          case "meters":
            //TODO
            break;
          case "kilometers":
            output = input;
            break;
          default:
            break;
        }
        break;
      default:
        break;
    }

    return output;
  }

  public static class Unit {
    public static final String METERS = "meters";
    public static final String KILOMETERS = "kilometers";
    public static final String FEET = "feet";
    public static final String MILES = "miles";
  }
}
