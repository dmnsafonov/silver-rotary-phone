package net.dimanss47.swpersona

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.util.*

val gson: Gson = GsonBuilder()
    .registerTypeAdapterFactory(PersonDetailsTypeAdapterFactory())
    .create()

class PersonDetailsTypeAdapterFactory : TypeAdapterFactory {
    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? = when(type.rawType) {
        PersonDetailsRaw::class.java -> object : TypeAdapter<T>() {
            override fun read(input: JsonReader): T {
                val innerTypeToken = object : TypeToken<Map<String, JsonElement>>() {}

                @Suppress("UNCHECKED_CAST")
                return PersonDetailsRaw(gson.fromJson(input, innerTypeToken.type)!!) as T
            }

            override fun write(out: JsonWriter, value: T) {
                throw UnsupportedOperationException()
            }
        }

        OrderedPersonDetails::class.java -> object : TypeAdapter<T>() {
            override fun read(input: JsonReader?): T {
                val innerTypeToken = object : TypeToken<Map<String, PersonalDetail>>() {}
                val raw: Map<String, PersonalDetail> = gson.fromJson(input, innerTypeToken.type)
                val data: SortedMap<String,PersonalDetail> = TreeMap(PersonDetailsEntriesComparator)
                raw.asSequence().associateTo(data) { (k,v) -> Pair(k,v) }

                @Suppress("UNCHECKED_CAST")
                return OrderedPersonDetails(data) as T
            }

            override fun write(out: JsonWriter?, valueRaw: T) {
                val value = valueRaw as OrderedPersonDetails
                gson.toJson(
                    value,
                    object : TypeToken<SortedMap<String, PersonalDetail>>() {}.type,
                    out
                )
            }
        }

        else -> null
    }
}
