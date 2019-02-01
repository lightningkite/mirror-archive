package com.lightningkite.mirror.archive.sql

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.info.FieldInfo
import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.info.type

open class SQLQuerySerializer(open val serializer: SQLSerializer) {
    open fun SQLQuery.Builder.constraint(constraint: Constraint) = constraint.run {
        val type = type
        sql.append(when (type) {
            Constraint.Type.PrimaryKey -> "$name PRIMARY KEY (${columns.joinToString()})"
            Constraint.Type.Unique -> "$name UNIQUE (${columns.joinToString()})"
            is Constraint.Type.ForeignKey -> "$name FOREIGN KEY (${columns.joinToString()}) REFERENCES ${type.otherSchema}.${type.otherTable} (${type.otherColumns.joinToString()}) ON DELETE SET NULL"
        })
    }

    open fun SQLQuery.Builder.column(column: Column) = sql.append(column.run {
        if (size == null) "$name $type" else "$name $type($size)"
    })

    open fun createIndex(table: Table, index: Index): SQLQuery = SQLQuery((if (index.unique) {
        "CREATE UNIQUE INDEX IF NOT EXISTS "
    } else {
        "CREATE INDEX IF NOT EXISTS "
    }) + "${index.name} ON ${table.fullName} USING ${index.usingMethod} (${index.columns.joinToString()})")

    open fun dropIndex(index: Index): SQLQuery = SQLQuery("DROP INDEX ${index.name}")

    open fun addConstraint(table: Table, constraint: Constraint): SQLQuery = SQLQuery.build {
        sql.append("ALTER TABLE ${table.fullName} ADD CONSTRAINT ")
        constraint(constraint)
    }

    open fun removeConstraint(table: Table, constraint: Constraint): SQLQuery = SQLQuery.build {
        sql.append("ALTER TABLE ${table.fullName} DROP CONSTRAINT ${constraint.name}")
    }

    open fun addColumn(table: Table, column: Column): SQLQuery = SQLQuery.build {
        sql.append("ALTER TABLE ${table.fullName} ADD COLUMN ")
        column(column)
    }

    open fun removeColumn(table: Table, column: Column): SQLQuery {
        return SQLQuery("ALTER TABLE ${table.fullName} DROP COLUMN ${column.name}")
    }

    open fun createTable(table: Table): List<SQLQuery> = table.run {
        return listOf(
                SQLQuery("CREATE SCHEMA IF NOT EXISTS $schemaName "),
                SQLQuery.build {
                    sql.append("CREATE TABLE IF NOT EXISTS $schemaName.$name (")
                    for (column in columns) {
                        column(column)
                    }
                    for (constraint in constraints) {
                        constraint(constraint)
                    }
                    sql.append(")")
                }
        ) + indexes.map { createIndex(table, it) }
    }

    open fun dropTable(table: Table): SQLQuery = SQLQuery("DROP TABLE ${table.fullName}")

    open fun select(
            table: Table,
            columns: List<Column> = table.columns,
            where: Condition<*>? = null,
            type: Type<*>,
            orderBy: Sort<*>?,
            limit: Int?
    ): SQLQuery = SQLQuery.build {
        sql.append("SELECT ")
        sql.append(columns.joinToString { it.name })
        sql.append(" FROM ")
        sql.append(table.fullName)
        if (where != null) {
            sql.append(" WHERE ")
            condition(where, type)
        }
        if (orderBy != null) {
            sql.append(" ORDER BY ")
            sql.append(orderBy.toColumnList(table).joinToString { it.name })
        }
        if (limit != null) {
            sql.append(" LIMIT ")
            sql.append(limit.toString())
        }
    }

    open fun insert(
            table: Table,
            writeColumns: SQLQuery.Builder.() -> Unit,
            writeValues: SQLQuery.Builder.() -> Unit
    ): SQLQuery = SQLQuery.build {
        sql.append("INSERT INTO ")
        sql.append(table.fullName)
        sql.append(" (")
        writeColumns()
        sql.append(") VALUES (")
        writeValues()
        sql.append(")")
    }

    open fun update(
            table: Table,
            type: Type<*>,
            operation: Operation<*>,
            condition: Condition<*>? = null
    ): SQLQuery = SQLQuery.build {
        sql.append("UPDATE ")
        sql.append(table.fullName)
        sql.append(" SET ")
        operation(operation, type)
        if (condition != null) {
            sql.append(" WHERE ")
            condition(condition, type)
        }
    }

    open fun delete(
            table: Table,
            type: Type<*>,
            condition: Condition<*>
    ): SQLQuery = SQLQuery.build {
        sql.append("DELETE FROM ")
        sql.append(table.fullName)
        sql.append(" WHERE ")
        condition(condition, type)
    }

