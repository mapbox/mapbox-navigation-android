package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.bindgen.Value
import com.mapbox.maps.extension.style.expressions.generated.Expression
import org.junit.Assert.assertEquals
import java.lang.AssertionError

class StringChecker(private val expected: String) : (Value) -> Unit {
    override fun invoke(p1: Value) {
        return assertEquals(expected, p1.toString())
    }

    override fun toString(): String {
        return expected
    }
}

class DoubleChecker(private val expected: Double) : (Value) -> Unit {

    override fun invoke(p1: Value) {
        return assertEquals(expected, p1.toString().toDouble(), 0.00001)
    }

    override fun toString(): String {
        return expected.toString()
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
    try {
        val actualList = actual.contents as List<Value>
        assertEquals(expected.size, actualList.size)
        actualList.forEachIndexed { index, actual ->
            expected[index](actual)
        }
    } catch (ex: AssertionError) {
        println(
            """
                Expressions do not match.
                expected: [${expected.joinToString(separator = ", ")}]
                actual:   $actual
            """.trimIndent(),
        )
        throw ex
    }
}
