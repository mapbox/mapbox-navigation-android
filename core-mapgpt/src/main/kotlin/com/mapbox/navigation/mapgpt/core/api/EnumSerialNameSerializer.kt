package com.mapbox.navigation.mapgpt.core.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Generic serializer that serializes string literals to enums and fallbacks to a value of choice in
 * case the the value is unrecognized.
 */
open class EnumSerialNameSerializer<E : Enum<E>>(
    private val values: Array<out E>,
    private val default: E,
) : KSerializer<E> {

    /**
     * Describes the structure of the serializable representation of E, produced by this serializer.
     */
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        values.first()::class.qualifiedName!!,
        PrimitiveKind.STRING
    )

    private val lookup = values.associateBy({ it }, { it.name })
    private val revLookup = values.associateBy { it.name }

    /**
     * Serializes the value of type E using the format that is represented by the given encoder.
     *
     * @param encoder to be used to serialize the [value]
     * @param value to be serialized
     */
    override fun serialize(encoder: Encoder, value: E) {
        encoder.encodeString(lookup.getValue(value))
    }

    /**
     * Deserializes the value of type E using the format that is represented by the given decoder.
     *
     * @param decoder to be used to deserialize
     */
    override fun deserialize(decoder: Decoder): E {
        return revLookup[decoder.decodeString()] ?: default
    }
}
