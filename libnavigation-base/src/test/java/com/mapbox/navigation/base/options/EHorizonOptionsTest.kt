package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EHorizonOptionsTest : BuilderTest<EHorizonOptions, EHorizonOptions.Builder>() {

    override fun getImplementationClass() = EHorizonOptions::class

    override fun getFilledUpBuilder() = EHorizonOptions.Builder()
        .length(1500.0)
        .expansion(1)
        .branchLength(150.0)
        .minTimeDeltaBetweenUpdates(1.0)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }

    @Test
    fun `when negative length passed default value used`() {
        buildEHorizonOptions(length = NEGATIVE_DOUBLE).apply {
            assertEquals(DEFAULT_LENGTH, length, 0.0) // changed to default
            assertEquals(EXPANSION, expansion)
            assertEquals(BRANCH_LENGTH, branchLength, 0.0)
            assertEquals(MIN_DELTA, minTimeDeltaBetweenUpdates)
        }
    }

    @Test
    fun `when negative expansion passed default value used`() {
        buildEHorizonOptions(expansion = NEGATIVE_INT).apply {
            assertEquals(LENGTH, length, 0.0)
            assertEquals(DEFAULT_EXPANSION, expansion) // changed to default
            assertEquals(BRANCH_LENGTH, branchLength, 0.0)
            assertEquals(MIN_DELTA, minTimeDeltaBetweenUpdates)
        }
    }

    @Test
    fun `when negative branchLength passed default value used`() {
        buildEHorizonOptions(branchLength = NEGATIVE_DOUBLE).apply {
            assertEquals(LENGTH, length, 0.0)
            assertEquals(EXPANSION, expansion)
            assertEquals(DEFAULT_BRANCH_LENGTH, branchLength, 0.0) // changed to default
            assertEquals(MIN_DELTA, minTimeDeltaBetweenUpdates)
        }
    }

    @Test
    fun `when negative minTimeDeltaBetweenUpdates passed default value used`() {
        buildEHorizonOptions(minTimeDeltaBetweenUpdates = NEGATIVE_DOUBLE).apply {
            assertEquals(LENGTH, length, 0.0)
            assertEquals(EXPANSION, expansion)
            assertEquals(BRANCH_LENGTH, branchLength, 0.0)
            assertEquals(DEFAULT_MIN_DELTA, minTimeDeltaBetweenUpdates) // changed to default
        }
    }

    private fun buildEHorizonOptions(
        length: Double = LENGTH,
        expansion: Int = EXPANSION,
        branchLength: Double = BRANCH_LENGTH,
        minTimeDeltaBetweenUpdates: Double? = MIN_DELTA,
    ) =
        EHorizonOptions.Builder()
            .length(length)
            .expansion(expansion)
            .branchLength(branchLength)
            .minTimeDeltaBetweenUpdates(minTimeDeltaBetweenUpdates)
            .build()

    private companion object {
        private const val NEGATIVE_INT = -1
        private const val NEGATIVE_DOUBLE = -1.0

        private const val LENGTH = 0.0
        private const val EXPANSION = 0
        private const val BRANCH_LENGTH = 0.0
        private val MIN_DELTA: Double? = 0.0

        private const val DEFAULT_LENGTH = 500.0
        private const val DEFAULT_EXPANSION = 0
        private const val DEFAULT_BRANCH_LENGTH = 50.0
        private val DEFAULT_MIN_DELTA: Double? = null
    }
}