    open fun migrate(
            old: Table,
            table: Table,
            dropColumns: Boolean = false,
            dropIndicies: Boolean = false
    ): List<SQLQuery> {

        val statements = ArrayList<SQLQuery>()

        run {
            val myColumns = table.columns.associate { it.name to it }
            val oldColumns = old.columns.associate { it.name to it }

            val newColumnsKeys = myColumns.keys - oldColumns.keys
            val deadColumnsKeys = oldColumns.keys - myColumns.keys
            val sameColumnsKeys = myColumns.keys intersect oldColumns.keys

            for (key in newColumnsKeys) {
                statements.add(addColumn(table, myColumns[key]!!))
            }
            for (key in deadColumnsKeys) {
                if (dropColumns) {
                    statements.add(removeColumn(table, oldColumns[key]!!))
                }
            }
            for (key in sameColumnsKeys) {
                val mine = myColumns[key]!!
                val other = oldColumns[key]!!
                if (mine != other) {
                    throw IllegalStateException("Column $key is trying to change types; this is not currently supported.\n$other\nmigrate to\n$mine")
                }
            }
        }

        run {
            val myConstraints = table.constraints.associate { it.name to it }
            val oldConstraints = old.constraints.associate { it.name to it }

            val newConstraintKeys = myConstraints.keys - oldConstraints.keys
            val deadConstraintKeys = oldConstraints.keys - myConstraints.keys
            val sameConstraintKeys = myConstraints.keys intersect oldConstraints.keys

            for (key in newConstraintKeys) {
                statements.add(addConstraint(table, myConstraints[key]!!))
            }
            for (key in deadConstraintKeys) {
                if (dropColumns) {
                    statements.add(removeConstraint(table, oldConstraints[key]!!))
                }
            }
            for (key in sameConstraintKeys) {
                val mine = myConstraints[key]!!
                val other = oldConstraints[key]!!
                if (mine != other) {
                    if (dropColumns) {
                        statements.add(removeConstraint(table, other))
                        statements.add(addConstraint(table, mine))
                    } else {
                        throw IllegalStateException("Constraint $key is trying to change a constraint; this is not currently supported.\n$other\nmigrate to\n$mine")
                    }
                }
            }
        }

        run {
            val myIndexes = table.indexes.associate { it.name to it }
            val oldIndexes = old.indexes.associate { it.name to it }

            val newIndexKeys = myIndexes.keys - oldIndexes.keys
            val deadIndexKeys = oldIndexes.keys - myIndexes.keys
            val sameIndexKeys = myIndexes.keys intersect oldIndexes.keys

            for (key in newIndexKeys) {
                statements.add(createIndex(table, myIndexes[key]!!))
            }
            for (key in deadIndexKeys) {
                if (!old.isBuiltInIndex(oldIndexes[key]!!) && dropIndicies) {
                    statements.add(dropIndex(myIndexes[key]!!))
                }
            }
            for (key in sameIndexKeys) {
                val mine = myIndexes[key]!!
                val other = oldIndexes[key]!!
                if (mine != other) {
                    throw IllegalStateException("Index $key is trying to change an index; this is not currently supported.\n$other\nmigrate to\n$mine")
                }
            }
        }

        return statements
    }

    open fun SQLQuery.Builder.operation(operation: Operation<*>, type: Type<*>) {
        val subDef = serializer.rawDefine(type)
        when (operation) {
            is Operation.Set -> {
                if (subDef.columns.size == 1) {
                    sql.append(field)
                    sql.append(" = ")
                    serializer.encode(this, operation.value, type as Type<Any?>)
                } else {
                    //Convert to a useable operation
                    val setTo = operation.value
                    if (setTo == null) {
                        sql.append(field nameAppend "_isNull")
                        sql.append(" = ")
                        serializer.encode(this, true, Boolean::class.type)
                    } else {
                        val classInfo = serializer.registry.classInfoRegistry[type.param(0).type.kClass]!! as ClassInfo<Any>
                        @Suppress("UNCHECKED_CAST") val converted = Operation.Multiple<Any>(
                                classInfo.fields.map { Operation.SetField(it as FieldInfo<Any, Any?>, it.get(operation.value!!)) }
                        )
                        operation(converted, type)
                    }
                }
            }
            is Operation.AddNumeric -> {
                sql.append(field)
                sql.append(" = ")
                sql.append(field)
                sql.append(" + ")
                serializer.encode(this, operation.amount, type as Type<Any?>)
            }
            is Operation.Append -> {
                sql.append(field)
                sql.append(" = ")
                sql.append(field)
                sql.append(" || ")
                serializer.encode(this, operation.string, type as Type<Any?>)
            }
            is Operation.Field<*, *> -> {
                withConditionFieldName(operation.field.name) {
                    operation(operation.operation, operation.field.type)
                }
            }
            is Operation.Multiple -> {
                operation.operations.forEachIndexed { index, op ->
                    operation(op, type)
                    if (index != operation.operations.lastIndex) {
                        sql.append(", ")
                    }
                }
            }
            else -> throw UnsupportedOperationException()
        }
    }

