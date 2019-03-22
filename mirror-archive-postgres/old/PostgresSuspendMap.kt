package com.lightningkite.mirror.archive.postgres

import com.lightningkite.mirror.archive.database.SuspendMap
import com.lightningkite.mirror.archive.database.SuspendMapProvider
import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.archive.model.and
import com.lightningkite.mirror.archive.sql.*
import com.lightningkite.mirror.info.AnyClassInfo
import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.info.FieldInfo
import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.serialization.TypeDecoder
import io.reactiverse.pgclient.PgPool

class PostgresSuspendMap<K, V : Any>(
        val schema: String = "generatedschema",
        val tableName: String,
        val connection: PgPool,
        val querySerializer: PostgresQuerySerializer,
        val keyType: Type<K>,
        val valueType: Type<V>,
        val generateKey: () -> K
) : SuspendMap<K, V> {

    val serializer = querySerializer.serializer

    class Provider(val querySerializer: PostgresQuerySerializer, val connection: PgPool) : SuspendMapProvider {
        override fun <K, V : Any> suspendMap(key: Type<K>, value: Type<V>, name: String?): PostgresSuspendMap<K, V> {
            val tableName = querySerializer.serializer.registry.kClassToExternalNameRegistry[key.kClass] + "_to_" + querySerializer.serializer.registry.kClassToExternalNameRegistry[value.kClass]
            @Suppress("UNCHECKED_CAST")
            return PostgresSuspendMap(
                    tableName = name ?: tableName,
                    connection = connection,
                    querySerializer = querySerializer,
                    keyType = key,
                    valueType = value,
                    generateKey = { throw UnsupportedOperationException() }
            )
        }

    }

    val virtualKeyField = FieldInfo<V, K>(
            owner = serializer.registry.classInfoRegistry[valueType.kClass]!! as ClassInfo<V>,
            name = "key",
            type = keyType,
            isOptional = false,
            get = { throw UnsupportedOperationException() },
            annotations = listOf()
    )
    val keyTablePartial = serializer.definition(keyType).let {
        it.copy(columns = it.columns.map { it.copy(name = "key" nameAppend it.name) })
    }
    val valueTablePartial = serializer.definition(valueType).let {
        it.copy(columns = it.columns.map { it.noNameToValue() })
    }
    val table = Table(
            schemaName = schema,
            name = tableName,
            columns = keyTablePartial.columns + valueTablePartial.columns,
            constraints = keyTablePartial.constraints + valueTablePartial.constraints + Constraint(
                    type = Constraint.Type.PrimaryKey,
                    columns = keyTablePartial.columns.map { it.name },
                    name = "primary_key"
            ),
            indexes = keyTablePartial.indexes + valueTablePartial.indexes
    )

    val keyDecoder = serializer.decoder(keyType)
    val valueDecoder = serializer.decoder(valueType)
    val decoder: TypeDecoder<SQLSerializer.RowReader, SuspendMap.Entry<K, V>> = {
        SuspendMap.Entry(keyDecoder(this), valueDecoder(this))
    }

    var isSetUp = false
    suspend fun setup() {
        val old = querySerializer.reflect(schema, tableName, connection)
        if (old == null) {
            println("Creating table...")
            querySerializer.createTable(table).forEach { connection.suspendQuery(it) }
        } else {
            println("Migrating table...")
            println("Old: $old")
            println("New: $table")
            querySerializer.migrate(old, table).forEach { connection.suspendQuery(it) }
        }
        isSetUp = true
    }

    suspend fun checkSetup() {
        if (isSetUp) return
        else setup()
    }

    override suspend fun getNewKey(): K = generateKey()

    override suspend fun get(key: K): V? {
        checkSetup()
        return connection.suspendQuery(querySerializer.select(
                table = table,
                type = valueType,
                columns = valueTablePartial.columns,
                orderBy = null,
                limit = 1
        )).firstOrNull()?.let {
            valueDecoder.invoke(SQLSerializer.RowReader(it.asList()))
        }
    }

    override suspend fun getMany(keys: List<K>): Map<K, V?> {
        checkSetup()
        return connection.suspendQuery(querySerializer.select(
                table = table,
                type = valueType,
                where = Condition.Field(virtualKeyField, Condition.EqualToOne(keys)),
                orderBy = null,
                limit = 1
        )).associate {
            decoder.invoke(SQLSerializer.RowReader(it.asList())).run { key to value }
        }
    }

    override suspend fun put(key: K, value: V, conditionIfExists: Condition<V>, create: Boolean): Boolean {
        checkSetup()
        if (create) {
            if (conditionIfExists is Condition.Never) {
                return try {
                    connection.suspendQuery(querySerializer.insert(table, writeColumns = {
                        for (keyColumn in keyTablePartial.columns) {
                            sql.append(keyColumn.name)
                        }
                        for (valueColumn in valueTablePartial.columns) {
                            sql.append(valueColumn.name)
                        }
                    }, writeValues = {
                        serializer.encode(this, key, keyType)
                        serializer.encode(this, value, valueType)
                    })).any()
                } catch (e: Exception) {
                    false
                }
            } else {
                return connection.suspendQuery(querySerializer.upsert(
                        table = table,
                        type = valueType,
                        condition = (Condition.Field(virtualKeyField, Condition.Equal(key)) and conditionIfExists)
                )).any()
            }
        } else {
            return connection.suspendQuery(querySerializer.update(
                    table = table,
                    type = valueType,
                    condition = (Condition.Field(virtualKeyField, Condition.Equal(key)) and conditionIfExists)
            )).any()
        }
    }

    override suspend fun modify(key: K, operation: Operation<V>, condition: Condition<V>): V? {
        checkSetup()
        val updateReturning = querySerializer.updateModifyReturning(
                table = table,
                modifications = querySerializer.convertToSQLSet(operation, valueType),
                condition = querySerializer.convertToSQLCondition(Condition.Field(virtualKeyField, Condition.Equal(key)) and condition, valueType)
        )
        if (updateReturning != null) {
            return connection.suspendQuery(updateReturning).toList().also { println("MOD RESULT: " + it.joinToString()) }.firstOrNull()?.let {
                valueDecoder.invoke(SQLSerializer.RowReader(it))
            }
        } else {
            val c = querySerializer.convertToSQLCondition(Condition.Field(virtualKeyField, Condition.Equal(key)) and condition, valueType)
            return connection.suspendQueryBatch(listOf(
                    querySerializer.updateModify(
                            table = table,
                            modifications = querySerializer.convertToSQLSet(operation, valueType),
                            condition = c
                    ),
                    querySerializer.select(
                            table = table,
                            columns = valueTablePartial.columns,
                            where = c,
                            limit = 1
                    )
            )).firstOrNull()?.let {
                valueDecoder.invoke(SQLSerializer.RowReader(it))
            }
        }
    }

    override suspend fun remove(key: K, condition: Condition<V>): Boolean {
        checkSetup()
        return connection.suspendQuery(querySerializer.delete(
                table = table,
                condition = querySerializer.convertToSQLCondition(Condition.Field(virtualKeyField, Condition.Equal(key)) and condition, valueType)
        )).any()
    }

    fun Sort<V>.toColumnList(): List<Column> = when (this) {
        is Sort.Field<*, *> -> listOf(table.columns.find { it.name == this.field.name }!!)
        is Sort.Multi -> this.comparators.flatMap { it.toColumnList() }
        else -> throw IllegalArgumentException()
    }

    override suspend fun query(
            condition: Condition<V>,
            keyCondition: Condition<K>,
            sortedBy: Sort<V>?,
            after: SuspendMap.Entry<K, V>?,
            count: Int
    ): List<SuspendMap.Entry<K, V>> {
        checkSetup()
        @Suppress("UNCHECKED_CAST") val extendedCondition = if (after != null)
            (sortedBy?.after(after.value)
                    ?: Condition.Field(virtualKeyField as FieldInfo<Any, Comparable<Any?>>, Condition.GreaterThan(after.key as Comparable<Any?>))) and condition
        else condition
        val sqlCondition = querySerializer.convertToSQLCondition(
                extendedCondition and Condition.Field(virtualKeyField, keyCondition),
                valueType
        )
        return connection.suspendQuery(querySerializer.select(
                table = table,
                where = sqlCondition,
                orderBy = sortedBy?.toColumnList() ?: keyTablePartial.columns,
                limit = count
        )).map { decoder.invoke(SQLSerializer.RowReader(it)) }.toList()
    }
}