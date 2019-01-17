package com.lightningkite.mirror.archive.sql

import com.lightningkite.lokalize.time.Date
import com.lightningkite.lokalize.time.DateTime
import com.lightningkite.lokalize.time.Time
import com.lightningkite.lokalize.time.TimeStamp
import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Id
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.info.*
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
) : Encoder<MutableList<String>>, DefinitionRepository<PartialTable>, Decoder<SQLSerializer.RowReader> {

    open fun constraint(constraint: Constraint): String = constraint.run {
        val type = type
        when (type) {
            Constraint.Type.PrimaryKey -> "$name PRIMARY KEY (${columns.joinToString()})"
            Constraint.Type.Unique -> "$name UNIQUE (${columns.joinToString()})"
            is Constraint.Type.ForeignKey -> "$name FOREIGN KEY (${columns.joinToString()}) REFERENCES ${type.otherSchema}.${type.otherTable} (${type.otherColumns.joinToString()}) ON DELETE SET NULL"
        }
    }

    open fun column(column: Column): String = column.run {
        if (size == null) "$name $type" else "$name $type($size)"
    }

    open fun createIndex(table: Table, index: Index): SQLQuery = SQLQuery((if (index.unique) {
        "CREATE UNIQUE INDEX IF NOT EXISTS "
    } else {
        "CREATE INDEX IF NOT EXISTS "
    }) + "${index.name} ON ${table.fullName} USING ${index.usingMethod} (${index.columns.joinToString()})")

    open fun dropIndex(index: Index): SQLQuery = SQLQuery("DROP INDEX ${index.name}")

    open fun addConstraint(table: Table, constraint: Constraint): SQLQuery {
        return SQLQuery("ALTER TABLE ${table.fullName} ADD CONSTRAINT ${constraint(constraint)}")
    }

    open fun removeConstraint(table: Table, constraint: Constraint): SQLQuery {
        return SQLQuery("ALTER TABLE ${table.fullName} DROP CONSTRAINT ${constraint.name}")
    }

    open fun addColumn(table: Table, column: Column): SQLQuery {
        return SQLQuery("ALTER TABLE ${table.fullName} ADD COLUMN ${column(column)}")
    }

    open fun removeColumn(table: Table, column: Column): SQLQuery {
        return SQLQuery("ALTER TABLE ${table.fullName} DROP COLUMN ${column.name}")
    }

    open fun createTable(table: Table): List<SQLQuery> = table.run {
        return listOf(
                SQLQuery("CREATE SCHEMA IF NOT EXISTS $schemaName "),
                SQLQuery("CREATE TABLE IF NOT EXISTS $schemaName.$name (${(columns + constraints).joinToString {
                    when (it) {
                        is Column -> column(it)
                        is Constraint -> "CONSTRAINT " + constraint(it)
                        else -> throw IllegalArgumentException()
                    }
                }})")
        ) + indexes.map { createIndex(table, it) }
    }

    open fun dropTable(table: Table): SQLQuery = SQLQuery("DROP TABLE ${table.fullName}")

    open fun convertToSQLCondition(condition: Condition<*>, baseType: Type<*>, fields: List<FieldInfo<*, *>> = listOf()): String {
        val fieldName = fields.joinToString("_") { it.name }
        val type = (fields.lastOrNull()?.type ?: baseType) as Type<Any?>
        val subFields = rawDefine(type)
        return when (condition) {
            is Condition.Never -> "FALSE"
            is Condition.Always -> "TRUE"
            is Condition.And -> condition.conditions.joinToString(" AND ", "(", ")") { convertToSQLCondition(it, baseType, fields) }
            is Condition.Or -> condition.conditions.joinToString(" OR ", "(", ")") { convertToSQLCondition(it, baseType, fields) }
            is Condition.Not -> "(NOT ${convertToSQLCondition(condition.condition, baseType, fields)})"
            is Condition.Field<*, *> -> convertToSQLCondition(condition.condition, baseType, fields = fields + condition.field)
            is Condition.Equal -> {
                val values = ArrayList<String>().also {
                    encoder(type).invoke(it, condition.value)
                }
                if (subFields.columns.size == 1) {
                    val column = fieldName nameAppend subFields.columns.first().name
                    "$column = " + values.first()
                } else {
                    subFields.columns.asSequence().zip(values.asSequence()).joinToString {
                        val column = fieldName nameAppend it.first.name
                        "$column = " + it.second
                    }
                }
            }
            is Condition.EqualToOne -> {
                if (subFields.columns.size == 1) {
                    val column = fieldName nameAppend subFields.columns.first().name
                    "$column IN (" + condition.values.asSequence().map { encodeSingle(it, type) }.joinToString() + ")"
                } else {
                    condition.values.joinToString(" OR ", "(", ")") { value ->
                        val values = ArrayList<String>().also { list ->
                            encoder(type).invoke(list, value)
                        }
                        subFields.columns.asSequence().zip(values.asSequence()).joinToString {
                            val column = fieldName nameAppend it.first.name
                            "$column = " + it.second
                        }
                    }
                }
            }
            is Condition.NotEqual -> {
                val values = ArrayList<String>().also {
                    encoder(type).invoke(it, condition.value)
                }
                if (subFields.columns.size == 1) {
                    val column = fieldName nameAppend subFields.columns.first().name
                    "$column <> " + values.first()
                } else {
                    subFields.columns.asSequence().zip(values.asSequence()).joinToString {
                        val column = fieldName nameAppend it.first.name
                        "$column <> " + it.second
                    }
                }
            }
            is Condition.LessThan -> {
                if (subFields.columns.size == 1) {
                    val column = fieldName nameAppend subFields.columns.first().name
                    "$column < " + encodeSingle(condition.value, type)
                } else throw UnsupportedOperationException()
            }
            is Condition.GreaterThan -> {
                if (subFields.columns.size == 1) {
                    val column = fieldName nameAppend subFields.columns.first().name
                    "$column > " + encodeSingle(condition.value, type)
                } else throw UnsupportedOperationException()
            }
            is Condition.LessThanOrEqual -> {
                if (subFields.columns.size == 1) {
                    val column = fieldName nameAppend subFields.columns.first().name
                    "$column <= " + encodeSingle(condition.value, type)
                } else throw UnsupportedOperationException()
            }
            is Condition.GreaterThanOrEqual -> {
                if (subFields.columns.size == 1) {
                    val column = fieldName nameAppend subFields.columns.first().name
                    "$column >= " + encodeSingle(condition.value, type)
                } else throw UnsupportedOperationException()
            }
            is Condition.TextSearch -> {
                if (subFields.columns.size == 1) {
                    val column = fieldName nameAppend subFields.columns.first().name
                    "$column LIKE " + "E'%" + subEscape(condition.query) + "%'"
                } else throw UnsupportedOperationException()
            }
            is Condition.RegexTextSearch -> throw UnsupportedOperationException()
        }
    }

    open fun convertToSQLSet(operation: Operation<*>, baseType: Type<*>, fields: List<FieldInfo<*, *>> = listOf()): List<String> {
        val fieldName = fields.joinToString("_") { it.name }
        val type = (fields.lastOrNull()?.type ?: baseType) as Type<Any?>
        val subFields = rawDefine(type)
        return when (operation) {
            is Operation.Set -> {
                if (subFields.columns.size == 1) {
                    val column = fieldName nameAppend subFields.columns.first().name
                    listOf("$column = " + encodeSingle(operation.value, type))
                } else {
                    val values = ArrayList<String>().also {
                        encoder(type).invoke(it, operation.value)
                    }
                    subFields.columns.asSequence().zip(values.asSequence()).map {
                        val column = fieldName nameAppend it.first.name
                        "$column = " + it.second
                    }.toList()
                }
            }
            is Operation.AddNumeric -> {
                val column = fieldName nameAppend subFields.columns.first().name
                listOf("$column = $column + " + encodeSingle(operation.amount, type))
            }
            is Operation.Append -> {
                val column = fieldName nameAppend subFields.columns.first().name
                listOf("$column = $column || " + encodeSingle(operation.string, type))
            }
            is Operation.Fields -> {
                operation.changes.entries.flatMap { convertToSQLSet(it.value, baseType, fields + it.key) }
            }
            else -> throw UnsupportedOperationException()
        }
    }

    open fun select(
            table: Table,
            columns: List<Column> = table.columns,
            where: String? = null,
            orderBy: List<Column> = listOf(),
            limit: Int = 100
    ): SQLQuery = SQLQuery(buildString {
        append("SELECT ")
        append(columns.joinToString { it.name })
        append(" FROM ")
        append(table.fullName)
        if (where != null) {
            append(" WHERE ")
            append(where)
        }
        if (orderBy.isNotEmpty()) {
            append(" ORDER BY ")
            append(orderBy.joinToString { it.name })
        }
        append(" LIMIT ")
        append(limit.toString())
    })

    open fun insert(
            table: Table,
            values: List<String>
    ): SQLQuery = SQLQuery(buildString {
        append("INSERT INTO ")
        append(table.fullName)
        append(" ")
        append(table.columns.joinToString(", ", "(", ")") { it.name })
        append(" VALUES ")
        append(values.joinToString(", ", "(", ")"))
    })

    open fun update(
            table: Table,
            columns: List<Column> = table.columns,
            values: List<String>,
            condition: String? = null
    ): SQLQuery = SQLQuery(buildString {
        append("UPDATE ")
        append(table.fullName)
        append(" SET ")
        append(columns.zip(values).joinToString(", ") { it.first.name + " = " + it.second })
        if (condition != null) {
            append(" WHERE ")
            append(condition)
        }
    })

    open fun updateModify(
            table: Table,
            resultColumns: List<Column> = table.columns,
            modifications: List<String>,
            condition: String? = null
    ): SQLQuery = SQLQuery(buildString {
        append("UPDATE ")
        append(table.fullName)
        append(" SET ")
        append(modifications.joinToString())
        if (condition != null) {
            append(" WHERE ")
            append(condition)
        }
    })

    open fun updateModifyReturning(
            table: Table,
            resultColumns: List<Column> = table.columns,
            modifications: List<String>,
            condition: String? = null
    ): SQLQuery? = null

    open fun delete(
            table: Table,
            condition: String?
    ): SQLQuery = SQLQuery(buildString {
        append("DELETE FROM ")
        append(table.fullName)
        if (condition != null) {
            append(" WHERE ")
            append(condition)
        }
    })

    open fun upsert(
            table: Table,
            values: List<String>,
            condition: String? = null
    ): SQLQuery? = null

    open fun migrate(
            old: Table,
            table: Table,
            dropColumns: Boolean = false,
            dropIndicies: Boolean = false
    ): List<SQLQuery> {

        val statements = ArrayList<SQLQuery>()

        run {
            val myColumns = table.columns.associate { it.name to it }
            val oldColumns = old.columns.associate { it.name to it }

            val newColumnsKeys = myColumns.keys - oldColumns.keys
            val deadColumnsKeys = oldColumns.keys - myColumns.keys
            val sameColumnsKeys = myColumns.keys intersect oldColumns.keys

            for (key in newColumnsKeys) {
                statements.add(addColumn(table, myColumns[key]!!))
            }
            for (key in deadColumnsKeys) {
                if (dropColumns) {
                    statements.add(removeColumn(table, oldColumns[key]!!))
                }
            }
            for (key in sameColumnsKeys) {
                val mine = myColumns[key]!!
                val other = oldColumns[key]!!
                if (mine != other) {
                    throw IllegalStateException("Column $key is trying to change types; this is not currently supported.\n$other\nmigrate to\n$mine")
                }
            }
        }

        run {
            val myConstraints = table.constraints.associate { it.name to it }
            val oldConstraints = old.constraints.associate { it.name to it }

            val newConstraintKeys = myConstraints.keys - oldConstraints.keys
            val deadConstraintKeys = oldConstraints.keys - myConstraints.keys
            val sameConstraintKeys = myConstraints.keys intersect oldConstraints.keys

            for (key in newConstraintKeys) {
                statements.add(addConstraint(table, myConstraints[key]!!))
            }
            for (key in deadConstraintKeys) {
                if (dropColumns) {
                    statements.add(removeConstraint(table, oldConstraints[key]!!))
                }
            }
            for (key in sameConstraintKeys) {
                val mine = myConstraints[key]!!
                val other = oldConstraints[key]!!
                if (mine != other) {
                    if (dropColumns) {
                        statements.add(removeConstraint(table, other))
                        statements.add(addConstraint(table, mine))
                    } else {
                        throw IllegalStateException("Constraint $key is trying to change a constraint; this is not currently supported.\n$other\nmigrate to\n$mine")
                    }
                }
            }
        }

        run {
            val myIndexes = table.indexes.associate { it.name to it }
            val oldIndexes = old.indexes.associate { it.name to it }

            val newIndexKeys = myIndexes.keys - oldIndexes.keys
            val deadIndexKeys = oldIndexes.keys - myIndexes.keys
            val sameIndexKeys = myIndexes.keys intersect oldIndexes.keys

            for (key in newIndexKeys) {
                statements.add(createIndex(table, myIndexes[key]!!))
            }
            for (key in deadIndexKeys) {
                if (!old.isBuiltInIndex(oldIndexes[key]!!) && dropIndicies) {
                    statements.add(dropIndex(myIndexes[key]!!))
                }
            }
            for (key in sameIndexKeys) {
                val mine = myIndexes[key]!!
                val other = oldIndexes[key]!!
                if (mine != other) {
                    throw IllegalStateException("Index $key is trying to change an index; this is not currently supported.\n$other\nmigrate to\n$mine")
                }
            }
        }

        return statements
    }

    fun <T> encodeSingle(value: T, type: Type<T>): String {
        val list = ArrayList<String>()
        if (value == null) {
            return "NULL"
        } else {
            rawEncoder(type).invoke(list, value)
        }
        return list.first()
    }

    fun encodeSingle(value: Any?): String {
        val list = ArrayList<String>()
        if (value == null) {
            return "NULL"
        } else {
            rawEncoder(value::class.type).invoke(list, value)
        }
        return list.first()
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

    override val arbitraryEncoders: MutableList<Encoder.Generator<MutableList<String>>> = ArrayList()
    override val encoders: MutableMap<Type<*>, TypeEncoder<MutableList<String>, Any?>> = HashMap()
    override val kClassEncoders: MutableMap<KClass<*>, (Type<*>) -> TypeEncoder<MutableList<String>, Any?>?> = HashMap()

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

    init {
        addDefinition(Boolean::class.type, PartialTable(listOf(Column("", "SMALLINT"))))
        addEncoder(Boolean::class.type) { add(if (it) "1" else "0") }
        addDecoder(Boolean::class.type) {
            val it = next()
            when (it) {
                is Boolean -> it
                is Number -> it != 0
                else -> throw UnsupportedOperationException()
            }
        }

        addDefinition(Char::class.type, PartialTable(listOf(Column("", "CHAR(1)"))))
        addEncoder(Char::class.type) { value -> add(escape(value.toString())) }
        addDecoder(Char::class.type) {
            val it = next()
            when (it) {
                is Char -> it
                is String -> it.first()
                else -> throw UnsupportedOperationException()
            }
        }

        addDefinition(String::class.type, PartialTable(listOf(Column("", "VARCHAR", 1023))))
        addEncoder(String::class.type) { value -> add(escape(value)) }
        addDecoderDirect(String::class.type)

        addDefinition(Byte::class.type, PartialTable(listOf(Column("", "SMALLINT"))))
        addEncoder(Byte::class.type) { value -> add(value.toString()) }
        addDecoder(Byte::class.type) { (next() as Number).toByte() }

        addDefinition(Short::class.type, PartialTable(listOf(Column("", "SMALLINT"))))
        addEncoder(Short::class.type) { value -> add(value.toString()) }
        addDecoder(Short::class.type) { (next() as Number).toShort() }

        addDefinition(Int::class.type, PartialTable(listOf(Column("", "INTEGER"))))
        addEncoder(Int::class.type) { value -> add(value.toString()) }
        addDecoder(Int::class.type) { (next() as Number).toInt() }

        addDefinition(Long::class.type, PartialTable(listOf(Column("", "BIGINT"))))
        addEncoder(Long::class.type) { value -> add(value.toString()) }
        addDecoder(Long::class.type) { (next() as Number).toLong() }

        addDefinition(Float::class.type, PartialTable(listOf(Column("", "REAL"))))
        addEncoder(Float::class.type) { value -> add(value.toString()) }
        addDecoder(Float::class.type) { (next() as Number).toFloat() }

        addDefinition(Double::class.type, PartialTable(listOf(Column("", "DOUBLE PRECISION"))))
        addEncoder(Double::class.type) { value -> add(value.toString()) }
        addDecoder(Double::class.type) { (next() as Number).toDouble() }

        addDefinition(Date::class.type, PartialTable(listOf(Column("", "DATE"))))
        addEncoder(Date::class.type) { value -> add("'" + value.iso8601() + "'") }
        addDecoderDirect(Date::class.type)

        addDefinition(Time::class.type, PartialTable(listOf(Column("", "TIME"))))
        addEncoder(Time::class.type) { value -> add("'" + value.iso8601() + "'") }
        addDecoderDirect(Time::class.type)

        addDefinition(DateTime::class.type, PartialTable(listOf(Column("", "TIMESTAMP"))))
        addEncoder(DateTime::class.type) { value -> add("'" + value.iso8601() + "'") }
        addDecoderDirect(DateTime::class.type)

        addDefinition(TimeStamp::class.type, PartialTable(listOf(Column("", "TIMESTAMP"))))
        addEncoder(TimeStamp::class.type) { value -> add("'" + value.iso8601() + "'") }
        addDecoderDirect(TimeStamp::class.type)

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

    inner class NullableGenerator : DefinitionRepository.Generator<PartialTable>, Encoder.Generator<MutableList<String>>, Decoder.Generator<RowReader> {

        override val description: String get() = "null"
        override val priority: Float get() = 1f

        override fun generateDefine(type: Type<*>): PartialTable? {
            if (!type.nullable) return null
            return definition(type.copy(nullable = false))
        }

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

        override fun generateDecoder(type: Type<*>): (RowReader.() -> Any?)? {
            if (!type.nullable) return null
            val nnType = type.copy(nullable = false)
            val definition = definition(nnType).columns.size
            val underlying = rawDecoder(nnType)
            return {
                if (this.get() == null) {
                    repeat(definition) {
                        skip()
                    }
                    null
                } else underlying.invoke(this)
            }
        }
    }

    inner class PolyGenerator : DefinitionRepository.Generator<PartialTable>, Encoder.Generator<MutableList<String>>, Decoder.Generator<RowReader> {

        override val description: String get() = "poly"
        override val priority: Float get() = .9f

        override fun generateDefine(type: Type<*>): PartialTable? {
            val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
            if (classInfo.canBeInstantiated) return null
            return PartialTable(columns = listOf(Column("", "TEXT")))
        }

        override fun generateEncoder(type: Type<*>): (MutableList<String>.(value: Any?) -> Unit)? {
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

    inner class ReflectiveGenerator : DefinitionRepository.Generator<PartialTable>, Encoder.Generator<MutableList<String>>, Decoder.Generator<RowReader> {

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

        override fun generateEncoder(type: Type<*>): (MutableList<String>.(value: Any?) -> Unit)? {
            if (type.nullable) return null
            val classInfo = registry.classInfoRegistry[type.kClass] ?: return null
            val lazySubCoders by lazy { classInfo.fields.map { it to rawEncoder(it.type as Type<*>) } }
            return {
                for ((field, coder) in lazySubCoders) {
                    coder.invoke(this, field.get.untyped(it!!))
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

