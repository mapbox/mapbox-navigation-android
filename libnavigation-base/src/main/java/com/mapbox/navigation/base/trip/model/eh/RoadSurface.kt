package com.mapbox.navigation.base.trip.model.eh

import androidx.annotation.StringDef

/**
 * Surface type of the road.
 * Check for details: https://wiki.openstreetmap.org/wiki/Key:surface
 */
object RoadSurface {

    /**
     * Paved smooth surface. The vehicle can traverse the route without significant risk of damage
     * (e.g., to vehicle undercarriage/drivetrain) or injury (e.g., by falling).
     */
    const val PAVED_SMOOTH = "PAVED_SMOOTH"

    /**
     * A type that is predominantly paved, it is covered with paving stones, concrete or bitumen.
     */
    const val PAVED = "PAVED"

    /**
     * Heavily damaged paved roads that need maintenance: many potholes, some of them quite deep.
     */
    const val PAVED_ROUGH = "PAVED_ROUGH"

    /**
     * A stable surface, it's a mixture of larger (e.g., gravel) and smaller (e.g., sand) parts,
     * compacted (e.g., with a roller).
     */
    const val COMPACTED = "COMPACTED"

    /**
     * No special surface, the ground itself has marks of human or animal usage.
     * It is prone to erosion and therefore often uneven.
     */
    const val DIRT = "DIRT"

    /**
     * Used for cases ranging from huge gravel pieces like track ballast used as surface,
     * through small pieces of gravel to compacted surface.
     */
    const val GRAVEL = "GRAVEL"

    /**
     * Grass or stepping stones surface. Used for pedestrian and bicycle routing only.
     */
    const val PATH = "PATH"

    /**
     * Road with such surface type can't be used for any routing.
     */
    const val IMPASSABLE = "IMPASSABLE"

    /**
     * Retention policy for the RoadSurface
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        PAVED_SMOOTH,
        PAVED,
        PAVED_ROUGH,
        COMPACTED,
        DIRT,
        GRAVEL,
        PATH,
        IMPASSABLE,
    )
    annotation class Type
}
