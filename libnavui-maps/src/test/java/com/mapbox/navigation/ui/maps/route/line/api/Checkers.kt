package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.bindgen.Value
import com.mapbox.maps.extension.style.expressions.generated.Expression
import org.junit.Assert.assertEquals

class StringChecker(private val expected: String) : (Value) -> Unit {
    override fun invoke(p1: Value) {
        return assertEquals(expected, p1.toString())
    }
}

class DoubleChecker(private val expected: Double) : (Value) -> Unit {

    override fun invoke(p1: Value) {
        return assertEquals(expected, p1.toString().toDouble(), 0.00001)
    }
}

class ListChecker(private val expected: List<(Value) -> Unit>) : (Value) -> Unit {

    constructor(vararg expected: (Value) -> Unit) : this(expected.asList())

    override fun invoke(p1: Value) {
        val actualList = p1.contents as List<Value>
        assertEquals(expected.size, actualList.size)
        actualList.forEachIndexed { index, actual ->
            expected[index](actual)
        }
    }
}

fun checkExpression(expected: List<(Value) -> Unit>, actual: Expression) {
    val actualList = actual.contents as List<Value>
    assertEquals(expected.size, actualList.size)
    actualList.forEachIndexed { index, actual ->
        expected[index](actual)
    }
}
