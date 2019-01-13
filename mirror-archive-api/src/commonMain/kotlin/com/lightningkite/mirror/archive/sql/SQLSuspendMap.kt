package com.lightningkite.mirror.archive.sql

import com.lightningkite.mirror.archive.database.SuspendMap
import com.lightningkite.mirror.archive.database.SuspendMapProvider
import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.info.*
import com.lightningkite.mirror.serialization.TypeDecoder

class SQLSuspendMap<K, V : Any>(
        val schema: String = "generatedschema",
        val tableName: String,
        val connection: SQLConnection,
        val serializer: SQLSerializer,
        val keyType: Type<K>,
        val valueType: Type<V>,
        val generateKey: () -> K
) : SuspendMap<K, V> {

    class Provider(val serializer: SQLSerializer, val connection: SQLConnection) : SuspendMapProvider {
        override fun <K, V : Any> suspendMap(key: Type<K>, value: Type<V>, name: String?): SQLSuspendMap<K, V> {
            val tableName = serializer.registry.kClassToExternalNameRegistry[key.kClass] + "_to_" + serializer.registry.kClassToExternalNameRegistry[value.kClass]
            @Suppress("UNCHECKED_CAST")
            return SQLSuspendMap(
                    tableName = name ?: tableName,
                    connection = connection,
                    serializer = serializer,
                    keyType = key,
                    valueType = value,
                    generateKey = { throw UnsupportedOperationException() }
            )
        }

    }

    val virtualKeyField = FieldInfo<Any, K>(AnyClassInfo, "key", keyType, false, { throw UnsupportedOperationException() }, listOf())
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
        val old = connection.reflect(schema, tableName)
        if (old == null) {
            println("Creating table...")
            serializer.createTable(table).forEach { connection.execute(it) }
        } else {
            println("Migrating table...")
            println("Old: $old")
            println("New: $table")
            serializer.migrate(old, table).forEach { connection.execute(it) }
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
        return connection.execute(serializer.select(
                table = table,
                columns = valueTablePartial.columns,
                where = serializer.convertToSQLCondition(Condition.Field(virtualKeyField, Condition.Equal(key)), valueType),
                limit = 1
        )).firstOrNull()?.let {
            valueDecoder.invoke(SQLSerializer.RowReader(it))
        }
    }

    override suspend fun getMany(keys: List<K>): Map<K, V?> {
        checkSetup()
        return connection.execute(serializer.select(
                table = table,
                where = serializer.convertToSQLCondition(Condition.Field(virtualKeyField, Condition.EqualToOne(keys)), valueType),
                limit = 1
        )).associate {
            decoder.invoke(SQLSerializer.RowReader(it)).run{ key to value }
        }
    }

    override suspend fun put(key: K, value: V, conditionIfExists: Condition<V>, create: Boolean): Boolean {
        checkSetup()
        val values = ArrayList<String>().also {
            serializer.encode(it, key, keyType)
            serializer.encode(it, value, valueType)
        }
        if (create) {
            if (conditionIfExists is Condition.Never) {
                return try {
                    connection.execute(serializer.insert(table, values)).any()
                } catch (e: Exception) {
                    false
                }
            } else {
                val upsertSpecial = serializer.upsert(
                        table = table,
                        values = values,
                        condition = serializer.convertToSQLCondition(conditionIfExists, valueType)
                )
                if (upsertSpecial != null) {
                    return connection.execute(upsertSpecial).also { println("GOT: " + it.joinToString()) }.any()
                } else {
                    val insertSucceeded = try {
                        connection.execute(serializer.insert(table, values)).any()
                    } catch (e: Exception) {
                        false
                    }
                    if (insertSucceeded) {
                        return true
                    } else {
                        return connection.execute(serializer.update(
                                table = table,
                                values = values,
                                condition = serializer.convertToSQLCondition(Condition.Field(virtualKeyField, Condition.Equal(key)) and conditionIfExists, valueType)
                        )).any()
                    }
                }
            }
        } else {
            return connection.execute(serializer.update(
                    table = table,
                    values = values,
                    condition = serializer.convertToSQLCondition(Condition.Field(virtualKeyField, Condition.Equal(key)) and conditionIfExists, valueType)
            )).any()
        }
    }

    override suspend fun modify(key: K, operation: Operation<V>, condition: Condition<V>): V? {
        checkSetup()
        val updateReturning = serializer.updateModifyReturning(
                table = table,
                modifications = serializer.convertToSQLSet(operation, valueType),
                condition = serializer.convertToSQLCondition(Condition.Field(virtualKeyField, Condition.Equal(key)) and condition, valueType)
        )
        if (updateReturning != null) {
            return connection.execute(updateReturning).toList().also { println("MOD RESULT: " + it.joinToString()) }.firstOrNull()?.let {
                valueDecoder.invoke(SQLSerializer.RowReader(it))
            }
        } else {
            val c = serializer.convertToSQLCondition(Condition.Field(virtualKeyField, Condition.Equal(key)) and condition, valueType)
            return connection.executeBatch(listOf(
                    serializer.updateModify(
                            table = table,
                            modifications = serializer.convertToSQLSet(operation, valueType),
                            condition = c
                    ),
                    serializer.select(
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
        return connection.execute(serializer.delete(
                table = table,
                condition = serializer.convertToSQLCondition(Condition.Field(virtualKeyField, Condition.Equal(key)) and condition, valueType)
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
        val sqlCondition = serializer.convertToSQLCondition(
                extendedCondition and Condition.Field(virtualKeyField, keyCondition),
                valueType
        )
        return connection.execute(serializer.select(
                table = table,
                where = sqlCondition,
                orderBy = sortedBy?.toColumnList() ?: keyTablePartial.columns,
                limit = count
        )).map { decoder.invoke(SQLSerializer.RowReader(it)) }.toList()
    }
}