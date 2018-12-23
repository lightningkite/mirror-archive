package com.lightningkite.mirror.archive.sql

import com.lightningkite.mirror.info.Type

interface SQLConnection {

    fun addConstraint(table: Table, constraint: Constraint): String
    fun removeConstraint(table: Table, constraint: Constraint): String

    fun addColumn(table: Table, column: Column): String
    fun removeColumn(table: Table, column: Column): String

    fun createTable(table: Table): String
    fun dropTable(table: Table): String

    fun query(queryBuilder: QueryBuilder): String
    fun update(writeBuilder: WriteBuilder): String
    fun upsert(writeBuilder: WriteBuilder): String

    fun writeValue(value: Any?): String

    suspend fun <T> execute(sql: String, type: Type<T>): ArrayList<T>
    suspend fun reflect(schema: String, table: String): Table

}