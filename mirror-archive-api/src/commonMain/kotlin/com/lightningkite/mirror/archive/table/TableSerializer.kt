package com.lightningkite.mirror.archive.table

import com.lightningkite.mirror.info.FieldInfo
import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.info.type
import com.lightningkite.mirror.serialization.*
import kotlin.reflect.KClass

data class Column(
        val name: String,
        val type: Type<*>,
        val fields: List<FieldInfo<*, *>> = listOf()
)

infix fun String.columnAppend(other: String) = when {
    this.isBlank() && other.isBlank() -> ""
    this.isBlank() -> other
    other.isBlank() -> this
    else -> this + "_" + other
}

class TableSerializer(
        override val registry: SerializationRegistry
) : DefinitionRepository<List<Column>>, Encoder<TableSerializer.MapWriter>, Decoder<TableSerializer.MapWriter> {

    data class MapWriter(val map: HashMap<String, Any?>, var prefix: String = "") {
        inline fun withPrefix(prefix: String, action: () -> Unit) {
            val oldPrefix = this.prefix
            this.prefix = this.prefix columnAppend prefix
            action()
            this.prefix = oldPrefix
        }

        fun put(value: Any?) = map.put(prefix, value)
        fun get(): Any? = map.get(prefix)
    }

    override val arbitraryDefines: MutableList<DefinitionRepository.Generator<List<Column>>> = ArrayList()
    override val definitions: MutableMap<Type<*>, List<Column>> = HashMap()
    override val kClassDefines: MutableMap<KClass<*>, (Type<*>) -> List<Column>?> = HashMap()

    override val arbitraryEncoders: MutableList<Encoder.Generator<MapWriter>> = ArrayList()
    override val encoders: MutableMap<Type<*>, TypeEncoder<MapWriter, Any?>> = HashMap()
    override val kClassEncoders: MutableMap<KClass<*>, (Type<*>) -> TypeEncoder<MapWriter, Any?>?> = HashMap()

    override val arbitraryDecoders: MutableList<Decoder.Generator<MapWriter>> = ArrayList()
    override val decoders: MutableMap<Type<*>, TypeDecoder<MapWriter, Any?>> = HashMap()
    override val kClassDecoders: MutableMap<KClass<*>, (Type<*>) -> TypeDecoder<MapWriter, Any?>?> = HashMap()

    inline fun <reified T> end(type: Type<T>) {
        addDefinition(type, listOf(Column("", type)))
        addEncoder(type) { put(it) }
        addDecoder(type) { get() as T }
    }

    init{
        end(Boolean::class.type)
        end(String::class.type)
        end(Int::class.type)
        end(Long::class.type)
        end(Float::class.type)
        end(Double::class.type)

        initializeEncoders()
        initializeDecoders()
        initializeDefinitions()
    }

    class Reflective(): EncoderG
}