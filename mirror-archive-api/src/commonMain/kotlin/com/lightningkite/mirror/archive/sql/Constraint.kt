package com.lightningkite.mirror.archive.sql

data class Constraint(
        var type: Type = Type.PrimaryKey,
        var columns: List<String> = listOf(),
        var otherSchema: String? = null,
        var otherTable: String? = null,
        var otherColumns: List<String> = columns,
        var name: String = columns.joinToString("_") + "_" + type.name
) {

    enum class Type {
        PrimaryKey, ForeignKey, Unique
    }
}