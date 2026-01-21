@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.google.jetstream.data.models.xtream

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

object FlexibleIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleIntSerializer", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val input = decoder as? JsonDecoder ?: return decoder.decodeInt()
        return parseInt(input.decodeJsonElement()) ?: 0
    }

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }
}

object FlexibleNullableIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleNullableIntSerializer", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int? {
        val input = decoder as? JsonDecoder ?: return decoder.decodeInt()
        return parseInt(input.decodeJsonElement())
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
    }
}

object FlexibleLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleLongSerializer", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        val input = decoder as? JsonDecoder ?: return decoder.decodeLong()
        return parseLong(input.decodeJsonElement()) ?: 0L
    }

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }
}

object FlexibleNullableLongSerializer : KSerializer<Long?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleNullableLongSerializer", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long? {
        val input = decoder as? JsonDecoder ?: return decoder.decodeLong()
        return parseLong(input.decodeJsonElement())
    }

    override fun serialize(encoder: Encoder, value: Long?) {
        if (value == null) encoder.encodeNull() else encoder.encodeLong(value)
    }
}

object FlexibleDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleDoubleSerializer", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double {
        val input = decoder as? JsonDecoder ?: return decoder.decodeDouble()
        return parseDouble(input.decodeJsonElement()) ?: 0.0
    }

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeDouble(value)
    }
}

object FlexibleNullableDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleNullableDoubleSerializer", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double? {
        val input = decoder as? JsonDecoder ?: return decoder.decodeDouble()
        return parseDouble(input.decodeJsonElement())
    }

    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) encoder.encodeNull() else encoder.encodeDouble(value)
    }
}

object FlexibleNullableStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleNullableStringSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val input = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = input.decodeJsonElement()
    if (element is JsonNull) return null
    if (element is JsonPrimitive) return element.content
    return element.toString()
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value)
    }
}

private fun parseInt(element: JsonElement): Int? = when (element) {
    is JsonNull -> null
    is JsonPrimitive -> element.content.toIntOrNull()
    else -> null
}

private fun parseLong(element: JsonElement): Long? = when (element) {
    is JsonNull -> null
    is JsonPrimitive -> element.content.toLongOrNull()
    else -> null
}

private fun parseDouble(element: JsonElement): Double? = when (element) {
    is JsonNull -> null
    is JsonPrimitive -> element.content.toDoubleOrNull()
    else -> null
}
