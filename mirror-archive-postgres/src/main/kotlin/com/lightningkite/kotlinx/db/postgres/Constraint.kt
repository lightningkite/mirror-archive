package com.lightningkite.kotlinx.db.postgres

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

    fun toSql() = when (type) {
        Type.PrimaryKey -> "CONSTRAINT $name PRIMARY KEY (${columns.joinToString()})"
        Type.ForeignKey -> "CONSTRAINT $name FOREIGN KEY (${columns.joinToString()}) REFERENCES $otherSchema.$otherTable (${otherColumns.joinToString()}) ON DELETE SET NULL"
        Type.Unique -> "CONSTRAINT $name UNIQUE (${columns.joinToString()})"
    }

    fun toLowerCase() {
        columns = columns.map { it.toLowerCase() }
        otherSchema = otherSchema?.toLowerCase()
        otherTable = otherTable?.toLowerCase()
        otherColumns = otherColumns.map { it.toLowerCase() }
        name = name.toLowerCase()
    }

    override fun equals(other: Any?): Boolean = toSql() == (other as? Constraint)?.toSql()
    override fun hashCode(): Int = toSql().hashCode()
}