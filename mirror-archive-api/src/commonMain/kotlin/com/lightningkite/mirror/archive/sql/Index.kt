package com.lightningkite.mirror.archive.sql

data class Index(
        var name: String,
        var columns: List<String>,
        var unique: Boolean = false,
        var usingMethod: String = "btree"
) {
    companion object {
        fun parse(sql: String): Index {
            return Index(
                    name = sql.substringAfter("INDEX").trim().substringBefore(' '),
                    columns = sql.substringAfterLast('(').substringBeforeLast(')').split(',').map { it.trim() },
                    unique = sql.contains("UNIQUE", true),
                    usingMethod = sql.substringAfter("USING").trim().substringBefore(' ')
            )
        }
    }
}
