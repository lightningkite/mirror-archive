package com.lightningkite.kotlinx.persistence

import com.lightningkite.kotlinx.locale.*
import com.lightningkite.kotlinx.reflection.*
import com.lightningkite.kotlinx.serialization.*
import com.lightningkite.kotlinx.serialization.json.JsonSerializer
import com.lightningkite.kotlinx.serialization.json.RawJsonReader
import com.lightningkite.kotlinx.serialization.json.RawJsonWriter
import org.dizitart.no2.Document
import kotlin.reflect.KClass

@Suppress("LeakingThis")
open class NitriteDocumentSerializer :
        StandardReaderRepository<Any?>,
        StandardWriterRepository<Unit, Any?> {

    companion object : NitriteDocumentSerializer()

    override val readerGenerators: MutableList<Pair<Float, TypeReaderGenerator<Any?>>> = ArrayList()
    override val readers: MutableMap<KClass<*>, TypeReader<Any?>> = HashMap()
    override val writerGenerators: MutableList<Pair<Float, TypeWriterGenerator<Unit, Any?>>> = ArrayList()
    override val writers: MutableMap<KClass<*>, TypeWriter<Unit, Any?>> = HashMap()

    inline fun <T : Any> setNullableReader(typeKClass: KClass<T>, crossinline read: Any?.(KxType) -> T) {
        setReader(typeKClass) {
            if (this == null) null
            else read(this, it)
        }
    }

    inline fun <T : Any> setNullableWriter(typeKClass: KClass<T>, crossinline write: Unit.(T, KxType) -> Any?) {
        setWriter(typeKClass) { it, type ->
            if (it == null) null
            else write(it, type)
        }
    }

    var boxWriter: Unit.(typeInfo: KxType, Any?) -> Any? = { typeInfo, value ->
        if (value == null) null
        else listOf(
                CommonSerialization.ExternalNames[value::class.kxReflect],
                writer(value::class).invoke(this, value, typeInfo)
        )
    }
    var boxReader: Any?.(typeInfo: KxType) -> Any? = {
        if (this == null) null
        else {
            val type = CommonSerialization.ExternalNames[(this as List<Any?>)[0] as String]!!
            reader(type.kclass).invoke(this[1], type.kxType)
        }
    }


    init {
        setReader(Unit::class) { Unit }
        setWriter(Unit::class) { it, _ -> null }

        fun <T: Any> noop(kClass: KClass<T>){
            setNullableReader<T>(kClass) { this as T }
            setNullableWriter<T>(kClass) { it, _ -> it }
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

        setNullableReader(Date::class) { Date.iso8601(this as String) }
        setNullableWriter(Date::class) { it, _ -> it.iso8601() }

        setNullableReader(Time::class) { Time.iso8601(this as String) }
        setNullableWriter(Time::class) { it, _ -> it.iso8601() }

        setNullableReader(DateTime::class) { DateTime.iso8601(this as String) }
        setNullableWriter(DateTime::class) { it, _ -> it.iso8601() }

        setNullableReader(TimeStamp::class) { TimeStamp.iso8601(this as String) }
        setNullableWriter(TimeStamp::class) { it, _ -> it.iso8601() }

        setNullableReader(List::class) { typeInfo ->
            val valueSubtype = typeInfo.typeParameters.getOrNull(0)?.takeUnless { it.isStar }?.type
                    ?: KxType(AnyReflection, true)
            val output = ArrayList<Any?>()
            val valueSubtypeReader = reader(valueSubtype.base.kclass)
            for (item in this as List<Any?>) {
                valueSubtypeReader.invoke(item, valueSubtype)
            }
            output
        }
        setNullableWriter(List::class) { value, typeInfo ->
            val valueSubtype = typeInfo.typeParameters.getOrNull(0)?.takeUnless { it.isStar }?.type
                    ?: KxType(AnyReflection, true)
            val valueSubtypeWriter = writer(valueSubtype.base.kclass)
            value.map { valueSubtypeWriter.invoke(Unit, it, valueSubtype) }
        }

        setNullableReader(Map::class) { typeInfo ->
            val keySubtype = typeInfo.typeParameters.getOrNull(0)?.takeUnless { it.isStar }?.type
                    ?: KxType(AnyReflection, true)
            val valueSubtype = typeInfo.typeParameters.getOrNull(1)?.takeUnless { it.isStar }?.type
                    ?: KxType(AnyReflection, true)
            val valueSubtypeReader = reader(valueSubtype.base.kclass)

            val map = LinkedHashMap<Any?, Any?>()
            if (keySubtype.base == StringReflection) {
                (this as Document).forEach { key, subvalue ->
                    map[key] = valueSubtypeReader.invoke(subvalue, valueSubtype)
                }
            } else {
                val keySubReader = JsonSerializer.reader(keySubtype.base.kclass)
                (this as Document).forEach { rawKey, subvalue ->
                    val key = rawKey.let {
                        keySubReader.invoke(RawJsonReader(it.iterator()), keySubtype)
                    }
                    map[key] = valueSubtypeReader.invoke(subvalue, valueSubtype)
                }
            }

            map
        }
        setNullableWriter(Map::class) { value, typeInfo ->
            val keySubtype = typeInfo.typeParameters.getOrNull(0)?.takeUnless { it.isStar }?.type
                    ?: KxType(AnyReflection, true)
            val valueSubtype = typeInfo.typeParameters.getOrNull(1)?.takeUnless { it.isStar }?.type
                    ?: KxType(AnyReflection, true)
            val valueSubtypeWriter = writer(valueSubtype.base.kclass)

            if (keySubtype.base == StringReflection) {
                Document().apply {
                    for ((key, subvalue) in value) {
                        this[key as String] = valueSubtypeWriter.invoke(Unit, subvalue, valueSubtype)
                    }
                }
            } else {
                val keySubWriter = JsonSerializer.writer(keySubtype.base.kclass)
                Document().apply {
                    for ((key, subvalue) in value) {
                        val stringifiedKey = StringBuilder().also {
                            keySubWriter.invoke(RawJsonWriter(it), key, keySubtype)
                        }.toString()
                        this[stringifiedKey] = valueSubtypeWriter.invoke(Unit, subvalue, valueSubtype)
                    }
                }
            }
        }

        val polyboxWriter: TypeWriter<Unit, Any?> = { value, t -> boxWriter.invoke(this, t, value) }
        val polyboxReader: TypeReader<Any?> = {
            @Suppress("UNCHECKED_CAST")
            boxReader.invoke(this, it)
        }

        setReader(Any::class, polyboxReader)
        setWriter(Any::class, polyboxWriter)

        addReaderGenerator(1f, EnumGenerators.readerGenerator<Any?>(this))
        addWriterGenerator(1f, EnumGenerators.writerGenerator<Unit, Any?>(this))

        //Any non-final polyboxing
        addReaderGenerator(.5f, { type: KClass<*> ->
            if (type.serializePolymorphic) {
                polyboxReader
            } else null
        } as TypeReaderGenerator<Any?>)
        addWriterGenerator(.5f, { type: KClass<*> ->
            if (type.serializePolymorphic) {
                polyboxWriter
            } else null
        } as TypeWriterGenerator<Unit, Any?>)

        addReaderGenerator(0f) { type: KClass<*> ->
            val helper = ReflectiveReaderHelper.tryInit(type, this)
                    ?: return@addReaderGenerator null
            return@addReaderGenerator { typeInfo ->
                if (this == null) null
                else {
                    val builder = helper.instanceBuilder()
                    (this as Document).forEach { key: String, value: Any? ->
                        if(key == "_id") {
                            builder.place("id", value) {}
                        } else {
                            builder.place(key, value) {}
                        }
                    }
                    builder.build()
                }
            }
        }
        addWriterGenerator(0f) { type: KClass<*> ->
            val vars = type.reflectiveWriterData(this)

            return@addWriterGenerator { value, typeInfo ->
                if (value == null) null
                else Document().apply {
                    vars.forEach {
                        val subvalue = it.getter(value)
                        if(it.key == "id") {
                            this["_id"] = it.writer.invoke(Unit, subvalue, it.valueType)
                        } else {
                            this[it.key] = it.writer.invoke(Unit, subvalue, it.valueType)
                        }
                    }
                }
            }
        }
    }
}
