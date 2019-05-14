//package com.lightningkite.mirror.archive.influxdb
//
//import com.lightningkite.kommon.atomic.AtomicValue
//import com.lightningkite.kommon.collection.forEachBetween
//import com.lightningkite.mirror.archive.database.Database
//import com.lightningkite.mirror.archive.database.MigrationHandler
//import com.lightningkite.mirror.archive.flatarray.FlatArrayFormat
//import com.lightningkite.mirror.archive.flatarray.IndexPath
//import com.lightningkite.mirror.archive.model.*
//import com.lightningkite.mirror.info.MirrorClass
//import kotlinx.serialization.UnionKind
//import org.influxdb.InfluxDB
//import org.influxdb.dto.BatchPoints
//import org.influxdb.dto.Point
//import com.lightningkite.lokalize.time.TimeStamp
//import kotlinx.serialization.PrimitiveKind
//import org.influxdb.dto.Query
//import java.time.Instant
//import java.util.concurrent.TimeUnit
//
//@Suppress("BlockingMethodInNonBlockingContext")
//class InfluxDatabase<T : Any>(
//        val mirror: MirrorClass<T>,
//        val default: T,
//        schemaNameRaw: String = "mySchema",
//        tableNameRaw: String = mirror.localName,
//        val migrationHandler: MigrationHandler<T> = { _, _, _ -> },
//        val client: InfluxDB
//) : Database<T> {
//
//    val schemaName = schemaNameRaw.replace('.', '_').replace('$', '_')
//    val tableName = tableNameRaw.replace('.', '_').replace('$', '_')
//
//    val schema = InfluxFlatArrayFormat.schema(mirror, default)
//    val isSetUp = AtomicValue(false)
//
//    val schemaNameWithQuotes get() = "\"$schemaName\""
//
//
//    suspend fun setup() {
//        if (!isSetUp.compareAndSet(expected = false, new = true)) return
//        client.query(Query("CREATE DATABASE \"$schemaName\"", ""))
//        client.query(Query("CREATE RETENTION POLICY " + "\"${schemaName}_retention\"" + " ON " + "\"$schemaName\"" + " DURATION 30h REPLICATION 2 SHARD DURATION 30m DEFAULT", ""))
//    }
//
//    override suspend fun get(condition: Condition<T>, sort: List<Sort<T, *>>, count: Int, after: T?): List<T> {
//        setup()
//
//        val query = QueryBuilder().apply {
//            append("SELECT ")
//            schema.columns.forEachBetween(
//                    forItem = { append("\"${it.name}\"") },
//                    between = { append(", ") }
//            )
//            append(" FROM ")
//            append("\"$tableName\"")
//            append(" WHERE ")
//            appendConditionFull(condition)
//            append(" LIMIT $count;")
//        }
//
//        val result = client.query(Query(query.toString(), schemaName))
//        return result.results.flatMap {
//            it.series.flatMap {
//                it.values.map {
//                    InfluxFlatArrayFormat.fromArray(mirror, rowToArray(it))
//                }
//            }
//        }
//    }
//
//    override suspend fun insert(values: List<T>): List<T> {
//        setup()
//        client.write(BatchPoints.database(schemaName).apply {
//            for (v in values) {
//                point(Point.measurement(tableName).apply {
//                    val arr = InfluxFlatArrayFormat.toArray(mirror, v)
//                    for (index in schema.columns.indices) {
//                        val column = schema.columns[index]
//                        val item = arr[index]
//                        when (item) {
//                            Unit -> {
//                            }
//                            is Boolean -> this.addField(column.name, item)
//                            is Number -> this.addField(column.name, item)
//                            is String -> this.addField(column.name, item)
//                            is Char -> this.addField(column.name, (item).toString())
//                            is TimeStamp -> this.time(item.millisecondsSinceEpoch, TimeUnit.MILLISECONDS)
//                            is UnionKind.ENUM_KIND -> this.addField(column.name, item as String)
//                        }
//                    }
//                }.build())
//            }
//        }.build())
//        return values
//    }
//
//    override suspend fun update(condition: Condition<T>, operation: Operation<T>, limit: Int?): Int = throw UnsupportedOperationException("You cannot update things in InfluxDB!")
//
//    override suspend fun delete(condition: Condition<T>): Int {
//        setup()
//        if (condition == Condition.Always) {
//            client.query(Query("DELETE FROM \"$tableName\"", schemaName))
//            return 0
//        }
//        throw UnsupportedOperationException("You cannot update things in InfluxDB!")
//    }
//
//    fun QueryBuilder.appendConditionFull(condition: Condition<T>) {
//        schema.conditionStream(
//                cond = condition,
//                indexPath = IndexPath.empty,
//                startGroup = {
//                    when (it) {
//                        FlatArrayFormat.Schema.ConditionMode.AND -> append("(")
//                        FlatArrayFormat.Schema.ConditionMode.OR -> append("(")
//                        FlatArrayFormat.Schema.ConditionMode.NOT -> append("NOT (")
//                    }
//                },
//                groupDivider = {
//                    when (it) {
//                        FlatArrayFormat.Schema.ConditionMode.AND -> append(" AND ")
//                        FlatArrayFormat.Schema.ConditionMode.OR -> append(" OR ")
//                        FlatArrayFormat.Schema.ConditionMode.NOT -> append(" ")
//                    }
//                },
//                endGroup = {
//                    append(")")
//                },
//                action = { condition, column ->
//                    when (condition) {
//                        Condition.Never -> append("FALSE")
//                        Condition.Always -> append("TRUE")
//                        is Condition.Equal -> {
//                            append("${column!!.name} = ")
//                            appendValue(condition.value)
//                        }
//                        is Condition.EqualToOne -> {
//                            append("${column!!.name} IN (")
//                            for (v in condition.values) {
//                                appendValue(v)
//                            }
//                            append(")")
//                        }
//                        is Condition.NotEqual -> {
//                            append("${column!!.name} <> ")
//                            appendValue(condition.value)
//                        }
//                        is Condition.LessThan -> {
//                            append("${column!!.name} < ")
//                            appendValue(condition.value)
//                        }
//                        is Condition.GreaterThan -> {
//                            append("${column!!.name} > ")
//                            appendValue(condition.value)
//                        }
//                        is Condition.LessThanOrEqual -> {
//                            append("${column!!.name} <= ")
//                            appendValue(condition.value)
//                        }
//                        is Condition.GreaterThanOrEqual -> {
//                            append("${column!!.name} >= ")
//                            appendValue(condition.value)
//                        }
//                        is Condition.TextSearch -> {
//                            append("${column!!.name} ~= ")
//                            appendValue(".*" + Regex.fromLiteral(condition.query) + ".*")
//                        }
//                        is Condition.StartsWith -> {
//                            append("${column!!.name} ~= ")
//                            appendValue(Regex.fromLiteral(condition.query).pattern + ".*")
//                        }
//                        is Condition.EndsWith -> {
//                            append("${column!!.name} ~= ")
//                            appendValue(".*" + Regex.fromLiteral(condition.query))
//                        }
//                        is Condition.RegexTextSearch -> {
//                            append("${column!!.name} ~= ")
//                            appendValue(condition.query)
//                        }
//                    }
//                }
//        )
//    }
//
//
//    fun rowToArray(row: List<Any>) = row.mapIndexed { index: Int, raw: Any ->
//        println("Got: ${row.mapIndexed{ index, item ->
//            (schema.columns.getOrNull(index)?.name ?: "null") + "  ==  " + item?.toString()
//        }.joinToString("\n")}")
//        val column = schema.columns[index]
//        val type = column.type
//        val value = when (raw) {
//            is Instant -> TimeStamp(raw.toEpochMilli())
//            else -> when (type.kind) {
//                PrimitiveKind.UNIT -> Unit
//                PrimitiveKind.BYTE -> (raw as Short).toByte()
//                PrimitiveKind.CHAR -> (raw as String).first()
//                PrimitiveKind.INT,
//                PrimitiveKind.BOOLEAN,
//                PrimitiveKind.SHORT,
//                PrimitiveKind.LONG,
//                PrimitiveKind.FLOAT,
//                PrimitiveKind.DOUBLE,
//                PrimitiveKind.STRING -> raw
//                is UnionKind.ENUM_KIND -> raw as String
//                else -> {
//
//                }
//            }
//        }
//        value
//    }
//}