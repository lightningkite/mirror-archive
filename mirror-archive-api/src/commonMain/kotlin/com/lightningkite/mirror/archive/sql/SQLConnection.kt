package com.lightningkite.mirror.archive.sql

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.info.Type

interface SQLConnection {
    suspend fun execute(sql: SQLQuery): Sequence<List<Any?>>
    /**
     * Returns results of the final query
     */
    suspend fun executeBatch(sql: List<SQLQuery>): Sequence<List<Any?>>
    suspend fun reflect(schema: String, table: String): Table?
}