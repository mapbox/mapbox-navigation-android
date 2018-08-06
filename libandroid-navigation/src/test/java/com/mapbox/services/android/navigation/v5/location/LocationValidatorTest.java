package com.mapbox.services.android.navigation.v5.location;

import android.location.Location;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocationValidatorTest {

  @Test
  public void isValidUpdate_trueOnFirstUpdate() {
    Location location = buildLocationWithAccuracy(10);
    int accuracyThreshold = 100;
    LocationValidator validator = new LocationValidator(accuracyThreshold);

    boolean isValid = validator.isValidUpdate(location);

    assertTrue(isValid);
  }

  @Test
  public void isValidUpdate_trueWhenUnder100MeterAccuracyThreshold() {
    Location location = buildLocationWithAccuracy(90);
    LocationValidator validator = buildValidatorWithUpdate();

    boolean isValid = validator.isValidUpdate(location);

    assertTrue(isValid);
  }

  @Test
  public void isValidUpdate_falseWhenOver100MeterAccuracyThreshold() {
    Location location = buildLocationWithAccuracy(110);
    LocationValidator validator = buildValidatorWithUpdate();

    boolean isValid = validator.isValidUpdate(location);

    assertFalse(isValid);
  }

  private LocationValidator buildValidatorWithUpdate() {
    Location location = buildLocationWithAccuracy(10);
    int accuracyThreshold = 100;
    LocationValidator validator = new LocationValidator(accuracyThreshold);
    validator.isValidUpdate(location);
    return validator;
  }

  private Location buildLocationWithAccuracy(float accuracy) {
    Location location = mock(Location.class);
    when(location.getAccuracy()).thenReturn(accuracy);
    return location;
  }
}