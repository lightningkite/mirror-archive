package com.lightningkite.mirror.archive.nitrite

import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.info.info
import com.lightningkite.mirror.info.type
import com.lightningkite.mirror.info.untyped
import com.lightningkite.mirror.serialization.*
import com.lightningkite.mirror.serialization.json.JsonSerializer
import com.lightningkite.mirror.string.CharIteratorReader
import org.dizitart.no2.Document
import kotlin.reflect.KClass

@Suppress("LeakingThis")
open class NitriteDocumentSerializer :
        Decoder<Any?>,
        Encoder<(Any?) -> Unit> {

    companion object : NitriteDocumentSerializer()

    override val arbitraryDecoders: MutableList<Decoder.Generator<Any?>> = ArrayList()
    override val decoders: MutableMap<Type<*>, Any?.() -> Any?> = HashMap()
    override val kClassDecoders: MutableMap<KClass<*>, (Type<*>) -> (Any?.() -> Any?)?> = HashMap()
    override val arbitraryEncoders: MutableList<Encoder.Generator<(Any?) -> Unit>> = ArrayList()
    override val encoders: MutableMap<Type<*>, ((Any?) -> Unit).(value: Any?) -> Unit> = HashMap()
    override val kClassEncoders: MutableMap<KClass<*>, (Type<*>) -> (((Any?) -> Unit).(value: Any?) -> Unit)?> = HashMap()

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
                val encoder = JsonSerializer.rawEncoder(type.param(0).type)
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
                val decoder = JsonSerializer.rawDecoder(type.param(0).type)
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

        addEncoder(PolymorphicEncoderGenerator())
        addDecoder(PolymorphicDecoderGenerator())
        addEncoder(ReflectiveEncoderGenerator())
        addDecoder(ReflectiveDecoderGenerator())
        addEncoder(NullableEncoderGenerator())
        addDecoder(NullableDecoderGenerator())
    }


    inner class ReflectiveEncoderGenerator : Encoder.Generator<(Any?) -> Unit> {
        override val priority: Float get() = 0f

        override fun generateEncoder(type: Type<*>): (((Any?) -> Unit).(value: Any?) -> Unit)? {

            if (type.nullable) return null
            val lazySubCoders by lazy { type.kClass.info.fields.associateWith { rawEncoder(it.type as Type<*>) } }

            return { value ->
                this.invoke(lazySubCoders.entries
                        .associateTo(Document()) { entry ->
                            val key = entry.key.name
                            (if(key == "id") "_id" else key) to getResult(entry.value, entry.key.get.untyped(value!!))
                        }
                )
            }
        }
    }

    inner class ReflectiveDecoderGenerator : Decoder.Generator<Any?> {
        override val priority: Float get() = 0f

        override fun generateDecoder(type: Type<*>): (Any?.() -> Any?)? {
            if (type.nullable) return null
            val fields = type.kClass.info.fields
            val subCoders by lazy { fields.associate { it.name to rawDecoder(it.type as Type<*>) } }

            return {
                val map = HashMap<String, Any?>()
                for ((rawKey, value) in (this as Document).entries) {
                    val key = if(rawKey == "_id") "id" else rawKey
                    if (!subCoders.containsKey(key)) continue
                    map[key] = subCoders[key]!!.invoke(value)
                }
                type.kClass.info.construct(map)
            }
        }
    }

    inner class PolymorphicEncoderGenerator : Encoder.Generator<(Any?) -> Unit> {
        override val priority: Float get() = .1f

        override fun generateEncoder(type: Type<*>): (((Any?) -> Unit).(value: Any?) -> Unit)? {
            if (!type.kClass.serializePolymorphic) return null
            return { value ->
                val underlyingType = when (value) {
                    is List<*> -> List::class
                    is Map<*, *> -> Map::class
                    else -> value!!::class
                }
                this.invoke(listOf(
                        underlyingType.externalName,
                        getResult(rawEncoder(underlyingType.type), value)
                ))
            }
        }
    }

    inner class PolymorphicDecoderGenerator : Decoder.Generator<Any?> {
        override val priority: Float get() = .1f

        override fun generateDecoder(type: Type<*>): (Any?.() -> Any?)? {
            if (!type.kClass.serializePolymorphic) return null
            return {
                val asList = this as List<Any?>
                val actualType = KClassesByExternalName[asList[0] as String]!!
                rawDecoder(actualType.type).invoke(asList[1])
            }
        }
    }

    inner class NullableEncoderGenerator : Encoder.Generator<(Any?) -> Unit> {
        override val priority: Float get() = 1f

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
