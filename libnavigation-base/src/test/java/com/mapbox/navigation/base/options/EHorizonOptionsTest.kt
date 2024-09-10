package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class EHorizonOptionsTest : BuilderTest<EHorizonOptions, EHorizonOptions.Builder>() {
    @get:Rule
    val expectedException = ExpectedException.none()

    override fun getImplementationClass() = EHorizonOptions::class

    override fun getFilledUpBuilder() = EHorizonOptions.Builder()
        .length(1500.0)
        .expansion(1)
        .branchLength(150.0)
        .minTimeDeltaBetweenUpdates(1.0)
        .alertServiceOptions(
            AlertServiceOptions.Builder()
                .collectTunnels(false)
                .collectBridges(false)
                .collectRestrictedAreas(true)
                .build(),
        )

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }

    @Test
    fun `when negative length passed exception is thrown`() {
        expectedException.expect(IllegalArgumentException::class.java)
        expectedException.expectMessage("EHorizonOptions.length can't be negative.")

        buildEHorizonOptions(length = NEGATIVE_DOUBLE)
    }

    @Test
    fun `when negative expansion passed exception is thrown`() {
        expectedException.expect(IllegalArgumentException::class.java)
        expectedException.expectMessage("EHorizonOptions.expansion can't be negative.")

        buildEHorizonOptions(expansion = NEGATIVE_INT)
    }

    @Test
    fun `when negative branchLength passed exception is thrown`() {
        expectedException.expect(IllegalArgumentException::class.java)
        expectedException.expectMessage("EHorizonOptions.branchLength can't be negative.")

        buildEHorizonOptions(branchLength = NEGATIVE_DOUBLE)
    }

    @Test
    fun `when negative minTimeDeltaBetweenUpdates passed exception is thrown`() {
        expectedException.expect(IllegalArgumentException::class.java)
        expectedException.expectMessage(
            "EHorizonOptions.minTimeDeltaBetweenUpdates can't be negative.",
        )

        buildEHorizonOptions(minTimeDeltaBetweenUpdates = NEGATIVE_DOUBLE)
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
    }
}
