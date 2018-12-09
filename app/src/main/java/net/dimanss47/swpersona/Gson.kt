package net.dimanss47.swpersona

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

val gson: Gson = GsonBuilder()
    .registerTypeAdapterFactory(PersonDetailsTypeAdapterFactory())
    .create()

class PersonDetailsTypeAdapterFactory : TypeAdapterFactory {
    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if(type.rawType != PersonDetailsRaw::class.java) return null

        return object : TypeAdapter<T>() {
            override fun read(input: JsonReader): T {
                val innerTypeToken = object : TypeToken<Map<String, JsonElement>>() {}

                @Suppress("UNCHECKED_CAST")
                return PersonDetailsRaw(gson.fromJson(input, innerTypeToken.type)!!) as T
            }

            override fun write(out: JsonWriter, value: T) {
                throw UnsupportedOperationException()
            }
        }
    }
}
