package com.lightningkite.mirror.archive.postgres

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.sql.*
import com.lightningkite.mirror.info.Type
import io.reactiverse.pgclient.PgPool

class PostgresQuerySerializer(override val serializer: PostgresSerializer) : SQLQuerySerializer(serializer) {
    suspend fun reflect(schema: String, table: String, pool: PgPool): Table? {
        val columns = reflectColumns(schema, table) { pool.suspendQuery(it).map { it.asList() } }
        if (columns.isEmpty()) return null
        val constraints = reflectConstraints(schema, table) { pool.suspendQuery(it).map { it.asList() } }
        val indexes = pool.suspendQuery("""
        SELECT indexdef
        FROM pg_indexes
        WHERE pg_indexes.schemaname = '${schema.toLowerCase()}' AND pg_indexes.tablename = '${table.toLowerCase()}'
        """.trimIndent())
                .mapNotNull { Index.parse(it.getString(0)) }

        //Check columns for serial
        columns.find { it.name == "id" }?.let {
            if (it.type.endsWith("int", true)) {
                it.type = it.type.toLowerCase().removeSuffix("int").plus("serial")
            }
        }

        return Table(
                schemaName = schema,
                name = table,
                columns = columns,
                constraints = constraints,
                indexes = indexes
        )
    }

    fun upsert(
            table: Table,
            type: Type<*>,
            writeColumns: SQLQuery.Builder.() -> Unit,
            writeValues: SQLQuery.Builder.() -> Unit,
            condition: Condition<*>? = null
    ): SQLQuery {
        return SQLQuery.build {
            sql.append("INSERT INTO ")
            sql.append(table.fullName)
            sql.append(" (")
            writeColumns()
            sql.append(") VALUES (")
            writeValues()
            sql.append(")")
            sql.append(" ON CONFLICT ON CONSTRAINT ")
            sql.append(table.constraints.find { it.type == Constraint.Type.PrimaryKey }!!.name)
            sql.append(" DO UPDATE SET ")
            operation(Operation.Multiple(listOf(
                    operation(Operation.Set(), type)
            )), type)
            if (condition != null) {
                sql.append(" WHERE ")
                //TODO: Mod condition to use 'excluded.'
                sql.append(condition)
            }
            sql.append(" RETURNING ${table.columns.first().name}")
        }
    }
}