package com.lightningkite.kotlinx.db.postgres

data class Index(
        var name: String,
        var columns: List<String>,
        var unique: Boolean = false,
        var usingMethod: String = "btree"
) {
    fun toCreateSql(table: Table): String = toString(table.schemaName + "." + table.name)

    fun toString(tableId: String): String = (if(unique){
        "CREATE UNIQUE INDEX IF NOT EXISTS "
    } else {
        "CREATE INDEX IF NOT EXISTS "
    }) + "$name ON $tableId USING $usingMethod (${columns.joinToString()})"

    override fun toString(): String = toString("")

    fun toLowerCase() {
        name = name.toLowerCase()
        columns = columns.map { it.toLowerCase() }
        usingMethod = usingMethod.toLowerCase()
    }

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

    override fun equals(other: Any?): Boolean = toString() == (other as? Constraint)?.toSql()
    override fun hashCode(): Int = toString().hashCode()
}
