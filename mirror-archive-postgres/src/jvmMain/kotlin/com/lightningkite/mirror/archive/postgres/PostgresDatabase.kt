package com.lightningkite.mirror.archive.postgres

import com.lightningkite.kommon.atomic.AtomicValue
import com.lightningkite.kommon.collection.forEachBetween
import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.database.MigrationHandler
import com.lightningkite.mirror.archive.flatarray.BinaryFlatArrayFormat
import com.lightningkite.mirror.archive.flatarray.FlatArrayFormat
import com.lightningkite.mirror.archive.flatarray.IndexPath
import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.info.MirrorClass
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.Row
import io.vertx.core.buffer.Buffer
import kotlinx.serialization.PrimitiveKind
import kotlinx.serialization.UnionKind

class PostgresDatabase<T : Any>(
        val mirror: MirrorClass<T>,
        val default: T,
        val primaryKey: MirrorClass.Field<T, *>,
        val schemaName: String = "mySchema",
        val tableName: String = mirror.localName,
        val migrationHandler: MigrationHandler<T> = { _, _, _ -> },
        val client: PgClient
) : Database<T> {

    val defaultSort = Sort(primaryKey as MirrorClass.Field<T, Comparable<Comparable<*>>>)
    val schema = PostgresFlatArrayFormat.schema(mirror, default).let {
        it.copy(columns = it.columns.map {
            val name = it.name
            it.copy(name = if (name.toUpperCase() in PostgresReservedKeywords) "row_$name" else name)
        })
    }
    val isSetUp = AtomicValue(false)

    fun rowToArray(row: Row) = (0 until row.size()).map {
        val raw = row.getValue(it)
        val key = row.getColumnName(it).toLowerCase()
        val column = schema.byLowercaseName[key]!!
        val type = column.type
        val value = when (type.kind) {
            PrimitiveKind.UNIT -> Unit
            PrimitiveKind.BYTE -> (raw as Short).toByte()
            PrimitiveKind.CHAR -> (raw as String).first()
            PrimitiveKind.INT,
            PrimitiveKind.BOOLEAN,
            PrimitiveKind.SHORT,
            PrimitiveKind.LONG,
            PrimitiveKind.FLOAT,
            PrimitiveKind.DOUBLE,
            PrimitiveKind.STRING -> raw
            is UnionKind.ENUM_KIND -> raw as String
            else -> (raw as Buffer).bytes
        }
        value
    }

    /*  val value = when (type.kind) {
            PrimitiveKind.UNIT -> Unit
            PrimitiveKind.BYTE -> (raw as? Short)?.toByte() ?: 0.toByte()
            PrimitiveKind.CHAR -> (raw as? String)?.first() ?: ' '
            PrimitiveKind.INT -> (raw as? Int) ?: 0
            PrimitiveKind.BOOLEAN -> (raw as? Boolean) ?: false
            PrimitiveKind.SHORT -> (raw as? Short) ?: 0.toShort()
            PrimitiveKind.LONG -> (raw as? Long) ?: 0L
            PrimitiveKind.FLOAT -> (raw as? Float) ?: 0f
            PrimitiveKind.DOUBLE -> (raw as? Double) ?: 0.0
            PrimitiveKind.STRING -> (raw as? String) ?: ""
            is UnionKind.ENUM_KIND -> raw as String
            else -> (raw as Buffer).bytes
        }*/

    fun QueryBuilder.appendConditionFull(condition: Condition<T>) {
        schema.conditionStream(
                cond = condition,
                indexPath = IndexPath.empty,
                startGroup = {
                    when (it) {
                        FlatArrayFormat.Schema.ConditionMode.AND -> append("(")
                        FlatArrayFormat.Schema.ConditionMode.OR -> append("(")
                        FlatArrayFormat.Schema.ConditionMode.NOT -> append("NOT (")
                    }
                },
                groupDivider = {
                    when (it) {
                        FlatArrayFormat.Schema.ConditionMode.AND -> append(" AND ")
                        FlatArrayFormat.Schema.ConditionMode.OR -> append(" OR ")
                        FlatArrayFormat.Schema.ConditionMode.NOT -> append(" ")
                    }
                },
                endGroup = {
                    append(")")
                },
                action = { condition, column ->
                    when (condition) {
                        Condition.Never -> append("FALSE")
                        Condition.Always -> append("TRUE")
                        is Condition.Equal -> {
                            append("${column!!.name} = ")
                            appendValue(condition.value)
                        }
                        is Condition.EqualToOne -> {
                            append("${column!!.name} IN (")
                            condition.values.forEachBetween(
                                    forItem = { v ->
                                        appendValue(v)
                                    },
                                    between = {
                                        append(", ")
                                    }
                            )
                            append(")")
                        }
                        is Condition.NotEqual -> {
                            append("${column!!.name} <> ")
                            appendValue(condition.value)
                        }
                        is Condition.LessThan -> {
                            append("${column!!.name} < ")
                            appendValue(condition.value)
                        }
                        is Condition.GreaterThan -> {
                            append("${column!!.name} > ")
                            appendValue(condition.value)
                        }
                        is Condition.LessThanOrEqual -> {
                            append("${column!!.name} <= ")
                            appendValue(condition.value)
                        }
                        is Condition.GreaterThanOrEqual -> {
                            append("${column!!.name} >= ")
                            appendValue(condition.value)
                        }
                        is Condition.TextSearch -> {
                            append("${column!!.name} LIKE ")
                            appendValue("%" + condition.query + "%")
                        }
                        is Condition.StartsWith -> {
                            append("${column!!.name} LIKE ")
                            appendValue(condition.query + "%")
                        }
                        is Condition.EndsWith -> {
                            append("${column!!.name} LIKE ")
                            appendValue("%" + condition.query)
                        }
                        is Condition.RegexTextSearch -> {
                            append("${column!!.name} SIMILAR TO ")
                            appendValue(buildString {
                                var escaped = false
                                var inBrackets = false
                                for (c in condition.query) {
                                    when (c) {
                                        '.' -> {
                                            if (escaped || inBrackets) {
                                                append(c)
                                            } else {
                                                append("%")
                                            }
                                        }
                                        '%' -> append("\\%")
                                        '[' -> {
                                            inBrackets = true
                                            append(c)
                                        }
                                        ']' -> {
                                            inBrackets = false
                                            append(c)
                                        }
                                        '\\' -> {
                                            escaped = true
                                            append(c)
                                        }
                                        else -> append(c)
                                    }
                                    if (c != '\\')
                                        escaped = false
                                }
                            })
                        }
                    }
                }
        )
    }

    fun QueryBuilder.appendOperationFull(operation: Operation<T>) {
        var isFirst = true
        schema.operationStream(
                op = operation,
                indexPath = IndexPath.empty,
                action = { op, column ->
                    if (isFirst) {
                        isFirst = false
                    } else {
                        append(", ")
                    }
                    when (op) {
                        is Operation.Set -> {
                            append("${column!!.name} = ")
                            appendValue(op.value)
                        }
                        is Operation.AddNumeric -> {
                            append("${column!!.name} = ${column.name} + ")
                            appendValue(op.amount)
                        }
                        is Operation.Append -> {
                            append("${column!!.name} = ${column.name} || ")
                            appendValue(op.string)
                        }
                    }
                }
        )
    }

    fun FlatArrayFormat.Column.nameAndType(): Pair<String, String> {
        val sqlType = when (type.kind) {
            PrimitiveKind.BOOLEAN -> "boolean"
            PrimitiveKind.BYTE,
            PrimitiveKind.SHORT -> "int2"
            PrimitiveKind.INT -> "int4"
            PrimitiveKind.LONG -> "int8"
            PrimitiveKind.FLOAT -> "float4"
            PrimitiveKind.DOUBLE -> "float8"
            PrimitiveKind.CHAR -> "character(1)"
            PrimitiveKind.STRING -> "text"
            UnionKind.ENUM_KIND -> "text"
            else -> "bytea"
        }
        return name.toLowerCase() to sqlType
    }

    suspend fun setup() {
        if (!isSetUp.compareAndSet(expected = false, new = true)) return

        val existingColumns = client.suspendQuery(PostgresMetadata.columns(tableName))
        if (existingColumns.rowCount() == 0) {
            //Create if it doesn't exist
            client.suspendQuery("CREATE SCHEMA IF NOT EXISTS $schemaName")
            client.suspendQuery {
                append("CREATE TABLE IF NOT EXISTS $schemaName.$tableName (")
                schema.columns.forEachBetween(
                        forItem = {
                            val nameAndType = it.nameAndType()
                            append(nameAndType.first)
                            append(' ')
                            append(nameAndType.second)
                        },
                        between = { append(", ") }
                )
                append(", PRIMARY KEY (")
                schema.columns.asSequence()
                        .filter { it.indexPath[0] == primaryKey.index }
                        .asIterable()
                        .forEachBetween(
                                forItem = {
                                    append(it.name)
                                },
                                between = {
                                    append(", ")
                                }
                        )
                append("))")
            }
        } else {
            val parsedExistingColumns = existingColumns.map {
                it.getString(PostgresMetadata.Columns.column_name).toLowerCase() to it.getString(PostgresMetadata.Columns.data_type).toLowerCase()
            }.toSet()
            val parsedCodeColumns = schema.columns.map { it.nameAndType() }.toSet()
            val newColumns = parsedCodeColumns.minus(parsedExistingColumns)
            val oldColumns = parsedExistingColumns.minus(parsedCodeColumns)

            for (column in newColumns) {
                client.suspendQuery("ALTER TABLE $schemaName.$tableName ADD COLUMN ${column.first} ${column.second}")
            }

            //TODO: Figure out how this works in detail
            migrationHandler(this, listOf(), listOf())

            for (column in oldColumns) {
                client.suspendQuery("ALTER TABLE $schemaName.$tableName DROP COLUMN ${column.first} ${column.second}")
            }

            //TODO: Migrate PK?
            //TODO: Migrate field type changes?
        }
        //Check record of which fields are present
        //Add new fields
        //Run migrations
        //Remove old fields
    }

    override suspend fun get(condition: Condition<T>, sort: List<Sort<T, *>>, count: Int, after: T?): List<T> = client.suspendQuery {
        setup()
        append("SELECT ")
        schema.columns.forEachBetween(
                forItem = { append(it.name) },
                between = { append(", ") }
        )
        append(" FROM ")
        append(schemaName)
        append('.')
        append(tableName)
        append(" WHERE ")
        val fullSort = sort + Sort(primaryKey as MirrorClass.Field<T, Comparable<Comparable<*>>>)
        val fullCondition = if (after == null) condition else condition and sort.after(after, defaultSort)
        appendConditionFull(fullCondition.simplify())
        append(" ORDER BY ")
        fullSort.forEachBetween(
                forItem = { sort ->
                    val name = schema.columns
                            .asSequence()
                            .filter { it.indexPath[0] == sort.field.index }
                            .first().name
                    append(name)
                    append(" ")
                    if (sort.ascending)
                        append("ASC")
                    else
                        append("DESC")
                },
                between = { append(", ") }
        )
        append(" LIMIT $count;")
    }.map {
        val rowAsList = rowToArray(it)
        println("We got result: ${rowAsList.joinToString()}")
        PostgresFlatArrayFormat.fromArray(mirror, rowAsList)
    }

    override suspend fun insert(values: List<T>): List<T> = client.suspendQuery {
        setup()
        append("INSERT INTO ")
        append(schemaName)
        append('.')
        append(tableName)
        append("(")
        schema.columns.forEachBetween(
                forItem = { append(it.name) },
                between = { append(", ") }
        )
        append(") VALUES ")
        values.forEachBetween(
                forItem = {
                    append("(")
                    val flattened = PostgresFlatArrayFormat.toArray(mirror, it)
                    flattened.forEachBetween(
                            forItem = { appendValue(it) },
                            between = { append(", ") }
                    )
                    append(")")
                },
                between = { append(", ") }
        )
        append(";")
    }.let { values }

    override suspend fun update(condition: Condition<T>, operation: Operation<T>, limit: Int?): Int = client.suspendQuery {
        setup()
        append("UPDATE ")
        append(schemaName)
        append('.')
        append(tableName)
        append(" SET ")
        appendOperationFull(operation)
        append(" WHERE ")
        if (limit != null) {
            append("ctid in (SELECT ctid FROM ")
            append(schemaName)
            append('.')
            append(tableName)
            append(" WHERE ")
            appendConditionFull(condition.simplify())
            append(" LIMIT ")
            append(limit.toString())
            append(")")
        } else {
            appendConditionFull(condition.simplify())
        }
        append(";")
    }.rowCount()

    override suspend fun delete(condition: Condition<T>): Int = client.suspendQuery {
        setup()
        append("DELETE FROM ")
        append(schemaName)
        append('.')
        append(tableName)
        append(" WHERE ")
        appendConditionFull(condition.simplify())
        append(";")
    }.rowCount()

}