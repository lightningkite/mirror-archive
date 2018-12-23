package com.lightningkite.mirror.archive.postgres

import com.lightningkite.mirror.archive.model.Condition

fun <T> Condition<T>.sql(serializer: PostgresSerializer, fields: List<String>): String {
    val field = if(fields.isEmpty()) "value" else fields.joinToString(".")
    return when(this){
        is Condition.Never -> "FALSE"
        is Condition.Always -> "TRUE"
        is Condition.And -> this.conditions.joinToString(" AND ", "(", ")"){ it.sql<T>(serializer, fields) }
        is Condition.Or -> this.conditions.joinToString(" OR ", "(", ")"){ it.sql<T>(serializer, fields) }
        is Condition.Not -> "NOT (${condition.sql(serializer, fields)})"
        is Condition.Field<*, *> -> this.condition.sql(serializer, fields + this.field.name)
        is Condition.Equal -> "$field = ${value.sqlLiteral(serializer)}"
        is Condition.EqualToOne -> "$field IN (${values.joinToString(", "){it.sqlLiteral(serializer)}}"
        is Condition.NotEqual -> "$field <> ${value.sqlLiteral(serializer)}"
        is Condition.LessThan -> "$field < ${value.sqlLiteral(serializer)}"
        is Condition.GreaterThan -> "$field > ${value.sqlLiteral(serializer)}"
        is Condition.LessThanOrEqual -> "$field <= ${value.sqlLiteral(serializer)}"
        is Condition.GreaterThanOrEqual -> "$field >= ${value.sqlLiteral(serializer)}"
        is Condition.TextSearch -> "$field LIKE ${("%$query%").sqlLiteral(serializer)}"
        is Condition.RegexTextSearch -> "$field SIMILAR TO ${query.sqlLiteral(serializer)}"
    }
}
