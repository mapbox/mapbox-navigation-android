package com.mapbox.navigation.voice.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TimeBasedNextVoiceInstructionsProviderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()
    private val observableTime = 100
    private val distanceRemaining = 400f
    private val prevStep = mockk<LegStep>(relaxed = true)
    private val currentStepInstruction = voiceInstructions(distanceRemaining - 50.0)
    private val currentStep = mockk<LegStep>(relaxed = true) {
        every { voiceInstructions() } returns listOf(currentStepInstruction)
    }
    private val nextStepDuration = 40.0
    private val stepAfterNext = mockk<LegStep>(relaxed = true) {
        every { voiceInstructions() } returns listOf(
            this@TimeBasedNextVoiceInstructionsProviderTest.voiceInstructions(),
        )
        every { duration() } returns 5.0
    }
    private val nextStep = mockk<LegStep>(relaxed = true) {
        every { voiceInstructions() } returns listOf(
            this@TimeBasedNextVoiceInstructionsProviderTest.voiceInstructions(),
        )
        every { duration() } returns nextStepDuration
    }
    private val prevLeg = mockk<RouteLeg>(relaxed = true)
    private val nextLegStep = mockk<LegStep>(relaxed = true)
    private val legAfterNextStep = mockk<LegStep>(relaxed = true)
    private val nextLeg = mockk<RouteLeg>(relaxed = true) {
        every { steps() } returns listOf(nextLegStep)
    }
    private val legAfterNext = mockk<RouteLeg>(relaxed = true) {
        every { steps() } returns listOf(legAfterNextStep)
    }
    private val currentLeg = mockk<RouteLeg>(relaxed = true) {
        every { steps() } returns listOf(currentStep)
    }
    private val currentRoute = mockk<DirectionsRoute>(relaxed = true) {
        every { legs() } returns listOf(currentLeg)
    }
    private val sut = TimeBasedNextVoiceInstructionsProvider(observableTime)

    @Test
    fun `getNextVoiceInstructions with null legs`() {
        every { currentRoute.legs() } returns null

        assertEquals(
            emptyList<VoiceInstructions>(),
            sut.getNextVoiceInstructions(routeProgressData()),
        )
    }

    @Test
    fun `getNextVoiceInstructions with empty legs`() {
        every { currentRoute.legs() } returns emptyList()

        assertEquals(
            emptyList<VoiceInstructions>(),
            sut.getNextVoiceInstructions(routeProgressData()),
        )
    }

    @Test
    fun `getNextVoiceInstructions with too large led index`() {
        every { currentRoute.legs() } returns listOf(mockk(), mockk())

        assertEquals(
            emptyList<VoiceInstructions>(),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 2)),
        )
    }

    @Test
    fun `getNextVoiceInstructions with null steps`() {
        every { currentLeg.steps() } returns null

        assertEquals(
            emptyList<VoiceInstructions>(),
            sut.getNextVoiceInstructions(routeProgressData()),
        )
    }

    @Test
    fun `getNextVoiceInstructions with empty steps`() {
        every { currentLeg.steps() } returns emptyList()

        assertEquals(
            emptyList<VoiceInstructions>(),
            sut.getNextVoiceInstructions(routeProgressData()),
        )
    }

    @Test
    fun `getNextVoiceInstructions with too large step index`() {
        every { currentLeg.steps() } returns listOf(mockk(), mockk())

        assertEquals(
            emptyList<VoiceInstructions>(),
            sut.getNextVoiceInstructions(routeProgressData(stepIndex = 2)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for single step with no instructions`() {
        every { currentStep.voiceInstructions() } returns null

        assertEquals(
            emptyList<VoiceInstructions>(),
            sut.getNextVoiceInstructions(routeProgressData()),
        )
    }

    @Test
    fun `getNextVoiceInstructions for single step with incomplete instructions`() {
        every { currentStep.voiceInstructions() } returns listOf(
            voiceInstructions(distanceAlongGeometry = null),
        )

        assertEquals(
            emptyList<VoiceInstructions>(),
            sut.getNextVoiceInstructions(routeProgressData()),
        )
    }

    @Test
    fun `getNextVoiceInstructions for single step with older instructions`() {
        every { currentStep.voiceInstructions() } returns listOf(
            voiceInstructions(distanceRemaining + 100.0),
            voiceInstructions(distanceRemaining + 0.01),
        )

        assertEquals(
            emptyList<VoiceInstructions>(),
            sut.getNextVoiceInstructions(routeProgressData()),
        )
    }

    @Test
    fun `getNextVoiceInstructions for single step with newer instructions`() {
        val instruction1 = voiceInstructions(distanceRemaining.toDouble())
        val instruction2 = voiceInstructions(distanceRemaining - 50.0)
        every { currentStep.voiceInstructions() } returns listOf(instruction1, instruction2)

        assertEquals(
            listOf(instruction1, instruction2),
            sut.getNextVoiceInstructions(routeProgressData()),
        )
    }

    @Test
    fun `getNextVoiceInstructions for single step with mixed instructions`() {
        val instruction1 = voiceInstructions(distanceRemaining - 50.0)
        val instruction2 = voiceInstructions(null)
        val instruction3 = voiceInstructions(distanceRemaining + 100.0)
        val instruction4 = voiceInstructions(distanceRemaining.toDouble())
        val instruction5 = voiceInstructions(distanceRemaining + 0.01)
        every { currentStep.voiceInstructions() } returns listOf(
            instruction1,
            instruction2,
            instruction3,
            instruction4,
            instruction5,
        )

        assertEquals(
            listOf(instruction1, instruction4),
            sut.getNextVoiceInstructions(routeProgressData()),
        )
    }

    @Test
    fun `getNextVoiceInstructions when current step duration is equal to observableTime`() {
        setUpFor2ActiveSteps()

        assertEquals(
            listOf(currentStepInstruction),
            sut.getNextVoiceInstructions(
                routeProgressData(stepIndex = 1, stepDurationRemaining = observableTime.toDouble()),
            ),
        )
    }

    @Test
    fun `getNextVoiceInstructions when current step duration is equal to observableTime (=0)`() {
        setUpFor2ActiveSteps()
        val sut = TimeBasedNextVoiceInstructionsProvider(0)

        assertEquals(
            listOf(currentStepInstruction),
            sut.getNextVoiceInstructions(
                routeProgressData(stepIndex = 1, stepDurationRemaining = 0.0),
            ),
        )
    }

    @Test
    fun `getNextVoiceInstructions when current step duration is greater than observableTime`() {
        setUpFor2ActiveSteps()

        assertEquals(
            listOf(currentStepInstruction),
            sut.getNextVoiceInstructions(
                routeProgressData(stepIndex = 1, stepDurationRemaining = observableTime + 50.0),
            ),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 2 steps when next step does not have instructions`() {
        setUpFor2ActiveSteps()
        every { nextStep.voiceInstructions() } returns null

        assertEquals(
            listOf(currentStepInstruction),
            sut.getNextVoiceInstructions(routeProgressData(stepIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 2 steps when all should be used, time less than limit`() {
        setUpFor2ActiveSteps()
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        every { nextStep.voiceInstructions() } returns listOf(instruction1, instruction2)
        every { nextStep.duration() } returns 5.0

        assertEquals(
            listOf(currentStepInstruction, instruction1, instruction2),
            sut.getNextVoiceInstructions(routeProgressData(stepIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 2 steps when all should be used, time greater than limit`() {
        setUpFor2ActiveSteps()
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        every { nextStep.voiceInstructions() } returns listOf(instruction1, instruction2)
        every { nextStep.duration() } returns 25.0

        assertEquals(
            listOf(currentStepInstruction, instruction1, instruction2),
            sut.getNextVoiceInstructions(routeProgressData(stepIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 2 steps when only next step has instructions`() {
        setUpFor2ActiveSteps()
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        every { nextStep.voiceInstructions() } returns listOf(instruction1, instruction2)
        every { currentStep.voiceInstructions() } returns null

        assertEquals(
            listOf(instruction1, instruction2),
            sut.getNextVoiceInstructions(routeProgressData(stepIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 2 steps when only 1 step should be used`() {
        setUpFor2ActiveSteps()
        val currentDurationRemaining = observableTime + 20.0
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        every { nextStep.voiceInstructions() } returns listOf(instruction1, instruction2)

        assertEquals(
            listOf(currentStepInstruction),
            sut.getNextVoiceInstructions(
                routeProgressData(stepIndex = 1, stepDurationRemaining = currentDurationRemaining),
            ),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 3 steps when all 3 should be used, time less than limit`() {
        setUpFor3ActiveSteps()
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        every { nextStep.voiceInstructions() } returns listOf(instruction1)
        every { nextStep.duration() } returns 10.0
        every { stepAfterNext.voiceInstructions() } returns listOf(instruction2)
        every { stepAfterNext.duration() } returns 5.0

        assertEquals(
            listOf(currentStepInstruction, instruction1, instruction2),
            sut.getNextVoiceInstructions(routeProgressData(stepIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 3 steps when all should be used, time greater than limit`() {
        setUpFor3ActiveSteps()
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        every { nextStep.voiceInstructions() } returns listOf(instruction1)
        every { nextStep.duration() } returns 10.0
        every { stepAfterNext.voiceInstructions() } returns listOf(instruction2)
        every { stepAfterNext.duration() } returns 15.0

        assertEquals(
            listOf(currentStepInstruction, instruction1, instruction2),
            sut.getNextVoiceInstructions(routeProgressData(stepIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 3 steps when only 2 should be used`() {
        setUpFor3ActiveSteps()
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        every { nextStep.voiceInstructions() } returns listOf(instruction1)
        every { nextStep.duration() } returns 21.0
        every { stepAfterNext.voiceInstructions() } returns listOf(instruction2)
        every { stepAfterNext.duration() } returns 5.0

        assertEquals(
            listOf(currentStepInstruction, instruction1),
            sut.getNextVoiceInstructions(routeProgressData(stepIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 3 steps when only 1 should be used`() {
        setUpFor3ActiveSteps()
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        val currentDurationRemaining = observableTime + 1.0
        every { nextStep.voiceInstructions() } returns listOf(instruction1)
        every { nextStep.duration() } returns 1.0
        every { stepAfterNext.voiceInstructions() } returns listOf(instruction2)
        every { stepAfterNext.duration() } returns 1.0

        assertEquals(
            listOf(currentStepInstruction),
            sut.getNextVoiceInstructions(
                routeProgressData(stepIndex = 1, stepDurationRemaining = currentDurationRemaining),
            ),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 2 legs when next leg does not have steps`() {
        setUpFor2ActiveSingleStepLegs()
        every { nextLeg.steps() } returns null

        assertEquals(
            listOf(currentStepInstruction),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 2 legs when next leg has empty steps`() {
        setUpFor2ActiveSingleStepLegs()
        every { nextLeg.steps() } returns emptyList()

        assertEquals(
            listOf(currentStepInstruction),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 2 legs when all should be used, time less than limit`() {
        setUpFor2ActiveSingleStepLegs()
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        every { nextLegStep.voiceInstructions() } returns listOf(instruction1, instruction2)
        every { nextLegStep.duration() } returns 5.0

        assertEquals(
            listOf(currentStepInstruction, instruction1, instruction2),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 2 legs when all should be used, time greater than limit`() {
        setUpFor2ActiveSingleStepLegs()
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        every { nextLegStep.voiceInstructions() } returns listOf(instruction1, instruction2)
        every { nextLegStep.duration() } returns 25.0

        assertEquals(
            listOf(currentStepInstruction, instruction1, instruction2),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 2 legs when only next leg has instructions`() {
        setUpFor2ActiveSingleStepLegs()
        every { currentStep.voiceInstructions() } returns null
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        every { nextLegStep.voiceInstructions() } returns listOf(instruction1, instruction2)

        assertEquals(
            listOf(instruction1, instruction2),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 2 legs when only 1 leg should be used`() {
        setUpFor2ActiveSingleStepLegs()
        val currentDurationRemaining = observableTime + 20.0
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        every { nextLegStep.voiceInstructions() } returns listOf(instruction1, instruction2)

        assertEquals(
            listOf(currentStepInstruction),
            sut.getNextVoiceInstructions(
                routeProgressData(legIndex = 1, stepDurationRemaining = currentDurationRemaining),
            ),
        )
    }

    @Test
    fun `getNextVoiceInstructions for (multi-step, single-step) legs, should use only 1st leg`() {
        setUpFor2ActiveSingleStepLegs()
        setUpFor2ActiveSteps()
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        every { nextStep.voiceInstructions() } returns listOf(instruction1)
        every { nextStep.duration() } returns 25.0
        every { nextLegStep.voiceInstructions() } returns listOf(instruction2)
        every { nextLegStep.duration() } returns 5.0

        assertEquals(
            listOf(currentStepInstruction, instruction1),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 1, stepIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for (multi-step, single-step) legs, should use both legs`() {
        setUpFor2ActiveSingleStepLegs()
        setUpFor2ActiveSteps()
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        every { nextStep.voiceInstructions() } returns listOf(instruction1)
        every { nextStep.duration() } returns 15.0
        every { nextLegStep.voiceInstructions() } returns listOf(instruction2)
        every { nextLegStep.duration() } returns 15.0

        assertEquals(
            listOf(currentStepInstruction, instruction1, instruction2),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 1, stepIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for (single-step, multi-step) legs, should use only first leg`() {
        val instruction1 = voiceInstructions(1.0)
        val nextLegNextStep = mockk<LegStep>(relaxed = true) {
            every { voiceInstructions() } returns listOf(instruction1)
        }
        setUpFor2ActiveSingleStepLegs()
        every { nextLeg.steps() } returns listOf(
            nextLegStep,
            nextLegNextStep,
        )
        val currentDurationRemaining = observableTime + 1.0

        assertEquals(
            listOf(currentStepInstruction),
            sut.getNextVoiceInstructions(
                routeProgressData(legIndex = 1, stepDurationRemaining = currentDurationRemaining),
            ),
        )
    }

    @Test
    fun `getNextVoiceInstructions for (single-step, multi-step) legs, should use both legs`() {
        val instruction1 = voiceInstructions(1.0)
        val nextLegNextStep = mockk<LegStep>(relaxed = true) {
            every { voiceInstructions() } returns listOf(instruction1)
            every { duration() } returns 35.0
        }
        setUpFor2ActiveSingleStepLegs()
        every { nextLeg.steps() } returns listOf(
            nextLegStep,
            nextLegNextStep,
        )
        val currentDurationRemaining = observableTime - 20.0

        assertEquals(
            listOf(currentStepInstruction, instruction1),
            sut.getNextVoiceInstructions(
                routeProgressData(legIndex = 1, stepDurationRemaining = currentDurationRemaining),
            ),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 3 single-step legs when all should be used`() {
        setUpFor3ActiveSingleStepLegs()
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        every { nextLegStep.voiceInstructions() } returns listOf(instruction1)
        every { nextLegStep.duration() } returns 10.0
        every { legAfterNextStep.voiceInstructions() } returns listOf(instruction2)
        every { legAfterNextStep.duration() } returns 5.0

        assertEquals(
            listOf(currentStepInstruction, instruction1, instruction2),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 3 single-step legs when only 2 should be used`() {
        setUpFor3ActiveSingleStepLegs()
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        every { nextLegStep.voiceInstructions() } returns listOf(instruction1)
        every { nextLegStep.duration() } returns 21.0
        every { legAfterNextStep.voiceInstructions() } returns listOf(instruction2)
        every { legAfterNextStep.duration() } returns 5.0

        assertEquals(
            listOf(currentStepInstruction, instruction1),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 3 single-step legs when only 1 should be used`() {
        setUpFor3ActiveSingleStepLegs()
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        val currentDurationRemaining = observableTime + 1.0
        every { nextLegStep.voiceInstructions() } returns listOf(instruction1)
        every { nextLegStep.duration() } returns 1.0
        every { legAfterNextStep.voiceInstructions() } returns listOf(instruction2)
        every { legAfterNextStep.duration() } returns 1.0

        assertEquals(
            listOf(currentStepInstruction),
            sut.getNextVoiceInstructions(
                routeProgressData(legIndex = 1, stepDurationRemaining = currentDurationRemaining),
            ),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 3 two-step legs when only first 2 steps should be used`() {
        setUpFor3ActiveTwoStepLegs()
        val nextStepInstruction = voiceInstructions(100.0)
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        val instruction3 = voiceInstructions(3.0)
        val instruction4 = voiceInstructions(4.0)
        every { nextStep.voiceInstructions() } returns listOf(nextStepInstruction)
        every { nextStep.duration() } returns 21.0
        every { nextLegStep.voiceInstructions() } returns listOf(instruction1)
        every { nextLegStep.duration() } returns 5.0
        every { nextLeg.steps()!![1].voiceInstructions() } returns listOf(instruction2)
        every { nextLeg.steps()!![1].duration() } returns 1.0
        every { legAfterNextStep.voiceInstructions() } returns listOf(instruction3)
        every { legAfterNextStep.duration() } returns 5.0
        every { legAfterNext.steps()!![1].voiceInstructions() } returns listOf(instruction4)
        every { legAfterNext.steps()!![1].duration() } returns 1.0

        assertEquals(
            listOf(currentStepInstruction, nextStepInstruction),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 3 two-step legs when only first 3 steps should be used`() {
        setUpFor3ActiveTwoStepLegs()
        val nextStepInstruction = voiceInstructions(100.0)
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        val instruction3 = voiceInstructions(3.0)
        val instruction4 = voiceInstructions(4.0)
        every { nextStep.voiceInstructions() } returns listOf(nextStepInstruction)
        every { nextStep.duration() } returns 19.0
        every { nextLegStep.voiceInstructions() } returns listOf(instruction1)
        every { nextLegStep.duration() } returns 5.0
        val nextLegSecondStep = nextLeg.steps()!![1]
        every { nextLegSecondStep.voiceInstructions() } returns listOf(instruction2)
        every { nextLegSecondStep.duration() } returns 5.0
        every { legAfterNextStep.voiceInstructions() } returns listOf(instruction3)
        every { legAfterNextStep.duration() } returns 5.0
        val legAfterNextSecondStep = legAfterNext.steps()!![1]
        every { legAfterNextSecondStep.voiceInstructions() } returns listOf(instruction4)
        every { legAfterNextSecondStep.duration() } returns 1.0

        assertEquals(
            listOf(currentStepInstruction, nextStepInstruction, instruction1),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 3 two-step legs when only first 5 steps should be used`() {
        setUpFor3ActiveTwoStepLegs()
        val nextStepInstruction = voiceInstructions(100.0)
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        val instruction3 = voiceInstructions(3.0)
        val instruction4 = voiceInstructions(4.0)
        every { nextStep.voiceInstructions() } returns listOf(nextStepInstruction)
        every { nextStep.duration() } returns 2.0
        every { nextLegStep.voiceInstructions() } returns listOf(instruction1)
        every { nextLegStep.duration() } returns 5.0
        val nextLegSecondStep = nextLeg.steps()!![1]
        every { nextLegSecondStep.voiceInstructions() } returns listOf(instruction2)
        every { nextLegSecondStep.duration() } returns 7.0
        every { legAfterNextStep.voiceInstructions() } returns listOf(instruction3)
        every { legAfterNextStep.duration() } returns 9.0
        val legAfterNextSecondStep = legAfterNext.steps()!![1]
        every { legAfterNextSecondStep.voiceInstructions() } returns listOf(instruction4)
        every { legAfterNextSecondStep.duration() } returns 1.0

        assertEquals(
            listOf(
                currentStepInstruction,
                nextStepInstruction,
                instruction1,
                instruction2,
                instruction3,
            ),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 3 two-step legs when all steps should be used`() {
        setUpFor3ActiveTwoStepLegs()
        val nextStepInstruction = voiceInstructions(100.0)
        val instruction1 = voiceInstructions(1.0)
        val instruction2 = voiceInstructions(2.0)
        val instruction3 = voiceInstructions(3.0)
        val instruction4 = voiceInstructions(4.0)
        every { nextStep.voiceInstructions() } returns listOf(nextStepInstruction)
        every { nextStep.duration() } returns 2.0
        every { nextLegStep.voiceInstructions() } returns listOf(instruction1)
        every { nextLegStep.duration() } returns 5.0
        val nextLegSecondStep = nextLeg.steps()!![1]
        every { nextLegSecondStep.voiceInstructions() } returns listOf(instruction2)
        every { nextLegSecondStep.duration() } returns 7.0
        every { legAfterNextStep.voiceInstructions() } returns listOf(instruction3)
        every { legAfterNextStep.duration() } returns 1.0
        val legAfterNextSecondStep = legAfterNext.steps()!![1]
        every { legAfterNextSecondStep.voiceInstructions() } returns listOf(instruction4)
        every { legAfterNext.duration() } returns 10.0

        assertEquals(
            listOf(
                currentStepInstruction,
                nextStepInstruction,
                instruction1,
                instruction2,
                instruction3,
                instruction4,
            ),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 3 legs when middle leg has null steps`() {
        setUpFor3ActiveTwoStepLegs()
        every { nextLeg.steps() } returns null
        val nextStepInstruction = voiceInstructions(1.0)
        val legAfterNextStepInstruction = voiceInstructions(2.0)
        every { nextStep.voiceInstructions() } returns listOf(nextStepInstruction)
        every { nextStep.duration() } returns 2.0
        every {
            legAfterNextStep.voiceInstructions()
        } returns listOf(legAfterNextStepInstruction)
        every { legAfterNextStep.duration() } returns 1.0

        assertEquals(
            listOf(currentStepInstruction, nextStepInstruction, legAfterNextStepInstruction),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 1)),
        )
    }

    @Test
    fun `getNextVoiceInstructions for 3 legs when middle leg has empty steps`() {
        setUpFor3ActiveTwoStepLegs()
        every { nextLeg.steps() } returns emptyList()
        val nextStepInstruction = voiceInstructions(1.0)
        val legAfterNextStepInstruction = voiceInstructions(2.0)
        every { nextStep.voiceInstructions() } returns listOf(nextStepInstruction)
        every { nextStep.duration() } returns 2.0
        every {
            legAfterNextStep.voiceInstructions()
        } returns listOf(legAfterNextStepInstruction)
        every { legAfterNextStep.duration() } returns 1.0

        assertEquals(
            listOf(currentStepInstruction, nextStepInstruction, legAfterNextStepInstruction),
            sut.getNextVoiceInstructions(routeProgressData(legIndex = 1)),
        )
    }

    private fun setUpFor2ActiveSteps() {
        every { currentLeg.steps() } returns listOf(
            prevStep,
            currentStep,
            nextStep,
        )
    }

    private fun setUpFor2ActiveSingleStepLegs() {
        every { currentRoute.legs() } returns listOf(
            prevLeg,
            currentLeg,
            nextLeg,
        )
    }

    private fun setUpFor3ActiveSteps() {
        every { currentLeg.steps() } returns listOf(
            prevStep,
            currentStep,
            nextStep,
            stepAfterNext,
        )
    }

    private fun setUpFor3ActiveSingleStepLegs() {
        every { currentRoute.legs() } returns listOf(
            prevLeg,
            currentLeg,
            nextLeg,
            legAfterNext,
        )
    }

    private fun setUpFor3ActiveTwoStepLegs() {
        every { currentRoute.legs() } returns listOf(
            prevLeg,
            currentLeg,
            nextLeg,
            legAfterNext,
        )
        every { currentLeg.steps() } returns listOf(currentStep, nextStep)
        every { nextLeg.steps() } returns listOf(nextLegStep, mockk(relaxed = true))
        every { legAfterNext.steps() } returns listOf(legAfterNextStep, mockk(relaxed = true))
    }

    private fun voiceInstructions(
        distanceAlongGeometry: Double? = null,
    ): VoiceInstructions {
        return VoiceInstructions.builder().distanceAlongGeometry(distanceAlongGeometry).build()
    }

    private fun routeProgressData(
        route: DirectionsRoute = currentRoute,
        legIndex: Int = 0,
        stepIndex: Int = 0,
        stepDurationRemaining: Double = observableTime - 20.0,
        stepDistanceRemaining: Double = distanceRemaining.toDouble(),
    ): RouteProgressData = RouteProgressData(
        route,
        legIndex,
        stepIndex,
        stepDurationRemaining,
        stepDistanceRemaining,
    )
}
