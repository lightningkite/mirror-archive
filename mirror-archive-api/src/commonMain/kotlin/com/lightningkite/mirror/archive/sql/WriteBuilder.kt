package com.lightningkite.mirror.archive.sql

data class WriteBuilder(
        var into: String = "table",
        val columns: ArrayList<Column> = ArrayList(),
        val values: ArrayList<String> = ArrayList(),
        var where: String = "TRUE",
        val subwrites: ArrayList<WriteBuilder> = ArrayList()
) {
    //SQLite and Postgres: INSERT INTO $into($columns) VALUES($values) ON CONFLICT(id) DO UPDATE SET $colvals WHERE $where
}