package com.lightningkite.mirror.archive.sql

data class QueryBuilder(
        val select: ArrayList<String> = ArrayList(),
        var from: String = "table",
        val joins: ArrayList<Join> = ArrayList(),
        var where: String = "TRUE",
        val limit: Int = 1000
) {
    data class Join(
            val table: String,
            val condition: String
    )
}