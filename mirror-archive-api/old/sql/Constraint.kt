package com.lightningkite.mirror.archive.sql

data class Constraint(
        var type: Type = Type.PrimaryKey,
        var columns: List<String> = listOf(),
        var name: String
) {
    init{
        columns = columns.map { it.toLowerCase() }
        name.toLowerCase()
    }

    sealed class Type {
        abstract val name: String
        object PrimaryKey: Type(){
            override val name: String
                get() = "PrimaryKey"
        }
        object Unique: Type(){
            override val name: String
                get() = "Unique"
        }
        data class ForeignKey(
                var otherSchema: String,
                var otherTable: String,
                var otherColumns: List<String>
        ): Type(){
            override val name: String
                get() = "ForeignKey"
            init{
                otherSchema.toLowerCase()
                otherTable.toLowerCase()
                otherColumns = otherColumns.map { it.toLowerCase() }
            }
        }
    }


}