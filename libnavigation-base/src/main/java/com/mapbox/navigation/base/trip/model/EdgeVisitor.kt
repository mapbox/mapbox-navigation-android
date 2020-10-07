package com.mapbox.navigation.base.trip.model

/**
 * Basic Visitor (needed to use dynamic dispatch)
 *
 * Electronic Horizon is still **experimental**, which means that the design of the
 * APIs has open issues which may (or may not) lead to their changes in the future.
 * Roughly speaking, there is a chance that those declarations will be deprecated in the near
 * future or the semantics of their behavior may change in some way that may break some code.
 */
interface EdgeVisitor<out R> {

    /**
     * For more control on how to visit the Edge tree
     */
    fun visit(edge: Edge): R
}
