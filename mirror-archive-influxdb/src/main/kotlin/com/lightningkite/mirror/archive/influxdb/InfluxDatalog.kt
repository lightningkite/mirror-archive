package com.lightningkite.mirror.archive.influxdb

import com.lightningkite.lokalize.TimeConstants
import com.lightningkite.lokalize.TimeStamp
import com.lightningkite.mirror.archive.*
import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.info.localName
import com.lightningkite.mirror.info.type
import com.lightningkite.mirror.serialization.SerializationRegistry
import com.lightningkite.mirror.serialization.StringSerializer
import org.influxdb.InfluxDB
import org.influxdb.dto.Point
import org.influxdb.dto.Query
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KClass

class InfluxDatalog(
        override val registry: SerializationRegistry,
        val connection: InfluxDB,
        val backupStringSerializer: StringSerializer,
        val database: String = "main"
) : Datalog {
    override fun <T : HasId> table(type: KClass<T>, name: String): Datalog.Table<T> = InfluxDatalogTable(
            database = database,
            connection = connection,
            classInfo = registry.classInfoRegistry[type]!!,
            backupStringSerializer = backupStringSerializer
    )

    class InfluxDatalogTable<T : HasId>(
            val database: String,
            val connection: InfluxDB,
            override val classInfo: ClassInfo<T>,
            val backupStringSerializer: StringSerializer
    ) : Datalog.Table<T> {

        val tableName = classInfo.localName

        val timestampField = classInfo.fields
                .filter { it.type == TimeStamp::class.type }
                .run {
                    find { it.name.equals("timestamp", true) }
                            ?: find { it.name.equals("date", true) }
                            ?: find { it.name.equals("time", true) }
                }

        init {
            connection.query(Query("CREATE DATABASE $database", database))
        }

        fun modelToPoint(model: T): Point {
            val point = Point.measurement(tableName)

            for (field in classInfo.fields) {
                val value = field.get(model)
                if (value != null) {
                    when (field.type.kClass) {
                        TimeStamp::class -> {
                            if (field == timestampField) {
                                point.time((value as TimeStamp).millisecondsSinceEpoch, TimeUnit.MILLISECONDS)
                            } else {
                                point.addField(field.name, (value as TimeStamp).millisecondsSinceEpoch * TimeConstants.NS_PER_MILLISECOND)
                            }
                        }
                        Int::class -> point.addField(field.name, value as Int)
                        Long::class -> point.addField(field.name, value as Long)
                        Boolean::class -> point.addField(field.name, value as Boolean)
                        Double::class -> point.addField(field.name, value as Double)
                        Number::class -> point.addField(field.name, value as Number)
                        String::class -> point.addField(field.name, value as String)
                        Id::class -> point.tag(field.name, (value as Id).toUUIDString())
                        else -> {
                            @Suppress("UNCHECKED_CAST")
                            point.addField(field.name, backupStringSerializer.write(value, field.type as Type<Any?>))
                        }
                    }
                }
            }

            return point.build()
        }

        fun modelsFromResult(it: org.influxdb.dto.QueryResult.Series): List<T> {
            val fields = it.columns.map { column ->
                if (column == "time") timestampField
                else classInfo.fields.find { it.name == column }
            }
            return it.values.map { values ->
                val map = HashMap<String, Any?>()
                fields.forEachIndexed { index, field ->
                    if (field != null) {
                        val value = values[index]
                        val castedValue = when (field.type.kClass) {
                            TimeStamp::class -> TimeStamp.iso8601(value as String)
                            Int::class -> value as Int
                            Long::class -> value as Long
                            Boolean::class -> value as Boolean
                            Double::class -> value as Double
                            Number::class -> value as Number
                            String::class -> value as String
                            Id::class -> Id.fromUUIDString(value as String)
                            else -> backupStringSerializer.read(value as String, field.type as Type<Any?>)
                        }
                        map[field.name] = castedValue
                    }
                }
                classInfo.construct(map)
            }
        }

        override suspend fun insert(transaction: Transaction, model: T): T {
            connection.write(database, "autogen", modelToPoint(model))
            return model
        }

        override suspend fun get(transaction: Transaction, id: Id): T? = suspendCoroutine { cont ->
            try {
                val query = Query("SELECT * FROM $tableName WHERE id = '${id.toUUIDString()}'", database)
                connection.query(query, {
                    val results = it.results.flatMap { it.series.flatMap { modelsFromResult(it) } }
                    cont.resume(results.firstOrNull())
                }, {
                    cont.resumeWithException(it)
                })
            } catch (e: Throwable) {
                cont.resumeWithException(e)
            }
        }

        fun ConditionOnItem<T>.convert(): String {
            return when (this) {
                is ConditionOnItem.Always -> ""
                is ConditionOnItem.Never -> throw IllegalArgumentException()
                is ConditionOnItem.And -> this.conditions.filter { it !is ConditionOnItem.Always }.joinToString(" AND ") { it.convert() }
                is ConditionOnItem.Or -> this.conditions.filter { it !is ConditionOnItem.Always }.joinToString(" OR ") { it.convert() }
                is ConditionOnItem.Not -> "NOT (${this.condition.convert()})"
                is ConditionOnItem.Equal<*, *> -> "${this.field.name} = ${this.value.toConditionString(this.field.type)}"
                is ConditionOnItem.EqualToOne<*, *> -> this.values.joinToString(" AND ") { "${this.field.name} = ${it.toConditionString(this.field.type)}" }
                is ConditionOnItem.NotEqual<*, *> -> "${this.field.name} <> ${this.value.toConditionString(this.field.type)}"
                is ConditionOnItem.LessThan<*, *> -> "${this.field.name} < ${this.value.toConditionString(this.field.type)}"
                is ConditionOnItem.GreaterThan<*, *> -> "${this.field.name} > ${this.value.toConditionString(this.field.type)}"
                is ConditionOnItem.LessThanOrEqual<*, *> -> "${this.field.name} <= ${this.value.toConditionString(this.field.type)}"
                is ConditionOnItem.GreaterThanOrEqual<*, *> -> "${this.field.name} >= ${this.value.toConditionString(this.field.type)}"
                is ConditionOnItem.TextSearch<*, *> -> "${this.field.name} ~= ${Regex.fromLiteral(this.query).pattern.toConditionString(this.field.type)}"
                is ConditionOnItem.RegexTextSearch<*, *> -> "${this.field.name} ~= ${this.query.pattern.toConditionString(this.field.type)}"
            }
        }

        fun Any?.toConditionString(type: Type<*>): String {
            return when (this) {
                is TimeStamp -> this.millisecondsSinceEpoch.toString() + "ms"
                is Id -> "'" + this.toUUIDString() + "'"
                is Int,
                is Long,
                is Boolean,
                is Double,
                is Number -> this.toString()
                is String -> "'" + this.replace("'", "\\'") + "'"
                else -> "'" + backupStringSerializer.write(this, type as Type<Any?>).replace("'", "\\'") + "'"
            }
        }

        override suspend fun query(
                transaction: Transaction,
                condition: ConditionOnItem<T>,
                sortedBy: List<SortOnItem<T, *>>,
                continuationToken: String?,
                count: Int
        ): QueryResult<T> = suspendCoroutine { cont ->

            //TODO: pagination

            try {
                if (sortedBy.isNotEmpty()) throw IllegalArgumentException()
                val queryFromCondition = condition.convert()
                val query = if (queryFromCondition.isNotBlank()) {
                    Query("SELECT * FROM $tableName WHERE $queryFromCondition", database)
                } else {
                    Query("SELECT * FROM $tableName", database)
                }
                connection.query(query, {
                    val results = it.results.flatMap { it.series.flatMap { modelsFromResult(it) } }
                    val result = QueryResult(
                            results = results,
                            continuationToken = null
                    )
                    cont.resume(result)
                }, {
                    cont.resumeWithException(it)
                })
            } catch (e: Throwable) {
                cont.resumeWithException(e)
            }
        }

    }
}