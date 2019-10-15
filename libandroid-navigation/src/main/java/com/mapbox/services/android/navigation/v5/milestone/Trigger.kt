package com.mapbox.services.android.navigation.v5.milestone

import android.util.SparseArray

/**
 * Utility to build Trigger expressions more easily.
 *
 * @since 0.4.0
 */
object Trigger {

    /**
     * Base Trigger statement. Subclassed to provide concrete statements.
     *
     * @since 0.4.0
     */
    abstract class Statement {

        /**
         * Validates whether the statement meets the specified trigger criteria.
         *
         * @param statementObjects a [Map] that contains all the trigger statements to determine
         * @return true if the statement is valid, otherwise false
         * @since 0.4.0
         */
        abstract fun isOccurring(statementObjects: SparseArray<Array<Number>>): Boolean
    }

    /*
     * Compound statements
     */

    /**
     * All class used to determine that all of the statements are valid.
     *
     * @since 0.4.0
     */
    private class AllStatement(
        private vararg val statements: Statement
    ) : Statement() {

        override fun isOccurring(statementObjects: SparseArray<Array<Number>>): Boolean {
            for (statement in statements) {
                if (!statement.isOccurring(statementObjects)) {
                    return false
                }
            }
            return true
        }
    }

    /**
     * None class used to determine that none of the statements are valid.
     *
     * @since 0.4.0
     */
    private class NoneStatement(
        private vararg val statements: Statement
    ) : Statement() {

        override fun isOccurring(statementObjects: SparseArray<Array<Number>>): Boolean {
            for (statement in statements) {
                if (statement.isOccurring(statementObjects)) {
                    return false
                }
            }
            return true
        }
    }

    /**
     * Any class used to determine that any of the statements are valid.
     *
     * @since 0.4.0
     */
    private class AnyStatement(
        private vararg val statements: Statement
    ) : Statement() {

        override fun isOccurring(statementObjects: SparseArray<Array<Number>>): Boolean {
            for (statement in statements) {
                if (statement.isOccurring(statementObjects)) {
                    return true
                }
            }
            return false
        }
    }

    /*
     * Simple statement
     */

    /**
     * Greater than class used to determine that the `RouteProgress` key property is greater than the specified
     * value.
     *
     * @since 0.4.0
     */
    private class GreaterThanStatement(
        private val key: Int,
        private val value: Any
    ) : Statement() {

        override fun isOccurring(statementObjects: SparseArray<Array<Number>>): Boolean =
            Operation.greaterThan(statementObjects.get(key), value as Number)
    }

    /**
     * Greater than equal class used to determine that the `RouteProgress` key property is greater than or equal
     * to the specified value.
     *
     * @since 0.4.0
     */
    private class GreaterThanEqualStatement(
        private val key: Int,
        private val value: Any
    ) : Statement() {

        override fun isOccurring(statementObjects: SparseArray<Array<Number>>): Boolean =
            Operation.greaterThanEqual(statementObjects.get(key), value as Number)
    }

    /**
     * Less than class used to determine that the `RouteProgress` key property is less than the specified value.
     *
     * @since 0.4.0
     */
    private class LessThanStatement(
        private val key: Int,
        private val value: Any
    ) : Statement() {

        override fun isOccurring(statementObjects: SparseArray<Array<Number>>): Boolean =
            Operation.lessThan(statementObjects.get(key), value as Number)
    }

    /**
     * Less than equal class used to determine that the `RouteProgress` key property is less than or equal to the
     * specified value.
     *
     * @since 0.4.0
     */
    private class LessThanEqualStatement(
        private val key: Int,
        private val value: Any
    ) : Statement() {

        override fun isOccurring(statementObjects: SparseArray<Array<Number>>): Boolean =
            Operation.lessThanEqual(statementObjects.get(key), value as Number)
    }

    /**
     * Not equals class used to determine that the `RouteProgress` key property does not equal the specified value.
     *
     * @since 0.4.0
     */
    private class NotEqualStatement(
        private val key: Int,
        private vararg val values: Any
    ) : Statement() {

        override fun isOccurring(statementObjects: SparseArray<Array<Number>>): Boolean =
            Operation.notEqual(statementObjects.get(key), values[0] as Number)
    }

    /**
     * Equals class used to determine that the `RouteProgress` key property equals the specified value.
     *
     * @since 0.4.0
     */
    private class EqualStatement(
        private val key: Int,
        private val value: Any
    ) : Statement() {

        override fun isOccurring(statementObjects: SparseArray<Array<Number>>): Boolean =
            Operation.equal(statementObjects.get(key), value as Number)
    }

    /**
     * Groups a collection of statements in an `all` relationship.
     *
     * @param statements the collection of statements
     * @return the statements compounded
     * @since 0.4.0
     */
    @JvmStatic
    fun all(vararg statements: Statement): Statement =
        AllStatement(*statements)

    /**
     * Groups a collection of statements in an `any` relationship.
     *
     * @param statements the collection of statements
     * @return the statements compounded
     * @since 0.4.0
     */
    @JvmStatic
    fun any(vararg statements: Statement): Statement =
        AnyStatement(*statements)

    /**
     * Groups a collection of statements in an `none` relationship.
     *
     * @param statements the collection of statements
     * @return the statements compounded
     * @since 0.4.0
     */
    @JvmStatic
    fun none(vararg statements: Statement): Statement =
        NoneStatement(*statements)

    /**
     * Check the property equals the given value.
     *
     * @param key the property key which must be one of the constants found in [TriggerProperty]
     * @param value the value to check against
     * @return the statement
     * @since 0.4.0
     */
    @JvmStatic
    fun eq(key: Int, value: Any): Statement =
        EqualStatement(key, value)

    /**
     * Check the property does not equals the given value.
     *
     * @param key the property key which must be one of the constants found in [TriggerProperty]
     * @param value the value to check against
     * @return the statement
     * @since 0.4.0
     */
    @JvmStatic
    fun neq(key: Int, value: Any): Statement =
        NotEqualStatement(key, value)

    /**
     * Check the property exceeds the given value.
     *
     * @param key the property key which must be one of the constants found in [TriggerProperty]
     * @param value the value to check against
     * @return the statement
     * @since 0.4.0
     */
    @JvmStatic
    fun gt(key: Int, value: Any): Statement =
        GreaterThanStatement(key, value)

    /**
     * Check the property does not exceeds the given value.
     *
     * @param key the property key which must be one of the constants found in [TriggerProperty]
     * @param value the value to check against
     * @return the statement
     * @since 0.4.0
     */
    @JvmStatic
    fun lt(key: Int, value: Any): Statement =
        LessThanStatement(key, value)

    /**
     * Check the property equals or does not exceeds the given value.
     *
     * @param key the property key which must be one of the constants found in [TriggerProperty]
     * @param value the value to check against
     * @return the statement
     * @since 0.4.0
     */
    @JvmStatic
    fun lte(key: Int, value: Any): Statement =
        LessThanEqualStatement(key, value)

    /**
     * Check the property exceeds or equals the given value.
     *
     * @param key the property key which must be one of the constants found in [TriggerProperty]
     * @param value the value to check against
     * @return the statement
     * @since 0.4.0
     */
    @JvmStatic
    fun gte(key: Int, value: Any): Statement =
        GreaterThanEqualStatement(key, value)
}
