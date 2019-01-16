package com.lightningkite.mirror.archive.sql

import com.lightningkite.lokalize.Date
import com.lightningkite.lokalize.DateTime
import com.lightningkite.lokalize.Time
import com.lightningkite.lokalize.TimeStamp
import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.info.canBeInstantiated
import com.lightningkite.mirror.info.type
import com.lightningkite.mirror.info.untyped
import com.lightningkite.mirror.serialization.*
import com.lightningkite.mirror.serialization.json.JsonSerializer
import kotlin.reflect.KClass

/**
 * LIMITATIONS
 * Comparisons can only be done with single-column types, and compare however the database does it.
 */
@Suppress("LeakingThis")
open class SQLSerializer(
        override val registry: SerializationRegistry,
        val backupSerializer: StringSerializer = JsonSerializer(registry)
) : Encoder<SQLQuery.Builder>, DefinitionRepository<PartialTable>, Decoder<SQLSerializer.RowReader> {

    fun SQLQuery.Builder.encodeNonPoly(value: Any?) {
        if (value == null) {
            sql.append("NULL")
        } else {
            rawEncoder(value::class.type).invoke(this, value)
        }
    }

    class RowReader(var row: List<Any?>) {
        var index = 0
        fun next(): Any? {
            val result = row[index]
            index++
            return result
        }

        fun skip() = index++
        fun get(): Any? = row[index]
    }

    override val arbitraryEncoders: MutableList<Encoder.Generator<SQLQuery.Builder>> = ArrayList()
    override val encoders: MutableMap<Type<*>, TypeEncoder<SQLQuery.Builder, Any?>> = HashMap()
    override val kClassEncoders: MutableMap<KClass<*>, (Type<*>) -> TypeEncoder<SQLQuery.Builder, Any?>?> = HashMap()

    override val arbitraryDecoders: MutableList<Decoder.Generator<RowReader>> = ArrayList()
    override val decoders: MutableMap<Type<*>, TypeDecoder<RowReader, Any?>> = HashMap()
    override val kClassDecoders: MutableMap<KClass<*>, (Type<*>) -> TypeDecoder<RowReader, Any?>?> = HashMap()

    override val arbitraryDefines: MutableList<DefinitionRepository.Generator<PartialTable>> = ArrayList()
    override val definitions: MutableMap<Type<*>, PartialTable> = HashMap()
    override val kClassDefines: MutableMap<KClass<*>, (Type<*>) -> PartialTable?> = HashMap()

    fun subEscape(string: String): String {
        return string.replace(Regex("[^a-zA-Z 0-9,.+=\\-/<>!@#$^&*(){}\\[\\]`~]")) {
            "\\x" + it.value.first().toInt().toString(16)
        }
    }

    fun escape(string: String): String {
        return "E'${subEscape(string)}'"
    }

    inline fun <reified T> addDecoderDirect(type: Type<T>) {
        addDecoder(type) { next() as T }
    }

    inline fun <reified T> addEncoderDirect(type: Type<T>) {
        addEncoder(type) { addValue(it) }
    }

    init {
        addDefinition(Boolean::class.type, PartialTable(listOf(Column("", "SMALLINT"))))
        addEncoder(Boolean::class.type) {
            addValue(if(it) 1 else 0)
        }
        addDecoder(Boolean::class.type) {
            next() as? Number != 0
        }

        addDefinition(Char::class.type, PartialTable(listOf(Column("", "CHAR(1)"))))
        addEncoderDirect(Char::class.type)
        addDecoder(Char::class.type) {
            val it = next()
            when (it) {
                is Char -> it
                is String -> it.first()
                else -> throw UnsupportedOperationException()
            }
        }

        addDefinition(String::class.type, PartialTable(listOf(Column("", "VARCHAR", 1023))))
        addEncoderDirect(String::class.type)
        addDecoderDirect(String::class.type)

        addDefinition(Byte::class.type, PartialTable(listOf(Column("", "SMALLINT"))))
        addEncoderDirect(Byte::class.type)
        addDecoder(Byte::class.type) { (next() as Number).toByte() }

        addDefinition(Short::class.type, PartialTable(listOf(Column("", "SMALLINT"))))
        addEncoderDirect(Short::class.type)
        addDecoder(Short::class.type) { (next() as Number).toShort() }

        addDefinition(Int::class.type, PartialTable(listOf(Column("", "INTEGER"))))
        addEncoderDirect(Int::class.type)
        addDecoder(Int::class.type) { (next() as Number).toInt() }

        addDefinition(Long::class.type, PartialTable(listOf(Column("", "BIGINT"))))
        addEncoderDirect(Long::class.type)
        addDecoder(Long::class.type) { (next() as Number).toLong() }

        addDefinition(Float::class.type, PartialTable(listOf(Column("", "REAL"))))
        addEncoderDirect(Float::class.type)
        addDecoder(Float::class.type) { (next() as Number).toFloat() }

        addDefinition(Double::class.type, PartialTable(listOf(Column("", "DOUBLE PRECISION"))))
        addEncoderDirect(Double::class.type)
        addDecoder(Double::class.type) { (next() as Number).toDouble() }

        addDefinition(Date::class.type, PartialTable(listOf(Column("", "DATE"))))
        addEncoderDirect(Date::class.type)
        addDecoderDirect(Date::class.type)

        addDefinition(Time::class.type, PartialTable(listOf(Column("", "TIME"))))
        addEncoderDirect(Time::class.type)
        addDecoderDirect(Time::class.type)

        addDefinition(DateTime::class.type, PartialTable(listOf(Column("", "TIMESTAMP"))))
        addEncoderDirect(DateTime::class.type)
        addDecoderDirect(DateTime::class.type)

        addDefinition(TimeStamp::class.type, PartialTable(listOf(Column("", "TIMESTAMP"))))
        addEncoderDirect(TimeStamp::class.type)
        addDecoderDirect(TimeStamp::class.type)

        addDefinition(ByteArray::class.type, PartialTable(listOf(Column("", "BLOB"))))
        addEncoderDirect(ByteArray::class.type)
        addDecoderDirect(ByteArray::class.type)

        initializeDefinitions()
        initializeEncoders()

        val nullGen = NullableGenerator()
        addEncoder(nullGen)
        addDecoder(nullGen)
        addDefinition(nullGen)

        val refGen = ReflectiveGenerator()
        addEncoder(refGen)
        addDecoder(refGen)
        addDefinition(refGen)

        val polyGen = PolyGenerator()
        addEncoder(polyGen)
        addDecoder(polyGen)
        addDefinition(polyGen)
    }

    inner class NullableGenerator : DefinitionRepository.Generator<PartialTable>, Encoder.Generator<SQLQuery.Builder>, Decoder.Generator<RowReader> {

        override val description: String get() = "null"
        override val priority: Float get() = 1f

        override fun generateDefine(type: Type<*>): PartialTable? {
            if (!type.nullable) return null
            val nnType = type.copy(nullable = false)
            val nnDef = definition(nnType)
            if(nnDef.columns.size == 1) {
                return nnDef
            } else {
                //Strip non-nulls?
                return PartialTable(
                        columns = definition(Boolean::class.type).columns
                                .map { it.copy(name = "_isNull" nameAppend it.name) } + nnDef.columns,
                        constraints = nnDef.constraints,
                        indexes = nnDef.indexes
                )
            }
        }

        override fun generateEncoder(type: Type<*>): (SQLQuery.Builder.(value: Any?) -> Unit)? {
            if (!type.nullable) return null
            val nnType = type.copy(nullable = false)
            val underlying = rawEncoder(type.copy(nullable = false))
            val numCols = definition(nnType).columns.size
            if(numCols == 1) {
                return { value ->
                    if (value == null) {
                        sql.append("NULL")
                    } else {
                        underlying.invoke(this, value)
                    }
                }
            } else {
                val boolEncoder = encoder(Boolean::class.type)
                return { value ->
                    if (value == null) {
                        boolEncoder.invoke(this, false)
                        repeat(numCols){
                            sql.append(", NULL")
                        }
                    } else {
                        boolEncoder.invoke(this, true)
                        sql.append(", ")
                        underlying.invoke(this, value)
                    }
                }
            }
        }

        override fun generateDecoder(type: Type<*>): (RowReader.() -> Any?)? {
            if (!type.nullable) return null
            val nnType = type.copy(nullable = false)
            val numCols = definition(nnType).columns.size
            val underlying = rawDecoder(nnType)
            if(numCols == 1) {
                return {
                    if (this.get() == null) {
                        skip()
                        null
                    } else underlying.invoke(this)
                }
            } else {
                val boolDecoder = decoder(Boolean::class.type)
                return {
                    val isNull = boolDecoder.invoke(this)
                    if(isNull){
                        repeat(numCols) {
                            skip()
                        }
                    } else underlying.invoke(this)
                }
            }
        }
    }

    inner class PolyGenerator : DefinitionRepository.Generator<PartialTable>, Encoder.Generator<SQLQuery.Builder>, Decoder.Generator<RowReader> {

        override val description: String get() = "poly"
        override val priority: Float get() = .9f

        override fun generateDefine(type: Type<*>): PartialTable? {
            val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
            if (classInfo.canBeInstantiated) return null
            return PartialTable(columns = listOf(Column("", "TEXT")))
        }

        override fun generateEncoder(type: Type<*>): (SQLQuery.Builder.(value: Any?) -> Unit)? {
            val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
            if (classInfo.canBeInstantiated) return null
            val stringCoder = encoder(String::class.type)
            return { value ->
                stringCoder(this, backupSerializer.write(value!!, Any::class.type))
            }
        }

        override fun generateDecoder(type: Type<*>): (RowReader.() -> Any?)? {
            val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
            if (classInfo.canBeInstantiated) return null
            return {
                backupSerializer.read(next() as String, Any::class.type)
            }
        }
    }

    inner class ReflectiveGenerator : DefinitionRepository.Generator<PartialTable>, Encoder.Generator<SQLQuery.Builder>, Decoder.Generator<RowReader> {

        override val description: String get() = "reflective"
        override val priority: Float get() = 0f

        override fun generateDefine(type: Type<*>): PartialTable? {
            val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
            val partials = classInfo.fields.map { it to definition(it.type) }
            return PartialTable(
                    columns = partials.flatMap { (field, it) ->
                        it.columns.asSequence().map {
                            val newName = it.name nameAppend field.name
                            it.copy(name = newName)
                        }.asIterable()
                    }
            )
        }

        override fun generateEncoder(type: Type<*>): (SQLQuery.Builder.(value: Any?) -> Unit)? {
            if (type.nullable) return null
            val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
            val lazySubCoders by lazy { classInfo.fields.map { it to rawEncoder(it.type as Type<*>) } }
            return {
                lazySubCoders.forEachIndexed { index, (field, coder) ->
                    coder.invoke(this, field.get.untyped(it!!))
                    if (index != lazySubCoders.lastIndex) {
                        sql.append(", ")
                    }
                }
            }
        }

        override fun generateDecoder(type: Type<*>): (RowReader.() -> Any?)? {
            if (type.nullable) return null
            val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
            val lazySubCoders by lazy { classInfo.fields.map { it.name to rawDecoder(it.type as Type<*>) } }
            return {
                val map = HashMap<String, Any?>()
                for ((field, coder) in lazySubCoders) {
                    map[field] = coder.invoke(this)
                }
                classInfo.construct(map)
            }
        }
    }
}

