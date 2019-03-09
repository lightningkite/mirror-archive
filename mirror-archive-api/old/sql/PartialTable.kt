package com.lightningkite.mirror.archive.sql

data class PartialTable(
        var columns: List<Column> = listOf(),
        var constraints: List<Constraint> = listOf(),
        var indexes: List<Index> = listOf()
)