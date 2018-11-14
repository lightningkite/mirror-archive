package com.lightningkite.kotlinx.db.mongo

import com.lightningkite.kotlinx.locale.*
import com.lightningkite.kotlinx.reflection.*
import com.lightningkite.kotlinx.serialization.*
import com.lightningkite.kotlinx.serialization.json.JsonSerializer
import com.lightningkite.kotlinx.serialization.json.RawJsonReader
import com.lightningkite.kotlinx.serialization.json.RawJsonWriter
import org.bson.*
import kotlin.reflect.KClass

@Suppress("LeakingThis")
open class BsonValueSerializer :
        StandardReaderRepository<BsonValue>,
        StandardWriterRepository<Unit, BsonValue> {

    companion object : BsonValueSerializer()

    override val readerGenerators: MutableList<Pair<Float, TypeReaderGenerator<BsonValue>>> = ArrayList()
    override val readers: MutableMap<KClass<*>, TypeReader<BsonValue>> = HashMap()
    override val writerGenerators: MutableList<Pair<Float, TypeWriterGenerator<Unit, BsonValue>>> = ArrayList()
    override val writers: MutableMap<KClass<*>, TypeWriter<Unit, BsonValue>> = HashMap()

    inline fun <T : Any> setNullableReader(typeKClass: KClass<T>, crossinline read: BsonValue.(KxType) -> T) {
        setReader(typeKClass) {
            if (this is BsonNull) null
            else read(this, it)
        }
    }

    inline fun <T : Any> setNullableWriter(typeKClass: KClass<T>, crossinline write: Unit.(T, KxType) -> BsonValue) {
        setWriter(typeKClass) { it, type ->
            if (it == null) BsonNull.VALUE
            else write(it, type)
        }
    }

    var boxWriter: Unit.(typeInfo: KxType, Any?) -> BsonValue = { typeInfo, value ->
        if (value == null) BsonNull.VALUE
        else BsonArray(listOf(
                BsonString(CommonSerialization.ExternalNames[value::class.kxReflect]),
                writer(value::class).invoke(this, value, typeInfo)
        ))
    }
    var boxReader: BsonValue.(typeInfo: KxType) -> Any? = {
        if (this is BsonNull) null
        else {
            val type = CommonSerialization.ExternalNames[this.asArray()[0].asString().value]!!
            reader(type.kclass).invoke(this.asArray()[1], type.kxType)
        }
    }


    init {
        setReader(Unit::class) { Unit }
        setWriter(Unit::class) { it, _ -> BsonNull.VALUE }

        setNullableReader(Int::class) { asInt32().value }
        setNullableWriter(Int::class) { it, _ -> BsonInt32(it) }

        setNullableReader(Short::class) { asInt32().value.toShort() }
        setNullableWriter(Short::class) { it, _ -> BsonInt32(it.toInt()) }

        setNullableReader(Byte::class) { asInt32().value.toByte() }
        setNullableWriter(Byte::class) { it, _ -> BsonInt32(it.toInt()) }

        setNullableReader(Long::class) { asInt64().value }
        setNullableWriter(Long::class) { it, _ -> BsonInt64(it) }

        setNullableReader(Float::class) { asDouble().value.toFloat() }
        setNullableWriter(Float::class) { it, _ -> BsonDouble(it.toDouble()) }

        setNullableReader(Double::class) { asDouble().value }
        setNullableWriter(Double::class) { it, _ -> BsonDouble(it) }

        setNullableReader(Number::class) { asDouble().value }
        setNullableWriter(Number::class) { it, _ -> BsonDouble(it.toDouble()) }

        setNullableReader(Boolean::class) { asBoolean().value }
        setNullableWriter(Boolean::class) { it, _ -> BsonBoolean.valueOf(it) }

        setNullableReader(String::class) { asString().value }
        setNullableWriter(String::class) { it, _ -> BsonString(it) }

        setNullableReader(Date::class) { Date.iso8601(asString().value) }
        setNullableWriter(Date::class) { it, _ -> BsonString(it.iso8601()) }

        setNullableReader(Time::class) { Time.iso8601(asString().value) }
        setNullableWriter(Time::class) { it, _ -> BsonString(it.iso8601()) }

        setNullableReader(DateTime::class) { DateTime.iso8601(asString().value) }
        setNullableWriter(DateTime::class) { it, _ -> BsonString(it.iso8601()) }

        setNullableReader(TimeStamp::class) { TimeStamp.iso8601(asString().value) }
        setNullableWriter(TimeStamp::class) { it, _ -> BsonString(it.iso8601()) }

        setNullableReader(List::class) { typeInfo ->
            val valueSubtype = typeInfo.typeParameters.getOrNull(0)?.takeUnless { it.isStar }?.type
                    ?: KxType(AnyReflection, true)
            val output = ArrayList<Any?>()
            val valueSubtypeReader = reader(valueSubtype.base.kclass)
            for (item in this.asArray().values) {
                valueSubtypeReader.invoke(item, valueSubtype)
            }
            output
        }
        setNullableWriter(List::class) { value, typeInfo ->
            val valueSubtype = typeInfo.typeParameters.getOrNull(0)?.takeUnless { it.isStar }?.type
                    ?: KxType(AnyReflection, true)
            val valueSubtypeWriter = writer(valueSubtype.base.kclass)
            BsonArray(value.map { valueSubtypeWriter.invoke(Unit, it, valueSubtype) })
        }

        setNullableReader(Map::class) { typeInfo ->
            val keySubtype = typeInfo.typeParameters.getOrNull(0)?.takeUnless { it.isStar }?.type
                    ?: KxType(AnyReflection, true)
            val valueSubtype = typeInfo.typeParameters.getOrNull(1)?.takeUnless { it.isStar }?.type
                    ?: KxType(AnyReflection, true)
            val valueSubtypeReader = reader(valueSubtype.base.kclass)

            val map = LinkedHashMap<Any?, Any?>()
            if (keySubtype.base == StringReflection) {
                this.asDocument().forEach { key, subvalue ->
                    map[key] = valueSubtypeReader.invoke(subvalue, valueSubtype)
                }
            } else {
                val keySubReader = JsonSerializer.reader(keySubtype.base.kclass)
                this.asDocument().forEach { rawKey, subvalue ->
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
                BsonDocument().apply {
                    for ((key, subvalue) in value) {
                        this[key as String] = valueSubtypeWriter.invoke(Unit, subvalue, valueSubtype)
                    }
                }
            } else {
                val keySubWriter = JsonSerializer.writer(keySubtype.base.kclass)
                BsonDocument().apply {
                    for ((key, subvalue) in value) {
                        val stringifiedKey = StringBuilder().also {
                            keySubWriter.invoke(RawJsonWriter(it), key, keySubtype)
                        }.toString()
                        this[stringifiedKey] = valueSubtypeWriter.invoke(Unit, subvalue, valueSubtype)
                    }
                }
            }
        }

        val polyboxWriter: TypeWriter<Unit, BsonValue> = { value, t -> boxWriter.invoke(this, t, value) }
        val polyboxReader: TypeReader<BsonValue> = {
            @Suppress("UNCHECKED_CAST")
            boxReader.invoke(this, it)
        }

        setReader(Any::class, polyboxReader)
        setWriter(Any::class, polyboxWriter)

        addReaderGenerator(1f, EnumGenerators.readerGenerator<BsonValue>(this))
        addWriterGenerator(1f, EnumGenerators.writerGenerator<Unit, BsonValue>(this))

        //Any non-final polyboxing
        addReaderGenerator(.5f, { type: KClass<*> ->
            if (type.serializePolymorphic) {
                polyboxReader
            } else null
        } as TypeReaderGenerator<BsonValue>)
        addWriterGenerator(.5f, { type: KClass<*> ->
            if (type.serializePolymorphic) {
                polyboxWriter
            } else null
        } as TypeWriterGenerator<Unit, BsonValue>)

        addReaderGenerator(0f) { type: KClass<*> ->
            val helper = ReflectiveReaderHelper.tryInit(type, this)
                    ?: return@addReaderGenerator null
            return@addReaderGenerator { typeInfo ->
                if (this is BsonNull) null
                else {
                    val builder = helper.instanceBuilder()
                    this.asDocument().forEach { key, bsonValue ->
                        if (key == "_id") {
                            builder.placeManual("id", bsonValue.asObjectId().value.toHexString())
                        } else {
                            builder.place(key, bsonValue) { }
                        }
                    }
                    builder.build()
                }
            }
        }
        addWriterGenerator(0f) { type: KClass<*> ->
            val vars = type.reflectiveWriterData(this)

            return@addWriterGenerator { value, typeInfo ->
                if (value == null) BsonNull.VALUE
                else BsonDocument().apply {
                    vars.forEach {
                        val subvalue = it.getter(value)
                        if (it.key != "id") {
                            this[it.key] = it.writer.invoke(Unit, subvalue, it.valueType)
                        }
                    }
                }
            }
        }
    }
}
