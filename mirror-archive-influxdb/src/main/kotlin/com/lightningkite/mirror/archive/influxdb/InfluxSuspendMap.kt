package com.lightningkite.mirror.archive.influxdb

import com.lightningkite.lokalize.TimeConstants
import com.lightningkite.lokalize.TimeStamp
import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.archive.database.*
import com.lightningkite.mirror.info.*
import com.lightningkite.mirror.serialization.StringSerializer
import org.influxdb.InfluxDB
import org.influxdb.dto.Point
import org.influxdb.dto.Query
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class InfluxSuspendMap<T: HasId>(
        val classInfo: ClassInfo<T>,
        val tableName: String = classInfo.localName,
        val connection: InfluxDB,
        val backupStringSerializer: StringSerializer,
        val database: String = "main"
): SuspendMap<Id, T> {

    class Provider(val serializer: StringSerializer, val connection: InfluxDB): SuspendMapProvider {
        @Suppress("UNCHECKED_CAST")
        override fun <K, V : Any> suspendMap(key: Type<K>, value: Type<V>): SuspendMap<K, V> {
            if(key != Id::class.type) throw UnsupportedOperationException()
            if(value.nullable) throw UnsupportedOperationException()
            return InfluxSuspendMap(
                    classInfo = serializer.registry.classInfoRegistry[value.kClass]!! as ClassInfo<HasId>,
                    connection = connection,
                    backupStringSerializer = serializer
            ) as SuspendMap<K, V>
        }
    }

    @Suppress("UNCHECKED_CAST")
    val timestampField = classInfo.fields
            .filter { it.type == TimeStamp::class.type }
            .run {
                find { it.name.equals("timestamp", true) }
                        ?: find { it.name.equals("date", true) }
                        ?: find { it.name.equals("time", true) }
            } as FieldInfo<T, TimeStamp>

    init {
        connection.query(Query("CREATE DATABASE $database", database))
    }

    override suspend fun getNewKey(): Id = Id.key()

    override suspend fun get(key: Id): T? = suspendCoroutine { cont ->
        try {
            val query = Query("SELECT * FROM $tableName WHERE id = '${key.toUUIDString()}'", database)
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

    override suspend fun put(key: Id, value: T, conditionIfExists: Condition<T>, create: Boolean): Boolean {
        if(!create) throw UnsupportedOperationException("Only insertions are allowed")
        connection.write(database, "autogen", modelToPoint(value))
        return true
    }

    override suspend fun modify(key: Id, operation: Operation<T>, condition: Condition<T>): T? = throw UnsupportedOperationException()

    override suspend fun remove(key: Id, condition: Condition<T>): Boolean = throw UnsupportedOperationException()

    override suspend fun query(condition: Condition<T>, sortedBy: Sort<T>?, after: Pair<Id, T>?, count: Int): List<Pair<Id, T>> = suspendCoroutine { cont ->
        val fullCondition = if(after != null) sortedBy?.after(after.second) ?: Condition.Field(timestampField, Condition.GreaterThan(timestampField.get(after.second))) and condition else condition

        try {
            val queryFromCondition = fullCondition.convert()
            val query = buildString {
                append("SELECT * FROM $tableName")

                if (queryFromCondition.isNotBlank()) {
                    append(" WHERE $queryFromCondition", database)
                }
                if(sortedBy != null){
                    append(" ORDER BY " + sortedBy.convert())
                }
            }
            connection.query(Query(query, database), {
                val results = it.results.flatMap { it.series.flatMap { modelsFromResult(it).map { it.id to it } } }
                cont.resume(results)
            }, {
                cont.resumeWithException(it)
            })
        } catch (e: Throwable) {
            cont.resumeWithException(e)
        }
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

    fun Sort<*>.convert(): String {
        return when (this) {
            is Sort.Multi -> this.comparators.joinToString { it.convert() }
            is Sort.Field<*, *> -> this.field.name + if(ascending) " ASC" else " DESC"
            else -> throw UnsupportedOperationException()
        }
    }

    fun Condition<*>.convert(field: String = "value", type: Type<*> = classInfo.type): String {
        return when (this) {
            is Condition.Never -> throw IllegalArgumentException()
            is Condition.Always -> ""
            is Condition.And -> this.conditions.filter { it !is Condition.Always }.joinToString(" AND ") { it.convert() }
            is Condition.Or -> this.conditions.filter { it !is Condition.Always }.joinToString(" OR ") { it.convert() }
            is Condition.Not -> "NOT (${this.condition.convert()})"
            is Condition.Field<*, *> -> this.condition.convert(field = field)
            is Condition.Equal -> "$field = ${this.value.toConditionString(type)}"
            is Condition.EqualToOne -> this.values.joinToString(" AND ") { "$field = ${it.toConditionString(type)}" }
            is Condition.NotEqual -> "$field <> ${this.value.toConditionString(type)}"
            is Condition.LessThan -> "$field < ${this.value.toConditionString(type)}"
            is Condition.GreaterThan -> "$field > ${this.value.toConditionString(type)}"
            is Condition.LessThanOrEqual -> "$field <= ${this.value.toConditionString(type)}"
            is Condition.GreaterThanOrEqual -> "$field >= ${this.value.toConditionString(type)}"
            is Condition.TextSearch -> "$field ~= ${Regex.fromLiteral(this.query).pattern.toConditionString(type)}"
            is Condition.RegexTextSearch -> "$field ~= ${this.query.pattern.toConditionString(type)}"
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
}