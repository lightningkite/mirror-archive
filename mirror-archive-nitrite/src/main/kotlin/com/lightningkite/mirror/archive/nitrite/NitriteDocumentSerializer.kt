package com.lightningkite.mirror.archive.nitrite

import com.lightningkite.mirror.archive.model.Id
import com.lightningkite.mirror.info.*
import com.lightningkite.mirror.serialization.*
import com.lightningkite.mirror.serialization.json.JsonSerializer
import com.lightningkite.mirror.string.CharIteratorReader
import org.dizitart.no2.Document
import org.dizitart.no2.NitriteId
import kotlin.reflect.KClass

@Suppress("LeakingThis")
class NitriteDocumentSerializer(override val registry: SerializationRegistry) :
        Decoder<Any?>,
        Encoder<(Any?) -> Unit> {

    val jsonSerializer = JsonSerializer(registry)

    override val arbitraryDecoders: MutableList<Decoder.Generator<Any?>> = ArrayList()
    override val decoders: MutableMap<Type<*>, TypeDecoder<Any?, Any?>> = HashMap()
    override val kClassDecoders: MutableMap<KClass<*>, (Type<*>) -> TypeDecoder<Any?, Any?>?> = HashMap()
    override val arbitraryEncoders: MutableList<Encoder.Generator<(Any?) -> Unit>> = ArrayList()
    override val encoders: MutableMap<Type<*>, TypeEncoder<(Any?) -> Unit, Any?>> = HashMap()
    override val kClassEncoders: MutableMap<KClass<*>, (Type<*>) -> TypeEncoder<(Any?) -> Unit, Any?>?> = HashMap()

    fun <T> encode(value: T, type: Type<T>): Any? = getResult(rawEncoder(type), value)


    init {
        addDecoder(Unit::class.type) { Unit }
        addEncoder(Unit::class.type) { invoke(null) }

        fun <T : Any> noop(kClass: KClass<T>) {
            addDecoder(kClass.type) { this as T }
            addEncoder(kClass.type) { invoke(it) }
        }

        noop(Int::class)
        noop(Short::class)
        noop(Byte::class)
        noop(Long::class)
        noop(Float::class)
        noop(Double::class)
        noop(Number::class)
        noop(Boolean::class)
        noop(String::class)
        noop(ByteArray::class)

        addEncoder(Id::class.type){
            invoke(it.toUUIDString())
        }
        addDecoder(Id::class.type){
            Id.fromUUIDString(this as String)
        }

        setNotNullEncoder(List::class) { type ->
            val subEncoder = rawEncoder(type.param(0).type)
            return@setNotNullEncoder {
                invoke(it.map { getResult(subEncoder, it) })
            }
        }
        setNotNullDecoder(List::class) { type ->
            val subDecoder = rawDecoder(type.param(0).type)
            return@setNotNullDecoder {
                (this as List<*>).map { subDecoder.invoke(it) }
            }
        }

        setNotNullEncoder(Map::class) { type ->
            val subKeyEncoder: ((Any?) -> Unit).(Any?) -> Unit = if (type.param(0).type == String::class.type) {
                rawEncoder(String::class.type)
            } else {
                val encoder = jsonSerializer.rawEncoder(type.param(0).type)
                0
                { value ->
                    this.invoke(buildString { encoder(this, value) })
                }
            }
            val subValueEncoder = rawEncoder(type.param(1).type)
            return@setNotNullEncoder {
                invoke(it.entries.associateTo(Document()) { "asdf" to getResult(subValueEncoder, it.value) })
                Unit
            }
        }
        setNotNullDecoder(Map::class) { type ->
            val subKeyDecoder: Any?.() -> Any? = if (type.param(0).type == String::class.type) {
                rawDecoder(String::class.type)
            } else {
                val decoder = jsonSerializer.rawDecoder(type.param(0).type)
                0
                {
                    decoder(CharIteratorReader((this as String).iterator()))
                }
            }
            val subValueDecoder = rawDecoder(type.param(1).type)
            return@setNotNullDecoder {
                (this as Document).entries.associate {
                    subKeyDecoder.invoke(it.key) to subValueDecoder.invoke(it.value)
                }
            }
        }

        initializeEncoders()
        initializeDecoders()

        addEncoder(PolymorphicEncoderGenerator())
        addDecoder(PolymorphicDecoderGenerator())
        addEncoder(ReflectiveEncoderGenerator())
        addDecoder(ReflectiveDecoderGenerator())
        addEncoder(NullableEncoderGenerator())
        addDecoder(NullableDecoderGenerator())
    }


    inner class ReflectiveEncoderGenerator : Encoder.Generator<(Any?) -> Unit> {
        override val priority: Float get() = 0f
        override val description: String = "reflective"

        override fun generateEncoder(type: Type<*>): (((Any?) -> Unit).(value: Any?) -> Unit)? {

            if (type.nullable) return null
            val lazySubCoders by lazy { registry.classInfoRegistry[type.kClass]!!.fields.associateWith { rawEncoder(it.type as Type<*>) } }

            return { value ->
                this.invoke(lazySubCoders.entries
                        .associateTo(Document()) { entry ->
                            val key = entry.key.name
                            key to getResult(entry.value, entry.key.get.untyped(value!!))
                        }
                )
            }
        }
    }

    inner class ReflectiveDecoderGenerator : Decoder.Generator<Any?> {
        override val priority: Float get() = 0f
        override val description: String = "reflective"

        override fun generateDecoder(type: Type<*>): (Any?.() -> Any?)? {
            if (type.nullable) return null
            val fields = registry.classInfoRegistry[type.kClass]!!.fields
            val subCoders by lazy { fields.associate { it.name to rawDecoder(it.type as Type<*>) } }

            return {
                val map = HashMap<String, Any?>()
                for ((key, value) in (this as Document).entries) {
                    if (!subCoders.containsKey(key)) continue
                    map[key] = subCoders[key]!!.invoke(value)
                }
                registry.classInfoRegistry[type.kClass]!!.construct(map)
            }
        }
    }

    inner class PolymorphicEncoderGenerator : Encoder.Generator<(Any?) -> Unit> {
        override val priority: Float get() = .1f
        override val description: String = "polymorphic"

        override fun generateEncoder(type: Type<*>): (((Any?) -> Unit).(value: Any?) -> Unit)? {
            if (registry.classInfoRegistry[type.kClass]!!.canBeInstantiated) return null
            return { value ->
                val underlyingType = when (value) {
                    is List<*> -> List::class
                    is Map<*, *> -> Map::class
                    else -> value!!::class
                }
                this.invoke(listOf(
                        registry.kClassToExternalNameRegistry[underlyingType],
                        getResult(rawEncoder(underlyingType.type), value)
                ))
            }
        }
    }

    inner class PolymorphicDecoderGenerator : Decoder.Generator<Any?> {
        override val priority: Float get() = .1f
        override val description: String = "polymorphic"

        override fun generateDecoder(type: Type<*>): (Any?.() -> Any?)? {
            if (registry.classInfoRegistry[type.kClass]!!.canBeInstantiated) return null
            return {
                val asList = this as List<Any?>
                val actualType = registry.externalNameToInfo[asList[0] as String]!!
                rawDecoder(actualType.type).invoke(asList[1])
            }
        }
    }

    inner class NullableEncoderGenerator : Encoder.Generator<(Any?) -> Unit> {
        override val priority: Float get() = 1f
        override val description: String = "null"

        override fun generateEncoder(type: Type<*>): (((Any?) -> Unit).(value: Any?) -> Unit)? {
            if (!type.nullable) return null
            val underlying = rawEncoder(type.copy(nullable = false))
            return { value ->
                if (value == null) {
                    this.invoke(null)
                } else {
                    underlying.invoke(this, value)
                }
            }
        }
    }

    inner class NullableDecoderGenerator : Decoder.Generator<Any?> {
        override val priority: Float get() = 1f
        override val description: String = "null"

        override fun generateDecoder(type: Type<*>): (Any?.() -> Any?)? {
            if (!type.nullable) return null
            val underlying = rawDecoder(type.copy(nullable = false))
            return {
                if (this == null) null else underlying.invoke(this)
            }
        }
    }

    inline fun getResult(encoder: (((Any?) -> Unit).(Any?) -> Unit), value: Any?): Any? {
        var subvalue: Any? = null
        encoder.invoke({ subvalue = it }, value)
        return subvalue
    }
}
