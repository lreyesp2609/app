package com.example.app.utils

import com.google.gson.*
import java.lang.reflect.Type

class DaysTypeAdapter : JsonDeserializer<List<String>?>, JsonSerializer<List<String>?> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<String>? {
        return when {
            json == null || json.isJsonNull -> null
            json.isJsonArray -> {
                json.asJsonArray.map { it.asString }
            }
            json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                json.asString
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }
            else -> null
        }
    }

    override fun serialize(
        src: List<String>?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return if (src.isNullOrEmpty()) {
            JsonNull.INSTANCE
        } else {
            JsonPrimitive(src.joinToString(","))
        }
    }
}