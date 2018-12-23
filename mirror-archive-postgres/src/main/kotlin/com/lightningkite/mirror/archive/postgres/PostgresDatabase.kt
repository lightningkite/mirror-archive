package com.lightningkite.mirror.archive.postgres

import com.lightningkite.mirror.archive.*
import com.lightningkite.mirror.archive.database.SuspendMap
import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.info.FieldInfo
import com.lightningkite.mirror.info.localName
import com.lightningkite.mirror.serialization.SerializationRegistry
import io.reactiverse.pgclient.PgPool
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.NoSuchElementException
import kotlin.reflect.KClass

class PostgresSuspendMap<T: HasId>(
        val pool: PgPool,
        val classInfo: ClassInfo<T>,
        val serializer: PostgresSerializer,
        val schemaName: String = "main",
        val tableName: String = classInfo.localName
): SuspendMap<Id, T> {

    val table = serializer.table(classInfo)
    val columnStrings = table.columns.map { it.name }.joinToString(", ")
    var isStarted = false
    suspend fun startup() {
        if (isStarted) return
        val oldTable = pool.recreate(schemaName, tableName)
        if (oldTable == null) {
            table.toCreateSql().forEach {
                pool.suspendQuery(it)
            }
        } else {
            table.toMigrateSql(oldTable).forEach {
                pool.suspendQuery(it)
            }
        }
        isStarted = true
    }


    override suspend fun getNewKey(): Id = Id.key()

    override suspend fun get(key: Id): T? {
        startup()
        pool.suspendQuery("SELECT $columnStrings FROM $tableName WHERE id = ${key.sqlLiteral(serializer)}").forEach {
            return serializer.readRow(classInfo.kClass, it)
        }
        return null
    }

    override suspend fun getMany(keys: Iterable<Id>): Map<Id, T?> {
        startup()
        return pool.suspendQuery("SELECT $columnStrings FROM $tableName WHERE id IN (${keys.joinToString { it.sqlLiteral(serializer) }})").associate {
            val result = serializer.readRow(classInfo.kClass, it)
            result.id to result
        }
    }

    override suspend fun put(key: Id, value: T, conditionIfExists: Condition<T>, create: Boolean): Boolean {
        startup()
        pool.suspendQuery("UPDATE $tableName SET $modificationsString WHERE id = ${id.sqlLiteral(serializer)} RETURNING $columnStrings").forEach {
            return serializer.readRow(classInfo.kClass, it)
        }
        throw IndexOutOfBoundsException("Id $id could not be found in the database.")
    }

    override suspend fun modify(key: Id, operation: Operation<T>, condition: Condition<T>): T? {
        return super.modify(key, operation, condition)
    }

    override suspend fun remove(key: Id, condition: Condition<T>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun query(condition: Condition<T>, sortedBy: Sort<T>, after: T?, count: Int): List<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class PostgresDatabase(
        val pool: PgPool,
        override val registry: SerializationRegistry,
        val serializer: PostgresSerializer = PostgresSerializer(registry = registry)
) : Database {
    val <T : Any> KClass<T>.info get() = registry.classInfoRegistry[this]!!

    override fun <T : HasId> table(type: KClass<T>, name: String): Database.Table<T> {
        return Table(type.info, serializer.schema, name, pool, serializer)
    }

    class Table<T : HasId>(
            override val classInfo: ClassInfo<T>,
            val schemaName: String,
            val tableName: String,
            val pool: PgPool,
            val serializer: PostgresSerializer
    ) : Database.Table<T> {

        val table = serializer.table(classInfo)
        val columnStrings = table.columns.map { it.name }.joinToString(", ")

        var isStarted = false
        suspend fun startup() {
            if (isStarted) return
            val oldTable = pool.recreate(schemaName, tableName)
            if (oldTable == null) {
                table.toCreateSql().forEach {
                    pool.suspendQuery(it)
                }
            } else {
                table.toMigrateSql(oldTable).forEach {
                    pool.suspendQuery(it)
                }
            }
            isStarted = true
        }

        override suspend fun get(id: Id): T? {
            startup()
            pool.suspendQuery("SELECT $columnStrings FROM $tableName WHERE id = ${id.sqlLiteral(serializer)}").forEach {
                return serializer.readRow(classInfo.kClass, it)
            }
            return null
        }

        override suspend fun getMany(ids: Iterable<Id>): Map<Id, T> {
            startup()
            return pool.suspendQuery("SELECT $columnStrings FROM $tableName WHERE id IN (${ids.joinToString { it.sqlLiteral(serializer) }})").associate {
                val result = serializer.readRow(classInfo.kClass, it)
                result.id to result
            }
        }

        override suspend fun insert(model: T): T {
            startup()
            val row = serializer.writeRow(classInfo.kClass, model)
            pool.suspendQuery("INSERT INTO $tableName ($columnStrings) VALUES ($row) RETURNING $columnStrings").forEach {
                return serializer.readRow(classInfo.kClass, it)
            }
            throw IndexOutOfBoundsException("Id ${model.id} could not be found in the database.")
        }

        override suspend fun insertMany(models: Collection<T>): Collection<T> {
            startup()
            val rows = models.joinToString { serializer.writeRow(classInfo.kClass, it) }
            return pool.suspendQuery("INSERT INTO $tableName ($columnStrings) VALUES $rows RETURNING $columnStrings").map {
                serializer.readRow(classInfo.kClass, it)
            }
        }

        override suspend fun update(model: T): T {
            startup()
            val row = serializer.writeRow(classInfo.kClass, model)
            pool.suspendQuery("INSERT INTO $tableName ($columnStrings) VALUES ($row) ON CONFLICT (id) DO UPDATE SET id = EXCLUDED.id")
            return model
        }

        override suspend fun modify(id: Id, modifications: List<ModificationOnItem<T, *>>): T {
            startup()
            val modificationsString = modifications.joinToString { it.sql(serializer) }
            pool.suspendQuery("UPDATE $tableName SET $modificationsString WHERE id = ${id.sqlLiteral(serializer)} RETURNING $columnStrings").forEach {
                return serializer.readRow(classInfo.kClass, it)
            }
            throw IndexOutOfBoundsException("Id $id could not be found in the database.")
        }

        override suspend fun query(
                condition: ConditionOnItem<T>,
                sortedBy: List<SortOnItem<T, *>>,
                continuationToken: String?,
                count: Int
        ): QueryResult<T> {
            startup()
            @Suppress("UNCHECKED_CAST") val actualSortedBy = if (sortedBy.isEmpty()) listOf(SortOnItem(
                    classInfo.primaryKey() as FieldInfo<T, Comparable<Comparable<*>>>,
                    true,
                    false
            )) else sortedBy
            val queryString = if (continuationToken == null) condition.sql(serializer) else "(${condition.sql(serializer)} AND ${decompress(continuationToken)})"
            val orderByString = if (sortedBy.isEmpty()) "ORDER BY id" else actualSortedBy.joinToString(", ", "ORDER BY ", "") {
                it.sql()
            }
            val query = """
            SELECT $columnStrings
            FROM $tableName
            WHERE $queryString
            $orderByString
            LIMIT $count
            """.trimIndent()
            val results = pool.suspendQuery(query).map {
                serializer.readRow(classInfo.kClass, it)
            }
            val newToken = if (results.size == count) compress(after(results.last(), actualSortedBy)) else null
            return QueryResult(
                    results = results,
                    continuationToken = newToken
            )
        }

        override suspend fun delete(id: Id) {
            startup()
            val deletedCount = pool.suspendQuery("DELETE FROM $tableName WHERE id = ${id.sqlLiteral(serializer)}").rowCount()
            if (deletedCount == 0) throw NoSuchElementException("Id $id could not be found in the database.")
        }

        private fun after(element: T, useSorts: List<SortOnItem<T, *>>): String {
            return useSorts.indices.joinToString {
                val pieces = useSorts.subList(0, it).map {
                    it.field.name + " = " + it.field.get(element).sqlLiteral(serializer)
                } + useSorts[it].let {
                    val op = if (it.ascending) " > " else " < "
                    it.field.name + op + it.field.get(element).sqlLiteral(serializer)
                }

                if (pieces.size > 1)
                    pieces.joinToString(" OR ", "(", ")")
                else
                    pieces.first()
            }
        }

        fun compress(string: String): String {
            val output = ByteArrayOutputStream()
            GZIPOutputStream(output).use {
                it.write(string.toByteArray(Charsets.UTF_8))
            }
            return Base64.getEncoder().encodeToString(output.toByteArray())
        }

        fun decompress(string: String): String {
            return GZIPInputStream(ByteArrayInputStream(Base64.getDecoder().decode(string))).use {
                it.reader().readText()
            }
        }
    }
}