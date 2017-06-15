package com.mapbox.services.android.navigation.v5.milestone;

import java.util.Map;

public class Trigger {

  public abstract static class Statement {

    public Statement() {
    }

    public abstract boolean check(Map<Integer, Number[]> factors);
  }

  /*
   * Compound statements
   */

  private static class AllStatement extends Statement {
    private final Statement[] statements;

    AllStatement(Statement... statements) {
      this.statements = statements;
    }

    @Override
    public boolean check(Map<Integer, Number[]> factors) {
      boolean all = true;
      for (Statement statement : statements) {
        if (!statement.check(factors)) {
          all = false;
        }
      }
      return all;
    }
  }

  private static class AnyStatement extends Statement {
    private final Statement[] statements;

    AnyStatement(Statement... statements) {
      this.statements = statements;
    }

    @Override
    public boolean check(Map<Integer, Number[]> factors) {
      for (Statement statement : statements) {
        if (statement.check(factors)) {
          return true;
        }
      }
      return false;
    }
  }

  /*
   * Simple statement
   */


  private static class GreaterThanStatement extends Statement {
    private final int key;
    private final Object value;

    GreaterThanStatement(int key, Object value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public boolean check(Map<Integer, Number[]> factors) {
      return Operation.greaterThan(factors.get(key), (Number) value);
    }
  }

  private static class LessThanStatement extends Statement {
    private final int key;
    private final Object value;

    LessThanStatement(int key, Object value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public boolean check(Map<Integer, Number[]> factors) {
      return Operation.lessThan(factors.get(key), (double) value);
    }
  }

  private static class NotEqualStatement extends Statement {
    private final int key;
    private final Object[] values;

    NotEqualStatement(int key, Object... values) {
      this.key = key;
      this.values = values;
    }

    @Override
    public boolean check(Map<Integer, Number[]> factors) {
      return Operation.notEqual(factors.get(key), (Number) values[0]);
    }
  }

  private static class EqualStatement extends Statement {
    private final int key;
    private final Object value;

    EqualStatement(int key, Object value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public boolean check(Map<Integer, Number[]> factors) {
      return Operation.equal(factors.get(key), (Number) value);
    }
  }

  public static Statement all(Statement... statements) {
    return new AllStatement(statements);
  }

  public static Statement any(Statement... statements) {
    return new AnyStatement(statements);
  }

  public static Statement neq(int key, Object value) {
    return new NotEqualStatement(key, value);
  }

  public static Statement eq(int key, Object value) {
    return new EqualStatement(key, value);
  }

  public static Statement gt(int key, Object value) {
    return new GreaterThanStatement(key, value);
  }

  public static Statement lt(int key, Object value) {
    return new LessThanStatement(key, value);
  }

  // TODO add these statements
//
//  public static Statement lte(int key, Object value) {
//    return new Statement("<=", key, value);
//  }
//
//  public static Statement gte(int key, Object value) {
//    return new Statement(">=", key, value);
//  }


}
