package com.mapbox.services.android.navigation.v5.milestone;

/**
 * Extracted operation methods are found in this class and are fundamental to how Triggers work.
 *
 * @since 0.4.0
 */
class Operation {

  private Operation() {
    // Private constructor to prevent initialization of class.
  }

  static boolean greaterThan(Number[] valueOne, Number valueTwo) {
    if (valueOne.length > 1) {
      if (valueTwo.equals(TriggerProperty.TRUE)) {
        return valueOne[0].doubleValue() > valueOne[1].doubleValue();
      } else {
        return valueOne[0].doubleValue() <= valueOne[1].doubleValue();
      }
    }
    return valueOne[0].doubleValue() > valueTwo.doubleValue();
  }

  static boolean lessThan(Number[] valueOne, Number valueTwo) {
    if (valueOne.length > 1) {
      if (valueTwo.equals(TriggerProperty.TRUE)) {
        return valueOne[0].doubleValue() < valueOne[1].doubleValue();
      } else {
        return valueOne[0].doubleValue() >= valueOne[1].doubleValue();
      }
    }
    return valueOne[0].doubleValue() < valueTwo.doubleValue();
  }

  static boolean notEqual(Number[] valueOne, Number valueTwo) {
    if (valueOne.length > 1) {
      if (valueTwo.equals(TriggerProperty.TRUE)) {
        return !valueOne[0].equals(valueOne[1]);
      } else {
        return valueOne[0].equals(valueOne[1]);
      }
    }
    return !valueOne[0].equals(valueTwo);
  }

  static boolean equal(Number[] valueOne, Number valueTwo) {
    if (valueOne.length > 1) {
      if (valueTwo.equals(TriggerProperty.TRUE)) {
        return valueOne[0].equals(valueOne[1]);
      } else {
        return !valueOne[0].equals(valueOne[1]);
      }
    }
    return valueOne[0].equals(valueTwo);
  }

  static boolean greaterThanEqual(Number[] valueOne, Number valueTwo) {
    if (valueOne.length > 1) {
      if (valueTwo.equals(TriggerProperty.TRUE)) {
        return valueOne[0].doubleValue() >= valueOne[1].doubleValue();
      } else {
        return valueOne[0].doubleValue() < valueOne[1].doubleValue();
      }
    }
    return valueOne[0].doubleValue() >= valueTwo.doubleValue();
  }

  static boolean lessThanEqual(Number[] valueOne, Number valueTwo) {
    if (valueOne.length > 1) {
      if (valueTwo.equals(TriggerProperty.TRUE)) {
        return valueOne[0].doubleValue() <= valueOne[1].doubleValue();
      } else {
        return valueOne[0].doubleValue() > valueOne[1].doubleValue();
      }
    }
    return valueOne[0].doubleValue() <= valueTwo.doubleValue();
  }

}
