package com.lightningkite.mirror.archive.sql

import com.lightningkite.kommon.collection.contentEquals

data class Table(
        var schemaName: String = "public",
        var name: String,
        var columns: List<Column>,
        var constraints: List<Constraint>,
        var indexes: List<Index> = listOf()
) {
    init {
        schemaName = schemaName.toLowerCase()
        name = name.toLowerCase()
    }

    val fullName: String get() = "$schemaName.$name"

    fun isBuiltInIndex(index: Index): Boolean {
        if (constraints.any { it.columns contentEquals index.columns }) {
            //Index was generated by constraint
            return true
        }
        return false
    }

//    fun toCreateSql(): List<String> = listOf("CREATE TABLE IF NOT EXISTS $schemaName.$name (${(columns + constraints).joinToString {
//        when (it) {
//            is Column -> it.toSql()
//            is Constraint -> it.toSql()
//            else -> throw IllegalArgumentException()
//        }
//    }})") + indexes.map { it.toCreateSql(this) }
//
//    fun toMigrateSql(old: Table, dropColumns: Boolean = false): List<String> {
//
//        val changes = ArrayList<String>()
//
//        val alterTableChanges = ArrayList<String>()
//
//        run {
//            val myColumns = columns.associate { it.name to it }
//            val oldColumns = old.columns.associate { it.name to it }
//
//            val newColumnsKeys = myColumns.keys - oldColumns.keys
//            val deadColumnsKeys = oldColumns.keys - myColumns.keys
//            val sameColumnsKeys = myColumns.keys intersect oldColumns.keys
//
//            for (key in newColumnsKeys) {
//                alterTableChanges += "ADD COLUMN ${myColumns[key]!!.toSql()}"
//            }
//            for (key in deadColumnsKeys) {
//                if (dropColumns) {
//                    alterTableChanges += "DROP COLUMN $key"
//                }
//            }
//            for (key in sameColumnsKeys) {
//                val mine = myColumns[key]!!
//                val other = oldColumns[key]!!
//                if (mine != other) {
//                    throw IllegalStateException("Column $key is trying to change types; this is not currently supported.\n$other\nmigrate to\n$mine")
//                }
//            }
//        }
//
//        run {
//            val myConstraints = constraints.associate { it.name to it }
//            val oldConstraints = old.constraints.associate { it.name to it }
//
//            val newConstraintKeys = myConstraints.keys - oldConstraints.keys
//            val deadConstraintKeys = oldConstraints.keys - myConstraints.keys
//            val sameConstraintKeys = myConstraints.keys intersect oldConstraints.keys
//
//            for (key in newConstraintKeys) {
//                alterTableChanges += "ADD ${myConstraints[key]!!.toSql()}"
//            }
//            for (key in deadConstraintKeys) {
//                if (dropColumns) {
//                    alterTableChanges += "DROP CONSTRAINT $key"
//                }
//            }
//            for (key in sameConstraintKeys) {
//                val mine = myConstraints[key]!!
//                val other = oldConstraints[key]!!
//                if (mine != other) {
//                    throw IllegalStateException("Constraint $key is trying to change a constraint; this is not currently supported.\n$other\nmigrate to\n$mine")
//                }
//            }
//        }
//
//        if(alterTableChanges.isNotEmpty()) {
//            changes += "ALTER TABLE $schemaName.$name ${alterTableChanges.joinToString(", ")}"
//        }
//
//        run {
//            val myIndexes = indexes.associate { it.name to it }
//            val oldIndexes = old.indexes.associate { it.name to it }
//
//            val newIndexKeys = myIndexes.keys - oldIndexes.keys
//            val deadIndexKeys = oldIndexes.keys - myIndexes.keys
//            val sameIndexKeys = myIndexes.keys intersect oldIndexes.keys
//
//            for (key in newIndexKeys) {
//                changes += myIndexes[key]!!.toCreateSql(this)
//            }
//            for (key in deadIndexKeys) {
//                if (!isBuiltInIndex(oldIndexes[key]!!)) {
//                    changes += "DROP INDEX $key"
//                }
//            }
//            for (key in sameIndexKeys) {
//                val mine = myIndexes[key]!!
//                val other = oldIndexes[key]!!
//                if (mine != other) {
//                    throw IllegalStateException("Index $key is trying to change an createIndex; this is not currently supported.\n$other\nmigrate to\n$mine")
//                }
//            }
//        }
//
//        return changes
//    }
}
