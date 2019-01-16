package com.lightningkite.mirror.archive.postgres

import com.lightningkite.mirror.archive.sql.Index
import com.lightningkite.mirror.archive.sql.SQLQuerySerializer
import com.lightningkite.mirror.archive.sql.Table
import io.reactiverse.pgclient.PgPool

class PostgresQuerySerializer(override val serializer: PostgresSerializer) : SQLQuerySerializer(serializer) {
    suspend fun reflect(schema: String, table: String, pool: PgPool): Table? {
        val columns = reflectColumns(schema, table){ pool.suspendQuery(it).map { it.asList() } }
        if(columns.isEmpty()) return null
        val constraints = reflectConstraints(schema, table){ pool.suspendQuery(it).map { it.asList() } }
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
}