package dev.materii.gloom.api.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// Null-safe accessors for walking raw GraphQL responses.

fun JsonElement?.obj(key: String): JsonObject? = (this as? JsonObject)?.get(key) as? JsonObject

fun JsonElement?.arr(key: String): JsonArray? = (this as? JsonObject)?.get(key) as? JsonArray

fun JsonElement?.str(key: String): String? {
    val value = (this as? JsonObject)?.get(key) as? JsonPrimitive ?: return null
    return if (value is JsonNull) null else value.content
}

fun JsonElement?.int(key: String): Int? = str(key)?.toIntOrNull()

fun JsonElement?.bool(key: String): Boolean? = str(key)?.toBooleanStrictOrNull()

/** The `nodes` array of a connection object under [key], as objects. */
fun JsonElement?.nodes(key: String): List<JsonObject> =
    obj(key).arr("nodes")?.mapNotNull { it as? JsonObject } ?: emptyList()
