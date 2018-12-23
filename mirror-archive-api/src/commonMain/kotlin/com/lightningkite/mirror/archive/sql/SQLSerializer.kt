package com.lightningkite.mirror.archive.sql

import com.lightningkite.lokalize.Date
import com.lightningkite.lokalize.DateTime
import com.lightningkite.lokalize.Time
import com.lightningkite.lokalize.TimeStamp
import com.lightningkite.mirror.archive.model.Id
import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.info.type
import com.lightningkite.mirror.serialization.*
import kotlin.reflect.KClass

class SQLSerializer(
        override val registry: SerializationRegistry
): Encoder<MutableList<String>>, DefinitionRepository<PartialTable> {
    override val arbitraryEncoders: MutableList<Encoder.Generator<MutableList<String>>> = ArrayList()
    override val encoders: MutableMap<Type<*>, TypeEncoder<MutableList<String>, Any?>> = HashMap()
    override val kClassEncoders: MutableMap<KClass<*>, (Type<*>) -> TypeEncoder<MutableList<String>, Any?>?> = HashMap()


    override val arbitraryDefines: MutableList<DefinitionRepository.Generator<PartialTable>> = ArrayList()
    override val definitions: MutableMap<Type<*>, PartialTable> = HashMap()
    override val kClassDefines: MutableMap<KClass<*>, (Type<*>) -> PartialTable?> = HashMap()

    fun escape(string: String): String {
        val escaped = string.replace(Regex("[^a-zA-Z 0-9,.+=-_/<>!@#$%^&*(){}\\[\\]`~]")) {
            "\\x" + it.value.first().toInt().toString(16)
        }
        return "E'$escaped'"
    }

    init {
        addDefinition(Boolean::class.type, PartialTable(listOf(Column("", "BOOLEAN"))))
        addEncoder(Boolean::class.type) { add(it.toString().toUpperCase()) }

        addDefinition(Char::class.type, PartialTable(listOf(Column("", "CHAR(1)"))))
        addEncoder(Char::class.type) { value -> add(escape(value.toString())) }

        addDefinition(String::class.type, PartialTable(listOf(Column("", "TEXT"))))
        addEncoder(String::class.type) { value -> add(escape(value)) }

        addDefinition(Byte::class.type, PartialTable(listOf(Column("", "SMALLINT"))))
        addEncoder(Byte::class.type) { value -> add(value.toString()) }

        addDefinition(Short::class.type, PartialTable(listOf(Column("", "SMALLINT"))))
        addEncoder(Short::class.type) { value -> add(value.toString()) }

        addDefinition(Int::class.type, PartialTable(listOf(Column("", "INT"))))
        addEncoder(Int::class.type) { value -> add(value.toString()) }

        addDefinition(Long::class.type, PartialTable(listOf(Column("", "BIGINT"))))
        addEncoder(Long::class.type) { value -> add(value.toString()) }

        addDefinition(Float::class.type, PartialTable(listOf(Column("", "REAL"))))
        addEncoder(Float::class.type) { value -> add(value.toString()) }

        addDefinition(Double::class.type, PartialTable(listOf(Column("", "DOUBLE PRECISION"))))
        addEncoder(Double::class.type) { value -> add(value.toString()) }

        initializeDefinitions()
        initializeEncoders()

        addDefinition(Id::class.type, PartialTable(listOf(Column("", "UUID"))))
        addEncoder(Id::class.type) { value ->
            add('\'' + value.toUUIDString() + '\'')
        }

        addDefinition(Date::class.type, PartialTable(listOf(Column("", "DATE"))))
        addEncoder(Date::class.type) { value -> add("'" + value.iso8601() + "'") }

        addDefinition(Time::class.type, PartialTable(listOf(Column("", "TIME"))))
        addEncoder(Time::class.type) { value -> add("'" + value.iso8601() + "'") }

        addDefinition(DateTime::class.type, PartialTable(listOf(Column("", "TIMESTAMP"))))
        addEncoder(DateTime::class.type) { value -> add("'" + value.iso8601() + "'") }

        addDefinition(TimeStamp::class.type, PartialTable(listOf(Column("", "TIMESTAMP"))))
        addEncoder(TimeStamp::class.type) { value -> add("'" + value.iso8601() + "'") }

        addEncoder(NullableEncoderGenerator())
    }

    inner class NullableEncoderGenerator : Encoder.Generator<MutableList<String>> {
        override val description: String get() = "null"
        override val priority: Float get() = 1f
        override fun generateEncoder(type: Type<*>): (MutableList<String>.(value: Any?) -> Unit)? {
            if (!type.nullable) return null
            val underlying = rawEncoder(type.copy(nullable = false))
            return { value ->
                if (value == null) {
                    add("NULL")
                } else {
                    underlying.invoke(this, value)
                }
            }
        }
    }
}

data class PartialTable(
        var columns: List<Column> = listOf(),
        var tables: List<Table> = listOf()
)

//data class SQLTypeStorage<T>(
//        var type: Type<T>,
//        var columns: List<Column>,
//        var tables: List<Table>,
//
//        var read: TypeDecoder<RowReader, T>,
//        var query: (QueryBuilder)->Unit,
//        var write: TypeEncoder<Appendable, T>
//) {
//    //When querying, join the tables by primary key, add the columns to SELECT
//    //When creating, add columns, constraints, indexes, and tables
//
//    fun asTable(schemaName: String, tableName: String): Table {
//        return Table(
//                schemaName = schemaName,
//                name = tableName,
//                columns = columns
//        )
//    }
//}