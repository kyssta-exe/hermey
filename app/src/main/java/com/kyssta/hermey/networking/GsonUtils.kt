package com.kyssta.hermey.networking

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import java.io.IOException
import java.lang.reflect.Type

object GsonSingleton {
    val instance: Gson by lazy {
        GsonBuilder()
            .setLenient()
            .registerTypeAdapter(JsonPrimitive::class.java, LenientPrimitiveDeserializer)
            .registerTypeAdapter(JsonObject::class.java, LenientObjectDeserializer)
            .create()
    }
}

// Tolerant deserializer: accepts string/number/bool in place of expected types
private object LenientPrimitiveDeserializer : JsonDeserializer<JsonPrimitive> {
    override fun deserialize(json: JsonElement, typeOfToken: Type, context: JsonDeserializationContext): JsonPrimitive {
        return JsonPrimitive(json.asString)
    }
}

private object LenientObjectDeserializer : JsonDeserializer<JsonObject> {
    override fun deserialize(json: JsonElement, typeOfToken: Type, context: JsonDeserializationContext): JsonObject {
        return json.asJsonObject
    }
}

fun <T> Gson.fromJsonSafe(json: String, clazz: Class<T>): T? = try { GsonSingleton.instance.fromJson(json, clazz) } catch (e: Exception) { null }
fun <T> Gson.fromJsonSafe(json: String, type: Type): T? = try { GsonSingleton.instance.fromJson(json, type) } catch (e: Exception) { null }