    open fun SQLQuery.Builder.condition(condition: Condition<*>, type: Type<*>) {
        when (condition) {
            is Condition.Never -> {
                sql.append("FALSE")
            }
            is Condition.Always -> {
                sql.append("TRUE")
            }
            is Condition.And -> {
                sql.append("(")
                condition.conditions.forEachIndexed { index, sub ->
                    condition(sub, type)
                    if (index != condition.conditions.lastIndex) {
                        sql.append(" AND ")
                    }
                }
                sql.append(")")
            }
            is Condition.Or -> {
                sql.append("(")
                condition.conditions.forEachIndexed { index, sub ->
                    condition(sub, type)
                    if (index != condition.conditions.lastIndex) {
                        sql.append(" OR ")
                    }
                }
                sql.append(")")
            }
            is Condition.Not -> {
                sql.append("NOT (")
                condition(condition.condition, type)
                sql.append(")")
            }
            is Condition.Field<*, *> -> {
                withConditionFieldName(condition.field.name) {
                    condition(condition.condition, type)
                }
            }
            is Condition.Equal -> {
                val subDef = serializer.rawDefine(type)

                if (subDef.columns.size == 1) {
                    sql.append(field)
                    sql.append(" = ")
                    @Suppress("UNCHECKED_CAST")
                    serializer.encode(this, condition.value, type as Type<Any?>)
                } else {
                    @Suppress("UNCHECKED_CAST") val classInfo = serializer.registry.classInfoRegistry[type.param(0).type.kClass]!! as ClassInfo<Any>
                    val checkValue = condition.value
                    if (checkValue == null) {
                        sql.append(field nameAppend "_isNull")
                        sql.append(" = ")
                        serializer.encoder(Boolean::class.type).invoke(this, true)
                    } else {
                        @Suppress("UNCHECKED_CAST") val converted = Condition.And<Any>(
                                conditions = classInfo.fields.map { Condition.Field(it, Condition.Equal(it.get(checkValue))) }
                        )
                        condition(converted, type)
                    }
                }
            }
            is Condition.NotEqual -> {
                val subDef = serializer.rawDefine(type)

                if (subDef.columns.size == 1) {
                    sql.append(field)
                    sql.append(" <> ")
                    @Suppress("UNCHECKED_CAST")
                    serializer.encode(this, condition.value, type as Type<Any?>)
                } else {
                    @Suppress("UNCHECKED_CAST") val classInfo = serializer.registry.classInfoRegistry[type.param(0).type.kClass]!! as ClassInfo<Any>
                    val checkValue = condition.value
                    if (checkValue == null) {
                        sql.append(field nameAppend "_isNull")
                        sql.append(" = ")
                        serializer.encoder(Boolean::class.type).invoke(this, false)
                    } else {
                        @Suppress("UNCHECKED_CAST") val converted = Condition.And<Any>(
                                conditions = classInfo.fields.map { Condition.Field(it, Condition.Equal(it.get(checkValue))) }
                        )
                        condition(converted, type)
                    }
                }
            }
            is Condition.EqualToOne -> {
                val subDef = serializer.rawDefine(type)
                if (subDef.columns.size == 1) {
                    sql.append(field)
                    sql.append(" IN (")
                    condition.values.forEachIndexed { index, v ->
                        @Suppress("UNCHECKED_CAST")
                        serializer.encode(this, v, type as Type<Any?>)
                        if (index != condition.values.lastIndex) {
                            sql.append(", ")
                        }
                    }
                    sql.append(")")
                } else throw UnsupportedOperationException()
            }
            is Condition.LessThan -> {
                val subDef = serializer.rawDefine(type)

                if (subDef.columns.size == 1) {
                    sql.append(field)
                    sql.append(" < ")
                    @Suppress("UNCHECKED_CAST")
                    serializer.encode(this, condition.value, type as Type<Any?>)
                } else throw UnsupportedOperationException()
            }
            is Condition.GreaterThan -> {
                val subDef = serializer.rawDefine(type)

                if (subDef.columns.size == 1) {
                    sql.append(field)
                    sql.append(" > ")
                    @Suppress("UNCHECKED_CAST")
                    serializer.encode(this, condition.value, type as Type<Any?>)
                } else throw UnsupportedOperationException()
            }
            is Condition.LessThanOrEqual -> {
                val subDef = serializer.rawDefine(type)

                if (subDef.columns.size == 1) {
                    sql.append(field)
                    sql.append(" <= ")
                    @Suppress("UNCHECKED_CAST")
                    serializer.encode(this, condition.value, type as Type<Any?>)
                } else throw UnsupportedOperationException()
            }
            is Condition.GreaterThanOrEqual -> {
                val subDef = serializer.rawDefine(type)

                if (subDef.columns.size == 1) {
                    sql.append(field)
                    sql.append(" >= ")
                    @Suppress("UNCHECKED_CAST")
                    serializer.encode(this, condition.value, type as Type<Any?>)
                } else throw UnsupportedOperationException()
            }
            is Condition.TextSearch -> {
                sql.append(field)
                sql.append(" LIKE ")
                @Suppress("UNCHECKED_CAST")
                serializer.encode(this, "%" + condition.query + "%", String::class.type)
            }
            is Condition.StartsWith -> {
                sql.append(field)
                sql.append(" LIKE ")
                @Suppress("UNCHECKED_CAST")
                serializer.encode(this, condition.query + "%", String::class.type)
            }
            is Condition.EndsWith -> {
                sql.append(field)
                sql.append(" LIKE ")
                @Suppress("UNCHECKED_CAST")
                serializer.encode(this, "%" + condition.query, String::class.type)
            }
            else -> throw UnsupportedOperationException()
        }
    }

