package com.lightningkite.mirror.archive.redis

import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.info.canBeInstantiated
import com.lightningkite.mirror.info.type
import com.lightningkite.mirror.info.untyped
import com.lightningkite.mirror.serialization.*
import com.lightningkite.mirror.serialization.json.JsonSerializer
import kotlin.reflect.KClass

class RedisSerializer(
        override val registry: SerializationRegistry,
        val backup: StringSerializer = JsonSerializer(registry)
) : Encoder<RedisSerializer.MapWriter>, Decoder<RedisSerializer.MapReader>, Serializer<Map<String, String>> {

    override val arbitraryEncoders: MutableList<Encoder.Generator<MapWriter>> = ArrayList()
    override val encoders: MutableMap<Type<*>, TypeEncoder<MapWriter, Any?>> = HashMap()
    override val kClassEncoders: MutableMap<KClass<*>, (Type<*>) -> TypeEncoder<MapWriter, Any?>?> = HashMap()
    override val arbitraryDecoders: MutableList<Decoder.Generator<MapReader>> = ArrayList()
    override val decoders: MutableMap<Type<*>, TypeDecoder<MapReader, Any?>> = HashMap()
    override val kClassDecoders: MutableMap<KClass<*>, (Type<*>) -> TypeDecoder<MapReader, Any?>?> = HashMap()

    override val contentType: String get() = "application/redis"

    override fun <V> read(from: Map<String, String>, type: Type<V>): V {
        val reader = MapReader(from)
        val result = decoder(type).invoke(reader)
        return result
    }

    override fun <V> write(value: V, type: Type<V>): Map<String, String> {
        val writer = MapWriter()
        encoder(type).invoke(writer, value)
        return writer.output
    }

    class MapWriter(){
        val output = HashMap<String, String>()
        val prefixes = ArrayList<String>()
        var prefix: String = "value"

        fun write(value: String) {
            output[prefix] = value
        }
        fun addPrefix(key: String){
            prefixes += key
            prefix = prefixes.joinToString("_")
        }
        fun removePrefix(){
            prefixes.removeAt(prefixes.lastIndex)
            if(prefixes.isEmpty()) {
                prefix = "value"
            } else {
                prefix = prefixes.joinToString("_")
            }
        }
    }

    class MapReader(val reading: Map<String, String>) {
        val prefixes = ArrayList<String>()
        var prefix: String = "value"

        fun read(): String? {
            return reading[prefix]
        }
        fun addPrefix(key: String){
            prefixes += key
            prefix = prefixes.joinToString("_")
        }
        fun removePrefix(){
            prefixes.removeAt(prefixes.lastIndex)
            if(prefixes.isEmpty()) {
                prefix = "value"
            } else {
                prefix = prefixes.joinToString("_")
            }
        }
    }

    init{
        addEncoder(Unit::class.type) { write("Unit") }
        addDecoder(Unit::class.type) { Unit }
        
        addEncoder(Boolean::class.type) { write(it.toString()) }
        addDecoder(Boolean::class.type) { read()!!.toBoolean() }
        
        addEncoder(Byte::class.type) { write(it.toString()) }
        addDecoder(Byte::class.type) { read()!!.toByte() }
        
        addEncoder(Short::class.type) { write(it.toString()) }
        addDecoder(Short::class.type) { read()!!.toShort() }

        addEncoder(Int::class.type) { write(it.toString()) }
        addDecoder(Int::class.type) { read()!!.toInt() }
        
        addEncoder(Long::class.type) { write(it.toString()) }
        addDecoder(Long::class.type) { read()!!.toLong() }
        
        addEncoder(Float::class.type) { write(it.toString()) }
        addDecoder(Float::class.type) { read()!!.toFloat() }
        
        addEncoder(Double::class.type) { write(it.toString()) }
        addDecoder(Double::class.type) { read()!!.toDouble() }

        addEncoder(String::class.type) { write(it) }
        addDecoder(String::class.type) { read()!! }

        addEncoder(Char::class.type) { write(it.toString()) }
        addDecoder(Char::class.type) { read()!!.first() }

        initializeEncoders()
        initializeDecoders()

        val nullGen = NullableGenerator()
        addEncoder(nullGen)
        addDecoder(nullGen)

        val refGen = ReflectiveGenerator()
        addEncoder(refGen)
        addDecoder(refGen)

        val jsonGen = JsonGenerator()
        addEncoder(jsonGen)
        addDecoder(jsonGen)

        val polyGen = PolyGenerator()
        addEncoder(polyGen)
        addDecoder(polyGen)
    }

    inner class NullableGenerator : Encoder.Generator<MapWriter>, Decoder.Generator<MapReader> {

        override val description: String get() = "null"
        override val priority: Float get() = 1f

        override fun generateEncoder(type: Type<*>): (MapWriter.(value: Any?) -> Unit)? {
            if (!type.nullable) return null
            val underlying = rawEncoder(type.copy(nullable = false))
            return { value ->
                if (value == null) {
                    write("NULL")
                } else {
                    write("NOT_NULL")
                    underlying.invoke(this, value)
                }
            }
        }

        override fun generateDecoder(type: Type<*>): (MapReader.() -> Any?)? {
            if (!type.nullable) return null
            val nnType = type.copy(nullable = false)
            val underlying = rawDecoder(nnType)
            return {
                val r = read()
                if (r == null || r == "NULL") {
                    null
                } else underlying.invoke(this)
            }
        }
    }

    inner class PolyGenerator : Encoder.Generator<MapWriter>, Decoder.Generator<MapReader> {

        override val description: String get() = "poly"
        override val priority: Float get() = .9f

        override fun generateEncoder(type: Type<*>): (MapWriter.(value: Any?) -> Unit)? {
            val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
            if (classInfo.canBeInstantiated) return null
            return { value ->
                val underlyingType = when (value) {
                    is List<*> -> List::class
                    is Map<*, *> -> Map::class
                    else -> value!!::class
                }
                addPrefix("__type")
                write(registry.kClassToExternalNameRegistry[value::class]!!)
                removePrefix()
                rawEncoder(underlyingType.type)
            }
        }

        override fun generateDecoder(type: Type<*>): (MapReader.() -> Any?)? {
            val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
            if (classInfo.canBeInstantiated) return null
            return {
                addPrefix("__type")
                val typeName = read()
                removePrefix()
                val coder = rawDecoder(registry.externalNameToInfo[typeName]!!.type)
                coder.invoke(this)
            }
        }
    }

    inner class JsonGenerator :  Encoder.Generator<MapWriter>, Decoder.Generator<MapReader> {

        override val description: String get() = "json"
        override val priority: Float get() = .1f

        override fun generateEncoder(type: Type<*>): (MapWriter.(value: Any?) -> Unit)? {
            if (type.nullable) return null
            when(type.kClass){
                    List::class,
                    Map::class -> {}
                else -> return null
            }
            return {
                @Suppress("UNCHECKED_CAST")
                write(backup.write(it, type as Type<Any?>))
            }
        }

        override fun generateDecoder(type: Type<*>): (MapReader.() -> Any?)? {
            if (type.nullable) return null
            val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
            val lazySubCoders by lazy { classInfo.fields.map { it.name to rawDecoder(it.type as Type<*>) } }
            return {
                val map = HashMap<String, Any?>()
                for ((field, coder) in lazySubCoders) {
                    addPrefix(field)
                    map[field] = coder.invoke(this)
                    removePrefix()
                }
                classInfo.construct(map)
            }
        }
    }

    inner class ReflectiveGenerator :  Encoder.Generator<MapWriter>, Decoder.Generator<MapReader> {

        override val description: String get() = "reflective"
        override val priority: Float get() = 0f

        override fun generateEncoder(type: Type<*>): (MapWriter.(value: Any?) -> Unit)? {
            if (type.nullable) return null
            val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
            val lazySubCoders by lazy { classInfo.fields.map { it to rawEncoder(it.type as Type<*>) } }
            return {
                for ((field, coder) in lazySubCoders) {
                    addPrefix(field.name)
                    coder.invoke(this, field.get.untyped(it!!))
                    removePrefix()
                }
            }
        }

        override fun generateDecoder(type: Type<*>): (MapReader.() -> Any?)? {
            if (type.nullable) return null
            val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
            val lazySubCoders by lazy { classInfo.fields.map { it.name to rawDecoder(it.type as Type<*>) } }
            return {
                val map = HashMap<String, Any?>()
                for ((field, coder) in lazySubCoders) {
                    addPrefix(field)
                    map[field] = coder.invoke(this)
                    removePrefix()
                }
                classInfo.construct(map)
            }
        }
    }
}