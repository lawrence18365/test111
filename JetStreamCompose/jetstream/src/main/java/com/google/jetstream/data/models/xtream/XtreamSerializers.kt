@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.google.jetstream.data.models.xtream

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeSerializableValue
import kotlinx.serialization.encoding.encodeSerializableValue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
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

private val stringListSerializer = ListSerializer(String.serializer())
private val episodesMapSerializer = MapSerializer(
    String.serializer(),
    ListSerializer(XtreamEpisode.serializer())
)

object FlexibleStringListSerializer : KSerializer<List<String>?> {
    override val descriptor: SerialDescriptor = stringListSerializer.descriptor

    override fun deserialize(decoder: Decoder): List<String>? {
        val input = decoder as? JsonDecoder
            ?: return decoder.decodeSerializableValue(stringListSerializer)
        val element = input.decodeJsonElement()
        return when (element) {
            is JsonNull -> null
            is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.content }
            is JsonPrimitive -> {
                val content = element.content
                if (content.isBlank()) emptyList() else listOf(content)
            }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeSerializableValue(stringListSerializer, value)
        }
    }
}

object FlexibleVideoInfoSerializer : KSerializer<XtreamVideoInfo?> {
    override val descriptor: SerialDescriptor = XtreamVideoInfo.serializer().descriptor

    override fun deserialize(decoder: Decoder): XtreamVideoInfo? {
        val input = decoder as? JsonDecoder
            ?: return decoder.decodeSerializableValue(XtreamVideoInfo.serializer())
        val element = input.decodeJsonElement()
        return when (element) {
            is JsonObject -> input.json.decodeFromJsonElement(XtreamVideoInfo.serializer(), element)
            is JsonNull -> null
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: XtreamVideoInfo?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeSerializableValue(XtreamVideoInfo.serializer(), value)
        }
    }
}

object FlexibleAudioInfoSerializer : KSerializer<XtreamAudioInfo?> {
    override val descriptor: SerialDescriptor = XtreamAudioInfo.serializer().descriptor

    override fun deserialize(decoder: Decoder): XtreamAudioInfo? {
        val input = decoder as? JsonDecoder
            ?: return decoder.decodeSerializableValue(XtreamAudioInfo.serializer())
        val element = input.decodeJsonElement()
        return when (element) {
            is JsonObject -> input.json.decodeFromJsonElement(XtreamAudioInfo.serializer(), element)
            is JsonNull -> null
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: XtreamAudioInfo?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeSerializableValue(XtreamAudioInfo.serializer(), value)
        }
    }
}

object FlexibleEpisodeInfoSerializer : KSerializer<XtreamEpisodeInfo?> {
    override val descriptor: SerialDescriptor = XtreamEpisodeInfo.serializer().descriptor

    override fun deserialize(decoder: Decoder): XtreamEpisodeInfo? {
        val input = decoder as? JsonDecoder
            ?: return decoder.decodeSerializableValue(XtreamEpisodeInfo.serializer())
        val element = input.decodeJsonElement()
        return when (element) {
            is JsonObject -> input.json.decodeFromJsonElement(XtreamEpisodeInfo.serializer(), element)
            is JsonNull -> null
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: XtreamEpisodeInfo?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeSerializableValue(XtreamEpisodeInfo.serializer(), value)
        }
    }
}

object FlexibleEpisodesMapSerializer : KSerializer<Map<String, List<XtreamEpisode>>> {
    override val descriptor: SerialDescriptor = episodesMapSerializer.descriptor

    override fun deserialize(decoder: Decoder): Map<String, List<XtreamEpisode>> {
        val input = decoder as? JsonDecoder
            ?: return decoder.decodeSerializableValue(episodesMapSerializer)
        val element = input.decodeJsonElement()
        return when (element) {
            is JsonObject -> input.json.decodeFromJsonElement(episodesMapSerializer, element)
            is JsonNull -> emptyMap()
            is JsonArray -> emptyMap()
            else -> emptyMap()
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, List<XtreamEpisode>>) {
        encoder.encodeSerializableValue(episodesMapSerializer, value)
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