    open fun Sort<*>.toColumnList(table: Table): List<Column> = when (this) {
        is Sort.Field<*, *> -> listOf(table.columns.find { it.name == this.field.name }!!)
        is Sort.Multi -> this.comparators.flatMap { it.toColumnList(table) }
        else -> throw IllegalArgumentException()
    }

    inline fun reflectColumns(schema: String, table: String, execute: (SQLQuery) -> List<List<Any?>>): List<Column> {
        return execute(SQLQuery("""
        SELECT
        columns.column_name,
        columns.data_type,
        columns.character_maximum_length
        FROM information_schema.columns as columns
        WHERE columns.table_schema = '${schema.toLowerCase()}' AND columns.table_name = '${table.toLowerCase()}'
        """.trimIndent())).map {
            Column(
                    name = it[0] as String,
                    type = it[1] as String,
                    size = it[2] as Int
            )
        }
    }

    inline fun reflectConstraints(schema: String, table: String, execute: (SQLQuery) -> List<List<Any?>>): List<Constraint> {
        return execute(SQLQuery("""
        SELECT
        usage.constraint_name,
        usage.column_name,
        constraints.constraint_type,
        target.table_schema as target_schema,
        target.table_name as target_table,
        target.column_name as target_column
        FROM information_schema.constraint_column_usage as usage
        LEFT JOIN information_schema.table_constraints as constraints ON constraints.constraint_name = usage.constraint_name
        LEFT JOIN information_schema.constraint_column_usage as target ON target.constraint_name = constraints.constraint_name
        WHERE usage.table_schema = '${schema.toLowerCase()}' AND usage.table_name = '${table.toLowerCase()}';
        """.trimIndent()))
                .asSequence()
                .mapNotNull {
                    Constraint(
                            name = it[0] as String,
                            type = (it[2] as String).let { type ->
                                when (type) {
                                    "PRIMARY KEY" -> Constraint.Type.PrimaryKey
                                    "FOREIGN KEY" -> Constraint.Type.ForeignKey(
                                            otherSchema = it[3] as String,
                                            otherTable = it[4] as String,
                                            otherColumns = listOf(it[5] as String)
                                    )
                                    "UNIQUE" -> Constraint.Type.Unique
                                    else -> return@mapNotNull null
                                }
                            },
                            columns = listOf(it[1] as String)
                    )
                }
                .groupBy { it.name }
                .values
                .map {
                    val first = it.first()
                    val firstType = first.type
                    first.copy(
                            columns = it.map { it.columns.first() },
                            type = when(firstType) {
                                Constraint.Type.PrimaryKey -> Constraint.Type.PrimaryKey
                                Constraint.Type.Unique -> Constraint.Type.Unique
                                is Constraint.Type.ForeignKey -> firstType.copy(
                                        otherColumns = it.map { it.type.let{ it as Constraint.Type.ForeignKey }.otherColumns.first() }
                                )
                            }
                    )
                }
    }

    /*
        val indexes = client.suspendQuery("""
        SELECT indexdef
        FROM pg_indexes
        WHERE pg_indexes.schemaname = '${schema.toLowerCase()}' AND pg_indexes.tablename = '${table.toLowerCase()}'
    """.trimIndent())
                .mapNotNull { Index.parse(it.getString(0)) }

     */
}