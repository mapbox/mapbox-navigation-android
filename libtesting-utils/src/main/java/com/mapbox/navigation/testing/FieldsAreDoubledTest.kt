package com.mapbox.navigation.testing

import org.junit.Test

abstract class FieldsAreDoubledTest<Doubler, Original> {

    open val excludedFields: Set<Pair<String, Class<*>>> = emptySet()

    abstract fun getDoublerClass(): Class<*>

    abstract fun getOriginalClass(): Class<*>

    // original to doubler
    open val fieldsTypesMap: Map<Class<*>, Class<*>> = emptyMap()

    // original to doubler
    open val fieldsNamesMap: Map<String, String> = emptyMap()

    @Test
    fun allFieldsAreDoubled() {
        val doublerFields = getDoublerClass().declaredFields
        val originalFieldsToCheck = getOriginalClass().declaredFields.filterNot { (it.name to it.type) in excludedFields }

        val expected = originalFieldsToCheck.map { getExpectedName(it.name) to getExpectedType(it.type) }.toSet()
        val actual = doublerFields.map { it.name to it.type }.toSet()

        val absentFields = expected.minus(actual)
        val extraFields = actual.minus(expected)

        if (absentFields.isNotEmpty()) {
            throw AssertionError("The following fields were expected in the Doubler class, but were not found: $absentFields")
        }
        if (extraFields.isNotEmpty()) {
            throw AssertionError("The following fields were not expected in the Doubler class, but were found: $absentFields")
        }
    }

    private fun getExpectedType(original: Class<*>): Class<*> {
        return fieldsTypesMap[original] ?: original
    }

    private fun getExpectedName(original: String): String {
        return fieldsNamesMap[original] ?: original
    }
}
