package com.lightningkite.mirror.archive.postgres

import com.lightningkite.kommon.atomic.AtomicValue
import com.lightningkite.kommon.collection.forEachBetween
import com.lightningkite.lokalize.time.TimeStamp
import com.lightningkite.lokalize.time.TimeStampMirror
import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.database.MigrationHandler
import com.lightningkite.mirror.archive.flatarray.BinaryFlatArrayFormat
import com.lightningkite.mirror.archive.flatarray.FlatArrayFormat
import com.lightningkite.mirror.archive.flatarray.IndexPath
import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.archive.property.RamSuspendProperty
import com.lightningkite.mirror.archive.property.SuspendProperty
import com.lightningkite.mirror.info.MirrorClass
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgConnectOptions
import io.reactiverse.pgclient.PgPoolOptions
import io.reactiverse.pgclient.Row
import io.vertx.core.buffer.Buffer
import kotlinx.serialization.PrimitiveKind
import kotlinx.serialization.StructureKind
import kotlinx.serialization.UnionKind
import java.io.File
import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class PostgresDatabase<T : Any>(
        val mirror: MirrorClass<T>,
        val default: T,
        schemaName: String = "mySchema",
        tableName: String = mirror.localName,
        val primaryKey: List<MirrorClass.Field<T, *>> = mirror.findPrimaryKey(),
        val client: PgClient
) : Database<T> {


    val schemaName = schemaName.filter { it in 'a'..'z' || it in 'A'..'Z' }.let {
        if (it in PostgresReservedKeywords) {
            "mirrorschema_$it"
        } else it
    }
    val tableName = tableName.filter { it in 'a'..'z' || it in 'A'..'Z' }.let {
        if (it in PostgresReservedKeywords) {
            "mirrortable_$it"
        } else it
    }
    val singleFieldIndices = mirror.fields.filter { it.shouldBeIndexed }
    val multiFieldIndices = mirror.multiIndexSequence().toList()

    /**
     * Available options:
     *
     * If source is not present OR is "embedded":
     * - cache - Where Postgres should be found/stored, defaults to '/pgcache'
     * - version - The version of Postgres to use, defaults to version 10
     * - files - Where the database should go, defaults to './build/pg'
     * - clear - If set to `true`, it will clear the files before starting
     * - port - The port to use, defaults to 5432
     *
     * Otherwise, the source should be the hostname or IP address of the Postgres instance:
     * - port - Defaults to 5432
     * - user - Defaults to 'postgres'
     * - password - Defaults to 'postgres'
     * - database - Defaults to 'postgres'
     *
     * */
    companion object FromConfiguration : Database.Provider.FromConfiguration {
        override val name: String get() = "Postgres"
        override val requiredArguments = arrayOf("source")
        override val optionalArguments = arrayOf(
                "cache",
                "version",
                "files",
                "clear",
                "port",
                "user",
                "password",
                "database"
        )

        override fun invoke(arguments: Map<String, String>) = Provider(
                schemaName = arguments["schema"] ?: "mySchema",
                client = {
                    val source = arguments["source"]
                    if (source == null || source == "embedded") {
                        EmbeddedPG.PoolProvider(
                                cache = arguments["cache"]?.let { File(it) } ?: File("/pgcache"),
                                version = arguments["version"] ?: EmbeddedPG.Versions.VERSION_10,
                                storeFiles = arguments["files"]?.let { File(it) } ?: File("./build/pg"),
                                clearBeforeStarting = arguments["clear"] == "true",
                                port = arguments["port"]?.toInt() ?: 5432
                        ).startWithAutoShutdown()
                    } else {
                        PgClient.pool(PgPoolOptions(PgConnectOptions().also {
                            it.host = source
                            it.port = arguments["port"]?.toInt() ?: 5432
                            it.user = arguments["user"] ?: "postgres"
                            it.password = arguments["password"] ?: "postgres"
                            it.database = arguments["database"] ?: "postgres"
                        }))
                    }
                }()
        )
    }

    class Provider(val schemaName: String, val client: PgClient) : Database.Provider {

        override fun <T : Any> get(mirrorClass: MirrorClass<T>, default: T, name: String): Database<T> {
            return PostgresDatabase(
                    mirror = mirrorClass,
                    default = default,
                    schemaName = schemaName,
                    tableName = name,
                    client = client
            )
        }
    }

    val defaultSort = primaryKey.map { Sort(it as MirrorClass.Field<T, Comparable<Comparable<*>>>) }
    val schema = PostgresFlatArrayFormat.schema(mirror, default).let {
        it.copy(columns = it.columns.map {
            val name = it.name
            it.copy(name = if (name.toUpperCase() in PostgresReservedKeywords) "field_$name" else name)
        })
    }

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
            else -> when (raw) {
                is Buffer -> raw.bytes
                is UUID -> Uuid(raw.mostSignificantBits, raw.leastSignificantBits)
                is LocalDateTime -> TimeStamp(raw.toInstant(ZoneOffset.UTC).toEpochMilli())
                else -> throw IllegalArgumentException("")
            }
        }
        value
    }

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
            else -> when (type) {
                UuidMirror.descriptor -> "uuid"
                else -> "bytea"
            }
        }
        return name.toLowerCase() to sqlType
    }

    val isSetUp = AtomicValue(false)
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
                        .filter { primaryKey.any { key -> key.index == it.indexPath[0] } }
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

            //TODO: Custom migrations?

            for (column in oldColumns) {
                client.suspendQuery("ALTER TABLE $schemaName.$tableName DROP COLUMN ${column.first} ${column.second}")
            }

            //TODO: Migrate PK?
            //TODO: Migrate field type changes?
        }

        //TODO: Indicies
    }

    override suspend fun get(condition: Condition<T>, sort: List<Sort<T, *>>, count: Int, after: T?): List<T> = if (condition.simplify() == Condition.Never) listOf() else client.suspendQuery {
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
        val fullSort = sort + defaultSort
        val fullCondition = if (after == null) condition else condition and (fullSort).after(after)
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
        PostgresFlatArrayFormat.fromArray(mirror, rowAsList)
    }

    override suspend fun insert(values: List<T>): List<T> = if (values.isEmpty()) values else client.suspendQuery {
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

    override suspend fun limitedUpdate(condition: Condition<T>, operation: Operation<T>, sort: List<Sort<T, *>>, limit: Int): Int = if (condition.simplify() == Condition.Never) 0 else client.suspendQuery {
        setup()
        append("UPDATE ")
        append(schemaName)
        append('.')
        append(tableName)
        append(" SET ")
        appendOperationFull(operation)
        append(" WHERE ")
        append("ctid in (SELECT ctid FROM ")
        append(schemaName)
        append('.')
        append(tableName)
        append(" WHERE ")
        appendConditionFull(condition.simplify())
        if (sort.isNotEmpty()) {
            append(" ORDER BY ")
            sort.forEachBetween(
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
        }
        append(" LIMIT ")
        append(limit.toString())
        append(")")
        append(";")
    }.rowCount()

    override suspend fun update(condition: Condition<T>, operation: Operation<T>): Int = if (condition.simplify() == Condition.Never) 0 else client.suspendQuery {
        setup()
        append("UPDATE ")
        append(schemaName)
        append('.')
        append(tableName)
        append(" SET ")
        appendOperationFull(operation)
        append(" WHERE ")
        appendConditionFull(condition.simplify())
        append(";")
    }.rowCount()

    override suspend fun delete(condition: Condition<T>): Int = if (condition.simplify() == Condition.Never) 0 else client.suspendQuery {
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