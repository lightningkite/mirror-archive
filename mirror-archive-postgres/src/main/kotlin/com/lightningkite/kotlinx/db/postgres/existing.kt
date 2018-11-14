package com.lightningkite.kotlinx.db.postgres

import io.reactiverse.pgclient.PgClient


suspend fun PgClient.recreate(schemaName: String, tableName: String): Table? {
    val columns = suspendQuery("""
        SELECT
        columns.column_name,
        columns.data_type,
        columns.character_maximum_length
        FROM information_schema.columns as columns
        WHERE columns.table_schema = '${schemaName.toLowerCase()}' AND columns.table_name = '${tableName.toLowerCase()}';
        """.trimIndent()
    ).map {
        Column(
                name = it.getString("column_name"),
                type = it.getString("data_type"),
                size = it.getInteger("character_maximum_length")
        )
    }
    if(columns.isEmpty()) return null
    val constraints = suspendQuery("""
        SELECT
        usage.constraint_name,
        usage.column_name,
        constraints.constraint_type,
        target.column_name as target_column,
        target.table_name as target_table,
        target.table_schema as target_schema
        FROM information_schema.constraint_column_usage as usage
        LEFT JOIN information_schema.table_constraints as constraints ON constraints.constraint_name = usage.constraint_name
        LEFT JOIN information_schema.constraint_column_usage as target ON target.constraint_name = constraints.constraint_name
        WHERE usage.table_schema = '${schemaName.toLowerCase()}' AND usage.table_name = '${tableName.toLowerCase()}';
        """.trimIndent()
    )
            .asSequence()
            .mapNotNull {
                Constraint(
                        type = it.getString("constraint_type").let {
                            when (it) {
                                "PRIMARY KEY" -> Constraint.Type.PrimaryKey
                                "FOREIGN KEY" -> Constraint.Type.ForeignKey
                                "UNIQUE" -> Constraint.Type.Unique
                                else -> return@mapNotNull null
                            }
                        },
                        columns = listOf(it.getString("column_name")),
                        otherSchema = it.getString("target_schema"),
                        otherTable = it.getString("target_table"),
                        otherColumns = listOf(it.getString("target_column"))
                )
            }
            .groupBy { it.name }
            .values
            .map {
                it.first().copy(
                        columns = it.map { it.columns.first() },
                        otherColumns = it.map { it.otherColumns.first() }
                )
            }

    val indexes = suspendQuery("""
        SELECT indexdef
        FROM pg_indexes
        WHERE pg_indexes.schemaname = '${schemaName.toLowerCase()}' AND pg_indexes.tablename = '${tableName.toLowerCase()}'
    """.trimIndent())
            .mapNotNull { Index.parse(it.getString(0)) }

    //Check columns for serial
    columns.find { it.name == "id" }?.let{
        if(it.type.endsWith("int", true)){
            it.type = it.type.toLowerCase().removeSuffix("int").plus("serial")
        }
    }

    return Table(
            schemaName = schemaName,
            name = tableName,
            columns = columns,
            constraints = constraints,
            indexes = indexes
    )
}
