package com.lightningkite.mirror.archive.postgres

import com.lightningkite.kommon.atomic.AtomicReference
import com.lightningkite.kommon.collection.forEachBetween
import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.database.MigrationHandler
import com.lightningkite.mirror.archive.flatarray.FlatArrayFormat
import com.lightningkite.mirror.archive.flatarray.IndexPath
import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.info.MirrorClass
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.Row
import io.vertx.core.buffer.Buffer
import kotlinx.serialization.PrimitiveKind
import kotlinx.serialization.UnionKind
import java.lang.IllegalArgumentException

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
    val schema = FlatArrayFormat.columns(mirror)
    val schemaByIndexPath = schema.associate { it.indexPath to it }
    val typeByName = schema.associate { it.sqlName.toLowerCase() to it.type }
    val isSetUp = AtomicReference(false)

    val FlatArrayFormat.Column.sqlName: String get() = if (name.toUpperCase() in PostgresReservedKeywords) "row_$name" else name

    fun rowToArray(row: Row) = (0 until row.size()).map {
        val raw = row.getValue(it)
        val key = row.getColumnName(it).toLowerCase()
        val type = typeByName[key]
                ?: throw IllegalArgumentException("Type for $key not found - available are ${typeByName.keys}")
        val value = when (typeByName.getValue(key).kind) {
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

    fun QueryBuilder.appendComparison(value: Any?, comparison: String = "", name: String = "") {
        append(name)
        append(' ')
        append(comparison)
        append(' ')
        appendValue(value)
    }

    fun QueryBuilder.appendCondition(condition: Condition<*>, indexPath: IndexPath = IndexPath.empty) {
        when (condition) {
            Condition.Never -> append("FALSE")
            Condition.Always -> append("TRUE")
            is Condition.And -> {
                if (condition.conditions.isEmpty()) throw IllegalArgumentException()
                append("(")
                var isFirst = true
                for (c in condition.conditions) {
                    if (isFirst)
                        isFirst = false
                    else
                        append(" AND ")

                    appendCondition(c, indexPath)
                }
                append(")")
            }
            is Condition.Or -> {
                if (condition.conditions.isEmpty()) throw IllegalArgumentException()
                append("(")
                var isFirst = true
                for (c in condition.conditions) {
                    if (isFirst)
                        isFirst = false
                    else
                        append(" OR ")

                    appendCondition(c, indexPath)
                }
                append(")")
            }
            is Condition.Not -> {
                append("NOT (")
                appendCondition(condition.condition, indexPath)
                append(")")
            }
            is Condition.Field<*, *> -> {
                appendCondition(condition.condition, indexPath + condition.field.index)
            }
            is Condition.Equal -> {
                append("(")
                val broken = FlatArrayFormat.toArrayPartial(mirror, default, condition.value, indexPath)
                var index = 0
                schema
                        .asSequence()
                        .filter { it.indexPath.startsWith(indexPath) }
                        .asIterable()
                        .forEachBetween(
                                forItem = { field ->
                                    appendComparison(broken[index], "=", field.sqlName)
                                    index++
                                },
                                between = { append(" AND ") }
                        )
                append(")")
            }
            is Condition.EqualToOne -> {
                append("(")
                val brokenValues = condition.values.map { FlatArrayFormat.toArrayPartial(mirror, default, it, indexPath) }
                var index = 0
                schema
                        .asSequence()
                        .filter { it.indexPath.startsWith(indexPath) }
                        .asIterable()
                        .forEachBetween(
                                forItem = { field ->
                                    append(field.sqlName)
                                    append(" IN (")
                                    var isFirst = true
                                    for (brokenValue in brokenValues) {
                                        if (isFirst)
                                            isFirst = false
                                        else
                                            append(", ")
                                        appendValue(brokenValue[index])
                                    }
                                    append(")")
                                    index++
                                },
                                between = { append(" AND ") }
                        )
                append(")")
            }
            is Condition.NotEqual -> {
                append("(")
                val broken = FlatArrayFormat.toArrayPartial(mirror, default, condition.value, indexPath)
                var index = 0
                schema
                        .asSequence()
                        .filter { it.indexPath.startsWith(indexPath) }
                        .asIterable()
                        .forEachBetween(
                                forItem = { field ->
                                    appendComparison(broken[index], "<>", field.sqlName)
                                    index++
                                },
                                between = { append(" OR ") }
                        )
                append(")")
            }
            is Condition.LessThan -> appendComparison(condition.value, "<", schemaByIndexPath[indexPath]!!.sqlName)
            is Condition.GreaterThan -> appendComparison(condition.value, ">", schemaByIndexPath[indexPath]!!.sqlName)
            is Condition.LessThanOrEqual -> appendComparison(condition.value, "<=", schemaByIndexPath[indexPath]!!.sqlName)
            is Condition.GreaterThanOrEqual -> appendComparison(condition.value, ">=", schemaByIndexPath[indexPath]!!.sqlName)
            is Condition.TextSearch -> appendComparison("%" + condition.query + "%", " LIKE ", schemaByIndexPath[indexPath]!!.sqlName)
            is Condition.StartsWith -> appendComparison(condition.query + "%", " LIKE ", schemaByIndexPath[indexPath]!!.sqlName)
            is Condition.EndsWith -> appendComparison("%" + condition.query, " LIKE ", schemaByIndexPath[indexPath]!!.sqlName)
            is Condition.RegexTextSearch -> {
                val encoded = buildString {
                    append('\'')
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
                    append('\'')
                }
                append(schemaByIndexPath[indexPath]!!.sqlName)
                append(" SIMILAR TO ")
                append(encoded)
            }
        }
    }

    fun QueryBuilder.appendOperation(operation: Operation<*>, indexPath: IndexPath = IndexPath.empty) {
        when (operation) {
            is Operation.Field<*, *> -> appendOperation(operation.operation, indexPath + operation.field.index)
            is Operation.Set -> {
                val broken = FlatArrayFormat.toArrayPartial(mirror, default, operation.value, indexPath)
                var index = 0
                schema
                        .asSequence()
                        .filter { it.indexPath.startsWith(indexPath) }
                        .asIterable()
                        .forEachBetween(
                                forItem = { field ->
                                    append(field.sqlName)
                                    append(" = ")
                                    appendValue(broken[index])
                                    index++
                                },
                                between = { append(", ") }
                        )
            }
            is Operation.AddNumeric -> {
                val name = schema
                        .asSequence()
                        .filter { it.indexPath.startsWith(indexPath) }
                        .first().sqlName
                append(name)
                append(" = ")
                append(name)
                append(" + ")
                appendValue(operation.amount)
            }
            is Operation.Append -> {
                val name = schema
                        .asSequence()
                        .filter { it.indexPath.startsWith(indexPath) }
                        .first().sqlName
                append(name)
                append(" = ")
                append(name)
                append(" || ")
                appendValue(operation.string)
            }
            is Operation.Multiple -> {
                operation.operations.forEachBetween(
                        forItem = { appendOperation(it, indexPath) },
                        between = { append(", ") }
                )
            }
            else -> throw UnsupportedOperationException()
        }
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
        return sqlName.toLowerCase() to sqlType
    }

    suspend fun setup() {
        if (!isSetUp.compareAndSet(expected = false, new = true)) return

        val existingColumns = client.suspendQuery(PostgresMetadata.columns(tableName))
        if (existingColumns.rowCount() == 0) {
            //Create if it doesn't exist
            client.suspendQuery("CREATE SCHEMA IF NOT EXISTS $schemaName")
            client.suspendQuery {
                append("CREATE TABLE IF NOT EXISTS $schemaName.$tableName (")
                schema.forEachBetween(
                        forItem = {
                            val nameAndType = it.nameAndType()
                            append(nameAndType.first)
                            append(' ')
                            append(nameAndType.second)
                        },
                        between = { append(", ") }
                )
                append(", PRIMARY KEY (")
                schema.asSequence()
                        .filter { it.indexPath[0] == primaryKey.index }
                        .asIterable()
                        .forEachBetween(
                                forItem = {
                                    append(it.sqlName)
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
            val parsedCodeColumns = schema.map { it.nameAndType() }.toSet()
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
        schema.forEachBetween(
                forItem = { append(it.sqlName) },
                between = { append(", ") }
        )
        append(" FROM ")
        append(schemaName)
        append('.')
        append(tableName)
        append(" WHERE ")
        val fullSort = sort + Sort(primaryKey as MirrorClass.Field<T, Comparable<Comparable<*>>>)
        val fullCondition = if (after == null) condition else condition and sort.after(after, defaultSort)
        appendCondition(fullCondition.simplify())
        append(" ORDER BY ")
        fullSort.forEachBetween(
                forItem = { sort ->
                    val name = schema
                            .asSequence()
                            .filter { it.indexPath[0] == sort.field.index }
                            .first().sqlName
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
        FlatArrayFormat.fromArray(mirror, rowAsList)
    }

    override suspend fun insert(values: List<T>): List<T> = client.suspendQuery {
        setup()
        append("INSERT INTO ")
        append(schemaName)
        append('.')
        append(tableName)
        append("(")
        schema.forEachBetween(
                forItem = { append(it.sqlName) },
                between = { append(", ") }
        )
        append(") VALUES ")
        values.forEachBetween(
                forItem = {
                    append("(")
                    val flattened = FlatArrayFormat.toArray(mirror, it)
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
        appendOperation(operation)
        append(" WHERE ")
        if (limit != null) {
            append("ctid in (SELECT ctid FROM ")
            append(schemaName)
            append('.')
            append(tableName)
            append(" WHERE ")
            appendCondition(condition.simplify())
            append(" LIMIT ")
            append(limit.toString())
            append(")")
        } else {
            appendCondition(condition.simplify())
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
        appendCondition(condition.simplify())
        append(";")
    }.rowCount()

}