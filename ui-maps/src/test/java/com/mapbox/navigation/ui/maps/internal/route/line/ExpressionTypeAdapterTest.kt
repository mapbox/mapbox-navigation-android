package com.mapbox.navigation.ui.maps.internal.route.line

import com.google.gson.GsonBuilder
import com.mapbox.maps.extension.style.expressions.generated.Expression
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ExpressionTypeAdapterTest {

    @Test
    fun serialiseNull() {
        val adapter = ExpressionTypeAdapter()
        val gson = GsonBuilder()
            .registerTypeAdapter(Expression::class.java, adapter)
            .create()
        val expression: Expression? = null
        val holder = Holder(
            1,
            "aaaaa",
            expression,
            10,
            "bbbb",
        )
        val json = gson.toJson(holder)
        val restored = gson.fromJson(json, Holder::class.java)
        assertEquals(holder, restored)
    }

    @Test
    fun serialiseNonNull() {
        val adapter = ExpressionTypeAdapter()
        val gson = GsonBuilder()
            .registerTypeAdapter(Expression::class.java, adapter)
            .create()
        val expressionBuilder = Expression.InterpolatorBuilder("interpolate")
        expressionBuilder.linear()
        expressionBuilder.lineProgress()
        expressionBuilder.stop {
            literal(0.2)
            color(10)
        }
        expressionBuilder.stop {
            literal(0.8)
            color(11)
        }
        val expression = expressionBuilder.build()
        val holder = Holder(
            1,
            "aaaaa",
            expression,
            10,
            "bbbb",
        )
        val json = gson.toJson(holder)
        val restored = gson.fromJson(json, Holder::class.java)
        assertEquals(holder, restored)
    }

    private data class Holder(
        val a: Int,
        val b: String,
        val exp: Expression?,
        val c: Long,
        val d: String,
    )
}
