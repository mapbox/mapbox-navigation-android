package com.mapbox.services.android.navigation.v5.milestone

import com.mapbox.services.android.navigation.v5.instruction.Instruction
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

/**
 * Base Milestone statement. Subclassed to provide concrete statements.
 *
 * @since 0.4.0
 */
// Public exposed for creation of milestone classes outside SDK
abstract class Milestone(private val builder: Builder) {

    /**
     * Milestone specific identifier as an `int` value, useful for deciphering which milestone
     * invoked [MilestoneEventListener.onMilestoneEvent].
     *
     * @return `int` representing the identifier
     * @since 0.4.0
     */
    open val identifier: Int
        get() = builder.getIdentifier()

    /**
     * Milestone specific [Instruction], which can be used to build a [String]
     * instruction specified by the superclass.
     *
     * @return [Instruction] to be used to build the [String] passed to
     * [MilestoneEventListener.onMilestoneEvent]
     * @since 0.4.0
     */
    open val instruction: Instruction?
        get() = builder.getInstruction()

    /**
     * A milestone can either be passed in to the
     * [com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation] object
     * (recommended) or validated directly inside your activity.
     *
     * @param previousRouteProgress last locations generated [RouteProgress] object used to
     * determine certain [TriggerProperty]s
     * @param routeProgress used to determine certain [TriggerProperty]s
     * @return true if the milestone trigger's valid, else false
     * @since 0.4.0
     */
    abstract fun isOccurring(
        previousRouteProgress: RouteProgress,
        routeProgress: RouteProgress
    ): Boolean

    /**
     * Build a new [Milestone]
     *
     * @since 0.4.0
     */
    abstract class Builder {

        private var identifier: Int = 0
        private var instruction: Instruction? = null

        /**
         * Milestone specific identifier as an `int` value, useful for deciphering which milestone
         * invoked [MilestoneEventListener.onMilestoneEvent].
         *
         * @return `int` representing the identifier
         * @since 0.4.0
         */
        fun getIdentifier(): Int = identifier

        /**
         * Milestone specific identifier as an `int` value, useful for deciphering which milestone
         * invoked [MilestoneEventListener.onMilestoneEvent].
         *
         * @param identifier an `int` used to identify this milestone instance
         * @return this builder
         * @since 0.4.0
         */
        fun setIdentifier(identifier: Int): Builder {
            this.identifier = identifier
            return this
        }

        /**
         * Milestone specific [Instruction], which can be used to build a [String]
         * instruction specified by the superclass
         *
         * @return this builder
         * @since 0.4.0
         */
        fun getInstruction(): Instruction? = instruction

        fun setInstruction(instruction: Instruction): Builder {
            this.instruction = instruction
            return this
        }

        /**
         * The list of triggers that are used to determine whether this milestone should invoke
         * [MilestoneEventListener.onMilestoneEvent]
         *
         * @param trigger a single simple statement or compound statement found in [Trigger]
         * @return this builder
         * @since 0.4.0
         */
        abstract fun setTrigger(trigger: Trigger.Statement?): Builder

        internal abstract fun getTrigger(): Trigger.Statement?

        /**
         * Build a new milestone
         *
         * @return A new [Milestone] object
         * @since 0.4.0
         */
        abstract fun build(): Milestone
    }
}
